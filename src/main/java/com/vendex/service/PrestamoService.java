package com.vendex.service;

import com.vendex.dao.EntidadFinancieraDAO;
import com.vendex.dao.EntidadFinancieraDAOPostgres;
import com.vendex.dao.PrestamoDAO;
import com.vendex.dao.PrestamoDAOPostgres;
import com.vendex.dao.VentaResumenDAO;
import com.vendex.dao.VentaResumenDAOPostgres;
import com.vendex.model.EntidadFinanciera;
import com.vendex.model.Prestamo;
import com.vendex.model.PrestamoCuota;
import com.vendex.model.PrestamoPago;
import com.vendex.model.VentaResumen;
import com.vendex.util.SesionActual;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de negocio del módulo de préstamos financieros.
 * Amortización francesa con tasa directa por período (mensual o semanal),
 * abonos con distribución interés→capital y verificación de metas de venta.
 */
public class PrestamoService {

    private static final int ESCALA = 2;
    private static final MathContext MC = new MathContext(24, RoundingMode.HALF_UP);

    private final PrestamoDAO prestamoDAO;
    private final EntidadFinancieraDAO entidadFinancieraDAO;
    private final VentaResumenDAO ventaResumenDAO;

    public PrestamoService() {
        this(new PrestamoDAOPostgres(), new EntidadFinancieraDAOPostgres(), new VentaResumenDAOPostgres());
    }

    public PrestamoService(PrestamoDAO prestamoDAO, EntidadFinancieraDAO entidadFinancieraDAO, VentaResumenDAO ventaResumenDAO) {
        this.prestamoDAO = prestamoDAO;
        this.entidadFinancieraDAO = entidadFinancieraDAO;
        this.ventaResumenDAO = ventaResumenDAO;
    }

    // ==================== Entidades financieras ====================

    public List<EntidadFinanciera> listarEntidades(boolean soloActivas) {
        return entidadFinancieraDAO.listar(soloActivas);
    }

    public int guardarEntidad(EntidadFinanciera e) {
        SesionActual.exigirPermiso("PRESTAMO_GESTIONAR");
        if (e.getNombre() == null || e.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la entidad es obligatorio");
        }
        e.setNombre(e.getNombre().trim());
        if (e.getId() > 0) {
            entidadFinancieraDAO.actualizar(e);
            return e.getId();
        }
        if (!e.isEstado()) e.setEstado(true);
        return entidadFinancieraDAO.insertar(e);
    }

    public void desactivarEntidad(int id) {
        SesionActual.exigirPermiso("PRESTAMO_GESTIONAR");
        entidadFinancieraDAO.desactivar(id);
    }

    // ==================== Creación de préstamo ====================

    /**
     * Cuota fija francesa: cuota = monto × i / (1 − (1+i)^−n). Si i=0 → monto/n.
     */
    public static BigDecimal calcularCuotaFrancesa(BigDecimal monto, BigDecimal tasaPeriodoPct, int numeroCuotas) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El monto debe ser mayor a cero");
        if (numeroCuotas <= 0) throw new IllegalArgumentException("El número de cuotas debe ser mayor a cero");
        BigDecimal i = tasaPeriodoPct != null ? tasaPeriodoPct.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (i.compareTo(BigDecimal.ZERO) == 0) {
            return monto.divide(BigDecimal.valueOf(numeroCuotas), ESCALA, RoundingMode.HALF_UP);
        }
        BigDecimal unoMasI = BigDecimal.ONE.add(i);
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(unoMasI.pow(numeroCuotas, MC), MC), MC);
        BigDecimal cuota = monto.multiply(i, MC).divide(factor, MC);
        return cuota.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Genera la tabla de amortización francesa completa.
     * La última cuota absorbe el redondeo para que la suma cierre exacta.
     */
    public static List<PrestamoCuota> generarTablaCuotas(int prestamoId, BigDecimal monto, BigDecimal tasaPeriodoPct,
                                                         int numeroCuotas, String periodicidad, LocalDate fechaInicio) {
        BigDecimal i = tasaPeriodoPct != null ? tasaPeriodoPct.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal cuota = calcularCuotaFrancesa(monto, tasaPeriodoPct, numeroCuotas);
        List<PrestamoCuota> cuotas = new ArrayList<>();
        BigDecimal saldo = monto.setScale(ESCALA, RoundingMode.HALF_UP);
        for (int n = 1; n <= numeroCuotas; n++) {
            BigDecimal interes = saldo.multiply(i, MC).setScale(ESCALA, RoundingMode.HALF_UP);
            BigDecimal capital;
            BigDecimal montoCuota;
            if (n == numeroCuotas) {
                // Última cuota: paga todo el saldo de capital restante + su interés (absorbe redondeos)
                capital = saldo;
                montoCuota = capital.add(interes);
                saldo = BigDecimal.ZERO.setScale(ESCALA, RoundingMode.HALF_UP);
            } else {
                capital = cuota.subtract(interes);
                saldo = saldo.subtract(capital).setScale(ESCALA, RoundingMode.HALF_UP);
                montoCuota = cuota;
            }
            LocalDate venc;
            if ("S".equalsIgnoreCase(periodicidad)) {
                venc = fechaInicio.plusDays(7L * n);
            } else {
                venc = fechaInicio.plusMonths(n);
            }
            PrestamoCuota c = new PrestamoCuota(prestamoId, n, venc, montoCuota, capital, interes,
                    saldo.add(capital), saldo);
            cuotas.add(c);
        }
        return cuotas;
    }

    /**
     * Crea el préstamo con su tabla de cuotas. Devuelve el préstamo persistido (con id).
     */
    public Prestamo crearPrestamo(int entidadId, BigDecimal monto, BigDecimal tasaPeriodoPct,
                                  String periodicidad, int numeroCuotas, LocalDate fechaInicio,
                                  String metaPeriodicidad, BigDecimal metaMonto, String notas) {
        SesionActual.exigirPermiso("PRESTAMO_GESTIONAR");
        if (entidadId <= 0) throw new IllegalArgumentException("Seleccione una entidad financiera");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El monto debe ser mayor a cero");
        if (tasaPeriodoPct == null || tasaPeriodoPct.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("La tasa del período no puede ser negativa");
        if (periodicidad == null || (!"S".equalsIgnoreCase(periodicidad) && !"M".equalsIgnoreCase(periodicidad))) {
            throw new IllegalArgumentException("La periodicidad debe ser SEMANAL o MENSUAL");
        }
        if (numeroCuotas <= 0) throw new IllegalArgumentException("El número de cuotas debe ser mayor a cero");
        if (fechaInicio == null) fechaInicio = LocalDate.now();

        BigDecimal cuota = calcularCuotaFrancesa(monto, tasaPeriodoPct, numeroCuotas);
        // totalAPagar exacto = suma de la tabla (la última cuota absorbe redondeos)
        List<PrestamoCuota> tablaTmp = generarTablaCuotas(0, monto, tasaPeriodoPct, numeroCuotas, periodicidad, fechaInicio);
        BigDecimal sumaTabla = BigDecimal.ZERO;
        for (PrestamoCuota c : tablaTmp) sumaTabla = sumaTabla.add(c.getMonto());
        BigDecimal totalAPagar = sumaTabla.setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal totalInteres = totalAPagar.subtract(monto).setScale(ESCALA, RoundingMode.HALF_UP);

        Prestamo p = new Prestamo(entidadId, monto.setScale(ESCALA, RoundingMode.HALF_UP), tasaPeriodoPct,
                periodicidad.toUpperCase(), numeroCuotas, fechaInicio);
        p.setCuotaMonto(cuota);
        p.setTotalAPagar(totalAPagar);
        p.setTotalInteres(totalInteres);
        p.setEstado("ACTIVO");
        p.setMetaPeriodicidad(metaPeriodicidad);
        p.setMetaMonto(metaMonto);
        p.setNotas(notas);

        int id = prestamoDAO.insertar(p);
        if (id <= 0) throw new IllegalStateException("No se pudo registrar el préstamo");
        p.setId(id);
        List<PrestamoCuota> cuotas = generarTablaCuotas(id, monto, tasaPeriodoPct, numeroCuotas, periodicidad, fechaInicio);
        prestamoDAO.insertarCuotas(cuotas);
        return p;
    }

    /**
     * Meta automática sugerida: cuota / períodos equivalentes del ciclo de la meta.
     */
    public static BigDecimal calcularMetaAutomatica(BigDecimal cuota, String periodicidadPrestamo, String metaPeriodicidad) {
        if (cuota == null) return BigDecimal.ZERO;
        boolean prestamoSemanal = "S".equalsIgnoreCase(periodicidadPrestamo);
        if ("DIARIA".equalsIgnoreCase(metaPeriodicidad)) {
            // semanal/7 o mensual/30
            int dias = prestamoSemanal ? 7 : 30;
            return cuota.divide(BigDecimal.valueOf(dias), ESCALA, RoundingMode.HALF_UP);
        }
        if ("SEMANAL".equalsIgnoreCase(metaPeriodicidad)) {
            if (prestamoSemanal) return cuota.setScale(ESCALA, RoundingMode.HALF_UP);
            // mensual ≈ 4.2857 semanas
            return cuota.divide(BigDecimal.valueOf(30).divide(BigDecimal.valueOf(7), MC), ESCALA, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    // ==================== Consultas ====================

    public List<Object[]> listarPrestamos() {
        return prestamoDAO.listarActivos();
    }

    public Prestamo obtenerPrestamo(int id) {
        return prestamoDAO.obtenerPorId(id);
    }

    public List<PrestamoCuota> listarCuotas(int prestamoId) {
        return prestamoDAO.listarCuotas(prestamoId);
    }

    public List<PrestamoPago> listarPagos(int prestamoId) {
        return prestamoDAO.listarPagos(prestamoId);
    }

    /** @return saldo pendiente total del préstamo */
    public BigDecimal saldoPendiente(int prestamoId) {
        BigDecimal saldo = BigDecimal.ZERO;
        for (PrestamoCuota c : prestamoDAO.listarCuotas(prestamoId)) {
            if (!c.estaPagada()) saldo = saldo.add(c.getFaltante());
        }
        return saldo.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** Próxima cuota no pagada (vencida primero si la hay). */
    public PrestamoCuota proximaCuota(int prestamoId) {
        PrestamoCuota vencida = null;
        PrestamoCuota proxima = null;
        for (PrestamoCuota c : prestamoDAO.listarCuotas(prestamoId)) {
            if (c.estaPagada()) continue;
            if (c.getFechaVencimiento() != null && c.getFechaVencimiento().isBefore(LocalDate.now())) {
                if (vencida == null || c.getFechaVencimiento().isBefore(vencida.getFechaVencimiento())) vencida = c;
            } else if (proxima == null || c.getFechaVencimiento().isBefore(proxima.getFechaVencimiento())) {
                proxima = c;
            }
        }
        return vencida != null ? vencida : proxima;
    }

    public void actualizarMeta(int prestamoId, String metaPeriodicidad, BigDecimal metaMonto) {
        SesionActual.exigirPermiso("PRESTAMO_GESTIONAR");
        prestamoDAO.actualizarMeta(prestamoId, metaPeriodicidad, metaMonto);
    }

    public void cancelarPrestamo(int prestamoId, String motivo) {
        SesionActual.exigirPermiso("PRESTAMO_GESTIONAR");
        prestamoDAO.actualizarEstado(prestamoId, "CANCELADO");
    }

    // ==================== Abonos ====================

    /**
     * Registra un abono. Valida permiso y delega en el DAO transaccional.
     * @return monto efectivamente aplicado
     */
    public BigDecimal registrarAbono(int prestamoId, BigDecimal monto, Integer cuotaIdPreferida,
                                     String fuente, Integer referenciaId, String notas) {
        SesionActual.exigirPermiso("PRESTAMO_ABONAR");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a cero");
        }
        Prestamo p = prestamoDAO.obtenerPorId(prestamoId);
        if (p == null) throw new IllegalArgumentException("Préstamo no encontrado");
        if (!"ACTIVO".equals(p.getEstado())) throw new IllegalStateException("El préstamo no está activo");
        var usuario = SesionActual.getUsuario();
        Integer usuarioId = usuario != null ? usuario.getId() : null;
        return prestamoDAO.registrarAbono(prestamoId, monto, cuotaIdPreferida, fuente, referenciaId, usuarioId, notas);
    }

    /**
     * Sugerencia de abono: cuotas pendientes ordenadas (vencidas primero, luego por fecha),
     * acumulando faltantes hasta agotar el monto disponible.
     * @return filas [cuotaId, prestamoId, entidad, numero, fechaVencimiento, monto, montoPagado, falta, estado, periodicidad, cuotaMonto]
     */
    public List<Object[]> sugerirAbonos(BigDecimal montoDisponible) {
        List<Object[]> pendientes = prestamoDAO.listarCuotasPendientes();
        List<Object[]> sugeridas = new ArrayList<>();
        if (montoDisponible == null || montoDisponible.compareTo(BigDecimal.ZERO) <= 0) return sugeridas;
        BigDecimal restante = montoDisponible;
        for (Object[] fila : pendientes) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal falta = (BigDecimal) fila[7];
            if (falta == null || falta.compareTo(BigDecimal.ZERO) <= 0) continue;
            sugeridas.add(fila);
            restante = restante.subtract(falta);
        }
        return sugeridas;
    }

    public List<Object[]> listarCuotasPendientes() {
        return prestamoDAO.listarCuotasPendientes();
    }

    // ==================== Metas de venta ====================

    /**
     * Rango de fechas del período anterior COMPLETO según la meta.
     * DIARIA → ayer; SEMANAL → últimos 7 días terminando ayer.
     */
    public static LocalDate[] rangoPeriodoAnterior(String metaPeriodicidad, LocalDate hoy) {
        if ("SEMANAL".equalsIgnoreCase(metaPeriodicidad)) {
            LocalDate hasta = hoy.minusDays(1);
            LocalDate desde = hasta.minusDays(6);
            return new LocalDate[]{desde, hasta};
        }
        // DIARIA por defecto
        LocalDate ayer = hoy.minusDays(1);
        return new LocalDate[]{ayer, ayer};
    }

    /**
     * Verifica el cumplimiento de la meta de ventas de un préstamo en un rango.
     * @return [cumplio (Boolean), totalVentas (BigDecimal), meta (BigDecimal), faltante (BigDecimal)]
     */
    public Object[] verificarMeta(Prestamo p, LocalDate desde, LocalDate hasta) {
        BigDecimal meta = p.getMetaMonto() != null ? p.getMetaMonto() : BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        try {
            List<VentaResumen> ventas = ventaResumenDAO.listarPorRango(desde, hasta);
            for (VentaResumen v : ventas) {
                if (v.getTotal() != null) total = total.add(v.getTotal());
            }
        } catch (Exception ignore) {
            // sin datos de ventas
        }
        total = total.setScale(ESCALA, RoundingMode.HALF_UP);
        boolean cumplio = total.compareTo(meta) >= 0;
        BigDecimal faltante = cumplio ? BigDecimal.ZERO.setScale(ESCALA, RoundingMode.HALF_UP)
                : meta.subtract(total).setScale(ESCALA, RoundingMode.HALF_UP);
        return new Object[]{cumplio, total, meta, faltante};
    }

    public List<Prestamo> listarPrestamosConMeta() {
        return prestamoDAO.listarPrestamosConMeta();
    }
}

package com.vendex.service;

import com.vendex.config.VendexiConfig;
import com.vendex.dao.VendexiConsumoDAO;
import com.vendex.model.VendexiConsumo;
import com.vendex.util.LicenseManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fase 3 Vendexi AI — medición de tokens/costos y aplicación de la capacidad
 * asignada a la licencia (spec §10).
 *
 * - La capacidad de la licencia v2 se expresa en TOKENS TOTALES (entrada+salida)
 *   del periodo vigente. capacidadIA = 0 → sin límite definido (no se corta).
 * - Los minutos se registran por separado (sesiones, §12) y nunca limitan.
 * - Al agotarse la capacidad se detienen nuevas operaciones de IA; el ERP
 *   continúa operativo (§10.2).
 * - Un fallo de medición NUNCA debe bloquear el ERP: todo es best-effort.
 */
public class VendexiConsumoService {

    private static final Logger LOGGER = Logger.getLogger(VendexiConsumoService.class.getName());

    private final VendexiConsumoDAO dao;
    private final java.util.function.LongSupplier capacidadFuente;

    public VendexiConsumoService(VendexiConsumoDAO dao) {
        this(dao, null);
    }

    /** Permite inyectar la fuente de capacidad en tests (por defecto: licencia). */
    public VendexiConsumoService(VendexiConsumoDAO dao, java.util.function.LongSupplier capacidadFuente) {
        this.dao = dao;
        this.capacidadFuente = capacidadFuente;
    }

    public static final class EstadoCapacidad {
        public final long capacidad;      // 0 = sin límite definido
        public final long consumido;      // tokens en el periodo
        public final BigDecimal costoPeriodo;
        public final LocalDateTime inicioPeriodo;

        public EstadoCapacidad(long capacidad, long consumido, BigDecimal costoPeriodo, LocalDateTime inicioPeriodo) {
            this.capacidad = capacidad;
            this.consumido = consumido;
            this.costoPeriodo = costoPeriodo;
            this.inicioPeriodo = inicioPeriodo;
        }

        public boolean agotada() {
            return capacidad > 0 && consumido >= capacidad;
        }

        public long restante() {
            if (capacidad <= 0) return -1; // sin límite
            return Math.max(0, capacidad - consumido);
        }
    }

    /** Capacidad de la licencia almacenada; 0 si no hay licencia v2 (sin límite definido). */
    public long capacidadLicencia() {
        if (capacidadFuente != null) {
            try {
                return Math.max(0, capacidadFuente.getAsLong());
            } catch (Exception e) {
                return 0;
            }
        }
        try {
            LicenseManager.LicenseInfo info = LicenseManager.getLicenseInfo();
            if (info != null && info.valida && info.version2 && info.incluyeIA) {
                return info.capacidadIA;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Sin info de licencia para capacidad", e);
        }
        return 0;
    }

    /** Capacidad restante del periodo; -1 = sin límite. */
    public long capacidadRestante() {
        return estado().restante();
    }

    public EstadoCapacidad estado() {
        long cap = capacidadLicencia();
        long consumido = 0;
        BigDecimal costo = BigDecimal.ZERO;
        LocalDateTime inicio = null;
        try {
            inicio = dao.obtenerInicioPeriodo();
            if (inicio == null) inicio = LocalDateTime.of(2000, 1, 1, 0, 0);
            consumido = dao.sumarTokensDesde(inicio);
            costo = dao.sumarCostoDesde(inicio);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Medición no disponible; se asume sin límite", e);
        }
        return new EstadoCapacidad(cap, consumido, costo, inicio);
    }

    /** true si se puede realizar una operación de IA (licencia con capacidad disponible). */
    public boolean hayCapacidadDisponible() {
        try {
            return !estado().agotada();
        } catch (Exception e) {
            return true; // la medición nunca debe bloquear el ERP
        }
    }

    /** Registra tokens de una operación de IA (best-effort). Costo estimado con VendexiConfig. */
    public void registrarTokens(String modulo, String operacion, String modelo,
                                long tokensEntrada, long tokensSalida) {
        try {
            VendexiConsumo c = new VendexiConsumo();
            c.setFecha(LocalDateTime.now());
            c.setModulo(modulo != null ? modulo : "desconocido");
            c.setOperacion(operacion != null ? operacion : "ia");
            c.setModelo(modelo);
            c.setTokensEntrada(Math.max(0, tokensEntrada));
            c.setTokensSalida(Math.max(0, tokensSalida));
            c.setCostoEstimado(VendexiConfig.estimarCosto(c.getTokensEntrada(), c.getTokensSalida()));
            dao.insertar(c);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo registrar consumo (best-effort)", e);
        }
    }

    /** Inicia una sesión de interacción (estadística, no es un límite). */
    public long iniciarSesion(String modulo, String tipo) {
        try {
            return dao.iniciarSesion(modulo != null ? modulo : "general",
                    tipo != null ? tipo : "CONSULTA");
        } catch (Exception e) {
            return -1;
        }
    }

    public void finalizarSesion(long idSesion) {
        if (idSesion <= 0) return;
        try {
            dao.finalizarSesion(idSesion);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudo finalizar sesión", e);
        }
    }

    /** Renueva el inicio del periodo de capacidad (acción administrativa). */
    public void renovarPeriodo() {
        dao.actualizarInicioPeriodo(LocalDateTime.now());
    }

    public List<VendexiConsumo> listarReciente(int limite) {
        try {
            return dao.listarReciente(limite);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Mensaje de referencia spec §10.2 al agotarse la capacidad. */
    public static String mensajeCapacidadAgotada() {
        return "La capacidad de IA asignada a esta licencia se ha agotado. "
                + "Las funciones habituales de Vendex que no dependen de IA siguen disponibles. "
                + "Para continuar con las tareas inteligentes, el administrador de Vendex "
                + "debe habilitar capacidad adicional.";
    }
}

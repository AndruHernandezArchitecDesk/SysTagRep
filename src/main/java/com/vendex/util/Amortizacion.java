package com.vendex.util;

import com.vendex.model.PrestamoCuota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica pura de amortización francesa y aplicación de pagos a cuotas.
 * Sin dependencia de BD: usada por PrestamoDAOPostgres y testeable unitariamente.
 */
public final class Amortizacion {

    private Amortizacion() {}

    /** Aplicación parcial de un pago a una cuota específica. */
    public static final class Aplicacion {
        public final int cuotaId;
        public final int cuotaNumero;
        public final BigDecimal monto;
        public final BigDecimal interes;
        public final BigDecimal capital;

        public Aplicacion(int cuotaId, int cuotaNumero, BigDecimal monto, BigDecimal interes, BigDecimal capital) {
            this.cuotaId = cuotaId;
            this.cuotaNumero = cuotaNumero;
            this.monto = monto;
            this.interes = interes;
            this.capital = capital;
        }
    }

    /** Resultado de aplicar un pago sobre una lista de cuotas. */
    public static final class Resultado {
        public final BigDecimal totalInteres;
        public final BigDecimal totalCapital;
        public final List<Aplicacion> aplicaciones;

        public Resultado(BigDecimal totalInteres, BigDecimal totalCapital, List<Aplicacion> aplicaciones) {
            this.totalInteres = totalInteres;
            this.totalCapital = totalCapital;
            this.aplicaciones = aplicaciones;
        }

        public BigDecimal totalAplicado() {
            BigDecimal t = BigDecimal.ZERO;
            for (Aplicacion a : aplicaciones) t = t.add(a.monto);
            return t;
        }
    }

    /**
     * Aplica un pago a las cuotas en orden cronológico (numero ASC), empezando por
     * {@code desdeNumero}. En cada cuota: primero interés pendiente, luego capital.
     * Muta las cuotas (montoPagado, interesPagado, saldoRestante, estado, fechaPago).
     * @param cuotas cuotas del préstamo (se re-ordenan por numero)
     * @param monto monto a aplicar
     * @param desdeNumero número de cuota desde la cual empezar (1 = desde la primera)
     */
    public static Resultado aplicarPago(List<PrestamoCuota> cuotas, BigDecimal monto, int desdeNumero) {
        List<Aplicacion> aplicaciones = new ArrayList<>();
        BigDecimal interesTotal = BigDecimal.ZERO;
        BigDecimal capitalTotal = BigDecimal.ZERO;
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0 || cuotas == null || cuotas.isEmpty()) {
            return new Resultado(BigDecimal.ZERO, BigDecimal.ZERO, aplicaciones);
        }
        List<PrestamoCuota> ordenadas = new ArrayList<>(cuotas);
        ordenadas.sort((a, b) -> Integer.compare(a.getNumero(), b.getNumero()));
        BigDecimal restante = monto;
        LocalDate hoy = LocalDate.now();
        for (PrestamoCuota c : ordenadas) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
            if (c.getNumero() < desdeNumero || c.estaPagada()) continue;
            BigDecimal faltante = c.getFaltante();
            if (faltante.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal aplicar = faltante.min(restante);
            BigDecimal interesDeuda = c.getInteres()
                    .subtract(c.getInteresPagado() != null ? c.getInteresPagado() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO)
                    .min(aplicar);
            BigDecimal capitalCobrar = aplicar.subtract(interesDeuda);
            BigDecimal nuevoPagado = (c.getMontoPagado() != null ? c.getMontoPagado() : BigDecimal.ZERO).add(aplicar);
            BigDecimal nuevoInteres = (c.getInteresPagado() != null ? c.getInteresPagado() : BigDecimal.ZERO).add(interesDeuda);
            BigDecimal nuevoSaldo = c.getSaldoAnterior().subtract(capitalCobrar).max(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            c.setMontoPagado(nuevoPagado);
            c.setInteresPagado(nuevoInteres);
            c.setSaldoRestante(nuevoSaldo);
            if (nuevoPagado.compareTo(c.getMonto()) >= 0) {
                c.setEstado("PAGADA");
                c.setFechaPago(hoy);
            } else {
                c.setEstado("PARCIAL");
            }
            aplicaciones.add(new Aplicacion(c.getId(), c.getNumero(), aplicar, interesDeuda, capitalCobrar));
            interesTotal = interesTotal.add(interesDeuda);
            capitalTotal = capitalTotal.add(capitalCobrar);
            restante = restante.subtract(aplicar);
        }
        return new Resultado(interesTotal, capitalTotal, aplicaciones);
    }

    /** ¿Quedan cuotas con saldo faltante? */
    public static boolean quedanPendientes(List<PrestamoCuota> cuotas) {
        for (PrestamoCuota c : cuotas) {
            if (!c.estaPagada() && c.getFaltante().compareTo(BigDecimal.ZERO) > 0) return true;
        }
        return false;
    }
}

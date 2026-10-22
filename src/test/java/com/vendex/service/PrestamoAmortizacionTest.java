package com.vendex.service;

import com.vendex.model.PrestamoCuota;
import com.vendex.util.Amortizacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Amortización francesa con tasa directa por período:
 * cuota fija, interés decreciente, ajuste de la última cuota y
 * distribución de pagos (interés primero, luego capital).
 */
class PrestamoAmortizacionTest {

    private static final BigDecimal TASA_5 = new BigDecimal("5");

    @Test
    void cuotaFrancesa_tasaCero_esDivisionExacta() {
        BigDecimal cuota = PrestamoService.calcularCuotaFrancesa(new BigDecimal("1000"), BigDecimal.ZERO, 4);
        assertEquals(0, cuota.compareTo(new BigDecimal("250.00")));
    }

    @Test
    void cuotaFrancesa_tasaDirecta5pct_6cuotas() {
        // monto 1000, 5% mensual, 6 cuotas → cuota ≈ 197.02
        BigDecimal cuota = PrestamoService.calcularCuotaFrancesa(new BigDecimal("1000"), TASA_5, 6);
        assertEquals(0, cuota.compareTo(new BigDecimal("197.02")));
    }

    @Test
    void tabla_cuotaFijaYCapitalCreciente() {
        List<PrestamoCuota> tabla = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        assertEquals(6, tabla.size());
        BigDecimal cuotaEsperada = PrestamoService.calcularCuotaFrancesa(new BigDecimal("1000"), TASA_5, 6);
        // las 5 primeras cuotas son iguales
        for (int i = 0; i < 5; i++) {
            assertEquals(0, tabla.get(i).getMonto().compareTo(cuotaEsperada), "cuota " + (i + 1));
        }
        // interés decreciente
        assertTrue(tabla.get(0).getInteres().compareTo(tabla.get(1).getInteres()) > 0);
        assertTrue(tabla.get(1).getInteres().compareTo(tabla.get(2).getInteres()) > 0);
        // capital creciente
        assertTrue(tabla.get(0).getCapital().compareTo(tabla.get(1).getCapital()) < 0);
    }

    @Test
    void tabla_sumaDeCuotas_masCapitalCierraElPrincipal() {
        BigDecimal monto = new BigDecimal("1000");
        List<PrestamoCuota> tabla = PrestamoService.generarTablaCuotas(1, monto, TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        BigDecimal sumaCapital = BigDecimal.ZERO;
        BigDecimal sumaTotal = BigDecimal.ZERO;
        for (PrestamoCuota c : tabla) {
            sumaCapital = sumaCapital.add(c.getCapital());
            sumaTotal = sumaTotal.add(c.getMonto());
        }
        // la suma de capitales debe cerrar exactamente el principal
        assertEquals(0, sumaCapital.compareTo(monto), "capitales suman " + sumaCapital);
        // última cuota paga todo el saldo restante
        assertEquals(0, tabla.get(5).getSaldoRestante().compareTo(BigDecimal.ZERO));
        assertEquals(0, tabla.get(4).getSaldoRestante().compareTo(tabla.get(5).getCapital()));
    }

    @Test
    void tabla_tasaCero_totalIgualAlPrincipal() {
        List<PrestamoCuota> tabla = PrestamoService.generarTablaCuotas(1, new BigDecimal("800"),
                BigDecimal.ZERO, 4, "S", LocalDate.of(2026, 1, 1));
        BigDecimal suma = BigDecimal.ZERO;
        for (PrestamoCuota c : tabla) suma = suma.add(c.getMonto());
        assertEquals(0, suma.compareTo(new BigDecimal("800.00")));
        for (PrestamoCuota c : tabla) {
            assertEquals(0, c.getInteres().compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    void fechas_semanales_avanzanDeSieteEnSiete() {
        List<PrestamoCuota> tabla = PrestamoService.generarTablaCuotas(1, new BigDecimal("700"),
                BigDecimal.ZERO, 3, "S", LocalDate.of(2026, 1, 1));
        assertEquals(LocalDate.of(2026, 1, 8), tabla.get(0).getFechaVencimiento());
        assertEquals(LocalDate.of(2026, 1, 15), tabla.get(1).getFechaVencimiento());
        assertEquals(LocalDate.of(2026, 1, 22), tabla.get(2).getFechaVencimiento());
    }

    @Test
    void fechas_mensuales_avanzanDeMesEnMes() {
        List<PrestamoCuota> tabla = PrestamoService.generarTablaCuotas(1, new BigDecimal("300"),
                BigDecimal.ZERO, 3, "M", LocalDate.of(2026, 1, 15));
        assertEquals(LocalDate.of(2026, 2, 15), tabla.get(0).getFechaVencimiento());
        assertEquals(LocalDate.of(2026, 3, 15), tabla.get(1).getFechaVencimiento());
        assertEquals(LocalDate.of(2026, 4, 15), tabla.get(2).getFechaVencimiento());
    }

    @Test
    void aplicarPago_interesPrimeroLuegoCapital() {
        List<PrestamoCuota> cuotas = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        // darles ids
        for (PrestamoCuota c : cuotas) c.setId(c.getNumero() * 10);

        BigDecimal pago = new BigDecimal("197.02"); // una cuota completa
        Amortizacion.Resultado res = Amortizacion.aplicarPago(cuotas, pago, 1);

        assertEquals(1, res.aplicaciones.size());
        Amortizacion.Aplicacion a = res.aplicaciones.get(0);
        assertEquals(1, a.cuotaNumero);
        assertEquals(0, a.interes.compareTo(cuotas.get(0).getInteres()), "el interés de la cuota se cobra primero");
        assertEquals(0, a.capital.compareTo(cuotas.get(0).getCapital()));
        assertEquals(0, a.monto.compareTo(pago));
        assertEquals("PAGADA", cuotas.get(0).getEstado());
        assertNotNull(cuotas.get(0).getFechaPago());
    }

    @Test
    void aplicarPago_parcial_cubreInteresYElRestoEsCapital() {
        List<PrestamoCuota> cuotas = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        cuotas.get(0).setId(10);
        BigDecimal interesCuota1 = cuotas.get(0).getInteres(); // 50.00
        BigDecimal pagoParcial = interesCuota1.add(new BigDecimal("30")); // 80: 50 interés + 30 capital

        Amortizacion.Resultado res = Amortizacion.aplicarPago(cuotas, pagoParcial, 1);

        assertEquals(1, res.aplicaciones.size());
        assertEquals(0, res.totalInteres.compareTo(interesCuota1));
        assertEquals(0, res.totalCapital.compareTo(new BigDecimal("30.00")));
        assertEquals("PARCIAL", cuotas.get(0).getEstado());
        assertEquals(0, cuotas.get(0).getMontoPagado().compareTo(pagoParcial));
        // saldo restante = saldo anterior − capital pagado
        assertEquals(0, cuotas.get(0).getSaldoRestante().compareTo(
                cuotas.get(0).getSaldoAnterior().subtract(new BigDecimal("30.00"))));
    }

    @Test
    void aplicarPago_excesoCubreVariasCuotas_enOrdenCronologico() {
        List<PrestamoCuota> cuotas = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        for (PrestamoCuota c : cuotas) c.setId(c.getNumero() * 10);

        BigDecimal cuota1 = cuotas.get(0).getMonto();
        BigDecimal cuota2 = cuotas.get(1).getMonto();
        Amortizacion.Resultado res = Amortizacion.aplicarPago(cuotas, cuota1.add(cuota2), 1);

        assertEquals(2, res.aplicaciones.size());
        assertEquals(1, res.aplicaciones.get(0).cuotaNumero);
        assertEquals(2, res.aplicaciones.get(1).cuotaNumero);
        assertEquals("PAGADA", cuotas.get(0).getEstado());
        assertEquals("PAGADA", cuotas.get(1).getEstado());
        assertFalse(Amortizacion.quedanPendientes(cuotas.subList(0, 2)));
        assertTrue(Amortizacion.quedanPendientes(cuotas));
    }

    @Test
    void aplicarPago_desdeCuotaPreferida() {
        List<PrestamoCuota> cuotas = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        for (PrestamoCuota c : cuotas) c.setId(c.getNumero() * 10);

        Amortizacion.Resultado res = Amortizacion.aplicarPago(cuotas, cuotas.get(2).getMonto(), 3);
        assertEquals(1, res.aplicaciones.size());
        assertEquals(3, res.aplicaciones.get(0).cuotaNumero);
        assertTrue(cuotas.get(0).getMontoPagado().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void aplicarPago_montoCeroNoHaceNada() {
        List<PrestamoCuota> cuotas = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        Amortizacion.Resultado res = Amortizacion.aplicarPago(cuotas, BigDecimal.ZERO, 1);
        assertEquals(0, res.aplicaciones.size());
        assertEquals(0, res.totalAplicado().compareTo(BigDecimal.ZERO));
    }

    @Test
    void aplicarPago_pagoTotalCierraTodasLasCuotas() {
        List<PrestamoCuota> cuotas = PrestamoService.generarTablaCuotas(1, new BigDecimal("1000"),
                TASA_5, 6, "M", LocalDate.of(2026, 1, 1));
        for (PrestamoCuota c : cuotas) c.setId(c.getNumero() * 10);
        BigDecimal total = BigDecimal.ZERO;
        for (PrestamoCuota c : cuotas) total = total.add(c.getMonto());

        Amortizacion.Resultado res = Amortizacion.aplicarPago(cuotas, total, 1);
        assertEquals(0, res.totalAplicado().compareTo(total.setScale(2, RoundingMode.HALF_UP)));
        assertFalse(Amortizacion.quedanPendientes(cuotas));
        for (PrestamoCuota c : cuotas) assertEquals("PAGADA", c.getEstado());
    }
}

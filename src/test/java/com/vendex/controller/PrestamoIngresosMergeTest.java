package com.vendex.controller;

import com.vendex.model.CajaMovimiento;
import com.vendex.model.FacturaRegistro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrestamoIngresosMergeTest {

    private FacturaRegistro factura(String codigo, LocalDateTime fecha, String formaPago, String total) {
        FacturaRegistro f = new FacturaRegistro();
        f.setCodigo(codigo);
        f.setFecha(fecha);
        f.setFormaPago(formaPago);
        f.setTotal(new BigDecimal(total));
        return f;
    }

    private CajaMovimiento movimiento(LocalDateTime fecha, String descripcion, String monto) {
        CajaMovimiento m = new CajaMovimiento();
        m.setFecha(fecha);
        m.setDescripcion(descripcion);
        m.setTipo("INGRESO");
        m.setMonto(new BigDecimal(monto));
        return m;
    }

    @Test
    void mezclaVentasYCajaOrdenadasPorFechaDescendente() {
        List<Object[]> filas = PrestamoController.mezclarIngresos(
                List.of(factura("F1", LocalDateTime.of(2026, 10, 21, 9, 0), "Efectivo", "100.00")),
                List.of(movimiento(LocalDateTime.of(2026, 10, 21, 12, 0), "Venta mostrador", "50.00")),
                50);
        assertEquals(2, filas.size());
        assertEquals("CAJA", filas.get(0)[1]);
        assertEquals("VENTA", filas.get(1)[1]);
        assertEquals(LocalDateTime.of(2026, 10, 21, 12, 0), filas.get(0)[0]);
        assertEquals(new BigDecimal("50.00"), filas.get(0)[4]);
        assertEquals("F1", filas.get(1)[2]);
        assertEquals(new BigDecimal("100.00"), filas.get(1)[4]);
    }

    @Test
    void respetaElLimiteRecortandoLoMasReciente() {
        List<FacturaRegistro> facturas = new ArrayList<>();
        List<CajaMovimiento> movimientos = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            facturas.add(factura("F" + i, LocalDateTime.of(2026, 10, 1, 8, 0).plusMinutes(i), "Efectivo", "10.00"));
            movimientos.add(movimiento(LocalDateTime.of(2026, 10, 2, 8, 0).plusMinutes(i), "M" + i, "5.00"));
        }
        List<Object[]> filas = PrestamoController.mezclarIngresos(facturas, movimientos, 50);
        assertEquals(50, filas.size());
        assertEquals(LocalDateTime.of(2026, 10, 2, 8, 39), filas.get(0)[0]);
        assertEquals(LocalDateTime.of(2026, 10, 1, 8, 30), filas.get(49)[0]);
    }

    @Test
    void toleraListasNulas() {
        List<Object[]> soloCaja = PrestamoController.mezclarIngresos(
                null, List.of(movimiento(LocalDateTime.of(2026, 10, 21, 10, 0), "Depósito", "25.00")), 50);
        assertEquals(1, soloCaja.size());
        assertEquals("CAJA", soloCaja.get(0)[1]);

        List<Object[]> soloVentas = PrestamoController.mezclarIngresos(
                List.of(factura("F2", LocalDateTime.of(2026, 10, 21, 11, 0), "Transferencia", "75.00")), null, 50);
        assertEquals(1, soloVentas.size());
        assertEquals("VENTA", soloVentas.get(0)[1]);

        assertTrue(PrestamoController.mezclarIngresos(null, null, 50).isEmpty());
    }

    @Test
    void fechaNulaVaAlFinal() {
        List<Object[]> filas = PrestamoController.mezclarIngresos(
                List.of(factura("F-sin-fecha", null, "Efectivo", "10.00")),
                List.of(movimiento(LocalDateTime.of(2026, 10, 21, 10, 0), "Caja", "5.00")),
                50);
        assertEquals(2, filas.size());
        assertEquals("CAJA", filas.get(0)[1]);
        assertNull(filas.get(1)[0]);
        assertEquals("F-sin-fecha", filas.get(1)[2]);
    }
}

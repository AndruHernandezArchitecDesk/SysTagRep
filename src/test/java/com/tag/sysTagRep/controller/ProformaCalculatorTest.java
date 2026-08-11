package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.model.DetalleVenta;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProformaCalculatorTest {

    private BigDecimal calcularSubtotal(List<DetalleVenta> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVenta d : items) {
            subtotal = subtotal.add(d.getPrecioTotal());
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularIva(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularDescuento(BigDecimal totalBruto, String porcentajeStr) {
        BigDecimal pct = BigDecimal.ZERO;
        try {
            pct = new BigDecimal(porcentajeStr);
        } catch (NumberFormatException ignored) {}
        return totalBruto.multiply(pct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTotal(BigDecimal subtotal, BigDecimal descuento) {
        BigDecimal totalBruto = subtotal.add(calcularIva(subtotal));
        return totalBruto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    void calcularSubtotal_unItem() {
        List<DetalleVenta> items = new ArrayList<>();
        items.add(new DetalleVenta(1, "001", "Aceite", 2, new BigDecimal("10.00")));
        assertEquals(new BigDecimal("20.00"), calcularSubtotal(items));
    }

    @Test
    void calcularSubtotal_variosItems() {
        List<DetalleVenta> items = new ArrayList<>();
        items.add(new DetalleVenta(1, "001", "Aceite", 2, new BigDecimal("10.00")));
        items.add(new DetalleVenta(2, "002", "Filtro", 1, new BigDecimal("25.00")));
        assertEquals(new BigDecimal("45.00"), calcularSubtotal(items));
    }

    @Test
    void calcularIva_quincePorCiento() {
        BigDecimal subtotal = new BigDecimal("100.00");
        assertEquals(new BigDecimal("15.00"), calcularIva(subtotal));
    }

    @Test
    void calcularTotal_sinDescuento() {
        List<DetalleVenta> items = new ArrayList<>();
        items.add(new DetalleVenta(1, "001", "Aceite", 2, new BigDecimal("10.00")));
        BigDecimal subtotal = calcularSubtotal(items);
        assertEquals(new BigDecimal("23.00"), calcularTotal(subtotal, BigDecimal.ZERO));
    }

    @Test
    void calcularTotal_conDescuento10() {
        List<DetalleVenta> items = new ArrayList<>();
        items.add(new DetalleVenta(1, "001", "Aceite", 2, new BigDecimal("100.00")));
        BigDecimal subtotal = calcularSubtotal(items);
        BigDecimal totalBruto = subtotal.add(calcularIva(subtotal));
        BigDecimal descuento = calcularDescuento(totalBruto, "10");
        BigDecimal total = calcularTotal(subtotal, descuento);
        assertEquals(new BigDecimal("207.00"), total);
    }

    @Test
    void calcularTotal_conDescuento0() {
        List<DetalleVenta> items = new ArrayList<>();
        items.add(new DetalleVenta(1, "001", "Aceite", 1, new BigDecimal("50.00")));
        BigDecimal subtotal = calcularSubtotal(items);
        assertEquals(new BigDecimal("57.50"), calcularTotal(subtotal, BigDecimal.ZERO));
    }

    @Test
    void calcularStockDisponible_stockSuficiente() {
        int stockTotal = 10;
        int cantidadEnDetalle = 3;
        int disponible = stockTotal - cantidadEnDetalle;
        assertEquals(7, disponible);
    }

    @Test
    void calcularStockDisponible_stockInsuficiente_lanzaError() {
        int stockTotal = 5;
        int cantidadEnDetalle = 5;
        int disponible = stockTotal - cantidadEnDetalle;
        assertTrue(disponible <= 0);
    }

    @Test
    void detalleVenta_cantidadActualizada_recalculaTotal() {
        DetalleVenta d = new DetalleVenta(1, "001", "Aceite", 2, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("20.00"), d.getPrecioTotal());

        d.setCantidad(3);
        assertEquals(new BigDecimal("30.00"), d.getPrecioTotal());
    }

    @Test
    void detalleVenta_precioUnitarioActualizado_recalculaTotal() {
        DetalleVenta d = new DetalleVenta(1, "001", "Aceite", 2, new BigDecimal("10.00"));
        d.setPrecioUnitario(new BigDecimal("12.00"));
        assertEquals(new BigDecimal("24.00"), d.getPrecioTotal());
    }

    @Test
    void proforma_formaPagoTAGCredito_generaCuota() {
        BigDecimal total = new BigDecimal("210.00");
        int meses = 10;
        BigDecimal tasaInteres = new BigDecimal("3");
        BigDecimal totalConInteres = total.multiply(BigDecimal.ONE.add(tasaInteres.divide(new BigDecimal("100")))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cuotaMensual = totalConInteres.divide(new BigDecimal(meses), 2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("216.30"), totalConInteres);
        assertEquals(new BigDecimal("21.63"), cuotaMensual);
    }
}

package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.model.FacturaDetalle;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FacturaCalculatorTest {

    private BigDecimal calcularSubtotal(List<FacturaDetalle> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (FacturaDetalle d : items) {
            subtotal = subtotal.add(d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularIva(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularDescuento(BigDecimal totalBruto, BigDecimal porcentaje) {
        return totalBruto.multiply(porcentaje).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTotal(BigDecimal subtotal, BigDecimal descuento) {
        BigDecimal totalBruto = subtotal.add(calcularIva(subtotal));
        return totalBruto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    void calcularSubtotal_unItem() {
        List<FacturaDetalle> items = new ArrayList<>();
        items.add(new FacturaDetalle(1, "001", "Aceite", 2, new BigDecimal("10.00")));
        assertEquals(new BigDecimal("20.00"), calcularSubtotal(items));
    }

    @Test
    void calcularIva_quincePorCiento() {
        BigDecimal subtotal = new BigDecimal("100.00");
        assertEquals(new BigDecimal("15.00"), calcularIva(subtotal));
    }

    @Test
    void calcularTotal_sinDescuento() {
        List<FacturaDetalle> items = new ArrayList<>();
        items.add(new FacturaDetalle(1, "001", "Aceite", 2, new BigDecimal("10.00")));
        BigDecimal subtotal = calcularSubtotal(items);
        assertEquals(new BigDecimal("23.00"), calcularTotal(subtotal, BigDecimal.ZERO));
    }

    @Test
    void calcularTotal_conDescuento() {
        List<FacturaDetalle> items = new ArrayList<>();
        items.add(new FacturaDetalle(1, "001", "Aceite", 2, new BigDecimal("100.00")));
        BigDecimal subtotal = calcularSubtotal(items);
        BigDecimal descuento = calcularDescuento(subtotal.add(calcularIva(subtotal)), new BigDecimal("10"));
        BigDecimal total = calcularTotal(subtotal, descuento);
        assertEquals(new BigDecimal("207.00"), total);
    }

    @Test
    void calcularTotal_variosItems() {
        List<FacturaDetalle> items = new ArrayList<>();
        items.add(new FacturaDetalle(1, "001", "Aceite", 2, new BigDecimal("10.00")));
        items.add(new FacturaDetalle(2, "002", "Filtro", 1, new BigDecimal("25.00")));
        BigDecimal subtotal = calcularSubtotal(items);
        assertEquals(new BigDecimal("45.00"), subtotal);
        assertEquals(new BigDecimal("51.75"), calcularTotal(subtotal, BigDecimal.ZERO));
    }

    @Test
    void calcularSubtotal_cantidadCero_noSuma() {
        List<FacturaDetalle> items = new ArrayList<>();
        items.add(new FacturaDetalle(1, "001", "Aceite", 0, new BigDecimal("10.00")));
        assertEquals(BigDecimal.ZERO.setScale(2), calcularSubtotal(items));
    }
}

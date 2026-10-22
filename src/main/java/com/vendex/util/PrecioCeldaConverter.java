package com.vendex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.util.StringConverter;

/**
 * Convierte precios en celdas editables (doble clic): acepta coma o punto decimal y
 * devuelve null en lugar de lanzar NumberFormatException cuando el texto no es numérico.
 */
public class PrecioCeldaConverter extends StringConverter<BigDecimal> {

    @Override
    public String toString(BigDecimal valor) {
        return valor == null ? "" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public BigDecimal fromString(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim().replace(',', '.');
        if (limpio.isEmpty()) return null;
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

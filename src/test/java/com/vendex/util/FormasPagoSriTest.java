package com.vendex.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormasPagoSriTest {

    @Test
    void itemsNoVacíosYCódigosVigentes() {
        List<String> items = FormasPagoSri.items();
        assertEquals(8, items.size());
        assertEquals("01-Sin utilizacion del sistema financiero", FormasPagoSri.defaultItem());

        List<String> codigos = List.of("01", "15", "16", "17", "18", "19", "20", "21");
        for (int i = 0; i < items.size(); i++) {
            assertTrue(items.get(i).startsWith(codigos.get(i) + "-"),
                    "Cada item debe iniciar con su código: " + items.get(i));
        }
    }

    @Test
    void extraeCodigoDelItem() {
        assertEquals("01", FormasPagoSri.codigo("01-Sin utilizacion del sistema financiero"));
        assertEquals("19", FormasPagoSri.codigo("19-Tarjeta de credito"));
        assertEquals("21", FormasPagoSri.codigo("21-Endoso de titulos"));
    }

    @Test
    void validaCódigos() {
        assertTrue(FormasPagoSri.esValido("01"));
        assertTrue(FormasPagoSri.esValido("19"));
        assertTrue(FormasPagoSri.esValido("21"));
        assertFalse(FormasPagoSri.esValido("02"));
        assertFalse(FormasPagoSri.esValido("05"));
        assertFalse(FormasPagoSri.esValido(null));
        assertFalse(FormasPagoSri.esValido("99"));
    }
}

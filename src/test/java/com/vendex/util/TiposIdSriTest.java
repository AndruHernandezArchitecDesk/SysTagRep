package com.vendex.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TiposIdSriTest {

    @Test
    void itemsNoVacíosYCódigosVigentes() {
        List<String> items = TiposIdSri.items();
        assertEquals(3, items.size());
        assertEquals("04-RUC", TiposIdSri.defaultItem());

        assertTrue(items.get(0).startsWith("04-"));
        assertTrue(items.get(1).startsWith("05-"));
        assertTrue(items.get(2).startsWith("06-"));
    }

    @Test
    void extraeCodigoDelItem() {
        assertEquals("04", TiposIdSri.codigo("04-RUC"));
        assertEquals("05", TiposIdSri.codigo("05-Cedula"));
        assertEquals("06", TiposIdSri.codigo("06-Pasaporte"));
    }

    @Test
    void validaCódigos() {
        assertTrue(TiposIdSri.esValido("04"));
        assertTrue(TiposIdSri.esValido("05"));
        assertTrue(TiposIdSri.esValido("06"));
        assertFalse(TiposIdSri.esValido("01"));
        assertFalse(TiposIdSri.esValido("07"));
        assertFalse(TiposIdSri.esValido(null));
        assertFalse(TiposIdSri.esValido("99"));
    }
}

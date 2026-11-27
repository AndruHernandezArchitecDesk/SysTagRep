package com.vendex.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RetencionCatalogosSriTest {

    @Test
    void codigosSustento() {
        List<String> items = RetencionCatalogosSri.codSustentoItems();
        assertEquals(List.of("01-Credito tributario IVA", "02-Costo o gasto renta", "03-Activo fijo IVA"), items);
        assertEquals("01-Credito tributario IVA", RetencionCatalogosSri.defaultCodSustento());
        assertEquals("01", RetencionCatalogosSri.codigo("01-Credito tributario IVA"));
        assertEquals("03", RetencionCatalogosSri.codigo("03-Activo fijo IVA"));
        assertTrue(RetencionCatalogosSri.esValido("01", items));
        assertFalse(RetencionCatalogosSri.esValido("04", items));
        assertFalse(RetencionCatalogosSri.esValido(null, items));
    }

    @Test
    void tiposDocumentoSustento() {
        List<String> items = RetencionCatalogosSri.codDocSustentoItems();
        assertEquals(List.of("01-Factura", "04-Nota de credito", "05-Nota de debito"), items);
        assertEquals("01-Factura", RetencionCatalogosSri.defaultCodDocSustento());
        assertEquals("05", RetencionCatalogosSri.codigo("05-Nota de debito"));
        assertTrue(RetencionCatalogosSri.esValido("04", items));
        assertFalse(RetencionCatalogosSri.esValido("06", items));
    }

    @Test
    void impuestos() {
        List<String> items = RetencionCatalogosSri.impuestoItems();
        assertEquals(List.of("1-Renta", "2-IVA"), items);
        assertEquals("1-Renta", RetencionCatalogosSri.defaultImpuesto());
        assertEquals("1", RetencionCatalogosSri.codigo("1-Renta"));
        assertEquals("2", RetencionCatalogosSri.codigo("2-IVA"));
        assertTrue(RetencionCatalogosSri.esValido("2", items));
        assertFalse(RetencionCatalogosSri.esValido("3", items));
    }

    @Test
    void itemDeImpuesto() {
        assertEquals("2-IVA", RetencionCatalogosSri.itemDeImpuesto("2"));
        assertEquals("1-Renta", RetencionCatalogosSri.itemDeImpuesto("1"));
        assertNull(RetencionCatalogosSri.itemDeImpuesto("3"));
        assertNull(RetencionCatalogosSri.itemDeImpuesto(null));
    }

    @Test
    void codigoDeItemSinGuion() {
        assertEquals("303", RetencionCatalogosSri.codigo("303"));
        assertEquals("303", RetencionCatalogosSri.codigo("303 - Honorarios profesionales (10%)"));
    }
}

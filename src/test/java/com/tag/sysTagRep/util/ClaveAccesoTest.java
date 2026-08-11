package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;

class ClaveAccesoTest {

    @Test
    void generar_claveTiene49Caracteres() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        assertNotNull(clave);
        assertEquals(49, clave.length());
    }

    @Test
    void generar_ambientePruebas_usaUno() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        assertEquals('1', clave.charAt(23));
    }

    @Test
    void generar_ambienteProduccion_usaDos() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRODUCCION", "001", "001", 1);
        assertEquals('2', clave.charAt(23));
    }

    @Test
    void generar_serieYSecuencial_seUbicanCorrectamente() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 25);
        assertEquals("001001", clave.substring(24, 30));
        assertEquals("000000025", clave.substring(30, 39));
    }

    @Test
    void generar_fechaCoincideConHoy() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        String fechaEsperada = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        assertEquals(fechaEsperada, clave.substring(0, 8));
    }

    @Test
    void generar_digitoVerificadorEsValido() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        int digito = Character.getNumericValue(clave.charAt(48));
        assertTrue(digito >= 0 && digito <= 9);
    }

    @Test
    void generar_tipoComprobanteFormateado() {
        String clave = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        assertEquals("01", clave.substring(8, 10));
    }

    @Test
    void generar_clavesDistintas_porCodigoNumerico() {
        String c1 = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        String c2 = ClaveAcceso.generar("01", "1790000000001", "PRUEBAS", "001", "001", 1);
        assertEquals(c1.substring(0, 39), c2.substring(0, 39));
        assertNotEquals(c1, c2);
    }
}

package com.vendex.controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El campo "Interés total del préstamo" es opcional: vacío = 0% (sin interés).
 */
class PrestamoTasaOpcionalTest {

    @Test
    void vacio_oNulo_devuelveCero() {
        assertEquals(0, PrestamoController.parseTasaOpcional(null).compareTo(BigDecimal.ZERO));
        assertEquals(0, PrestamoController.parseTasaOpcional("").compareTo(BigDecimal.ZERO));
        assertEquals(0, PrestamoController.parseTasaOpcional("   ").compareTo(BigDecimal.ZERO));
    }

    @Test
    void valorIngresado_seParseaIgualQueElRestoDelFormulario() {
        assertEquals(0, PrestamoController.parseTasaOpcional("16.50").compareTo(new BigDecimal("16.50")));
        assertEquals(0, PrestamoController.parseTasaOpcional(" 16,50 ").compareTo(new BigDecimal("16.50")));
        assertEquals(0, PrestamoController.parseTasaOpcional("0").compareTo(BigDecimal.ZERO));
    }

    @Test
    void textoInvalido_lanzaErrorDeParseo() {
        assertThrows(NumberFormatException.class, () -> PrestamoController.parseTasaOpcional("abc"));
    }
}

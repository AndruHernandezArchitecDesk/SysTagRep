package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CifradoTest {

    @Test
    void encriptarYDesencriptar_textoSimple() throws Exception {
        String original = "mi-clave-123";
        String encriptado = Cifrado.encriptar(original);
        assertNotNull(encriptado);
        assertTrue(encriptado.contains(":"));
        assertEquals(original, Cifrado.desencriptar(encriptado));
    }

    @Test
    void encriptar_textoVacio_noFalla() throws Exception {
        String encriptado = Cifrado.encriptar("");
        assertNotNull(encriptado);
        assertEquals("", Cifrado.desencriptar(encriptado));
    }

    @Test
    void encriptar_textoNulo_noFalla() throws Exception {
        String encriptado = Cifrado.encriptar(null);
        assertNotNull(encriptado);
        assertEquals("", Cifrado.desencriptar(encriptado));
    }

    @Test
    void desencriptar_textoMalFormado_retornaVacio() throws Exception {
        assertEquals("", Cifrado.desencriptar("sin-dos-puntos"));
    }

    @Test
    void desencriptar_textoNulo_retornaVacio() throws Exception {
        assertEquals("", Cifrado.desencriptar(null));
    }

    @Test
    void encriptar_textoConCaracteresEspeciales() throws Exception {
        String original = "p@ssw0rd!#$%";
        String encriptado = Cifrado.encriptar(original);
        assertEquals(original, Cifrado.desencriptar(encriptado));
    }

    @Test
    void encriptar_textoLargo() throws Exception {
        String original = "A".repeat(500);
        String encriptado = Cifrado.encriptar(original);
        assertEquals(original, Cifrado.desencriptar(encriptado));
    }
}

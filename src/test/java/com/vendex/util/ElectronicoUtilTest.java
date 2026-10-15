package com.vendex.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectronicoUtilTest {

    @Test
    void debeEnviarNotificacion_autorizadoConDatos_retornaTrue() {
        assertTrue(ElectronicoUtil.debeEnviarNotificacion("AUTORIZADO",
                "1234567890123456789012345678901234567890123456789", "12/08/2026 10:00:00"));
    }

    @Test
    void debeEnviarNotificacion_pendiente_retornaFalse() {
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("PENDIENTE",
                "1234567890123456789012345678901234567890123456789", "12/08/2026 10:00:00"));
    }

    @Test
    void debeEnviarNotificacion_rechazada_retornaFalse() {
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("RECHAZADA",
                "1234567890123456789012345678901234567890123456789", "12/08/2026 10:00:00"));
    }

    @Test
    void debeEnviarNotificacion_devuelta_retornaFalse() {
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("DEVUELTA",
                "1234567890123456789012345678901234567890123456789", "12/08/2026 10:00:00"));
    }

    @Test
    void debeEnviarNotificacion_autorizadoSinNumero_retornaFalse() {
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("AUTORIZADO", null, "12/08/2026 10:00:00"));
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("AUTORIZADO", "  ", "12/08/2026 10:00:00"));
    }

    @Test
    void debeEnviarNotificacion_autorizadoSinFecha_retornaFalse() {
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("AUTORIZADO",
                "1234567890123456789012345678901234567890123456789", null));
        assertFalse(ElectronicoUtil.debeEnviarNotificacion("AUTORIZADO",
                "1234567890123456789012345678901234567890123456789", ""));
    }

    @Test
    void resolverNumero_conNumero_retornaIntacto() {
        assertEquals("NUM123", ElectronicoUtil.resolverNumero("1510202604171685454000120010010000000019748821513", "NUM123"));
    }

    @Test
    void resolverNumero_vacioConClave49_retornaClave() {
        String clave = "1510202604171685454000120010010000000019748821513";
        assertEquals(clave, ElectronicoUtil.resolverNumero(clave, null));
        assertEquals(clave, ElectronicoUtil.resolverNumero(clave, "  "));
    }

    @Test
    void resolverNumero_vacioSinClaveValida_retornaOriginal() {
        assertNull(ElectronicoUtil.resolverNumero(null, null));
        assertNull(ElectronicoUtil.resolverNumero("corta", null));
    }

    @Test
    void resolverMensaje_vacio_retornaAutorizadoSri() {
        assertEquals("Autorizado SRI", ElectronicoUtil.resolverMensaje(null));
        assertEquals("Autorizado SRI", ElectronicoUtil.resolverMensaje("  "));
        assertEquals("OK", ElectronicoUtil.resolverMensaje("OK"));
    }

    @Test
    void rescatarAutorizacion_autorizadoSinNumero_marcaRescate() {
        String clave = "1510202604171685454000120010010000000019748821513";
        ElectronicoUtil.AutRescatada r = ElectronicoUtil.rescatarAutorizacion(
                clave, "AUTORIZADO", null, null, null, null, null);
        assertTrue(r.rescatado);
        assertEquals(clave, r.numero);
        assertEquals("Autorizado SRI", r.mensaje);
    }

    @Test
    void rescatarAutorizacion_noAutorizado_noRescata() {
        ElectronicoUtil.AutRescatada r = ElectronicoUtil.rescatarAutorizacion(
                "1510202604171685454000120010010000000019748821513", "PENDIENTE", null, null, null, null, null);
        assertFalse(r.rescatado);
    }

    @Test
    void rescatarAutorizacion_conNumero_noRescata() {
        ElectronicoUtil.AutRescatada r = ElectronicoUtil.rescatarAutorizacion(
                "1510202604171685454000120010010000000019748821513", "AUTORIZADO", "OK", "NUM", null, null, null);
        assertFalse(r.rescatado);
        assertEquals("NUM", r.numero);
    }
}

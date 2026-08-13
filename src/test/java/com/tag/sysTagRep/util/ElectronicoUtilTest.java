package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}

package com.vendex.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MensajeSriUsuarioTest {
    @Test
    void explicaEstadosYConservaRecomendacionSinInventarCausa() {
        assertEquals("El SRI recibió el comprobante; todavía no confirma la autorización.",
                MensajeSriUsuario.explicar("PENDIENTE", "Comprobante recibido por el SRI"));
        assertEquals("El SRI rechazó el comprobante. Revisa el detalle para corregir los datos señalados.",
                MensajeSriUsuario.explicar("RECHAZADA", "[ERROR 43] dato inválido"));
        assertEquals("Se detuvieron los reintentos localmente. Esto no anula el comprobante ante el SRI.",
                MensajeSriUsuario.explicar("ANULADO", "Anulado localmente"));
    }
}

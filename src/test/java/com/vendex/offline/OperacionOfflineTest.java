package com.vendex.offline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperacionOfflineTest {

    @Test
    void crearOperacion_fijaTipoYPayload() {
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"test\": true}");

        assertEquals("FACTURA", op.getTipo());
        assertEquals("{\"test\": true}", op.getPayload());
        assertEquals(OperacionOffline.Estado.PENDIENTE, op.getEstado());
        assertNotNull(op.getCreadoEn());
    }
}

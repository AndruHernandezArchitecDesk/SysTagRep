package com.vendex.offline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OfflineHelperTest {

    @Test
    void generarPayload_serializaObjeto() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("clienteId", 1);
        body.put("total", 100.5);

        String payload = OfflineHelper.generarPayload(body);

        assertNotNull(payload);
        assertTrue(payload.contains("clienteId"));
        assertTrue(payload.contains("total"));
    }
}

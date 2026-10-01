package com.vendex.offline;

import com.vendex.remote.ApiConfig;

public class OfflineHelper {

    public static boolean debeUsarModoOffline() {
        return ApiConfig.isModoRemoto() && !OfflineModeManager.INSTANCE.isOnline();
    }

    public static String generarPayload(Object operacion) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(operacion);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando operación offline", e);
        }
    }
}

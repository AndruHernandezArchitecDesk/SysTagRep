package com.vendex.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fase 7: idempotencia de escrituras reintentadas (cola offline).
 * El cliente envía {@code Idempotency-Key} estable entre reintentos; si la petición
 * ya fue procesada con éxito, se devuelve la respuesta cacheada sin re-ejecutarla
 * (evita facturas/ND duplicadas cuando la respuesta se pierde tras el commit).
 */
public final class IdempotencyStore {

    public record Entrada(int status, String body, long creadoEn) {}

    private static final int MAX_ENTRADAS = 1000;
    private static final long TTL_MS = 10 * 60 * 1000; // 10 min (ciclo de sync: 30s)

    private static final Map<String, Entrada> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entrada> eldest) {
            return size() > MAX_ENTRADAS;
        }
    };

    private IdempotencyStore() {}

    public static synchronized Entrada get(String key) {
        limpiarExpiradas();
        return CACHE.get(key);
    }

    public static synchronized void put(String key, int status, String body) {
        if (key == null || key.isBlank() || status < 200 || status >= 300 || body == null) return;
        limpiarExpiradas();
        CACHE.put(key, new Entrada(status, body, System.currentTimeMillis()));
    }

    private static void limpiarExpiradas() {
        long ahora = System.currentTimeMillis();
        CACHE.values().removeIf(e -> ahora - e.creadoEn() > TTL_MS);
    }

    /** Solo para tests. */
    public static synchronized void limpiar() {
        CACHE.clear();
    }
}

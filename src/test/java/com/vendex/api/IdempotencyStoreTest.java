package com.vendex.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 7: idempotencia de escrituras reintentadas (Idempotency-Key). */
class IdempotencyStoreTest {

    @BeforeEach
    @AfterEach
    void limpiar() {
        IdempotencyStore.limpiar();
    }

    @Test
    void respuestaExitosaSeCachea_ySeRepite() {
        IdempotencyStore.put("k1", 201, "{\"claveAcceso\":\"ABC\"}");
        IdempotencyStore.Entrada e = IdempotencyStore.get("k1");
        assertNotNull(e);
        assertEquals(201, e.status());
        assertEquals("{\"claveAcceso\":\"ABC\"}", e.body());
    }

    @Test
    void errores4xxNoSeCachean_seReintentaElProceso() {
        IdempotencyStore.put("k2", 400, "{\"error\":\"invalido\"}");
        IdempotencyStore.put("k2", 409, "{\"error\":\"duplicado\"}");
        assertNull(IdempotencyStore.get("k2"), "Un 4xx no debe dar por procesada la operación");
    }

    @Test
    void claveDesconocida_devuelveNull() {
        assertNull(IdempotencyStore.get("no-existe"));
        assertNull(IdempotencyStore.get(null));
        assertNull(IdempotencyStore.get("  "));
    }

    @Test
    void putInvalido_noAlmacena() {
        IdempotencyStore.put(null, 200, "x");
        IdempotencyStore.put("k3", 200, null);
        assertNull(IdempotencyStore.get("k3"));
    }

    @Test
    void accesoConcurrente_aClavesDistintas_noSePierden() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tareas = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                final int n = i;
                tareas.add(() -> {
                    IdempotencyStore.put("c" + n, 200, "body" + n);
                    return null;
                });
            }
            List<Future<Void>> futures = pool.invokeAll(tareas);
            for (Future<Void> f : futures) f.get();
        } finally {
            pool.shutdownNow();
        }
        int encontrados = 0;
        for (int i = 0; i < 200; i++) {
            if (IdempotencyStore.get("c" + i) != null) encontrados++;
        }
        assertEquals(200, encontrados);
    }
}

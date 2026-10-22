package com.vendex.offline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 7: concurrencia de la cola offline — N hilos encolando no deben perder operaciones. */
class ColaOfflineConcurrenciaTest {

    private static final int HILOS = 8;
    private static final int POR_HILO = 25;

    @BeforeEach
    void setUp() throws Exception {
        LocalOperationQueue.setDirectorioPrueba(Files.createTempDirectory("cola_concurrencia"));
    }

    @Test
    void encolarConcurrente_noPierdeOperaciones() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listo = new CountDownLatch(HILOS);
        CountDownLatch arranca = new CountDownLatch(1);
        try {
            for (int h = 0; h < HILOS; h++) {
                final int hilo = h;
                pool.submit(() -> {
                    LocalOperationQueue cola = new LocalOperationQueue(); // instancia distinta por hilo (como los controllers)
                    listo.countDown();
                    try {
                        arranca.await();
                        for (int i = 0; i < POR_HILO; i++) {
                            cola.encolar(new OperacionOffline("FACTURA",
                                    "{\"hilo\":" + hilo + ",\"i\":" + i + "}"));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertTrue(listo.await(10, TimeUnit.SECONDS));
            arranca.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        List<OperacionOffline> todas = new LocalOperationQueue().listarTodas();
        assertEquals(HILOS * POR_HILO, todas.size(),
                "Operaciones perdidas: read-modify-write del archivo debe ser atómico entre instancias");
        long idsUnicos = todas.stream().map(OperacionOffline::getId).distinct().count();
        assertEquals(HILOS * POR_HILO, idsUnicos, "IDs duplicados");
    }
}

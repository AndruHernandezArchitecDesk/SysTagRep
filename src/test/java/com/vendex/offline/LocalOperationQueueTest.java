package com.vendex.offline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class LocalOperationQueueTest {

    private Path directorioTemporal;

    @BeforeEach
    void setUp() throws Exception {
        directorioTemporal = Files.createTempDirectory("vendex-offline-test");
        LocalOperationQueue.setDirectorioPrueba(directorioTemporal);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(directorioTemporal.resolve("operaciones.json"));
        Files.deleteIfExists(directorioTemporal);
    }

    @Test
    void encolarYListarPendientes_funciona() throws Exception {
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"test\": true}");
        queue.encolar(op);

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(1, pendientes.size());
        assertEquals("FACTURA", pendientes.get(0).getTipo());
    }

    @Test
    void marcarEnviada_actualizaEstado() throws Exception {
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"test\": true}");
        queue.encolar(op);
        int id = op.getId();

        queue.marcarEnviada(id);

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(0, pendientes.size());
    }

    @Test
    void marcarConflicto_guardaError() throws Exception {
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"test\": true}");
        queue.encolar(op);
        int id = op.getId();

        queue.marcarConflicto(id, "Error de prueba");

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(0, pendientes.size());
    }
}

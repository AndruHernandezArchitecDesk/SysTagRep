package com.vendex.offline;

import com.vendex.remote.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.List;

class SyncServiceIntegrationTest {

    private HttpServer server;
    private int puerto;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        puerto = server.getAddress().getPort();
        server.setExecutor(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sincronizar_operaConExito_yMarcaEnviada() throws Exception {
        server.createContext("/facturas/registro", ex -> {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, 0);
                ex.getResponseBody().close();
            } else {
                ex.sendResponseHeaders(405, 0);
                ex.getResponseBody().close();
            }
        });
        server.start();

        SyncService service = SyncService.paraTesting(new RestClient("http://localhost:" + puerto));
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"clienteId\":1}");
        queue.encolar(op);

        service.intentarSincronizacion();

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(0, pendientes.size(), "La operación debería estar enviada");
    }

    @Test
    void sincronizar_marcaConflicto_enError400() throws Exception {
        server.createContext("/facturas/registro", ex -> {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(400, 0);
                ex.getResponseBody().write("{\"error\":\"datos invalidos\"}".getBytes());
                ex.getResponseBody().close();
            } else {
                ex.sendResponseHeaders(405, 0);
                ex.getResponseBody().close();
            }
        });
        server.start();

        SyncService service = SyncService.paraTesting(new RestClient("http://localhost:" + puerto));
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"clienteId\":1}");
        queue.encolar(op);

        service.intentarSincronizacion();

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(0, pendientes.size(), "La operación debería estar en conflicto");
    }
}

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
        // Cola aislada por test: el directorio real (~/.vendex) acumula estado entre corridas.
        LocalOperationQueue.setDirectorioPrueba(java.nio.file.Files.createTempDirectory("cola_offline_test"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sincronizar_operaConExito_yMarcaEnviada() throws Exception {
        server.createContext("/facturas", ex -> {
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
        server.createContext("/facturas", ex -> {
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

    @Test
    void falloDeConexion_siguePendiente_conIntentoRegistrado() throws Exception {
        // Puerto libre sin listener: conexión rechazada inmediatamente (sin servidor que acepte y no responda)
        int puertoLibre;
        try (java.net.ServerSocket ss = new java.net.ServerSocket(0)) {
            puertoLibre = ss.getLocalPort();
        }
        SyncService service = SyncService.paraTesting(new RestClient("http://localhost:" + puertoLibre));
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"clienteId\":1}");
        queue.encolar(op);

        service.intentarSincronizacion();

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(1, pendientes.size(), "Sin conexión debe seguir PENDIENTE para reintentar");
        assertEquals(1, pendientes.get(0).getIntentos(), "Debe registrar el intento fallido");
    }

    @Test
    void error500_siguePendiente_paraReintento() throws Exception {
        server.createContext("/facturas", ex -> {
            ex.sendResponseHeaders(500, 0);
            ex.getResponseBody().close();
        });
        server.start();

        SyncService service = SyncService.paraTesting(new RestClient("http://localhost:" + puerto));
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"clienteId\":1}");
        queue.encolar(op);

        service.intentarSincronizacion();

        List<OperacionOffline> pendientes = queue.listarPendientes();
        assertEquals(1, pendientes.size(), "5xx es transitorio: debe seguir PENDIENTE");
        assertEquals(1, pendientes.get(0).getIntentos());
    }

    @Test
    void alcanzaMaximoDeIntentos_marcaFallida() throws Exception {
        server.createContext("/facturas", ex -> {
            ex.sendResponseHeaders(500, 0);
            ex.getResponseBody().close();
        });
        server.start();

        SyncService service = SyncService.paraTesting(new RestClient("http://localhost:" + puerto));
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"clienteId\":1}");
        queue.encolar(op);
        // 4 intentos previos registrados en la cola: el siguiente (5º) debe agotar
        for (int i = 0; i < 4; i++) queue.registrarIntento(op.getId(), "intento previo");

        service.intentarSincronizacion();

        assertEquals(0, queue.listarPendientes().size());
        List<OperacionOffline> todas = queue.listarTodas();
        assertEquals(1, todas.size());
        assertEquals(OperacionOffline.Estado.FALLIDA, todas.get(0).getEstado());
    }

    @Test
    void reenvio_llevaIdempotencyKeyEstable() throws Exception {
        java.util.concurrent.atomic.AtomicReference<String> header = new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/facturas", ex -> {
            header.set(ex.getRequestHeaders().getFirst("Idempotency-Key"));
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();
        });
        server.start();

        SyncService service = SyncService.paraTesting(new RestClient("http://localhost:" + puerto));
        LocalOperationQueue queue = new LocalOperationQueue();
        OperacionOffline op = new OperacionOffline("FACTURA", "{\"clienteId\":1}");
        queue.encolar(op);

        service.intentarSincronizacion();

        assertNotNull(header.get(), "Debe enviar Idempotency-Key");
        assertTrue(header.get().startsWith("FACTURA-" + op.getId() + "-"),
                "La clave debe ser estable por operación: " + header.get());
    }
}

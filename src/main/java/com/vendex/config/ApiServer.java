package com.vendex.config;

import com.vendex.controller.api.InventarioApiController;
import com.vendex.controller.api.SucursalApiController;
import com.vendex.controller.api.TransferenciaApiController;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API REST V1 — Javalin ligero, misma BD dbVendex en vendex-db.
 * Endpoints para tienda web / app móvil multi-sucursal.
 * Reusa AppContext DAOs y HikariCP (maxPool=6 por PC).
 */
public class ApiServer {

    private static final Logger LOG = Logger.getLogger(ApiServer.class.getName());
    private static Javalin app;
    private static final int DEFAULT_PORT = 7070;

    public static synchronized void startIfEnabled() {
        String portProp = System.getProperty("api.port", System.getenv("API_PORT") != null ? System.getenv("API_PORT") : "");
        boolean enabled = "true".equalsIgnoreCase(System.getProperty("api.enabled", System.getenv("API_ENABLED") != null ? System.getenv("API_ENABLED") : "false"));
        // También habilitar si se pasa -Dapi.port
        if (!portProp.isBlank()) enabled = true;
        if (!enabled) {
            LOG.info("API REST deshabilitada (usar -Dapi.enabled=true -Dapi.port=7070 para habilitar)");
            return;
        }
        int port = DEFAULT_PORT;
        try { if (!portProp.isBlank()) port = Integer.parseInt(portProp); } catch (NumberFormatException ignore) {}
        start(port);
    }

    public static synchronized void start(int port) {
        if (app != null) {
            LOG.warning("API ya iniciada en puerto " + port);
            return;
        }
        AppContext ctx = AppContext.getInstance();
        SucursalApiController sucursalCtrl = new SucursalApiController(ctx.sucursalDAO);
        InventarioApiController inventarioCtrl = new InventarioApiController(ctx.inventarioDAO);
        TransferenciaApiController transfCtrl = new TransferenciaApiController(ctx.transferenciaInventarioService, ctx.transferenciaInventarioDAO);

        app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        // Health
        app.get("/api/health", ctx2 -> ctx2.json(java.util.Map.of("status", "UP", "sucursales", ctx.sucursalDAO.listar().size())));

        // Sucursales
        app.get("/api/sucursales", sucursalCtrl::listar);
        app.get("/api/sucursales/{id}", sucursalCtrl::obtenerPorId);
        app.post("/api/sucursales", sucursalCtrl::crear);

        // Inventario por sucursal
        app.get("/api/inventario", inventarioCtrl::listar);
        app.get("/api/inventario/{id}", inventarioCtrl::obtenerPorId);

        // Transferencias
        app.post("/api/transferencias", transfCtrl::transferir);
        app.get("/api/transferencias", transfCtrl::listar);

        // Error handler
        app.exception(Exception.class, (e, ctx2) -> {
            LOG.log(Level.WARNING, "API error", e);
            ctx2.status(500).json(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        });

        app.start(port);
        LOG.info("API REST Vendex iniciada en http://localhost:" + port + "/api/health");
    }

    public static synchronized void stop() {
        if (app != null) {
            app.stop();
            app = null;
            LOG.info("API REST detenida");
        }
    }

    public static boolean isRunning() {
        return app != null;
    }

    // Para tests
    public static Javalin getApp() { return app; }
}

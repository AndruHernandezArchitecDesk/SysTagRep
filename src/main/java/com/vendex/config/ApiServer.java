package com.vendex.config;

import com.vendex.controller.api.CajaApiController;
import com.vendex.controller.api.ClienteApiController;
import com.vendex.controller.api.ComprobanteApiController;
import com.vendex.controller.api.CuentaPorCobrarApiController;
import com.vendex.controller.api.DashboardApiController;
import com.vendex.controller.api.EmpresaApiController;
import com.vendex.controller.api.FacturaApiController;
import com.vendex.controller.api.FacturaDetalleApiController;
import com.vendex.controller.api.GuiaRemisionApiController;
import com.vendex.controller.api.HistorialProductoApiController;
import com.vendex.controller.api.InventarioApiController;
import com.vendex.controller.api.LogApiController;
import com.vendex.controller.api.LoginApiController;
import com.vendex.controller.api.NotaCreditoApiController;
import com.vendex.controller.api.NotaVentaApiController;
import com.vendex.controller.api.RetencionApiController;
import com.vendex.controller.api.SecuenciaDocumentoApiController;
import com.vendex.controller.api.SucursalApiController;
import com.vendex.controller.api.TransferenciaApiController;
import com.vendex.controller.api.VehiculoApiController;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiServer {

    private static final Logger LOG = Logger.getLogger(ApiServer.class.getName());
    private static Javalin app;
    private static final int DEFAULT_PORT = 7070;

    public static synchronized void startIfEnabled() {
        String portProp = System.getProperty("api.port", System.getenv("API_PORT") != null ? System.getenv("API_PORT") : "");
        boolean enabled = "true".equalsIgnoreCase(System.getProperty("api.enabled", System.getenv("API_ENABLED") != null ? System.getenv("API_ENABLED") : "false"));
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
        InventarioApiController inventarioCtrl = new InventarioApiController(ctx.inventarioDAO, ctx.vehiculoDAO, ctx.inventarioVehiculoDAO);
        TransferenciaApiController transfCtrl = new TransferenciaApiController(ctx.transferenciaInventarioService, ctx.transferenciaInventarioDAO);
        VehiculoApiController vehiculoCtrl = new VehiculoApiController(ctx.vehiculoDAO, ctx.inventarioVehiculoDAO);
        FacturaApiController facturaCtrl = new FacturaApiController(ctx);
        FacturaDetalleApiController facturaDetalleCtrl = new FacturaDetalleApiController(ctx);
        CajaApiController cajaCtrl = new CajaApiController(ctx);
        LoginApiController loginCtrl = new LoginApiController(ctx);
        ClienteApiController clienteCtrl = new ClienteApiController(ctx);
        ComprobanteApiController comprobanteCtrl = new ComprobanteApiController(ctx);
        CuentaPorCobrarApiController cuentaCtrl = new CuentaPorCobrarApiController(ctx);
        NotaVentaApiController notaVentaCtrl = new NotaVentaApiController(ctx);
        NotaCreditoApiController notaCreditoCtrl = new NotaCreditoApiController(ctx);
        GuiaRemisionApiController guiaCtrl = new GuiaRemisionApiController(ctx);
        HistorialProductoApiController historialCtrl = new HistorialProductoApiController(ctx);
        RetencionApiController retencionCtrl = new RetencionApiController(ctx);
        DashboardApiController dashboardCtrl = new DashboardApiController(ctx);
        SecuenciaDocumentoApiController secuenciaCtrl = new SecuenciaDocumentoApiController(ctx);
        EmpresaApiController empresaCtrl = new EmpresaApiController(ctx);
        LogApiController logCtrl = new LogApiController(ctx);

        app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        app.get("/api/health", ctx2 -> ctx2.json(java.util.Map.of("status", "UP", "sucursales", ctx.sucursalDAO.listar().size())));

        app.post("/api/auth/login", loginCtrl::login);
        app.post("/api/auth/logout", loginCtrl::logout);
        app.get("/api/auth/me", loginCtrl::me);

        app.get("/api/sucursales", sucursalCtrl::listar);
        app.get("/api/sucursales/{id}", sucursalCtrl::obtenerPorId);
        app.post("/api/sucursales", sucursalCtrl::crear);

        app.get("/api/empresa", empresaCtrl::listar);
        app.get("/api/empresa/{id}", empresaCtrl::obtenerPorId);
        app.put("/api/empresa/{id}", empresaCtrl::actualizar);

        app.get("/api/inventario", inventarioCtrl::listar);
        app.get("/api/inventario/{id}", inventarioCtrl::obtenerPorId);
        app.post("/api/inventario", inventarioCtrl::guardar);
        app.put("/api/inventario/{id}", inventarioCtrl::actualizar);
        app.delete("/api/inventario/{id}", inventarioCtrl::eliminar);
        app.post("/api/inventario/{id}/stock/descontar", inventarioCtrl::descontarStock);
        app.post("/api/inventario/{id}/stock/devolver", inventarioCtrl::devolverStock);
        app.get("/api/inventario/sucursal/{sucursalId}", inventarioCtrl::listarPorSucursal);
        app.get("/api/inventario/codigo/{codigo}/sucursal/{sucursalId}", inventarioCtrl::obtenerPorCodigo);
        app.get("/api/facturas", facturaCtrl::listar);
        app.get("/api/facturas/{id}", facturaCtrl::obtenerPorId);
        app.get("/api/facturas/clave", facturaCtrl::obtenerPorClave);
        app.post("/api/facturas", facturaCtrl::emitir);
        app.post("/api/facturas/registro", facturaCtrl::insertarRegistro);
        app.put("/api/facturas/estado", facturaCtrl::actualizarEstado);
        app.get("/api/facturas/pendientes-sri", facturaCtrl::listarPendientesSri);
        app.get("/api/facturas/contar", facturaCtrl::contar);
        app.post("/api/facturas/{id}/detalles", facturaDetalleCtrl::insertar);
        app.get("/api/facturas/{id}/detalles", facturaDetalleCtrl::listarPorFactura);
        app.post("/api/comprobantes", comprobanteCtrl::insertar);
        app.put("/api/comprobantes/estado", comprobanteCtrl::actualizarEstado);
        app.post("/api/comprobantes/envio", comprobanteCtrl::guardarEnvio);
        app.get("/api/comprobantes/secuencial/{tipo}", comprobanteCtrl::obtenerSecuencial);
        app.post("/api/cuenta-por-cobrar", cuentaCtrl::insertar);
        app.post("/api/cuenta-por-cobrar/{id}/pagar", cuentaCtrl::marcarPagado);
        app.get("/api/cuenta-por-cobrar/creditos-activos", cuentaCtrl::creditosActivos);
        app.get("/api/cuenta-por-cobrar/cliente/{clienteId}", cuentaCtrl::porCliente);
        app.get("/api/cuenta-por-cobrar/detalles-venta/{notaVentaId}", cuentaCtrl::detallesVenta);
        app.post("/api/historial-producto", historialCtrl::insertar);
        app.get("/api/historial-producto", historialCtrl::listarPorFecha);

        app.get("/api/caja/abierta", cajaCtrl::abierta);
        app.post("/api/caja/abrir", cajaCtrl::abrir);
        app.post("/api/caja/cerrar", cajaCtrl::cerrar);
        app.get("/api/caja/sesiones/{id}", cajaCtrl::obtenerPorId);
        app.get("/api/caja/movimientos/{sesionId}", cajaCtrl::movimientos);
        app.get("/api/caja/movimientos/{sesionId}/total", cajaCtrl::totalMovimientos);
        app.get("/api/caja/resumen/{sesionId}", cajaCtrl::resumen);
        app.get("/api/caja/sesiones", cajaCtrl::listarPorFecha);

        app.post("/api/transferencias", transfCtrl::transferir);
        app.get("/api/transferencias", transfCtrl::listar);

        app.get("/api/vehiculos", vehiculoCtrl::listar);
        app.get("/api/vehiculos/{id}", vehiculoCtrl::obtenerPorId);
        app.post("/api/vehiculos", vehiculoCtrl::crear);
        app.get("/api/vehiculos/vin/{vin}", vehiculoCtrl::buscarPorVin);
        app.post("/api/vehiculos/import", vehiculoCtrl::importar);
        app.get("/api/inventario/{id}/compatibilidades", vehiculoCtrl::listarCompatibilidades);
        app.post("/api/inventario/{id}/compatibilidades", vehiculoCtrl::asociarCompatibilidades);

        app.get("/api/clientes", clienteCtrl::listar);
        app.get("/api/clientes/{id}", clienteCtrl::obtenerPorId);
        app.post("/api/clientes", clienteCtrl::crear);
        app.put("/api/clientes/{id}", clienteCtrl::actualizar);
        app.delete("/api/clientes/{id}", clienteCtrl::eliminar);

        app.get("/api/nota-venta", notaVentaCtrl::listar);
        app.get("/api/nota-venta/{id}", notaVentaCtrl::obtenerPorId);
        app.post("/api/nota-venta", notaVentaCtrl::emitir);

        app.get("/api/nota-credito", notaCreditoCtrl::listar);
        app.get("/api/nota-credito/{id}", notaCreditoCtrl::obtenerPorId);
        app.get("/api/nota-credito/clave", notaCreditoCtrl::obtenerPorClave);
        app.post("/api/nota-credito", notaCreditoCtrl::insertar);
        app.put("/api/nota-credito/estado", notaCreditoCtrl::actualizarEstado);
        app.put("/api/nota-credito/xml", notaCreditoCtrl::actualizarXmlFirmado);
        app.get("/api/nota-credito/por-factura/{facturaRegistroId}", notaCreditoCtrl::listarPorFactura);
        app.get("/api/nota-credito/pendientes-sri", notaCreditoCtrl::listarPendientesSri);
        app.get("/api/nota-credito/contar", notaCreditoCtrl::contar);
        app.post("/api/nota-credito/emitir", notaCreditoCtrl::emitir);

        app.get("/api/guia-remision", guiaCtrl::listar);
        app.get("/api/guia-remision/{id}", guiaCtrl::obtenerPorId);
        app.get("/api/guia-remision/clave", guiaCtrl::obtenerPorClave);
        app.post("/api/guia-remision", guiaCtrl::insertar);
        app.put("/api/guia-remision/estado", guiaCtrl::actualizarEstado);
        app.get("/api/guia-remision/pendientes-sri", guiaCtrl::listarPendientesSri);

        app.get("/api/retencion", retencionCtrl::listar);
        app.get("/api/retencion/{id}", retencionCtrl::obtenerPorId);
        app.get("/api/retencion/clave", retencionCtrl::obtenerPorClave);
        app.post("/api/retencion", retencionCtrl::insertar);
        app.put("/api/retencion/estado", retencionCtrl::actualizarEstado);
        app.get("/api/retencion/pendientes-sri", retencionCtrl::listarPendientesSri);

        app.get("/api/dashboard/metricas", dashboardCtrl::metricas);

        app.post("/api/logs", logCtrl::guardar);

        app.get("/api/secuencia-documento/{puntoEmisionId}/{tipo}", secuenciaCtrl::obtener);
        app.post("/api/secuencia-documento/{puntoEmisionId}/{tipo}/marcar-usado", secuenciaCtrl::marcarUsado);
        app.post("/api/secuencia-documento/bloque", secuenciaCtrl::reservarBloque);
        app.get("/api/secuencia-documento/bloque-disponible", secuenciaCtrl::obtenerBloqueDisponible);
        app.get("/api/secuencia-documento/bloques", secuenciaCtrl::listarBloques);
        app.post("/api/secuencia-documento/bloques/liberar-expirados", secuenciaCtrl::liberarExpirados);

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

    public static Javalin getApp() { return app; }
}

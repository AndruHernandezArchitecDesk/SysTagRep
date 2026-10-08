package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.CuentaPorCobrarDAO;
import com.vendex.model.CuentaPorCobrar;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.Map;

public class CuentaPorCobrarApiController {

    private final CuentaPorCobrarDAO dao;

    public CuentaPorCobrarApiController() {
        this(AppContext.getInstance());
    }

    public CuentaPorCobrarApiController(AppContext ctx) {
        this.dao = ctx.cuentaPorCobrarDAO;
    }

    public void insertar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            CuentaPorCobrar cpc = new CuentaPorCobrar();
            cpc.setClienteId(((Number) body.get("clienteId")).intValue());
            cpc.setNotaVentaId(body.get("notaVentaId") != null ? ((Number) body.get("notaVentaId")).intValue() : null);
            cpc.setFacturaRegistroId(body.get("facturaRegistroId") != null ? ((Number) body.get("facturaRegistroId")).intValue() : null);
            cpc.setTotal(new BigDecimal(body.get("monto").toString()));
            cpc.setCuotaMensual(new BigDecimal(body.get("saldo").toString()));
            dao.insertar(cpc);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void marcarPagado(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        try {
            dao.marcarPagado(id);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void creditosActivos(Context ctx) {
        try {
            ctx.json(dao.listarCreditosActivos());
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void porCliente(Context ctx) {
        int clienteId = Integer.parseInt(ctx.pathParam("clienteId"));
        try {
            ctx.json(dao.listarPorCliente(clienteId));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void detallesVenta(Context ctx) {
        int notaVentaId = Integer.parseInt(ctx.pathParam("notaVentaId"));
        try {
            ctx.json(dao.obtenerDetallesVenta(notaVentaId));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

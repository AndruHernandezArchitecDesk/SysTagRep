package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.ComprobanteDAO;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class ComprobanteApiController {

    private final ComprobanteDAO dao;

    public ComprobanteApiController() {
        this(AppContext.getInstance());
    }

    public ComprobanteApiController(AppContext ctx) {
        this.dao = ctx.comprobanteDAO;
    }

    public void insertar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String claveAcceso = (String) body.get("claveAcceso");
            Integer idRelacionado = body.get("idRelacionado") != null ? ((Number) body.get("idRelacionado")).intValue() : null;
            String numeroComprobante = (String) body.get("numeroComprobante");
            String ambiente = (String) body.get("ambiente");
            String xmlGenerado = (String) body.get("xmlGenerado");
            String tipoComprobante = (String) body.getOrDefault("tipoComprobante", "FACTURA");
            dao.insertar(claveAcceso, idRelacionado, numeroComprobante, ambiente, xmlGenerado, tipoComprobante);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void actualizarEstado(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String claveAcceso = (String) body.get("claveAcceso");
            String estado = (String) body.get("estado");
            String mensaje = (String) body.getOrDefault("mensaje", "");
            String xmlAutorizado = (String) body.getOrDefault("xmlAutorizado", "");
            String numeroAutorizacion = (String) body.getOrDefault("numeroAutorizacion", "");
            String fechaAutorizacion = (String) body.getOrDefault("fechaAutorizacion", "");
            dao.actualizarEstado(claveAcceso, estado, mensaje, xmlAutorizado, numeroAutorizacion, fechaAutorizacion);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void guardarEnvio(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String claveAcceso = (String) body.get("claveAcceso");
            String numeroComprobante = (String) body.get("numeroComprobante");
            String ambiente = (String) body.get("ambiente");
            String xmlEnviado = (String) body.get("xmlEnviado");
            String respuestaRecepcion = (String) body.get("respuestaRecepcion");
            String respuestaAutorizacion = (String) body.get("respuestaAutorizacion");
            String estado = (String) body.get("estado");
            String mensaje = (String) body.getOrDefault("mensaje", "");
            String numeroAutorizacion = (String) body.getOrDefault("numeroAutorizacion", "");
            String fechaAutorizacion = (String) body.getOrDefault("fechaAutorizacion", "");
            String tipoComprobante = (String) body.getOrDefault("tipoComprobante", "FACTURA");
            dao.guardarEnvio(claveAcceso, numeroComprobante, ambiente, xmlEnviado, respuestaRecepcion, respuestaAutorizacion, estado, mensaje, numeroAutorizacion, fechaAutorizacion, tipoComprobante);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerSecuencial(Context ctx) {
        String tipo = ctx.pathParam("tipo");
        try {
            int sec = dao.obtenerSecuencial(tipo);
            ctx.json(Map.of("secuencial", sec));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

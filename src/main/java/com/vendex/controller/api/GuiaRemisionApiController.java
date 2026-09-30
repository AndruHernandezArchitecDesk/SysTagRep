package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.GuiaRemisionRegistroDAO;
import com.vendex.model.GuiaRemisionRegistro;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuiaRemisionApiController {

    private final GuiaRemisionRegistroDAO dao;

    public GuiaRemisionApiController() {
        this(AppContext.getInstance());
    }

    public GuiaRemisionApiController(AppContext ctx) {
        this.dao = ctx.guiaRemisionRegistroDAO;
    }

    public void listar(Context ctx) {
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        try {
            List<GuiaRemisionRegistro> all = dao.listarPendientesSri();
            int from = (page - 1) * size;
            int to = Math.min(from + size, all.size());
            List<GuiaRemisionRegistro> pageItems = from < all.size() ? all.subList(from, to) : List.of();
            List<Map<String, Object>> out = new ArrayList<>();
            for (GuiaRemisionRegistro g : pageItems) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", g.getId());
                m.put("claveAcceso", g.getClaveAcceso());
                m.put("numComprobante", g.getNumComprobante());
                m.put("fechaEmision", g.getFechaEmision() != null ? g.getFechaEmision().toString() : null);
                m.put("estadoSri", g.getEstadoSri());
                out.add(m);
            }
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("items", out);
            resp.put("total", all.size());
            resp.put("page", page);
            resp.put("size", size);
            ctx.json(resp);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        GuiaRemisionRegistro g = dao.obtenerPorId(id);
        if (g == null) ctx.status(404).json(Map.of("error", "Guía de remisión no encontrada"));
        else {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", g.getId());
            resp.put("claveAcceso", g.getClaveAcceso());
            resp.put("numComprobante", g.getNumComprobante());
            resp.put("fechaEmision", g.getFechaEmision() != null ? g.getFechaEmision().toString() : null);
            resp.put("estadoSri", g.getEstadoSri());
            ctx.json(resp);
        }
    }

    public void insertar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            GuiaRemisionRegistro g = new GuiaRemisionRegistro();
            g.setClaveAcceso((String) body.get("claveAcceso"));
            g.setEstablecimiento((String) body.getOrDefault("establecimiento", "001"));
            g.setPuntoEmision((String) body.getOrDefault("puntoEmision", "001"));
            g.setSecuencial((String) body.get("secuencial"));
            String fechaStr = (String) body.get("fechaEmision");
            if (fechaStr != null) g.setFechaEmision(java.time.LocalDateTime.parse(fechaStr));
            g.setEstadoSri((String) body.getOrDefault("estadoSri", "PENDIENTE"));
            g.setSucursalId(com.vendex.util.SucursalActual.getId());
            int id = dao.insertar(g);
            ctx.json(Map.of("id", id));
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
            String numeroAutorizacion = (String) body.getOrDefault("numeroAutorizacion", "");
            String fechaAutorizacion = (String) body.getOrDefault("fechaAutorizacion", "");
            dao.actualizarEstado(claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorClave(Context ctx) {
        String clave = ctx.queryParam("claveAcceso");
        if (clave == null || clave.isBlank()) { ctx.status(400).json(Map.of("error", "claveAcceso requerida")); return; }
        GuiaRemisionRegistro g = dao.obtenerPorClave(clave);
        if (g == null) ctx.status(404).json(Map.of("error", "Guía de remisión no encontrada"));
        else ctx.json(Map.of(
                "id", g.getId(),
                "claveAcceso", g.getClaveAcceso(),
                "numComprobante", g.getNumComprobante(),
                "fechaEmision", g.getFechaEmision() != null ? g.getFechaEmision().toString() : null,
                "estadoSri", g.getEstadoSri()
        ));
    }

    public void listarPendientesSri(Context ctx) {
        try {
            List<GuiaRemisionRegistro> base = dao.listarPendientesSri();
            List<Map<String, Object>> out = new ArrayList<>();
            for (GuiaRemisionRegistro g : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", g.getId());
                m.put("claveAcceso", g.getClaveAcceso());
                m.put("numComprobante", g.getNumComprobante());
                m.put("fechaEmision", g.getFechaEmision() != null ? g.getFechaEmision().toString() : null);
                m.put("estadoSri", g.getEstadoSri());
                out.add(m);
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

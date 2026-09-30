package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.RetencionRegistroDAO;
import com.vendex.model.RetencionRegistro;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RetencionApiController {

    private final RetencionRegistroDAO dao;

    public RetencionApiController() {
        this(AppContext.getInstance());
    }

    public RetencionApiController(AppContext ctx) {
        this.dao = ctx.retencionRegistroDAO;
    }

    public void listar(Context ctx) {
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        try {
            List<RetencionRegistro> all = dao.listarPendientesSri();
            int from = (page - 1) * size;
            int to = Math.min(from + size, all.size());
            List<RetencionRegistro> pageItems = from < all.size() ? all.subList(from, to) : List.of();
            List<Map<String, Object>> out = new ArrayList<>();
            for (RetencionRegistro r : pageItems) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("claveAcceso", r.getClaveAcceso());
                m.put("numComprobante", r.getNumComprobante());
                m.put("fechaEmision", r.getFechaEmision() != null ? r.getFechaEmision().toString() : null);
                m.put("estadoSri", r.getEstadoSri());
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
        RetencionRegistro r = dao.obtenerPorId(id);
        if (r == null) ctx.status(404).json(Map.of("error", "Retención no encontrada"));
        else ctx.json(Map.of(
                "id", r.getId(),
                "claveAcceso", r.getClaveAcceso(),
                "numComprobante", r.getNumComprobante(),
                "fechaEmision", r.getFechaEmision() != null ? r.getFechaEmision().toString() : null,
                "estadoSri", r.getEstadoSri()
        ));
    }

    public void insertar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            RetencionRegistro r = new RetencionRegistro();
            r.setClaveAcceso((String) body.get("claveAcceso"));
            r.setEstablecimiento((String) body.getOrDefault("establecimiento", "001"));
            r.setPuntoEmision((String) body.getOrDefault("puntoEmision", "001"));
            r.setSecuencial((String) body.get("secuencial"));
            String fechaStr = (String) body.get("fechaEmision");
            if (fechaStr != null) r.setFechaEmision(java.time.LocalDateTime.parse(fechaStr));
            r.setEstadoSri((String) body.getOrDefault("estadoSri", "PENDIENTE"));
            r.setSucursalId(com.vendex.util.SucursalActual.getId());
            int id = dao.insertar(r);
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
        RetencionRegistro r = dao.obtenerPorClave(clave);
        if (r == null) ctx.status(404).json(Map.of("error", "Retención no encontrada"));
        else ctx.json(Map.of(
                "id", r.getId(),
                "claveAcceso", r.getClaveAcceso(),
                "numComprobante", r.getNumComprobante(),
                "fechaEmision", r.getFechaEmision() != null ? r.getFechaEmision().toString() : null,
                "estadoSri", r.getEstadoSri()
        ));
    }

    public void listarPendientesSri(Context ctx) {
        try {
            List<RetencionRegistro> base = dao.listarPendientesSri();
            List<Map<String, Object>> out = new ArrayList<>();
            for (RetencionRegistro r : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("claveAcceso", r.getClaveAcceso());
                m.put("numComprobante", r.getNumComprobante());
                m.put("fechaEmision", r.getFechaEmision() != null ? r.getFechaEmision().toString() : null);
                m.put("estadoSri", r.getEstadoSri());
                out.add(m);
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

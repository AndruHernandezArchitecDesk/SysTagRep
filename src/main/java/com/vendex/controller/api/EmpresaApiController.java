package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.EmpresaDAO;
import com.vendex.model.Empresa;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmpresaApiController {
    private static final int MAX_LOGO_BYTES = 5 * 1024 * 1024;

    private final EmpresaDAO dao;

    public EmpresaApiController() {
        this(AppContext.getInstance());
    }

    public EmpresaApiController(AppContext ctx) {
        this.dao = ctx.empresaDAO;
    }

    public EmpresaApiController(EmpresaDAO dao) {
        this.dao = java.util.Objects.requireNonNull(dao, "dao");
    }

    public void listar(Context ctx) {
        try {
            List<Empresa> base = dao.listar();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Empresa e : base) {
                out.add(aMapa(e));
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Empresa e = dao.obtenerPorId(id);
        if (e == null) ctx.status(404).json(Map.of("error", "Empresa no encontrada"));
        else ctx.json(aMapa(e));
    }

    private static Map<String, Object> aMapa(Empresa e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("razonSocial", e.getRazonSocial());
        m.put("ruc", e.getRuc());
        m.put("titulo", e.getTitulo());
        m.put("sucursal", e.getSucursal());
        m.put("direccionCallePrincipal", e.getDireccionCallePrincipal());
        m.put("direccionCalleSecundaria", e.getDireccionCalleSecundaria());
        m.put("telefono", e.getTelefono());
        m.put("celular", e.getCelular());
        m.put("correo", e.getCorreo());
        m.put("logoUrl", e.getLogoUrl());
        m.put("logoBytes", e.getLogoBytes());
        m.put("agenteRetencion", e.getAgenteRetencion());
        m.put("resolucion", e.getResolucion());
        m.put("obligadoContabilidad", e.getObligadoContabilidad());
        m.put("estado", e.isEstado());
        return m;
    }

    private static void aplicarMapa(Empresa e, Map<String, Object> body) {
        if (body.get("razonSocial") instanceof String s) e.setRazonSocial(s);
        if (body.get("ruc") instanceof String s) e.setRuc(s);
        if (body.get("titulo") instanceof String s) e.setTitulo(s);
        if (body.get("sucursal") instanceof String s) e.setSucursal(s);
        if (body.get("direccionCallePrincipal") instanceof String s) e.setDireccionCallePrincipal(s);
        if (body.get("direccionCalleSecundaria") instanceof String s) e.setDireccionCalleSecundaria(s);
        if (body.get("telefono") instanceof String s) e.setTelefono(s);
        if (body.get("celular") instanceof String s) e.setCelular(s);
        if (body.get("correo") instanceof String s) e.setCorreo(s);
        if (body.get("logoUrl") instanceof String s) e.setLogoUrl(s);
        if (body.get("logoBytes") instanceof String s) {
            long maxBase64Length = ((long) MAX_LOGO_BYTES + 2) / 3 * 4;
            if (s.length() > maxBase64Length)
                throw new IllegalArgumentException("El logo no puede superar 5 MB.");
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(s);
                if (bytes.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("El logo no puede superar 5 MB.");
                e.setLogoBytes(bytes);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Los bytes del logo no contienen Base64 válido o superan 5 MB.", ex);
            }
        }
        // null significa que este cliente no está cambiando el logo; conservar el guardado.
        if (body.get("agenteRetencion") instanceof String s) e.setAgenteRetencion(s);
        if (body.get("resolucion") instanceof String s) e.setResolucion(s);
        if (body.get("obligadoContabilidad") instanceof String s) e.setObligadoContabilidad(s);
        if (body.get("estado") instanceof Boolean b) e.setEstado(b);
    }

    public void actualizar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int id = ((Number) body.get("id")).intValue();
            Empresa e = dao.obtenerPorId(id);
            if (e == null) { ctx.status(404).json(Map.of("error", "Empresa no encontrada")); return; }
            aplicarMapa(e, body);
            if (!dao.actualizar(e)) { ctx.status(500).json(Map.of("error", "No se pudo actualizar la empresa")); return; }
            ctx.json(Map.of("ok", true));
        } catch (Exception e2) {
            ctx.status(500).json(Map.of("error", e2.getMessage() != null ? e2.getMessage() : "Error interno"));
        }
    }

    public void crear(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Empresa e = new Empresa();
            e.setEstado(true);
            aplicarMapa(e, body);
            Empresa creada = dao.crear(e);
            if (creada == null) { ctx.status(500).json(Map.of("error", "No se pudo crear la empresa")); return; }
            ctx.status(201).json(aMapa(creada));
        } catch (Exception e2) {
            ctx.status(500).json(Map.of("error", e2.getMessage() != null ? e2.getMessage() : "Error interno"));
        }
    }
}

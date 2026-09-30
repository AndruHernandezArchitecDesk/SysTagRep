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

    private final EmpresaDAO dao;

    public EmpresaApiController() {
        this(AppContext.getInstance());
    }

    public EmpresaApiController(AppContext ctx) {
        this.dao = ctx.empresaDAO;
    }

    public void listar(Context ctx) {
        try {
            List<Empresa> base = dao.listar();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Empresa e : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.getId());
                m.put("razonSocial", e.getRazonSocial());
                m.put("ruc", e.getRuc());
                m.put("direccionCallePrincipal", e.getDireccionCallePrincipal());
                m.put("direccionCalleSecundaria", e.getDireccionCalleSecundaria());
                m.put("telefono", e.getTelefono());
                m.put("celular", e.getCelular());
                m.put("correo", e.getCorreo());
                out.add(m);
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
        else ctx.json(Map.of(
                "id", e.getId(),
                "razonSocial", e.getRazonSocial(),
                "ruc", e.getRuc(),
                "direccionCallePrincipal", e.getDireccionCallePrincipal(),
                "direccionCalleSecundaria", e.getDireccionCalleSecundaria(),
                "telefono", e.getTelefono(),
                "celular", e.getCelular(),
                "correo", e.getCorreo()
        ));
    }

    public void actualizar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int id = ((Number) body.get("id")).intValue();
            Empresa e = dao.obtenerPorId(id);
            if (e == null) { ctx.status(404).json(Map.of("error", "Empresa no encontrada")); return; }
            e.setRazonSocial((String) body.get("razonSocial"));
            e.setRuc((String) body.get("ruc"));
            e.setDireccionCallePrincipal((String) body.get("direccionCallePrincipal"));
            e.setDireccionCalleSecundaria((String) body.get("direccionCalleSecundaria"));
            e.setTelefono((String) body.get("telefono"));
            e.setCelular((String) body.get("celular"));
            e.setCorreo((String) body.get("correo"));
            dao.actualizar(e);
            ctx.json(Map.of("ok", true));
        } catch (Exception e2) {
            ctx.status(500).json(Map.of("error", e2.getMessage() != null ? e2.getMessage() : "Error interno"));
        }
    }
}

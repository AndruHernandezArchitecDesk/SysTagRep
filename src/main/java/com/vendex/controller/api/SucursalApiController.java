package com.vendex.controller.api;

import com.vendex.dao.SucursalDAO;
import com.vendex.model.Sucursal;
import io.javalin.http.Context;

import java.util.Map;

public class SucursalApiController {

    private final SucursalDAO dao;

    public SucursalApiController(SucursalDAO dao) { this.dao = dao; }

    public void listar(Context ctx) {
        ctx.json(dao.listar());
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var opt = dao.obtenerPorId(id);
        if (opt.isEmpty()) ctx.status(404).json(Map.of("error", "Sucursal no encontrada"));
        else ctx.json(opt.get());
    }

    public void crear(Context ctx) {
        Sucursal s = ctx.bodyAsClass(Sucursal.class);
        if (s.getCodigo() == null || s.getCodigo().trim().isEmpty() || s.getNombre() == null || s.getNombre().trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "codigo y nombre son obligatorios"));
            return;
        }
        if (dao.obtenerPorCodigo(s.getCodigo()).isPresent()) {
            ctx.status(409).json(Map.of("error", "Código ya existe"));
            return;
        }
        int id = dao.guardar(s);
        s.setId(id);
        ctx.status(201).json(s);
    }
}

package com.vendex.controller.api;

import com.vendex.dao.InventarioDAO;
import io.javalin.http.Context;

import java.util.Map;

public class InventarioApiController {

    private final InventarioDAO dao;

    public InventarioApiController(InventarioDAO dao) { this.dao = dao; }

    public void listar(Context ctx) {
        String sucursalIdStr = ctx.queryParam("sucursalId");
        String q = ctx.queryParam("q");
        String numeroFactura = ctx.queryParam("numeroFactura");
        if (sucursalIdStr != null && !sucursalIdStr.isBlank()) {
            try {
                int sucursalId = Integer.parseInt(sucursalIdStr);
                var lista = dao.listarPorSucursal(sucursalId);
                // Filtro opcional q en memoria si viene
                if (q != null && !q.isBlank()) {
                    String f = q.toLowerCase();
                    lista = lista.stream().filter(i ->
                            (i.getDescripcion() != null && i.getDescripcion().toLowerCase().contains(f)) ||
                            (i.getCodigo() != null && i.getCodigo().toLowerCase().contains(f))
                    ).toList();
                }
                ctx.json(lista);
                return;
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "sucursalId debe ser numérico"));
                return;
            }
        }
        // Sin sucursalId: paginado global
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        ctx.json(dao.listarPaginado(page, size, q, numeroFactura));
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var inv = dao.obtenerPorId(id);
        if (inv == null) ctx.status(404).json(Map.of("error", "Inventario no encontrado"));
        else ctx.json(inv);
    }
}

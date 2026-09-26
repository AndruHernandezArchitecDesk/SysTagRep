package com.vendex.controller.api;

import com.vendex.dao.InventarioDAO;
import com.vendex.dao.InventarioVehiculoDAO;
import com.vendex.dao.VehiculoDAO;
import com.vendex.model.Inventario;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InventarioApiController {

    private final InventarioDAO dao;
    private final VehiculoDAO vehiculoDAO;
    private final InventarioVehiculoDAO inventarioVehiculoDAO;

    public InventarioApiController(InventarioDAO dao) {
        this(dao, null, null);
    }

    public InventarioApiController(InventarioDAO dao, VehiculoDAO vehiculoDAO, InventarioVehiculoDAO inventarioVehiculoDAO) {
        this.dao = dao;
        this.vehiculoDAO = vehiculoDAO;
        this.inventarioVehiculoDAO = inventarioVehiculoDAO;
    }

    /** IDs de inventario asociados al filtro de vehículo; null si no hay filtro. */
    private Set<Integer> idsFiltroVehiculo(Context ctx) {
        String marca = ctx.queryParam("vehMarca");
        String modelo = ctx.queryParam("vehModelo");
        String anioStr = ctx.queryParam("vehAnio");
        if ((marca == null || marca.isBlank()) && (modelo == null || modelo.isBlank()) && (anioStr == null || anioStr.isBlank())) return null;
        if (vehiculoDAO == null || inventarioVehiculoDAO == null) return Set.of();
        Integer anio = null;
        try { if (anioStr != null && !anioStr.isBlank()) anio = Integer.parseInt(anioStr.trim()); } catch (NumberFormatException e) {
            ctx.status(400).json(Map.of("error", "vehAnio debe ser numérico"));
            return Set.of();
        }
        try {
            java.util.Set<Integer> ids = new java.util.HashSet<>();
            for (var v : vehiculoDAO.buscar(marca, modelo, anio)) {
                ids.addAll(inventarioVehiculoDAO.listarInventarioIdsPorVehiculo(v.getId()));
            }
            return ids;
        } catch (Exception e) {
            return Set.of();
        }
    }

    public void listar(Context ctx) {
        String sucursalIdStr = ctx.queryParam("sucursalId");
        String q = ctx.queryParam("q");
        String numeroFactura = ctx.queryParam("numeroFactura");
        Set<Integer> idsVeh = idsFiltroVehiculo(ctx);
        if (sucursalIdStr != null && !sucursalIdStr.isBlank()) {
            try {
                int sucursalId = Integer.parseInt(sucursalIdStr);
                var lista = dao.listarPorSucursal(sucursalId);
                ctx.json(filtrar(lista, q, numeroFactura, idsVeh));
                return;
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "sucursalId debe ser numérico"));
                return;
            }
        }
        // Sin sucursalId: si hay filtro de vehículo, listar completo y paginar en memoria
        if (idsVeh != null) {
            List<Inventario> base = dao.listar();
            List<Inventario> filtrado = filtrar(base, q, null, idsVeh);
            String pageStr = ctx.queryParam("page");
            String sizeStr = ctx.queryParam("size");
            int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
            int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
            int from = (page - 1) * size;
            if (from >= filtrado.size()) { ctx.json(List.of()); return; }
            ctx.json(filtrado.subList(from, Math.min(from + size, filtrado.size())));
            return;
        }
        // Sin sucursalId: paginado global
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        ctx.json(dao.listarPaginado(page, size, q, numeroFactura));
    }

    private List<Inventario> filtrar(List<Inventario> lista, String q, String numeroFactura, Set<Integer> idsVeh) {
        List<Inventario> result = lista;
        if (q != null && !q.isBlank()) {
            String f = q.toLowerCase();
            result = result.stream().filter(i ->
                    (i.getDescripcion() != null && i.getDescripcion().toLowerCase().contains(f)) ||
                    (i.getCodigo() != null && i.getCodigo().toLowerCase().contains(f))
            ).toList();
        }
        if (numeroFactura != null && !numeroFactura.isBlank()) {
            String nf = numeroFactura.toLowerCase();
            result = result.stream().filter(i -> i.getNumeroFactura() != null && i.getNumeroFactura().toLowerCase().contains(nf)).toList();
        }
        if (idsVeh != null) {
            result = result.stream().filter(i -> idsVeh.contains(i.getId())).toList();
        }
        return result;
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var inv = dao.obtenerPorId(id);
        if (inv == null) ctx.status(404).json(Map.of("error", "Inventario no encontrado"));
        else ctx.json(inv);
    }
}

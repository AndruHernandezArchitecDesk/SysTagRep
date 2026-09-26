package com.vendex.controller.api;

import com.vendex.dao.InventarioVehiculoDAO;
import com.vendex.dao.VehiculoDAO;
import com.vendex.model.Vehiculo;
import com.vendex.service.VehiculoImportService;
import io.javalin.http.Context;

import java.util.Map;

public class VehiculoApiController {

    private final VehiculoDAO vehiculoDAO;
    private final InventarioVehiculoDAO ivDAO;
    private final VehiculoImportService importService;

    public VehiculoApiController(VehiculoDAO vehiculoDAO, InventarioVehiculoDAO ivDAO) {
        this.vehiculoDAO = vehiculoDAO;
        this.ivDAO = ivDAO;
        this.importService = new VehiculoImportService(vehiculoDAO);
    }

    public void listar(Context ctx) {
        String marca = ctx.queryParam("marca");
        String modelo = ctx.queryParam("modelo");
        String anioStr = ctx.queryParam("anio");
        Integer anio = null;
        try { if (anioStr != null) anio = Integer.parseInt(anioStr); } catch (NumberFormatException ignore) {}
        if (marca != null || modelo != null || anio != null) {
            ctx.json(vehiculoDAO.buscar(marca, modelo, anio));
        } else {
            String q = ctx.queryParam("q");
            if (q != null && !q.isBlank()) {
                ctx.json(vehiculoDAO.buscar(q, q, null));
            } else {
                ctx.json(vehiculoDAO.listar());
            }
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var opt = vehiculoDAO.obtenerPorId(id);
        if (opt.isEmpty()) ctx.status(404).json(Map.of("error", "Vehículo no encontrado"));
        else ctx.json(opt.get());
    }

    public void crear(Context ctx) {
        Vehiculo v = ctx.bodyAsClass(Vehiculo.class);
        if (v.getMarca() == null || v.getMarca().isBlank() || v.getModelo() == null || v.getModelo().isBlank()) {
            ctx.status(400).json(Map.of("error", "marca y modelo obligatorios"));
            return;
        }
        int id = vehiculoDAO.guardar(v);
        v.setId(id);
        ctx.status(201).json(v);
    }

    public void buscarPorVin(Context ctx) {
        String vin = ctx.pathParam("vin");
        if (vin == null || vin.length() != 17) {
            ctx.status(400).json(Map.of("error", "VIN debe tener 17 caracteres"));
            return;
        }
        var opt = vehiculoDAO.buscarPorVin(vin);
        if (opt.isEmpty()) {
            // Intentar importar desde NHTSA
            int imported = importService.importarVin(vin);
            if (imported > 0) {
                var opt2 = vehiculoDAO.buscarPorVin(vin);
                if (opt2.isPresent()) { ctx.json(opt2.get()); return; }
            }
            ctx.status(404).json(Map.of("error", "Vehículo no encontrado para VIN"));
        } else {
            ctx.json(opt.get());
        }
    }

    public void importar(Context ctx) {
        String make = ctx.queryParam("make");
        String limitStr = ctx.queryParam("limit");
        int limit = 5;
        try { if (limitStr != null) limit = Integer.parseInt(limitStr); } catch (NumberFormatException ignore) {}
        if (make != null && !make.isBlank()) {
            int c = importService.importarModelosParaMake(make, limit);
            ctx.json(Map.of("importados", c, "make", make));
        } else {
            int c = importService.importarMakes(limit, true);
            ctx.json(Map.of("importados", c));
        }
    }

    public void listarCompatibilidades(Context ctx) {
        int inventarioId = Integer.parseInt(ctx.pathParam("id"));
        ctx.json(ivDAO.listarVehiculosPorInventario(inventarioId));
    }

    public void asociarCompatibilidades(Context ctx) {
        int inventarioId = Integer.parseInt(ctx.pathParam("id"));
        var body = ctx.bodyAsClass(java.util.Map.class);
        var ids = (java.util.List<Integer>) body.get("vehiculoIds");
        if (ids == null) ids = java.util.List.of();
        ivDAO.asociarMultiple(inventarioId, ids);
        ctx.json(Map.of("ok", true, "vehiculoIds", ids));
    }
}

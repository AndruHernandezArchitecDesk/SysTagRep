package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.FacturaDetalleDAO;
import com.vendex.model.FacturaDetalle;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FacturaDetalleApiController {

    private final FacturaDetalleDAO dao;

    public FacturaDetalleApiController() {
        this(AppContext.getInstance());
    }

    public FacturaDetalleApiController(AppContext ctx) {
        this.dao = ctx.facturaDetalleDAO;
    }

    public void insertar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int facturaRegistroId = ((Number) body.get("facturaRegistroId")).intValue();
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) body.get("detalles");
            List<FacturaDetalle> detalles = new ArrayList<>();
            for (Map<String, Object> it : itemsRaw) {
                FacturaDetalle d = new FacturaDetalle();
                d.setCodigo((String) it.get("codigo"));
                d.setDescripcion((String) it.get("descripcion"));
                d.setCantidad(((Number) it.get("cantidad")).intValue());
                d.setPrecioUnitario(new java.math.BigDecimal(it.get("precioUnitario").toString()));
                detalles.add(d);
            }
            dao.insertarDetalle(facturaRegistroId, detalles);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void listarPorFactura(Context ctx) {
        int facturaRegistroId = Integer.parseInt(ctx.pathParam("facturaRegistroId"));
        try {
            List<FacturaDetalle> base = dao.listarPorFacturaRegistroId(facturaRegistroId);
            List<Map<String, Object>> out = new ArrayList<>();
            for (FacturaDetalle d : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("codigo", d.getCodigo());
                m.put("descripcion", d.getDescripcion());
                m.put("cantidad", d.getCantidad());
                m.put("precioUnitario", d.getPrecioUnitario() != null ? d.getPrecioUnitario().toString() : "0");
                m.put("precioTotal", d.getPrecioTotal() != null ? d.getPrecioTotal().toString() : "0");
                out.add(m);
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

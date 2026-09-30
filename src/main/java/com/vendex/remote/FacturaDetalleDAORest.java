package com.vendex.remote;

import com.vendex.dao.FacturaDetalleDAO;
import com.vendex.model.FacturaDetalle;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FacturaDetalleDAORest extends RestDao<FacturaDetalle> implements FacturaDetalleDAO {

    public FacturaDetalleDAORest(RestClient rest) {
        super(rest, "/facturas-detalle", FacturaDetalle.class);
    }

    @Override
    public void insertarDetalle(int facturaRegistroId, List<FacturaDetalle> detalles) {
        try {
            List<Map<String, Object>> items = new ArrayList<>();
            for (FacturaDetalle d : detalles) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("codigo", d.getCodigo());
                m.put("descripcion", d.getDescripcion());
                m.put("cantidad", d.getCantidad());
                m.put("precioUnitario", d.getPrecioUnitario() != null ? d.getPrecioUnitario().toString() : "0");
                items.add(m);
            }
            Map<String, Object> body = Map.of(
                    "facturaRegistroId", facturaRegistroId,
                    "detalles", items
            );
            rest.post("/api/facturas/" + facturaRegistroId + "/detalles", body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error insertando detalle factura vía REST", e);
        }
    }

    @Override
    public void insertarDetalle(Connection con, int facturaRegistroId, List<FacturaDetalle> detalles) {
        insertarDetalle(facturaRegistroId, detalles);
    }

    @Override
    public List<FacturaDetalle> listarPorFacturaRegistroId(int facturaRegistroId) {
        try {
            return rest.get("/api/facturas/" + facturaRegistroId + "/detalles", new com.fasterxml.jackson.core.type.TypeReference<List<FacturaDetalle>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean existeVentaPorInventarioIds(List<Integer> inventarioIds) {
        throw new UnsupportedOperationException("FacturaDetalle.existeVentaPorInventarioIds rest");
    }
}

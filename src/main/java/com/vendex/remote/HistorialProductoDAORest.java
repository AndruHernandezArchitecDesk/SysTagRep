package com.vendex.remote;

import com.vendex.dao.HistorialProductoDAO;
import com.vendex.model.HistorialProducto;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class HistorialProductoDAORest extends RestDao<HistorialProducto> implements HistorialProductoDAO {

    public HistorialProductoDAORest(RestClient rest) {
        super(rest, "/historial-producto", HistorialProducto.class);
    }

    @Override
    public void insertar(List<HistorialProducto> lista) {
        try {
            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (HistorialProducto h : lista) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("inventarioId", h.getProductoId());
                m.put("codigo", h.getProductoCodigo());
                m.put("descripcion", h.getProductoDescripcion());
                m.put("cantidad", h.getCantidad());
                m.put("precioUnitario", h.getPrecioUnitario() != null ? h.getPrecioUnitario().toString() : "0");
                m.put("tipoMovimiento", h.getTipoComprobante());
                m.put("numeroComprobante", h.getCodigoComprobante());
                m.put("clienteNombre", h.getClienteNombre());
                m.put("proveedorNombre", h.getProveedorNombre());
                items.add(m);
            }
            rest.post("/api/historial-producto", items, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error insertando historial producto vía REST", e);
        }
    }

    @Override
    public void insertar(Connection con, List<HistorialProducto> lista) {
        insertar(lista);
    }

    @Override
    public List<HistorialProducto> listar() {
        try {
            return rest.get("/api/historial-producto", new com.fasterxml.jackson.core.type.TypeReference<List<HistorialProducto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<HistorialProducto> listarPorFecha(LocalDate fecha) {
        try {
            return rest.get("/api/historial-producto?fecha=" + fecha.toString(), new com.fasterxml.jackson.core.type.TypeReference<List<HistorialProducto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean existeVentaPorInventarioIds(List<Integer> inventarioIds) {
        throw new UnsupportedOperationException("HistorialProducto.existeVentaPorInventarioIds rest");
    }
}

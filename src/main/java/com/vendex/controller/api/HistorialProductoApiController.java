package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.HistorialProductoDAO;
import com.vendex.model.HistorialProducto;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistorialProductoApiController {

    private final HistorialProductoDAO dao;

    public HistorialProductoApiController() {
        this(AppContext.getInstance());
    }

    public HistorialProductoApiController(AppContext ctx) {
        this.dao = ctx.historialProductoDAO;
    }

    public void insertar(Context ctx) {
        try {
            List<Map<String, Object>> body = ctx.bodyAsClass(List.class);
            List<HistorialProducto> lista = new java.util.ArrayList<>();
            for (Map<String, Object> it : body) {
                HistorialProducto h = new HistorialProducto();
                h.setProductoId(((Number) it.get("inventarioId")).intValue());
                h.setProductoCodigo((String) it.get("codigo"));
                h.setProductoDescripcion((String) it.get("descripcion"));
                h.setCantidad(((Number) it.get("cantidad")).intValue());
                h.setPrecioUnitario(new java.math.BigDecimal(it.get("precioUnitario").toString()));
                h.setTipoComprobante((String) it.get("tipoMovimiento"));
                h.setCodigoComprobante((String) it.get("numeroComprobante"));
                h.setClienteNombre((String) it.get("clienteNombre"));
                h.setProveedorNombre((String) it.get("proveedorNombre"));
                h.setFechaVenta(java.time.LocalDateTime.now());
                lista.add(h);
            }
            dao.insertar(lista);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void listarPorFecha(Context ctx) {
        String fechaStr = ctx.queryParam("fecha");
        LocalDate fecha = fechaStr != null ? LocalDate.parse(fechaStr) : LocalDate.now();
        try {
            List<HistorialProducto> base = dao.listarPorFecha(fecha);
            ctx.json(base);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

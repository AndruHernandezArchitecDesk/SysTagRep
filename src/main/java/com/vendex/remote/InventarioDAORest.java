package com.vendex.remote;

import com.vendex.dao.InventarioDAO;
import com.vendex.model.Inventario;
import com.vendex.util.SesionActual;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class InventarioDAORest extends RestDao<Inventario> implements InventarioDAO {

    public InventarioDAORest(RestClient rest) {
        super(rest, "/inventario", Inventario.class);
    }

    @Override
    public List<Inventario> listar() {
        try {
            return rest.get("/api/inventario", new com.fasterxml.jackson.core.type.TypeReference<List<Inventario>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Inventario> listarPaginado(int page, int pageSize, String filtro) {
        try {
            String path = "/api/inventario?page=" + page + "&size=" + pageSize + (filtro != null && !filtro.isBlank() ? "&q=" + URLEncoder.encode(filtro, StandardCharsets.UTF_8) : "");
            return rest.get(path, new com.fasterxml.jackson.core.type.TypeReference<List<Inventario>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Inventario> listarPaginado(int page, int pageSize, String filtro, String numeroFactura) {
        try {
            String path = "/api/inventario?page=" + page + "&size=" + pageSize + (filtro != null && !filtro.isBlank() ? "&q=" + URLEncoder.encode(filtro, StandardCharsets.UTF_8) : "") + (numeroFactura != null && !numeroFactura.isBlank() ? "&numeroFactura=" + URLEncoder.encode(numeroFactura, StandardCharsets.UTF_8) : "");
            return rest.get(path, new com.fasterxml.jackson.core.type.TypeReference<List<Inventario>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public int contar(String filtro) {
        throw new UnsupportedOperationException("Inventario.contar rest");
    }

    @Override
    public int contar(String filtro, String numeroFactura) {
        throw new UnsupportedOperationException("Inventario.contar rest");
    }

    @Override
    public List<Inventario> listarPorRango(LocalDate desde, LocalDate hasta) {
        throw new UnsupportedOperationException("Inventario.listarPorRango rest");
    }

    @Override
    public int guardar(Inventario inv) {
        try {
            Map<String, Object> body = Map.of(
                    "codigo", inv.getCodigo(),
                    "descripcion", inv.getDescripcion(),
                    "cantidad", inv.getCantidad(),
                    "precioCompra", inv.getCostoSinIVA() != null ? inv.getCostoSinIVA().toString() : "0",
                    "precioVenta", inv.getPrecioVenta() != null ? inv.getPrecioVenta().toString() : "0",
                    "sucursalId", SesionActual.getSucursalId()
            );
            Map<String, Object> resp = rest.post("/api/inventario", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error guardando inventario vía REST", e);
        }
    }

    @Override
    public void actualizar(Inventario inv) {
        try {
            rest.put("/api/inventario/" + inv.getId(), Map.of(
                    "id", inv.getId(),
                    "descripcion", inv.getDescripcion(),
                    "precioVenta", inv.getPrecioVenta() != null ? inv.getPrecioVenta().toString() : "0"
            ), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando inventario vía REST", e);
        }
    }

    @Override
    public void actualizarPrecioVenta(int id, BigDecimal precio) {
        try {
            rest.put("/api/inventario/" + id, Map.of("id", id, "precioVenta", precio.toString()), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando precio venta vía REST", e);
        }
    }

    @Override
    public void eliminar(int id) {
        try {
            rest.delete("/api/inventario/" + id);
        } catch (Exception e) {
            throw new RuntimeException("Error eliminando inventario vía REST", e);
        }
    }

    @Override
    public List<Inventario> listarPorNumeroFactura(String numeroFactura, int proveedorId) {
        throw new UnsupportedOperationException("Inventario.listarPorNumeroFactura rest");
    }

    @Override
    public void eliminarPorFactura(String numeroFactura, int proveedorId) {
        throw new UnsupportedOperationException("Inventario.eliminarPorFactura rest");
    }

    @Override
    public void eliminarFacturaConDependencias(String numeroFactura, int proveedorId) {
        throw new UnsupportedOperationException("Inventario.eliminarFacturaConDependencias rest");
    }

    @Override
    public String obtenerProveedorNombre(int productoId) {
        throw new UnsupportedOperationException("Inventario.obtenerProveedorNombre rest");
    }

    @Override
    public String obtenerProveedorNombre(Connection con, int productoId) {
        return obtenerProveedorNombre(productoId);
    }

    @Override
    public void descontarStock(int productoId, int cantidad) {
        try {
            rest.post("/api/inventario/" + productoId + "/stock/descontar?cantidad=" + cantidad, Map.of(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error descontando stock vía REST", e);
        }
    }

    @Override
    public void descontarStock(Connection con, int productoId, int cantidad) {
        descontarStock(productoId, cantidad);
    }

    @Override
    public void devolverStock(int productoId, BigDecimal cantidad) {
        try {
            rest.post("/api/inventario/" + productoId + "/stock/devolver?cantidad=" + cantidad, Map.of(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error devolviendo stock vía REST", e);
        }
    }

    @Override
    public void devolverStock(Connection con, int productoId, BigDecimal cantidad) {
        devolverStock(productoId, cantidad);
    }

    @Override
    public List<Inventario> listarStockBajo(int umbral) {
        throw new UnsupportedOperationException("Inventario.listarStockBajo rest");
    }

    @Override
    public List<Inventario> listarActivosConStock() {
        throw new UnsupportedOperationException("Inventario.listarActivosConStock rest");
    }

    @Override
    public Inventario obtenerPorId(int id) {
        try {
            return rest.get("/api/inventario/" + id, Inventario.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Inventario obtenerPorId(Connection con, int id) {
        return obtenerPorId(id);
    }

    @Override
    public Inventario obtenerPorCodigoYSucursal(String codigo, int sucursalId) {
        try {
            String path = "/api/inventario/codigo/" + URLEncoder.encode(codigo, StandardCharsets.UTF_8) + "/sucursal/" + sucursalId;
            return rest.get(path, Inventario.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Inventario obtenerPorCodigoYSucursal(Connection con, String codigo, int sucursalId) {
        return obtenerPorCodigoYSucursal(codigo, sucursalId);
    }

    @Override
    public List<Inventario> listarPorSucursal(int sucursalId) {
        try {
            return rest.get("/api/inventario/sucursal/" + sucursalId, new com.fasterxml.jackson.core.type.TypeReference<List<Inventario>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<String> buscarDescripciones(String filtro, int limit) {
        throw new UnsupportedOperationException("Inventario.buscarDescripciones rest");
    }
}

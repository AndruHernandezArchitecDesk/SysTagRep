package com.vendex.remote;

import com.vendex.dao.InventarioDAO;
import com.vendex.model.Inventario;
import java.sql.Connection;
import java.util.List;
import java.time.LocalDate;
import java.lang.String;

public class InventarioDAORest extends RestDao<Inventario> implements InventarioDAO {
    public InventarioDAORest(RestClient rest) { super(rest, "/inventario", Inventario.class); }
    public List<Inventario> listar() { throw new UnsupportedOperationException("Inventario.listar rest"); }
    public List<Inventario> listarPaginado(int page, int pageSize, String filtro) { throw new UnsupportedOperationException("Inventario.listarPaginado rest"); }
    public List<Inventario> listarPaginado(int page, int pageSize, String filtro, String numeroFactura) { throw new UnsupportedOperationException("Inventario.listarPaginado rest"); }
    public int contar(String filtro) { throw new UnsupportedOperationException("Inventario.contar rest"); }
    public int contar(String filtro, String numeroFactura) { throw new UnsupportedOperationException("Inventario.contar rest"); }
    public List<Inventario> listarPorRango(LocalDate desde, LocalDate hasta) { throw new UnsupportedOperationException("Inventario.listarPorRango rest"); }
    public int guardar(Inventario inv) { throw new UnsupportedOperationException("Inventario.guardar rest"); }
    public void actualizar(Inventario inv) { throw new UnsupportedOperationException("Inventario.actualizar rest"); }
    public void actualizarPrecioVenta(int id, java.math.BigDecimal precio) { throw new UnsupportedOperationException("Inventario.actualizarPrecioVenta rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Inventario.eliminar rest"); }
    public List<Inventario> listarPorNumeroFactura(String numeroFactura, int proveedorId) { throw new UnsupportedOperationException("Inventario.listarPorNumeroFactura rest"); }
    public void eliminarPorFactura(String numeroFactura, int proveedorId) { throw new UnsupportedOperationException("Inventario.eliminarPorFactura rest"); }
    public void eliminarFacturaConDependencias(String numeroFactura, int proveedorId) { throw new UnsupportedOperationException("Inventario.eliminarFacturaConDependencias rest"); }
    public String obtenerProveedorNombre(int productoId) { throw new UnsupportedOperationException("Inventario.obtenerProveedorNombre rest"); }
    public String obtenerProveedorNombre(Connection con, int productoId) { throw new UnsupportedOperationException("Inventario.obtenerProveedorNombre rest"); }
    public void descontarStock(int productoId, int cantidad) { throw new UnsupportedOperationException("Inventario.descontarStock rest"); }
    public void descontarStock(Connection con, int productoId, int cantidad) { throw new UnsupportedOperationException("Inventario.descontarStock rest"); }
    public void devolverStock(int productoId, java.math.BigDecimal cantidad) { throw new UnsupportedOperationException("Inventario.devolverStock rest"); }
    public void devolverStock(Connection con, int productoId, java.math.BigDecimal cantidad) { throw new UnsupportedOperationException("Inventario.devolverStock rest"); }
    public List<Inventario> listarStockBajo(int umbral) { throw new UnsupportedOperationException("Inventario.listarStockBajo rest"); }
    public List<Inventario> listarActivosConStock() { throw new UnsupportedOperationException("Inventario.listarActivosConStock rest"); }
    public Inventario obtenerPorId(int id) { throw new UnsupportedOperationException("Inventario.obtenerPorId rest"); }
    public Inventario obtenerPorId(Connection con, int id) { throw new UnsupportedOperationException("Inventario.obtenerPorId rest"); }
    public Inventario obtenerPorCodigoYSucursal(String codigo, int sucursalId) { throw new UnsupportedOperationException("Inventario.obtenerPorCodigoYSucursal rest"); }
    public Inventario obtenerPorCodigoYSucursal(Connection con, String codigo, int sucursalId) { throw new UnsupportedOperationException("Inventario.obtenerPorCodigoYSucursal rest"); }
    public List<Inventario> listarPorSucursal(int sucursalId) { throw new UnsupportedOperationException("Inventario.listarPorSucursal rest"); }
    public List<String> buscarDescripciones(String filtro, int limit) { throw new UnsupportedOperationException("Inventario.buscarDescripciones rest"); }
}
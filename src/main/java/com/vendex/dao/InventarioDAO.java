package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Inventario;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface InventarioDAO {

    List<Inventario> listar();

    List<Inventario> listarPaginado(int page, int pageSize, String filtro);

    List<Inventario> listarPaginado(int page, int pageSize, String filtro, String numeroFactura);

    int contar(String filtro);

    int contar(String filtro, String numeroFactura);

    List<Inventario> listarPorRango(LocalDate desde, LocalDate hasta);

    int guardar(Inventario inv);

    /** Alta de código nuevo dentro de la emisión; el descuento de stock lo realiza FacturaService. */
    default int crearTemporalFactura(Connection con, String codigo, String descripcion,
                                     BigDecimal precioVenta, int sucursalId) throws SQLException {
        throw new UnsupportedOperationException("El temporal debe registrarse en la transacción del servidor de facturación");
    }

    void actualizar(Inventario inv);

    void actualizarPrecioVenta(int id, java.math.BigDecimal precio);

    void eliminar(int id);

    List<Inventario> listarPorNumeroFactura(String numeroFactura, int proveedorId);

    void eliminarPorFactura(String numeroFactura, int proveedorId) throws SQLException;

    void eliminarFacturaConDependencias(String numeroFactura, int proveedorId);

    String obtenerProveedorNombre(int productoId);

    String obtenerProveedorNombre(Connection con, int productoId) throws SQLException;

    void descontarStock(int productoId, int cantidad);

    void descontarStock(Connection con, int productoId, int cantidad) throws SQLException;

    void devolverStock(int productoId, java.math.BigDecimal cantidad);

    void devolverStock(Connection con, int productoId, java.math.BigDecimal cantidad) throws SQLException;

    List<Inventario> listarStockBajo(int umbral);

    List<Inventario> listarStockNegativo();

    /** Panel de ingreso: activos negativos de la sucursal, fecha de alta DESC, nulos al final. */
    default List<com.vendex.model.StockNegativoItem> listarStockNegativoPorFecha(int sucursalId) throws Exception {
        throw new UnsupportedOperationException("Consulta de stock negativo por fecha no implementada");
    }

    List<Inventario> listarActivosConStock();

    Inventario obtenerPorId(int id);

    Inventario obtenerPorId(Connection con, int id) throws SQLException;

    Inventario obtenerPorCodigoYSucursal(String codigo, int sucursalId);

    Inventario obtenerPorCodigoYSucursal(Connection con, String codigo, int sucursalId) throws SQLException;

    List<Inventario> listarPorSucursal(int sucursalId);

    List<String> buscarDescripciones(String filtro, int limit);

}

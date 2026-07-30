package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.HistorialProducto;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistorialProductoDAO {

    public void insertar(List<HistorialProducto> lista) {
        String sql = "INSERT INTO historial_producto(producto_id, producto_codigo, producto_descripcion, cantidad, precio_unitario, tipo_comprobante, codigo_comprobante, cliente_nombre, proveedor_nombre, fecha_venta) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (HistorialProducto h : lista) {
                ps.setInt(1, h.getProductoId());
                ps.setString(2, h.getProductoCodigo());
                ps.setString(3, h.getProductoDescripcion());
                ps.setInt(4, h.getCantidad());
                ps.setBigDecimal(5, h.getPrecioUnitario());
                ps.setString(6, h.getTipoComprobante());
                ps.setString(7, h.getCodigoComprobante());
                ps.setString(8, h.getClienteNombre());
                ps.setString(9, h.getProveedorNombre());
                ps.setObject(10, h.getFechaVenta());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<HistorialProducto> listar() {
        return listarPorFecha(null);
    }

    public List<HistorialProducto> listarPorFecha(LocalDate fecha) {
        List<HistorialProducto> lista = new ArrayList<>();
        String sql = "SELECT * FROM historial_producto WHERE CAST(fecha_venta AS DATE) = ? ORDER BY fecha_venta DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha != null ? fecha : LocalDate.now());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistorialProducto h = new HistorialProducto();
                    h.setId(rs.getInt("id"));
                    h.setProductoId(rs.getInt("producto_id"));
                    h.setProductoCodigo(rs.getString("producto_codigo"));
                    h.setProductoDescripcion(rs.getString("producto_descripcion"));
                    h.setCantidad(rs.getInt("cantidad"));
                    h.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    h.setTipoComprobante(rs.getString("tipo_comprobante"));
                    h.setCodigoComprobante(rs.getString("codigo_comprobante"));
                    h.setClienteNombre(rs.getString("cliente_nombre"));
                    h.setProveedorNombre(rs.getString("proveedor_nombre"));
                    h.setFechaVenta(rs.getObject("fecha_venta", LocalDateTime.class));
                    h.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
                    lista.add(h);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}

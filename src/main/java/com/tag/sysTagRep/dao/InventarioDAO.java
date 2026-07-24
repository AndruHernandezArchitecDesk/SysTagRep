package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Inventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {

    public List<Inventario> listar() {
        List<Inventario> lista = new ArrayList<>();
        String sql = "SELECT i.*, p.nombre as nombre_proveedor FROM inventario i " +
                     "LEFT JOIN proveedor p ON p.id = i.proveedor_id " +
                     "ORDER BY i.id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Inventario v = new Inventario();
                v.setId(rs.getInt("id"));
                v.setDescripcion(rs.getString("descripcion"));
                v.setGrupo(rs.getString("grupo"));
                v.setMarca(rs.getString("marca"));
                v.setCostoSinIVA(rs.getBigDecimal("costo_sin_iva"));
                v.setCantidad(rs.getInt("cantidad"));
                v.setUbicacionPercha(rs.getString("ubicacion_percha"));
                v.setPrecioVenta(rs.getBigDecimal("precio_venta"));
                v.setFecha_ingreso(rs.getObject("fecha_ingreso", LocalDateTime.class));
                v.setEstado(rs.getBoolean("estado"));
                v.setCodigo(rs.getString("codigo"));
                v.setProveedor(rs.getString("nombre_proveedor"));
                v.setProveedorId(rs.getInt("proveedor_id"));
                lista.add(v);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public void guardar(Inventario inv) {
        String sql = "INSERT INTO inventario(descripcion, grupo, marca, costo_sin_iva, cantidad, ubicacion_percha, precio_venta, fecha_ingreso, estado, codigo, proveedor_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, inv.getDescripcion());
            ps.setString(2, inv.getGrupo());
            ps.setString(3, inv.getMarca());
            ps.setBigDecimal(4, inv.getCostoSinIVA());
            ps.setInt(5, inv.getCantidad());
            ps.setString(6, inv.getUbicacionPercha());
            ps.setBigDecimal(7, inv.getPrecioVenta());
            ps.setObject(8, LocalDateTime.now());
            ps.setObject(9, true);
            ps.setString(10, inv.getCodigo());
            if (inv.getProveedorId() > 0) ps.setInt(11, inv.getProveedorId());
            else ps.setNull(11, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void actualizar(Inventario inv) {
        String sql = "UPDATE inventario SET descripcion=?, grupo=?, marca=?, costo_sin_iva=?, cantidad=?, ubicacion_percha=?, precio_venta=?, fecha_ingreso=?, codigo=?, proveedor_id=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, inv.getDescripcion());
            ps.setString(2, inv.getGrupo());
            ps.setString(3, inv.getMarca());
            ps.setBigDecimal(4, inv.getCostoSinIVA());
            ps.setInt(5, inv.getCantidad());
            ps.setString(6, inv.getUbicacionPercha());
            ps.setBigDecimal(7, inv.getPrecioVenta());
            ps.setObject(8, inv.getFecha_ingreso());
            ps.setString(9, inv.getCodigo());
            if (inv.getProveedorId() > 0) ps.setInt(10, inv.getProveedorId());
            else ps.setNull(10, java.sql.Types.INTEGER);
            ps.setInt(11, inv.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM inventario WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void descontarStock(int productoId, int cantidad) {
        String sql = "UPDATE inventario SET cantidad = cantidad - ? WHERE id = ? AND cantidad >= ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            ps.setInt(3, cantidad);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

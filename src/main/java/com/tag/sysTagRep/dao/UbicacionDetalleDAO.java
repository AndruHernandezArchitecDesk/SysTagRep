package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.UbicacionDetalle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UbicacionDetalleDAO {

    public void guardar(UbicacionDetalle u) {
        String sql = "INSERT INTO ubicacion(codigo_ubicacion, id_perchero, estado) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getCodigoUbicacion());
            ps.setInt(2, u.getIdPerchero());
            ps.setString(3, u.getEstado());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void generarUbicaciones(int idPerchero, String prefijo, int cantidad) {
        String sql = "INSERT INTO ubicacion(codigo_ubicacion, id_perchero, estado) VALUES (?, ?, 'DISPONIBLE')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= cantidad; i++) {
                ps.setString(1, prefijo + i);
                ps.setInt(2, idPerchero);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void ocupar(int idUbicacion, int idProducto, int cantidad) {
        String sql = "UPDATE ubicacion SET estado='OCUPADO', id_producto=?, cantidad=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, cantidad);
            ps.setInt(3, idUbicacion);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void liberar(int id) {
        String sql = "UPDATE ubicacion SET estado='DISPONIBLE', id_producto=NULL, cantidad=NULL WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void eliminarLugar(int id) {
        String sql = "DELETE FROM ubicacion WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<UbicacionDetalle> listarOcupados() {
        List<UbicacionDetalle> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.codigo_ubicacion, u.id_perchero, u.estado, u.id_producto, u.cantidad AS stock_asignado, "
                + "p.nombre_perchero, p.seccion, i.descripcion AS producto_desc, i.codigo AS producto_cod, i.cantidad, "
                + "g.nombre AS grupo_nombre, m.nombre AS marca_nombre "
                + "FROM ubicacion u "
                + "JOIN perchero p ON p.id = u.id_perchero "
                + "JOIN inventario i ON i.id = u.id_producto "
                + "LEFT JOIN grupo g ON g.id = i.grupo_id "
                + "LEFT JOIN marca m ON m.id = i.marca_id "
                + "WHERE u.estado = 'OCUPADO' AND u.id_producto IS NOT NULL "
                + "ORDER BY substring(u.codigo_ubicacion, '^[A-Z]+'), substring(u.codigo_ubicacion, '[0-9]+$')::int";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UbicacionDetalle u = new UbicacionDetalle();
                    u.setId(rs.getInt("id"));
                    u.setCodigoUbicacion(rs.getString("codigo_ubicacion"));
                    u.setIdPerchero(rs.getInt("id_perchero"));
                    u.setNombrePerchero(rs.getString("nombre_perchero"));
                    u.setSeccion(rs.getString("seccion"));
                    u.setEstado(rs.getString("estado"));
                    u.setIdProducto(rs.getInt("id_producto"));
                    u.setProductoDescripcion(rs.getString("producto_desc"));
                    u.setProductoCodigo(rs.getString("producto_cod"));
                    u.setCantidad(rs.getInt("cantidad"));
                    u.setStockAsignado(rs.getObject("stock_asignado") != null ? rs.getInt("stock_asignado") : null);
                    u.setGrupoNombre(rs.getString("grupo_nombre"));
                    u.setMarcaNombre(rs.getString("marca_nombre"));
                    lista.add(u);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public void eliminarPorPerchero(int idPerchero) {
        String sql = "DELETE FROM ubicacion WHERE id_perchero=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPerchero);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<UbicacionDetalle> listarPorPerchero(int idPerchero) {
        List<UbicacionDetalle> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.codigo_ubicacion, u.id_perchero, u.estado, u.id_producto, u.cantidad AS stock_asignado, "
                + "p.nombre_perchero, p.seccion, i.descripcion AS producto_desc, i.codigo AS producto_cod, i.cantidad, "
                + "g.nombre AS grupo_nombre, m.nombre AS marca_nombre "
                + "FROM ubicacion u "
                + "JOIN perchero p ON p.id = u.id_perchero "
                + "LEFT JOIN inventario i ON i.id = u.id_producto "
                + "LEFT JOIN grupo g ON g.id = i.grupo_id "
                + "LEFT JOIN marca m ON m.id = i.marca_id "
                + "WHERE u.id_perchero = ? ORDER BY substring(u.codigo_ubicacion, '^[A-Z]+'), substring(u.codigo_ubicacion, '[0-9]+$')::int";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPerchero);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UbicacionDetalle u = new UbicacionDetalle();
                    u.setId(rs.getInt("id"));
                    u.setCodigoUbicacion(rs.getString("codigo_ubicacion"));
                    u.setIdPerchero(rs.getInt("id_perchero"));
                    u.setNombrePerchero(rs.getString("nombre_perchero"));
                    u.setSeccion(rs.getString("seccion"));
                    u.setEstado(rs.getString("estado"));
                    u.setIdProducto(rs.getObject("id_producto") != null ? rs.getInt("id_producto") : null);
                    u.setProductoDescripcion(rs.getString("producto_desc"));
                    u.setProductoCodigo(rs.getString("producto_cod"));
                    u.setCantidad(rs.getInt("cantidad"));
                    u.setStockAsignado(rs.getObject("stock_asignado") != null ? rs.getInt("stock_asignado") : null);
                    u.setGrupoNombre(rs.getString("grupo_nombre"));
                    u.setMarcaNombre(rs.getString("marca_nombre"));
                    lista.add(u);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public List<com.tag.sysTagRep.model.Inventario> listarProductosDisponibles() {
        List<com.tag.sysTagRep.model.Inventario> lista = new ArrayList<>();
        String sql = "SELECT i.id, i.codigo, i.descripcion, i.cantidad, g.nombre AS nombre_grupo, m.nombre AS nombre_marca, " +
                "(i.cantidad - COALESCE((SELECT SUM(u.cantidad) FROM ubicacion u WHERE u.id_producto = i.id AND u.estado = 'OCUPADO'), 0)) AS stock_restante " +
                "FROM inventario i " +
                "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                "LEFT JOIN marca m ON m.id = i.marca_id " +
                "WHERE i.estado = true AND i.cantidad >= 1 " +
                "AND (i.cantidad - COALESCE((SELECT SUM(u.cantidad) FROM ubicacion u WHERE u.id_producto = i.id AND u.estado = 'OCUPADO'), 0)) > 0 " +
                "ORDER BY i.descripcion";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.tag.sysTagRep.model.Inventario inv = new com.tag.sysTagRep.model.Inventario();
                    inv.setId(rs.getInt("id"));
                    inv.setCodigo(rs.getString("codigo"));
                    inv.setDescripcion(rs.getString("descripcion"));
                    inv.setCantidad(rs.getInt("stock_restante"));
                    inv.setGrupo(rs.getString("nombre_grupo"));
                    inv.setMarca(rs.getString("nombre_marca"));
                    lista.add(inv);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Rol;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RolDAOPostgres implements RolDAO {

    public List<Rol> listar() {
        List<Rol> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, descripcion, limite_descuento_pct FROM rol ORDER BY nombre";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Rol r = new Rol();
                r.setId(rs.getInt("id"));
                r.setNombre(rs.getString("nombre"));
                r.setDescripcion(rs.getString("descripcion"));
                r.setLimiteDescuentoPct(rs.getBigDecimal("limite_descuento_pct"));
                lista.add(r);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public Rol obtenerPorId(int id) {
        String sql = "SELECT id, nombre, descripcion, limite_descuento_pct FROM rol WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Rol r = new Rol();
                    r.setId(rs.getInt("id")); r.setNombre(rs.getString("nombre"));
                    r.setDescripcion(rs.getString("descripcion"));
                    r.setLimiteDescuentoPct(rs.getBigDecimal("limite_descuento_pct"));
                    return r;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Rol obtenerPorNombre(String nombre) {
        String sql = "SELECT id, nombre, descripcion, limite_descuento_pct FROM rol WHERE nombre=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Rol r = new Rol();
                    r.setId(rs.getInt("id")); r.setNombre(rs.getString("nombre"));
                    r.setDescripcion(rs.getString("descripcion"));
                    r.setLimiteDescuentoPct(rs.getBigDecimal("limite_descuento_pct"));
                    return r;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void actualizarLimiteDescuento(int rolId, java.math.BigDecimal limite) {
        String sql = "UPDATE rol SET limite_descuento_pct=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (limite == null) ps.setNull(1, Types.NUMERIC);
            else ps.setBigDecimal(1, limite);
            ps.setInt(2, rolId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}

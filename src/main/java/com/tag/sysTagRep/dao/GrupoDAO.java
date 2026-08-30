package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Grupo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GrupoDAO {

    public void guardar(Grupo g) throws SQLException {
        String sql = "INSERT INTO grupo(nombre, estado) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.getNombre());
            ps.setBoolean(2, g.isEstado());
            ps.executeUpdate();
        }
    }

    public void actualizar(Grupo g) {
        String sql = "UPDATE grupo SET nombre=?, estado=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, g.getNombre());
            ps.setBoolean(2, g.isEstado());
            ps.setInt(3, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean existe(String nombre) {
        String sql = "SELECT 1 FROM grupo WHERE UPPER(nombre) = UPPER(?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM grupo WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Grupo> listar() {
        List<Grupo> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, estado FROM grupo ORDER BY nombre";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Grupo g = new Grupo();
                g.setId(rs.getInt("id"));
                g.setNombre(rs.getString("nombre"));
                g.setEstado(rs.getBoolean("estado"));
                lista.add(g);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}

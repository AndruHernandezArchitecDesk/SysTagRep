package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Marca;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MarcaDAO {

    public void guardar(Marca m) {
        String sql = "INSERT INTO marca(nombre, estado) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getNombre());
            ps.setBoolean(2, m.isEstado());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void actualizar(Marca m) {
        String sql = "UPDATE marca SET nombre=?, estado=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getNombre());
            ps.setBoolean(2, m.isEstado());
            ps.setInt(3, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean existe(String nombre) {
        String sql = "SELECT 1 FROM marca WHERE UPPER(nombre) = UPPER(?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM marca WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Marca> listar() {
        List<Marca> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, estado FROM marca ORDER BY nombre";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Marca m = new Marca();
                m.setId(rs.getInt("id"));
                m.setNombre(rs.getString("nombre"));
                m.setEstado(rs.getBoolean("estado"));
                lista.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}

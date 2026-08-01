package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Codigo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CodigoDAO {

    public void guardar(Codigo c) {
        String sql = "INSERT INTO codigo(nombre, estado) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setBoolean(2, c.isEstado());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean existe(String nombre) {
        String sql = "SELECT 1 FROM codigo WHERE nombre = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void actualizar(Codigo c) {
        String sql = "UPDATE codigo SET nombre=?, estado=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setBoolean(2, c.isEstado());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM codigo WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Codigo> listar() {
        List<Codigo> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, estado FROM codigo ORDER BY nombre";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Codigo c = new Codigo();
                c.setId(rs.getInt("id"));
                c.setNombre(rs.getString("nombre"));
                c.setEstado(rs.getBoolean("estado"));
                lista.add(c);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}

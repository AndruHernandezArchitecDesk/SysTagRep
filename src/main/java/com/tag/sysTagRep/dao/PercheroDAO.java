package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Perchero;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PercheroDAO {

    public void guardar(Perchero p) {
        String sql = "INSERT INTO perchero(nombre_perchero, seccion, cantidad_lugares, estado) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombrePerchero());
            ps.setString(2, p.getSeccion());
            ps.setInt(3, p.getCantidadLugares());
            ps.setBoolean(4, p.isEstado());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void actualizar(Perchero p) {
        String sql = "UPDATE perchero SET nombre_perchero=?, seccion=?, cantidad_lugares=?, estado=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombrePerchero());
            ps.setString(2, p.getSeccion());
            ps.setInt(3, p.getCantidadLugares());
            ps.setBoolean(4, p.isEstado());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM perchero WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void eliminarPorNombre(String nombrePerchero) {
        String sql = "DELETE FROM perchero WHERE nombre_perchero=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombrePerchero);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Perchero> listar() {
        List<Perchero> lista = new ArrayList<>();
        String sql = "SELECT id, nombre_perchero, seccion, cantidad_lugares, estado FROM perchero ORDER BY nombre_perchero, seccion";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Perchero p = new Perchero();
                p.setId(rs.getInt("id"));
                p.setNombrePerchero(rs.getString("nombre_perchero"));
                p.setSeccion(rs.getString("seccion"));
                p.setCantidadLugares(rs.getInt("cantidad_lugares"));
                p.setEstado(rs.getBoolean("estado"));
                lista.add(p);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public List<String> listarNombres() {
        List<String> nombres = new ArrayList<>();
        String sql = "SELECT DISTINCT nombre_perchero FROM perchero WHERE estado=true ORDER BY nombre_perchero";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                nombres.add(rs.getString("nombre_perchero"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return nombres;
    }

    public List<Perchero> listarPorNombre(String nombrePerchero) {
        List<Perchero> lista = new ArrayList<>();
        String sql = "SELECT id, nombre_perchero, seccion, cantidad_lugares, estado FROM perchero WHERE nombre_perchero=? ORDER BY seccion";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombrePerchero);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Perchero p = new Perchero();
                    p.setId(rs.getInt("id"));
                    p.setNombrePerchero(rs.getString("nombre_perchero"));
                    p.setSeccion(rs.getString("seccion"));
                    p.setCantidadLugares(rs.getInt("cantidad_lugares"));
                    p.setEstado(rs.getBoolean("estado"));
                    lista.add(p);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public Perchero obtenerPorId(int id) {
        String sql = "SELECT id, nombre_perchero, seccion, cantidad_lugares, estado FROM perchero WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Perchero p = new Perchero();
                    p.setId(rs.getInt("id"));
                    p.setNombrePerchero(rs.getString("nombre_perchero"));
                    p.setSeccion(rs.getString("seccion"));
                    p.setCantidadLugares(rs.getInt("cantidad_lugares"));
                    p.setEstado(rs.getBoolean("estado"));
                    return p;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}

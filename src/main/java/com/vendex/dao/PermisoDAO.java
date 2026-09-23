package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Permiso;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermisoDAO {

    public List<Permiso> listarTodos() {
        List<Permiso> lista = new ArrayList<>();
        String sql = "SELECT codigo, descripcion, categoria FROM permiso ORDER BY categoria, codigo";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Permiso(rs.getString("codigo"), rs.getString("descripcion"), rs.getString("categoria")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public Set<String> listarPorRol(int rolId) {
        Set<String> set = new HashSet<>();
        String sql = "SELECT permiso_codigo FROM rol_permiso WHERE rol_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getString(1));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return set;
    }

    public void actualizarPermisosDeRol(int rolId, List<String> codigos) {
        String del = "DELETE FROM rol_permiso WHERE rol_id=?";
        String ins = "INSERT INTO rol_permiso (rol_id, permiso_codigo) VALUES (?,?)";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(del)) {
                ps.setInt(1, rolId); ps.executeUpdate();
            }
            if (codigos != null && !codigos.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(ins)) {
                    for (String c : codigos) {
                        ps.setInt(1, rolId); ps.setString(2, c); ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) { e.printStackTrace(); }
    }
}

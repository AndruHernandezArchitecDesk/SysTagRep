package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.UbicacionPercha;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UbicacionPerchaDAO {

    public void guardar(UbicacionPercha u) {
        String sql = "INSERT INTO ubicacion_percha(nombre, estado) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setBoolean(2, u.isEstado());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void actualizar(UbicacionPercha u) {
        String sql = "UPDATE ubicacion_percha SET nombre=?, estado=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setBoolean(2, u.isEstado());
            ps.setInt(3, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM ubicacion_percha WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<UbicacionPercha> listar() {
        List<UbicacionPercha> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, estado FROM ubicacion_percha ORDER BY nombre";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                UbicacionPercha u = new UbicacionPercha();
                u.setId(rs.getInt("id"));
                u.setNombre(rs.getString("nombre"));
                u.setEstado(rs.getBoolean("estado"));
                lista.add(u);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }
}

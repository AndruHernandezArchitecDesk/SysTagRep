package com.vendex.dao;

import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginIntentoLogDAO {

    public void registrar(String usuarioInput, boolean exitoso, String equipo) {
        String sql = "INSERT INTO login_intento_log (usuario_input, exitoso, equipo) VALUES (?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuarioInput == null ? "" : usuarioInput);
            ps.setBoolean(2, exitoso);
            ps.setString(3, equipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            // no bloquear login por fallo de auditoría
            e.printStackTrace();
        }
    }

    public int contarFallosRecientes(String usuarioInput, int ventanaMinutos) {
        String sql = "SELECT COUNT(*) FROM login_intento_log WHERE usuario_input=? AND exitoso=false AND creado_en > now() - (? || ' minutes')::interval";
        // HSQLDB fallback: usar DATEADD
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuarioInput);
            ps.setString(2, String.valueOf(ventanaMinutos));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // intentar HSQLDB syntax
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT COUNT(*) FROM login_intento_log WHERE usuario_input=? AND exitoso=false AND creado_en > DATEADD('MINUTE', ?, CURRENT_TIMESTAMP)")) {
                ps.setString(1, usuarioInput);
                ps.setInt(2, -ventanaMinutos);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        return 0;
    }
}

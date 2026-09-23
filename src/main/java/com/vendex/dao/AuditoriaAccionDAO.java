package com.vendex.dao;

import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditoriaAccionDAO {

    public void registrar(int usuarioId, String permisoCodigo, String resultado, String detalle) {
        String sql = "INSERT INTO auditoria_accion (usuario_id, permiso_codigo, resultado, detalle) VALUES (?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, permisoCodigo);
            ps.setString(3, resultado);
            ps.setString(4, detalle != null && detalle.length() > 300 ? detalle.substring(0,300) : detalle);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}

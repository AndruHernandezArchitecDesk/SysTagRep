package com.vendex.dao;

import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditoriaAccionDAOPostgres implements AuditoriaAccionDAO {

    public void registrar(int usuarioId, String permisoCodigo, String resultado, String detalle) {
        // Fase 7: la auditoría registra también la sucursal de la sesión
        String sql = "INSERT INTO auditoria_accion (usuario_id, permiso_codigo, resultado, detalle, sucursal_id) VALUES (?,?,?,?,?)";
        Integer sucursalId = null;
        try {
            if (com.vendex.util.SesionActual.getUsuario() != null) {
                sucursalId = com.vendex.util.SesionActual.getSucursalId();
            }
        } catch (Exception ignored) {}
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, permisoCodigo);
            ps.setString(3, resultado);
            ps.setString(4, detalle != null && detalle.length() > 300 ? detalle.substring(0,300) : detalle);
            if (sucursalId != null) ps.setInt(5, sucursalId); else ps.setNull(5, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioPermisoDAOPostgres implements UsuarioPermisoDAO {

    private static final Logger LOGGER = Logger.getLogger(UsuarioPermisoDAOPostgres.class.getName());

    @Override
    public Map<String, Boolean> listarPorUsuario(int usuarioId) {
        String sql = "SELECT permiso_codigo, otorgar FROM usuario_permiso WHERE usuario_id = ?";
        Map<String, Boolean> out = new LinkedHashMap<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getBoolean(2));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron leer permisos granulares del usuario " + usuarioId, e);
        }
        return out;
    }

    @Override
    public void reemplazar(int usuarioId, Map<String, Boolean> permisos) {
        String del = "DELETE FROM usuario_permiso WHERE usuario_id = ?";
        String ins = "INSERT INTO usuario_permiso (usuario_id, permiso_codigo, otorgar) VALUES (?, ?, ?) " +
                "ON CONFLICT (usuario_id, permiso_codigo) DO UPDATE SET otorgar = EXCLUDED.otorgar";
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean prev = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(del)) {
                    ps.setInt(1, usuarioId);
                    ps.executeUpdate();
                }
                if (permisos != null && !permisos.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(ins)) {
                        for (Map.Entry<String, Boolean> e : permisos.entrySet()) {
                            if (e.getKey() == null || e.getKey().isBlank()) continue;
                            ps.setInt(1, usuarioId);
                            ps.setString(2, e.getKey().trim().toUpperCase(java.util.Locale.ROOT));
                            ps.setBoolean(3, Boolean.TRUE.equals(e.getValue()));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron reemplazar permisos granulares del usuario " + usuarioId, e);
        }
    }
}

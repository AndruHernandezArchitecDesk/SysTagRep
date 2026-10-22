package com.vendex.dao;

import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsuarioSucursalDAOPostgres implements UsuarioSucursalDAO {

    private static final Logger LOGGER = Logger.getLogger(UsuarioSucursalDAOPostgres.class.getName());

    @Override
    public List<Integer> listarSucursales(int usuarioId) {
        String sql = "SELECT sucursal_id FROM usuario_sucursal WHERE usuario_id = ? ORDER BY sucursal_id";
        List<Integer> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudieron leer sucursales del usuario " + usuarioId, e);
        }
        return out;
    }

    @Override
    public void reemplazar(int usuarioId, List<Integer> sucursalIds) {
        String del = "DELETE FROM usuario_sucursal WHERE usuario_id = ?";
        String ins = "INSERT INTO usuario_sucursal (usuario_id, sucursal_id) VALUES (?, ?) " +
                "ON CONFLICT (usuario_id, sucursal_id) DO NOTHING";
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean prev = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(del)) {
                    ps.setInt(1, usuarioId);
                    ps.executeUpdate();
                }
                if (sucursalIds != null && !sucursalIds.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(ins)) {
                        Set<Integer> unicos = new LinkedHashSet<>(sucursalIds);
                        for (Integer s : unicos) {
                            if (s == null || s <= 0) continue;
                            ps.setInt(1, usuarioId);
                            ps.setInt(2, s);
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
            LOGGER.log(Level.WARNING, "No se pudieron reemplazar sucursales del usuario " + usuarioId, e);
        }
    }

    @Override
    public int sucursalValidaPara(int usuarioId, Integer sucursalPreferida) {
        return resolver(sucursalPreferida, listarSucursales(usuarioId));
    }

    /** Fase 7: lógica pura de selección (testeable sin BD): preferida si está permitida; si no, la primera; si no hay filas, la preferida. */
    static int resolver(Integer sucursalPreferida, List<Integer> permitidas) {
        if (permitidas == null || permitidas.isEmpty()) return sucursalPreferida != null ? sucursalPreferida : 1;
        if (sucursalPreferida != null && permitidas.contains(sucursalPreferida)) return sucursalPreferida;
        return permitidas.get(0);
    }
}

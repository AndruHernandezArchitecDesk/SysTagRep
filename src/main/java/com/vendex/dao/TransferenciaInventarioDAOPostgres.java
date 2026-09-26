package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.TransferenciaInventario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransferenciaInventarioDAOPostgres implements TransferenciaInventarioDAO {

    private static final Logger LOGGER = Logger.getLogger(TransferenciaInventarioDAOPostgres.class.getName());

    @Override
    public int guardar(TransferenciaInventario t) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return guardar(con, t);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "TransferenciaInventarioDAO.guardar", e);
        }
        return -1;
    }

    @Override
    public int guardar(Connection con, TransferenciaInventario t) throws SQLException {
        String sql = "INSERT INTO transferencia_inventario(inventario_id, origen_sucursal_id, destino_sucursal_id, cantidad, estado, usuario_id, motivo) VALUES (?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, t.getInventarioId());
            ps.setInt(2, t.getOrigenSucursalId());
            ps.setInt(3, t.getDestinoSucursalId());
            ps.setInt(4, t.getCantidad());
            ps.setString(5, t.getEstado() != null ? t.getEstado() : "COMPLETADA");
            if (t.getUsuarioId() != null) ps.setInt(6, t.getUsuarioId()); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, t.getMotivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public List<TransferenciaInventario> listarPorInventario(int inventarioId) {
        List<TransferenciaInventario> lista = new ArrayList<>();
        String sql = "SELECT * FROM transferencia_inventario WHERE inventario_id=? ORDER BY creado_en DESC LIMIT 100";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inventarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "TransferenciaInventarioDAO.listarPorInventario", e);
        }
        return lista;
    }

    @Override
    public List<TransferenciaInventario> listarPorSucursal(int sucursalId, int limit) {
        List<TransferenciaInventario> lista = new ArrayList<>();
        String sql = "SELECT * FROM transferencia_inventario WHERE origen_sucursal_id=? OR destino_sucursal_id=? ORDER BY creado_en DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sucursalId);
            ps.setInt(2, sucursalId);
            ps.setInt(3, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "TransferenciaInventarioDAO.listarPorSucursal", e);
        }
        return lista;
    }

    @Override
    public List<TransferenciaInventario> listarTodas(int limit) {
        List<TransferenciaInventario> lista = new ArrayList<>();
        String sql = "SELECT * FROM transferencia_inventario ORDER BY creado_en DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "TransferenciaInventarioDAO.listarTodas", e);
        }
        return lista;
    }

    private TransferenciaInventario mapear(ResultSet rs) throws SQLException {
        TransferenciaInventario t = new TransferenciaInventario();
        t.setId(rs.getInt("id"));
        t.setInventarioId(rs.getInt("inventario_id"));
        t.setOrigenSucursalId(rs.getInt("origen_sucursal_id"));
        t.setDestinoSucursalId(rs.getInt("destino_sucursal_id"));
        t.setCantidad(rs.getInt("cantidad"));
        t.setEstado(rs.getString("estado"));
        int uid = rs.getInt("usuario_id");
        t.setUsuarioId(rs.wasNull() ? null : uid);
        t.setMotivo(rs.getString("motivo"));
        Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null) t.setCreadoEn(ts.toLocalDateTime());
        return t;
    }
}

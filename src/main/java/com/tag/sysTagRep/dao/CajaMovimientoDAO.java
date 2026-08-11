package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.CajaMovimiento;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CajaMovimientoDAO {

    private static final Logger LOGGER = Logger.getLogger(CajaMovimientoDAO.class.getName());

    public int insertar(CajaMovimiento m) {
        String sql = "INSERT INTO caja_movimiento(sesion_id, tipo, monto, descripcion, referencia_id, referencia_tipo, usuario_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getSesionId());
            ps.setString(2, m.getTipo());
            ps.setBigDecimal(3, m.getMonto());
            ps.setString(4, m.getDescripcion());
            if (m.getReferenciaId() != null) ps.setInt(5, m.getReferenciaId());
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, m.getReferenciaTipo());
            ps.setInt(7, m.getUsuarioId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaMovimientoDAO.insertar", e);
        }
        return -1;
    }

    public List<CajaMovimiento> listarPorSesion(int sesionId) {
        List<CajaMovimiento> lista = new ArrayList<>();
        String sql = "SELECT * FROM caja_movimiento WHERE sesion_id = ? ORDER BY fecha ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaMovimientoDAO.listarPorSesion", e);
        }
        return lista;
    }

    public List<CajaMovimiento> listarPorFecha(java.time.LocalDate desde, java.time.LocalDate hasta) {
        List<CajaMovimiento> lista = new ArrayList<>();
        String sql = "SELECT * FROM caja_movimiento WHERE CAST(fecha AS DATE) BETWEEN ? AND ? ORDER BY fecha DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaMovimientoDAO.listarPorFecha", e);
        }
        return lista;
    }

    public BigDecimal totalPorTipo(int sesionId, String tipo) {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM caja_movimiento WHERE sesion_id = ? AND tipo = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sesionId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaMovimientoDAO.totalPorTipo", e);
        }
        return BigDecimal.ZERO;
    }

    private CajaMovimiento mapear(ResultSet rs) throws SQLException {
        CajaMovimiento m = new CajaMovimiento();
        m.setId(rs.getInt("id"));
        m.setSesionId(rs.getInt("sesion_id"));
        m.setTipo(rs.getString("tipo"));
        m.setMonto(rs.getBigDecimal("monto"));
        m.setDescripcion(rs.getString("descripcion"));
        int refId = rs.getInt("referencia_id");
        m.setReferenciaId(rs.wasNull() ? null : refId);
        m.setReferenciaTipo(rs.getString("referencia_tipo"));
        Timestamp f = rs.getTimestamp("fecha");
        if (f != null) m.setFecha(f.toLocalDateTime());
        m.setUsuarioId(rs.getInt("usuario_id"));
        return m;
    }
}

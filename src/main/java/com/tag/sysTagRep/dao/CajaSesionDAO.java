package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.CajaSesion;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CajaSesionDAO {

    private static final Logger LOGGER = Logger.getLogger(CajaSesionDAO.class.getName());

    public int abrir(CajaSesion s) {
        String sql = "INSERT INTO caja_sesion(usuario_id, monto_inicial, observaciones, estado) VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getUsuarioId());
            ps.setBigDecimal(2, s.getMontoInicial());
            ps.setString(3, s.getObservaciones());
            ps.setString(4, s.getEstado());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaSesionDAO.abrir", e);
        }
        return -1;
    }

    public CajaSesion obtenerAbierta() {
        String sql = "SELECT * FROM caja_sesion WHERE estado = 'ABIERTA' ORDER BY fecha_apertura DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaSesionDAO.obtenerAbierta", e);
        }
        return null;
    }

    public CajaSesion obtenerPorId(int id) {
        String sql = "SELECT * FROM caja_sesion WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaSesionDAO.obtenerPorId", e);
        }
        return null;
    }

    public List<CajaSesion> listarPorFecha(java.time.LocalDate desde, java.time.LocalDate hasta) {
        List<CajaSesion> lista = new ArrayList<>();
        String sql = "SELECT * FROM caja_sesion WHERE CAST(fecha_apertura AS DATE) BETWEEN ? AND ? ORDER BY fecha_apertura DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde);
            ps.setObject(2, hasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaSesionDAO.listarPorFecha", e);
        }
        return lista;
    }

    public boolean cerrar(int id, BigDecimal montoFisico, String observaciones) {
        String sql = "UPDATE caja_sesion SET estado = 'CERRADA', fecha_cierre = NOW(), monto_fisico = ?, diferencia = ?, observaciones = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, montoFisico);
            ps.setString(2, observaciones);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaSesionDAO.cerrar", e);
        }
        return false;
    }

    public BigDecimal calcularTotalMovimientos(int sesionId) {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM caja_movimiento WHERE sesion_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sesionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en CajaSesionDAO.calcularTotalMovimientos", e);
        }
        return BigDecimal.ZERO;
    }

    private CajaSesion mapear(ResultSet rs) throws SQLException {
        CajaSesion s = new CajaSesion();
        s.setId(rs.getInt("id"));
        s.setUsuarioId(rs.getInt("usuario_id"));
        Timestamp fa = rs.getTimestamp("fecha_apertura");
        if (fa != null) s.setFechaApertura(fa.toLocalDateTime());
        Timestamp fc = rs.getTimestamp("fecha_cierre");
        if (fc != null) s.setFechaCierre(fc.toLocalDateTime());
        s.setMontoInicial(rs.getBigDecimal("monto_inicial"));
        s.setMontoFisico(rs.getBigDecimal("monto_fisico"));
        s.setDiferencia(rs.getBigDecimal("diferencia"));
        s.setEstado(rs.getString("estado"));
        s.setObservaciones(rs.getString("observaciones"));
        return s;
    }
}

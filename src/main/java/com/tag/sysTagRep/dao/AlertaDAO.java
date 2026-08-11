package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Alerta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlertaDAO {

    private static final Logger LOGGER = Logger.getLogger(AlertaDAO.class.getName());

    public int insertar(Alerta a) {
        String sql = "INSERT INTO alertas(tipo, mensaje, referencia_id, referencia_tipo, leida, fecha_creacion) " +
                     "VALUES (?, ?, ?, ?, FALSE, NOW()) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getTipo());
            ps.setString(2, a.getMensaje());
            if (a.getReferenciaId() != null) ps.setInt(3, a.getReferenciaId());
            else ps.setNull(3, Types.INTEGER);
            ps.setString(4, a.getReferenciaTipo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.insertar", e);
        }
        return -1;
    }

    public List<Alerta> listarTodas() {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT * FROM alertas ORDER BY leida ASC, fecha_creacion DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.listarTodas", e);
        }
        return lista;
    }

    public List<Alerta> listarNoLeidas() {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT * FROM alertas WHERE leida = FALSE ORDER BY fecha_creacion DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.listarNoLeidas", e);
        }
        return lista;
    }

    public int contarNoLeidas() {
        String sql = "SELECT COUNT(*) FROM alertas WHERE leida = FALSE";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.contarNoLeidas", e);
        }
        return 0;
    }

    public void marcarComoLeida(int id) {
        String sql = "UPDATE alertas SET leida = TRUE, fecha_lectura = NOW() WHERE id = ? AND leida = FALSE";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.marcarComoLeida", e);
        }
    }

    public void marcarTodasComoLeidas() {
        String sql = "UPDATE alertas SET leida = TRUE, fecha_lectura = NOW() WHERE leida = FALSE";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.marcarTodasComoLeidas", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM alertas WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.eliminar", e);
        }
    }

    public void eliminarLeidas() {
        String sql = "DELETE FROM alertas WHERE leida = TRUE";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.eliminarLeidas", e);
        }
    }

    public void limpiar() {
        String sql = "DELETE FROM alertas";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AlertaDAO.limpiar", e);
        }
    }

    private Alerta mapear(ResultSet rs) throws SQLException {
        Alerta a = new Alerta();
        a.setId(rs.getInt("id"));
        a.setTipo(rs.getString("tipo"));
        a.setMensaje(rs.getString("mensaje"));
        int refId = rs.getInt("referencia_id");
        a.setReferenciaId(rs.wasNull() ? null : refId);
        a.setReferenciaTipo(rs.getString("referencia_tipo"));
        a.setLeida(rs.getBoolean("leida"));
        Timestamp fc = rs.getTimestamp("fecha_creacion");
        if (fc != null) a.setFechaCreacion(fc.toLocalDateTime());
        Timestamp fl = rs.getTimestamp("fecha_lectura");
        if (fl != null) a.setFechaLectura(fl.toLocalDateTime());
        return a;
    }
}

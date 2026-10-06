package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.AccesoDirecto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccesoDirectoDAOPostgres implements AccesoDirectoDAO {

    private static final Logger LOGGER = Logger.getLogger(AccesoDirectoDAO.class.getName());

    public List<AccesoDirecto> listarPorUsuario(int usuarioId) {
        List<AccesoDirecto> lista = new ArrayList<>();
        String sql = "SELECT * FROM acceso_directo WHERE usuario_id = ? ORDER BY posicion ASC, id ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.listarPorUsuario", e);
        }
        return lista;
    }

    public int contarPorUsuario(int usuarioId) {
        String sql = "SELECT COUNT(*) FROM acceso_directo WHERE usuario_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.contarPorUsuario", e);
        }
        return 0;
    }

    public boolean existe(int usuarioId, String vistaRuta) {
        String sql = "SELECT 1 FROM acceso_directo WHERE usuario_id = ? AND vista_ruta = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, vistaRuta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.existe", e);
        }
        return false;
    }

    public int agregar(AccesoDirecto a) {
        String sql = "INSERT INTO acceso_directo(usuario_id, vista_ruta, titulo, icono_literal, posicion, creado_en) " +
                     "VALUES (?, ?, ?, ?, ?, NOW()) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getUsuarioId());
            ps.setString(2, a.getVistaRuta());
            ps.setString(3, a.getTitulo());
            ps.setString(4, a.getIconoLiteral());
            ps.setInt(5, a.getPosicion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.agregar", e);
        }
        return -1;
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM acceso_directo WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.eliminar", e);
        }
    }

    public void eliminarPorVista(int usuarioId, String vistaRuta) {
        String sql = "DELETE FROM acceso_directo WHERE usuario_id = ? AND vista_ruta = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, vistaRuta);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.eliminarPorVista", e);
        }
    }

    public void reordenar(int usuarioId, List<Integer> idsEnOrden) {
        String sql = "UPDATE acceso_directo SET posicion = ? WHERE id = ? AND usuario_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < idsEnOrden.size(); i++) {
                ps.setInt(1, i);
                ps.setInt(2, idsEnOrden.get(i));
                ps.setInt(3, usuarioId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en AccesoDirectoDAO.reordenar", e);
        }
    }

    private AccesoDirecto mapear(ResultSet rs) throws SQLException {
        AccesoDirecto a = new AccesoDirecto();
        a.setId(rs.getInt("id"));
        a.setUsuarioId(rs.getInt("usuario_id"));
        a.setVistaRuta(rs.getString("vista_ruta"));
        a.setTitulo(rs.getString("titulo"));
        a.setIconoLiteral(rs.getString("icono_literal"));
        a.setPosicion(rs.getInt("posicion"));
        Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null) a.setCreadoEn(ts.toLocalDateTime());
        return a;
    }
}

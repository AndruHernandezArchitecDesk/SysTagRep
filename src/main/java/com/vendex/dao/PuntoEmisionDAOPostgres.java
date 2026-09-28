package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.PuntoEmision;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PuntoEmisionDAOPostgres implements PuntoEmisionDAO {

    private static final Logger LOGGER = Logger.getLogger(PuntoEmisionDAOPostgres.class.getName());

    @Override
    public List<PuntoEmision> listarPorSucursal(int sucursalId) {
        List<PuntoEmision> lista = new ArrayList<>();
        String sql = "SELECT * FROM punto_emision WHERE sucursal_id=? ORDER BY codigo";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sucursalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOGGER.log(Level.SEVERE, "PuntoEmisionDAO.listarPorSucursal", e);
        }
        return lista;
    }

    @Override
    public Optional<PuntoEmision> obtenerPorId(int id) {
        String sql = "SELECT * FROM punto_emision WHERE id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return Optional.empty();
            LOGGER.log(Level.SEVERE, "PuntoEmisionDAO.obtenerPorId", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PuntoEmision> obtenerPorCodigoSucursal(int sucursalId, String codigo) {
        String sql = "SELECT * FROM punto_emision WHERE sucursal_id=? AND codigo=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sucursalId);
            ps.setString(2, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return Optional.empty();
            LOGGER.log(Level.SEVERE, "PuntoEmisionDAO.obtenerPorCodigoSucursal", e);
        }
        return Optional.empty();
    }

    @Override
    public int guardar(PuntoEmision p) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return guardar(con, p);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "PuntoEmisionDAO.guardar", e);
        }
        return -1;
    }

    @Override
    public int guardar(Connection con, PuntoEmision p) throws SQLException {
        String sql = "INSERT INTO punto_emision(sucursal_id, codigo, descripcion) VALUES (?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getSucursalId());
            ps.setString(2, p.getCodigo());
            ps.setString(3, p.getDescripcion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void actualizar(PuntoEmision p) {
        try (Connection con = DatabaseConnection.getConnection()) {
            actualizar(con, p);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "PuntoEmisionDAO.actualizar", e);
        }
    }

    @Override
    public void actualizar(Connection con, PuntoEmision p) throws SQLException {
        String sql = "UPDATE punto_emision SET sucursal_id=?, codigo=?, descripcion=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getSucursalId());
            ps.setString(2, p.getCodigo());
            ps.setString(3, p.getDescripcion());
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void desactivar(int id) {
        String sql = "UPDATE punto_emision SET activo=false WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "PuntoEmisionDAO.desactivar", e);
        }
    }

    private PuntoEmision mapear(ResultSet rs) throws SQLException {
        PuntoEmision p = new PuntoEmision();
        p.setId(rs.getInt("id"));
        p.setSucursalId(rs.getInt("sucursal_id"));
        p.setCodigo(rs.getString("codigo"));
        p.setDescripcion(rs.getString("descripcion"));
        return p;
    }

    private boolean isNoExiste(SQLException e) {
        String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return m.contains("does not exist") || m.contains("no existe");
    }
}

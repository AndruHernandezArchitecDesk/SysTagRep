package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.EntidadFinanciera;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntidadFinancieraDAOPostgres implements EntidadFinancieraDAO {

    private static final Logger LOGGER = Logger.getLogger(EntidadFinancieraDAOPostgres.class.getName());

    @Override
    public int insertar(EntidadFinanciera e) {
        String sql = "INSERT INTO entidad_financiera(nombre, tipo, identificacion, telefono, contacto, notas, estado, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getTipo() != null ? e.getTipo() : "BANCO");
            ps.setString(3, e.getIdentificacion());
            ps.setString(4, e.getTelefono());
            ps.setString(5, e.getContacto());
            ps.setString(6, e.getNotas());
            ps.setBoolean(7, e.isEstado());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return insertar(e);
            }
            LOGGER.log(Level.SEVERE, "insertar EntidadFinanciera", ex);
        }
        return -1;
    }

    @Override
    public void actualizar(EntidadFinanciera e) {
        String sql = "UPDATE entidad_financiera SET nombre=?, tipo=?, identificacion=?, telefono=?, contacto=?, notas=?, estado=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getTipo());
            ps.setString(3, e.getIdentificacion());
            ps.setString(4, e.getTelefono());
            ps.setString(5, e.getContacto());
            ps.setString(6, e.getNotas());
            ps.setBoolean(7, e.isEstado());
            ps.setInt(8, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                actualizar(e);
                return;
            }
            LOGGER.log(Level.SEVERE, "actualizar EntidadFinanciera", ex);
        }
    }

    @Override
    public void desactivar(int id) {
        String sql = "UPDATE entidad_financiera SET estado = FALSE WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "desactivar EntidadFinanciera", ex);
        }
    }

    @Override
    public List<EntidadFinanciera> listar(boolean soloActivas) {
        List<EntidadFinanciera> lista = new ArrayList<>();
        String sql = "SELECT * FROM entidad_financiera" + (soloActivas ? " WHERE estado = TRUE" : "") + " ORDER BY nombre";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return listar(soloActivas);
            }
            LOGGER.log(Level.SEVERE, "listar EntidadFinanciera", ex);
        }
        return lista;
    }

    @Override
    public EntidadFinanciera obtenerPorId(int id) {
        String sql = "SELECT * FROM entidad_financiera WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "obtenerPorId EntidadFinanciera", ex);
        }
        return null;
    }

    private EntidadFinanciera mapear(ResultSet rs) throws SQLException {
        EntidadFinanciera e = new EntidadFinanciera();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setTipo(rs.getString("tipo"));
        e.setIdentificacion(rs.getString("identificacion"));
        e.setTelefono(rs.getString("telefono"));
        e.setContacto(rs.getString("contacto"));
        e.setNotas(rs.getString("notas"));
        e.setEstado(rs.getBoolean("estado"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) e.setFechaRegistro(ts.toLocalDateTime());
        return e;
    }
}

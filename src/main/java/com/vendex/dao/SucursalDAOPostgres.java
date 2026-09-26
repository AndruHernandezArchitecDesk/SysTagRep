package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Sucursal;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SucursalDAOPostgres implements SucursalDAO {

    private static final Logger LOGGER = Logger.getLogger(SucursalDAOPostgres.class.getName());

    @Override
    public List<Sucursal> listar() {
        List<Sucursal> lista = new ArrayList<>();
        String sql = "SELECT * FROM sucursal ORDER BY codigo";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOGGER.log(Level.SEVERE, "SucursalDAO.listar", e);
        }
        return lista;
    }

    @Override
    public List<Sucursal> listarActivas() {
        List<Sucursal> lista = new ArrayList<>();
        String sql = "SELECT * FROM sucursal WHERE activo=true ORDER BY codigo";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOGGER.log(Level.SEVERE, "SucursalDAO.listarActivas", e);
        }
        return lista;
    }

    @Override
    public Optional<Sucursal> obtenerPorId(int id) {
        String sql = "SELECT * FROM sucursal WHERE id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return Optional.empty();
            LOGGER.log(Level.SEVERE, "SucursalDAO.obtenerPorId", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Sucursal> obtenerPorCodigo(String codigo) {
        String sql = "SELECT * FROM sucursal WHERE codigo=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return Optional.empty();
            LOGGER.log(Level.SEVERE, "SucursalDAO.obtenerPorCodigo", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Sucursal> obtenerCentral() {
        String sql = "SELECT * FROM sucursal WHERE es_central=true LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (SQLException e) {
            if (isNoExiste(e)) return Optional.empty();
            LOGGER.log(Level.SEVERE, "SucursalDAO.obtenerCentral", e);
        }
        return Optional.empty();
    }

    @Override
    public int guardar(Sucursal s) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return guardar(con, s);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SucursalDAO.guardar", e);
        }
        return -1;
    }

    @Override
    public int guardar(Connection con, Sucursal s) throws SQLException {
        String sql = "INSERT INTO sucursal(codigo, nombre, direccion, telefono, es_central, activo) VALUES (?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getCodigo());
            ps.setString(2, s.getNombre());
            ps.setString(3, s.getDireccion());
            ps.setString(4, s.getTelefono());
            ps.setBoolean(5, s.isEsCentral());
            ps.setBoolean(6, s.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void actualizar(Sucursal s) {
        try (Connection con = DatabaseConnection.getConnection()) {
            actualizar(con, s);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SucursalDAO.actualizar", e);
        }
    }

    @Override
    public void actualizar(Connection con, Sucursal s) throws SQLException {
        String sql = "UPDATE sucursal SET codigo=?, nombre=?, direccion=?, telefono=?, es_central=?, activo=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getCodigo());
            ps.setString(2, s.getNombre());
            ps.setString(3, s.getDireccion());
            ps.setString(4, s.getTelefono());
            ps.setBoolean(5, s.isEsCentral());
            ps.setBoolean(6, s.isActivo());
            ps.setInt(7, s.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void desactivar(int id) {
        String sql = "UPDATE sucursal SET activo=false WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SucursalDAO.desactivar", e);
        }
    }

    private Sucursal mapear(ResultSet rs) throws SQLException {
        Sucursal s = new Sucursal();
        s.setId(rs.getInt("id"));
        s.setCodigo(rs.getString("codigo"));
        s.setNombre(rs.getString("nombre"));
        s.setDireccion(rs.getString("direccion"));
        s.setTelefono(rs.getString("telefono"));
        s.setEsCentral(rs.getBoolean("es_central"));
        s.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null) s.setCreadoEn(ts.toLocalDateTime());
        return s;
    }

    private boolean isNoExiste(SQLException e) {
        String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return m.contains("does not exist") || m.contains("no existe");
    }
}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.LicenciaEmitida;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LicenciaCentralDAOPostgres implements LicenciaCentralDAO {

    private static final Logger LOGGER = Logger.getLogger(LicenciaCentralDAOPostgres.class.getName());

    @Override
    public void insertarEmitida(LicenciaEmitida e) {
        String sql = "INSERT INTO licencia_emitida (machine_code, clave_hash, modalidad, incluye_ia, roles, capacidad, expiracion, emitida_por) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getMachineCode());
            ps.setString(2, e.getClaveHash());
            ps.setString(3, e.getModalidad());
            ps.setBoolean(4, e.isIncluyeIA());
            ps.setString(5, e.getRoles() != null ? e.getRoles() : "-");
            ps.setLong(6, e.getCapacidad());
            if (e.getExpiracion() != null) ps.setDate(7, Date.valueOf(e.getExpiracion())); else ps.setNull(7, java.sql.Types.DATE);
            if (e.getEmitidaPor() != null) ps.setInt(8, e.getEmitidaPor()); else ps.setNull(8, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "No se pudo registrar licencia emitida", ex);
        }
    }

    @Override
    public List<LicenciaEmitida> listarEmitidas(int limite) {
        String sql = "SELECT * FROM licencia_emitida ORDER BY fecha DESC LIMIT ?";
        List<LicenciaEmitida> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, Math.min(limite, 1000)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo listar licencias emitidas", e);
        }
        return out;
    }

    @Override
    public Optional<LicenciaEmitida> buscarPorClaveHash(String claveHash) {
        String sql = "SELECT * FROM licencia_emitida WHERE clave_hash = ? ORDER BY fecha DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, claveHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo buscar licencia por hash", e);
            return Optional.empty();
        }
    }

    @Override
    public void revocar(String machineCode, String motivo, Integer usuarioId) {
        String sql = "INSERT INTO licencia_revocada (machine_code, motivo, revocada_por) VALUES (?, ?, ?) " +
                "ON CONFLICT (machine_code) DO UPDATE SET motivo = EXCLUDED.motivo, revocada_por = EXCLUDED.revocada_por, fecha = now()";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, machineCode);
            ps.setString(2, motivo);
            if (usuarioId != null) ps.setInt(3, usuarioId); else ps.setNull(3, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo revocar licencia de " + machineCode, e);
        }
    }

    @Override
    public boolean estaRevocada(String machineCode) {
        String sql = "SELECT 1 FROM licencia_revocada WHERE machine_code = ? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, machineCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo consultar revocación de " + machineCode, e);
            return false;
        }
    }

    @Override
    public List<String> listarRevocadas(int limite) {
        String sql = "SELECT machine_code FROM licencia_revocada ORDER BY fecha DESC LIMIT ?";
        List<String> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, Math.min(limite, 1000)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo listar revocaciones", e);
        }
        return out;
    }

    private LicenciaEmitida mapear(ResultSet rs) throws SQLException {
        LicenciaEmitida e = new LicenciaEmitida();
        e.setId(rs.getLong("id"));
        e.setMachineCode(rs.getString("machine_code"));
        e.setClaveHash(rs.getString("clave_hash"));
        e.setModalidad(rs.getString("modalidad"));
        e.setIncluyeIA(rs.getBoolean("incluye_ia"));
        e.setRoles(rs.getString("roles"));
        e.setCapacidad(rs.getLong("capacidad"));
        Date exp = rs.getDate("expiracion");
        if (exp != null) e.setExpiracion(exp.toLocalDate());
        int por = rs.getInt("emitida_por");
        if (!rs.wasNull()) e.setEmitidaPor(por);
        Timestamp f = rs.getTimestamp("fecha");
        if (f != null) e.setFecha(f.toLocalDateTime());
        return e;
    }
}

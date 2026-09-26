package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.RetencionRegistro;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetencionRegistroDAOPostgres implements RetencionRegistroDAO {

    private static final Logger LOGGER = Logger.getLogger(RetencionRegistroDAO.class.getName());
    private final LogDAO logDAO = new LogDAO();

    public int insertar(RetencionRegistro r) {
        try (Connection con = DatabaseConnection.getConnection()) { return insertar(con, r); } catch (SQLException e) { if (e.getMessage() != null && e.getMessage().contains("does not exist")) { DatabaseConnection.ensureRetencionSchema(); return insertar(r); } LOGGER.log(Level.SEVERE, "insertar Retencion", e); } return -1; }
    public int insertar(Connection con, RetencionRegistro r) throws SQLException {
        String sql = "INSERT INTO retencion_registro(clave_acceso, establecimiento, punto_emision, secuencial, fecha_emision, periodo_fiscal, proveedor_id, tipo_identificacion_sujeto, razon_social_sujeto, identificacion_sujeto, estado_sri, xml_firmado, usuario_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getClaveAcceso());
            ps.setString(2, r.getEstablecimiento());
            ps.setString(3, r.getPuntoEmision());
            ps.setString(4, r.getSecuencial());
            ps.setObject(5, r.getFechaEmision());
            ps.setString(6, r.getPeriodoFiscal());
            if (r.getProveedorId() != null) ps.setInt(7, r.getProveedorId()); else ps.setObject(7, null);
            ps.setString(8, r.getTipoIdentificacionSujeto());
            ps.setString(9, r.getRazonSocialSujeto());
            ps.setString(10, r.getIdentificacionSujeto());
            ps.setString(11, r.getEstadoSri());
            ps.setString(12, r.getXmlFirmado());
            ps.setInt(13, r.getUsuarioId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public RetencionRegistro obtenerPorClave(String clave) {
        String sql = "SELECT * FROM retencion_registro WHERE clave_acceso=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "obtenerPorClave Retencion", e); }
        return null;
    }

    public RetencionRegistro obtenerPorId(int id) {
        String sql = "SELECT * FROM retencion_registro WHERE id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "obtenerPorId Retencion", e); }
        return null;
    }

    public List<RetencionRegistro> listarPendientesSri() {
        String sql = "SELECT * FROM retencion_registro WHERE estado_sri IN ('PENDIENTE','RECIBIDA','ERROR') OR estado_sri LIKE 'PENDIENTE%' ORDER BY fecha_emision ASC LIMIT 50";
        List<RetencionRegistro> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensureRetencionSchema(); } catch (Exception ignore) {}
            }
        }
        return lista;
    }

    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        try (Connection con = DatabaseConnection.getConnection()) { actualizarEstado(con, claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion); } catch (SQLException e) { LOGGER.log(Level.WARNING, "actualizarEstado Retencion", e); }
    }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException {
        String sql = "UPDATE retencion_registro SET estado_sri=?, mensaje_sri=?, numero_autorizacion=?, fecha_autorizacion=? WHERE clave_acceso=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje != null && mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje);
            ps.setString(3, numeroAutorizacion);
            if (fechaAutorizacion != null) {
                try { ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.parse(fechaAutorizacion.replace("T", " ").substring(0, 19)))); } catch (Exception e) { ps.setObject(4, null); }
            } else ps.setObject(4, null);
            ps.setString(5, claveAcceso);
            ps.executeUpdate();
        }
    }

    private RetencionRegistro mapear(ResultSet rs) throws SQLException {
        RetencionRegistro r = new RetencionRegistro();
        r.setId(rs.getInt("id"));
        r.setClaveAcceso(rs.getString("clave_acceso"));
        r.setEstablecimiento(rs.getString("establecimiento"));
        r.setPuntoEmision(rs.getString("punto_emision"));
        r.setSecuencial(rs.getString("secuencial"));
        r.setNumComprobante(r.getEstablecimiento() + "-" + r.getPuntoEmision() + "-" + r.getSecuencial());
        r.setPeriodoFiscal(rs.getString("periodo_fiscal"));
        int prov = rs.getInt("proveedor_id"); r.setProveedorId(rs.wasNull() ? null : prov);
        r.setTipoIdentificacionSujeto(rs.getString("tipo_identificacion_sujeto"));
        r.setRazonSocialSujeto(rs.getString("razon_social_sujeto"));
        r.setIdentificacionSujeto(rs.getString("identificacion_sujeto"));
        try { r.setFechaEmision(rs.getObject("fecha_emision", LocalDateTime.class)); } catch (Exception e) { r.setFechaEmision(rs.getTimestamp("fecha_emision") != null ? rs.getTimestamp("fecha_emision").toLocalDateTime() : null); }
        r.setEstadoSri(rs.getString("estado_sri"));
        r.setMensajeSri(rs.getString("mensaje_sri"));
        r.setNumeroAutorizacion(rs.getString("numero_autorizacion"));
        try { r.setFechaAutorizacion(rs.getObject("fecha_autorizacion", LocalDateTime.class)); } catch (Exception e) { Timestamp t = rs.getTimestamp("fecha_autorizacion"); r.setFechaAutorizacion(t != null ? t.toLocalDateTime() : null); }
        r.setXmlFirmado(rs.getString("xml_firmado"));
        r.setUsuarioId(rs.getInt("usuario_id"));
        return r;
    }
}

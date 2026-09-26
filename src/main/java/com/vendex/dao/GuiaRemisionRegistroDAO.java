package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.GuiaRemisionRegistro;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiaRemisionRegistroDAO {

    private static final Logger LOGGER = Logger.getLogger(GuiaRemisionRegistroDAO.class.getName());
    private final LogDAO logDAO = new LogDAO();

    public int insertar(GuiaRemisionRegistro r) {
        try (Connection con = DatabaseConnection.getConnection()) { return insertar(con, r); } catch (SQLException e) { if (e.getMessage() != null && e.getMessage().contains("does not exist")) { DatabaseConnection.ensureGuiaRemisionSchema(); return insertar(r); } LOGGER.log(Level.SEVERE, "insertar GR", e); } return -1; }
    public int insertar(Connection con, GuiaRemisionRegistro r) throws SQLException {
        String sql = "INSERT INTO guia_remision_registro(clave_acceso, establecimiento, punto_emision, secuencial, fecha_emision, dir_partida, razon_social_transportista, tipo_identificacion_transportista, ruc_transportista, placa, fecha_ini_transporte, fecha_fin_transporte, estado_sri, xml_firmado, usuario_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getClaveAcceso());
            ps.setString(2, r.getEstablecimiento());
            ps.setString(3, r.getPuntoEmision());
            ps.setString(4, r.getSecuencial());
            ps.setObject(5, r.getFechaEmision());
            ps.setString(6, r.getDirPartida());
            ps.setString(7, r.getRazonSocialTransportista());
            ps.setString(8, r.getTipoIdentificacionTransportista());
            ps.setString(9, r.getRucTransportista());
            ps.setString(10, r.getPlaca());
            ps.setObject(11, r.getFechaIniTransporte());
            ps.setObject(12, r.getFechaFinTransporte());
            ps.setString(13, r.getEstadoSri());
            ps.setString(14, r.getXmlFirmado());
            ps.setInt(15, r.getUsuarioId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public GuiaRemisionRegistro obtenerPorClave(String clave) {
        String sql = "SELECT * FROM guia_remision_registro WHERE clave_acceso=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "obtenerPorClave GR", e); }
        return null;
    }

    public GuiaRemisionRegistro obtenerPorId(int id) {
        String sql = "SELECT * FROM guia_remision_registro WHERE id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "obtenerPorId GR", e); }
        return null;
    }

    public List<GuiaRemisionRegistro> listarPendientesSri() {
        String sql = "SELECT * FROM guia_remision_registro WHERE estado_sri IN ('PENDIENTE','RECIBIDA','ERROR') OR estado_sri LIKE 'PENDIENTE%' ORDER BY fecha_emision ASC LIMIT 50";
        List<GuiaRemisionRegistro> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensureGuiaRemisionSchema(); } catch (Exception ignore) {}
            }
        }
        return lista;
    }

    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        try (Connection con = DatabaseConnection.getConnection()) { actualizarEstado(con, claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion); } catch (SQLException e) { LOGGER.log(Level.WARNING, "actualizarEstado GR", e); }
    }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException {
        String sql = "UPDATE guia_remision_registro SET estado_sri=?, mensaje_sri=?, numero_autorizacion=?, fecha_autorizacion=? WHERE clave_acceso=?";
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

    private GuiaRemisionRegistro mapear(ResultSet rs) throws SQLException {
        GuiaRemisionRegistro r = new GuiaRemisionRegistro();
        r.setId(rs.getInt("id"));
        r.setClaveAcceso(rs.getString("clave_acceso"));
        r.setEstablecimiento(rs.getString("establecimiento"));
        r.setPuntoEmision(rs.getString("punto_emision"));
        r.setSecuencial(rs.getString("secuencial"));
        r.setNumComprobante(r.getEstablecimiento() + "-" + r.getPuntoEmision() + "-" + r.getSecuencial());
        r.setDirPartida(rs.getString("dir_partida"));
        r.setRazonSocialTransportista(rs.getString("razon_social_transportista"));
        r.setTipoIdentificacionTransportista(rs.getString("tipo_identificacion_transportista"));
        r.setRucTransportista(rs.getString("ruc_transportista"));
        r.setPlaca(rs.getString("placa"));
        try { r.setFechaEmision(rs.getObject("fecha_emision", LocalDateTime.class)); } catch (Exception e) { r.setFechaEmision(rs.getTimestamp("fecha_emision") != null ? rs.getTimestamp("fecha_emision").toLocalDateTime() : null); }
        try { r.setFechaIniTransporte(rs.getObject("fecha_ini_transporte", LocalDate.class)); } catch (Exception e) { java.sql.Date d = rs.getDate("fecha_ini_transporte"); r.setFechaIniTransporte(d != null ? d.toLocalDate() : null); }
        try { r.setFechaFinTransporte(rs.getObject("fecha_fin_transporte", LocalDate.class)); } catch (Exception e) { java.sql.Date d = rs.getDate("fecha_fin_transporte"); r.setFechaFinTransporte(d != null ? d.toLocalDate() : null); }
        r.setEstadoSri(rs.getString("estado_sri"));
        r.setMensajeSri(rs.getString("mensaje_sri"));
        r.setNumeroAutorizacion(rs.getString("numero_autorizacion"));
        try { r.setFechaAutorizacion(rs.getObject("fecha_autorizacion", LocalDateTime.class)); } catch (Exception e) { Timestamp t = rs.getTimestamp("fecha_autorizacion"); r.setFechaAutorizacion(t != null ? t.toLocalDateTime() : null); }
        r.setXmlFirmado(rs.getString("xml_firmado"));
        r.setUsuarioId(rs.getInt("usuario_id"));
        return r;
    }
}

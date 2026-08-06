package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComprobanteDAO {

    private static final Logger LOGGER = Logger.getLogger(ComprobanteDAO.class.getName());

    public int obtenerSecuencial(String tipoComprobante) {
        String insertar = "INSERT INTO secuenciales(tipo_comprobante, secuencial, punto_emision, establecimiento) " +
                          "VALUES (?, 0, '001', '001') ON CONFLICT (tipo_comprobante) DO NOTHING";
        String actualizar = "UPDATE secuenciales SET secuencial = secuencial + 1 WHERE tipo_comprobante = ? RETURNING secuencial";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement psIns = con.prepareStatement(insertar)) {
            psIns.setString(1, tipoComprobante);
            psIns.executeUpdate();
            try (PreparedStatement ps = con.prepareStatement(actualizar)) {
                ps.setString(1, tipoComprobante);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de ComprobanteDAO", e);
        }
        return 1;
    }

    public void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante,
                         String ambiente, String xmlGenerado) {
        String sql = "INSERT INTO comprobantes_electronicos(nota_venta_id, tipo_comprobante, clave_acceso, " +
                     "numero_comprobante, ambiente, estado_sri, xml_generado) VALUES (?, '01', ?, ?, ?, 'PENDIENTE', ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, idRelacionado, Types.INTEGER);
            ps.setString(2, claveAcceso);
            ps.setString(3, numeroComprobante);
            ps.setString(4, ambiente);
            ps.setString(5, xmlGenerado);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de ComprobanteDAO", e);
        }
    }

    public void actualizarEstado(String claveAcceso, String estado, String mensaje,
                                  String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion) {
        String sql = "UPDATE comprobantes_electronicos SET estado_sri = ?, mensaje_sri = ?, " +
                     "xml_autorizado = ?, numero_autorizacion = ?, fecha_autorizacion = ? WHERE clave_acceso = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, xmlAutorizado);
            ps.setString(4, numeroAutorizacion);
            ps.setTimestamp(5, parsearFechaAutorizacion(fechaAutorizacion));
            ps.setString(6, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de ComprobanteDAO", e);
        }
    }

    public void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente,
                             String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion,
                             String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        String sql = "INSERT INTO xml_enviados(clave_acceso, numero_comprobante, ambiente, tipo_comprobante, " +
                     "xml_enviado, respuesta_recepcion, respuesta_autorizacion, estado_sri, mensaje_sri, " +
                     "numero_autorizacion, fecha_autorizacion) VALUES (?, ?, ?, '01', ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, claveAcceso);
            ps.setString(2, numeroComprobante);
            ps.setString(3, ambiente);
            ps.setString(4, xmlEnviado);
            ps.setString(5, respuestaRecepcion);
            ps.setString(6, respuestaAutorizacion);
            ps.setString(7, estado);
            ps.setString(8, mensaje);
            ps.setString(9, numeroAutorizacion);
            ps.setTimestamp(10, parsearFechaAutorizacion(fechaAutorizacion));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de ComprobanteDAO", e);
        }
    }

    /**
     * Convierte la fecha de autorización devuelta por el SRI (ISO-8601, ej.
     * "2026-08-02T22:41:00.000-05:00" o "...Z") a timestamp local. Devuelve null
     * si el valor es nulo o no es parseable.
     */
    private java.sql.Timestamp parsearFechaAutorizacion(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return null;
        try {
            String valor = fecha.trim().replace("Z", "+00:00");
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(valor,
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return java.sql.Timestamp.valueOf(
                    odt.atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Devuelve el siguiente número de secuencia para el tipo de comprobante, sin incrementarlo.
     */
    public int consultarSecuencial(String tipoComprobante) {
        String sql = "SELECT secuencial + 1 FROM secuenciales WHERE tipo_comprobante = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipoComprobante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de ComprobanteDAO", e);
        }
        return 1;
    }
}

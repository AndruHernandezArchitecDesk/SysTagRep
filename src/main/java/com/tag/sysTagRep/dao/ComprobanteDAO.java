package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;

import java.sql.*;

public class ComprobanteDAO {

    public int obtenerSecuencial(String tipoComprobante) {
        String insertar = "INSERT INTO secuenciales(tipo_comprobante, secuencial, punto_emision, establecimiento) " +
                          "VALUES (?, 1, '001', '001') ON CONFLICT (tipo_comprobante) DO NOTHING";
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public void actualizarEstado(String claveAcceso, String estado, String mensaje,
                                  String xmlAutorizado, String numeroAutorizacion) {
        String sql = "UPDATE comprobantes_electronicos SET estado_sri = ?, mensaje_sri = ?, " +
                     "xml_autorizado = ?, fecha_autorizacion = NOW(), numero_autorizacion = ? WHERE clave_acceso = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, xmlAutorizado);
            ps.setString(4, numeroAutorizacion);
            ps.setString(5, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente,
                             String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion,
                             String estado, String mensaje, String numeroAutorizacion) {
        String sql = "INSERT INTO xml_enviados(clave_acceso, numero_comprobante, ambiente, tipo_comprobante, " +
                     "xml_enviado, respuesta_recepcion, respuesta_autorizacion, estado_sri, mensaje_sri, " +
                     "numero_autorizacion, fecha_autorizacion) VALUES (?, ?, ?, '01', ?, ?, ?, ?, ?, ?, " +
                     "CASE WHEN ? IS NULL THEN NULL ELSE NOW() END)";
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
            ps.setString(10, numeroAutorizacion);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return 1;
    }
}

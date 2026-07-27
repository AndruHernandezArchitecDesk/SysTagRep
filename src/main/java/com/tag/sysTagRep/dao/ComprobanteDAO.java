package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;

import java.sql.*;

public class ComprobanteDAO {

    public int obtenerSecuencial(String tipoComprobante) {
        String sql = "UPDATE secuenciales SET secuencial = secuencial + 1 WHERE tipo_comprobante = ? RETURNING secuencial";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipoComprobante);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public void insertar(String claveAcceso, int notaVentaId, String numeroComprobante,
                         String ambiente, String xmlGenerado) {
        String sql = "INSERT INTO comprobantes_electronicos(nota_venta_id, tipo_comprobante, clave_acceso, " +
                     "numero_comprobante, ambiente, estado_sri, xml_generado) VALUES (?, '01', ?, ?, ?, 'PENDIENTE', ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notaVentaId);
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
                                  String xmlAutorizado) {
        String sql = "UPDATE comprobantes_electronicos SET estado_sri = ?, mensaje_sri = ?, " +
                     "xml_autorizado = ?, fecha_autorizacion = NOW() WHERE clave_acceso = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, xmlAutorizado);
            ps.setString(4, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

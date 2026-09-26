package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CertificadoEstadoDAOPostgres implements CertificadoEstadoDAO {

    private static final Logger LOGGER = Logger.getLogger(CertificadoEstadoDAO.class.getName());

    public void guardarEstado(String rutaP12, String titular, LocalDate emision, LocalDate expiracion, int dias, String nivel) {
        String sql = "INSERT INTO certificado_estado(ruta_p12, titular, fecha_emision, fecha_expiracion, dias_restantes, nivel_severidad, ultima_verificacion) VALUES (?,?,?,?,?,?,NOW())";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rutaP12);
            ps.setString(2, titular != null && titular.length() > 300 ? titular.substring(0,300) : titular);
            ps.setObject(3, emision);
            ps.setObject(4, expiracion);
            ps.setInt(5, dias);
            ps.setString(6, nivel);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage()!=null && e.getMessage().contains("does not exist")) {
                DatabaseConnection.ensureCertificadoEstadoSchema();
                guardarEstado(rutaP12, titular, emision, expiracion, dias, nivel);
            } else LOGGER.log(Level.WARNING, "guardarEstado cert", e);
        }
    }

    public Timestamp obtenerUltimoEmailEnviado(String rutaP12) {
        String sql = "SELECT ultimo_email_enviado FROM certificado_estado WHERE ruta_p12=? ORDER BY ultima_verificacion DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rutaP12);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getTimestamp("ultimo_email_enviado");
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "obtenerUltimoEmail", e); }
        return null;
    }

    public void actualizarUltimoEmailEnviado(String rutaP12) {
        String sql = "UPDATE certificado_estado SET ultimo_email_enviado=NOW() WHERE id=(SELECT id FROM certificado_estado WHERE ruta_p12=? ORDER BY ultima_verificacion DESC LIMIT 1)";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rutaP12);
            ps.executeUpdate();
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "actualizarUltimoEmail", e); }
    }

    public boolean debeEnviarCorreo(String rutaP12, String nivel, Timestamp ultimoEnvio) {
        if (ultimoEnvio == null) return !"VIGENTE".equals(nivel) && !"AVISO".equals(nivel);
        long horas = (System.currentTimeMillis() - ultimoEnvio.getTime()) / (1000*60*60);
        switch (nivel) {
            case "ADVERTENCIA": return horas >= 24*7;
            case "CRITICO":
            case "EXPIRADO": return horas >= 24;
            default: return false;
        }
    }
}

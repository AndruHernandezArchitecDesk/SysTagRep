package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.ConfiguracionEmail;
import com.vendex.util.Cifrado;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;

public class ConfiguracionEmailDAO {

    public Optional<ConfiguracionEmail> obtenerActiva() {
        String sql = "SELECT id, host_smtp, puerto_smtp, usar_tls, email_remitente, nombre_remitente, " +
                "usuario_smtp, password_cifrado, reply_to, activo, actualizado_en, actualizado_por " +
                "FROM configuracion_email WHERE activo = true ORDER BY id DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void guardar(ConfiguracionEmail cfg) throws SQLException {
        String update = "UPDATE configuracion_email SET host_smtp=?, puerto_smtp=?, usar_tls=?, " +
                "email_remitente=?, nombre_remitente=?, usuario_smtp=?, password_cifrado=?, reply_to=?, " +
                "activo=true, actualizado_en=now(), actualizado_por=? WHERE id=?";
        String insert = "INSERT INTO configuracion_email " +
                "(host_smtp, puerto_smtp, usar_tls, email_remitente, nombre_remitente, usuario_smtp, password_cifrado, reply_to, activo, actualizado_en, actualizado_por) " +
                "VALUES (?,?,?,?,?,?,?,?,true,now(),?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            if (cfg.getId() > 0) {
                try (PreparedStatement ps = con.prepareStatement(update)) {
                    ps.setString(1, cfg.getHostSmtp());
                    ps.setInt(2, cfg.getPuertoSmtp());
                    ps.setBoolean(3, cfg.isUsarTls());
                    ps.setString(4, cfg.getEmailRemitente());
                    ps.setString(5, cfg.getNombreRemitente());
                    ps.setString(6, cfg.getUsuarioSmtp());
                    ps.setString(7, cfg.getPasswordCifrado());
                    ps.setString(8, cfg.getReplyTo());
                    if (cfg.getActualizadoPor() != null) ps.setInt(9, cfg.getActualizadoPor());
                    else ps.setNull(9, Types.INTEGER);
                    ps.setInt(10, cfg.getId());
                    int rows = ps.executeUpdate();
                    if (rows > 0) return;
                }
            }
            // desactivar anteriores y luego insertar (garantiza una sola activa)
            try (Statement st = con.createStatement()) {
                st.execute("UPDATE configuracion_email SET activo=false WHERE activo=true");
            } catch (SQLException ignore) {}
            try (PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, cfg.getHostSmtp());
                ps.setInt(2, cfg.getPuertoSmtp());
                ps.setBoolean(3, cfg.isUsarTls());
                ps.setString(4, cfg.getEmailRemitente());
                ps.setString(5, cfg.getNombreRemitente());
                ps.setString(6, cfg.getUsuarioSmtp());
                ps.setString(7, cfg.getPasswordCifrado());
                ps.setString(8, cfg.getReplyTo());
                if (cfg.getActualizadoPor() != null) ps.setInt(9, cfg.getActualizadoPor());
                else ps.setNull(9, Types.INTEGER);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) cfg.setId(keys.getInt(1));
                }
            }
        }
    }

    public String obtenerPasswordPlano(ConfiguracionEmail cfg) {
        if (cfg == null || cfg.getPasswordCifrado() == null || cfg.getPasswordCifrado().isEmpty()) return "";
        // Tier 2 por instalación (compartido): probar master key, fallback Tier1/legacy para compat
        try {
            return com.vendex.util.SecureConfigStore.descifrarConMasterKey(cfg.getPasswordCifrado());
        } catch (com.vendex.exception.SecretoNoEncontradoException se) {
            // sin master key en esta PC → explicar claro
            throw new RuntimeException("Clave maestra de instalación no importada en esta PC. Importa la vendex-master-key desde la primera PC (wizard BD).", se);
        } catch (Exception e) {
            try { return Cifrado.desencriptar(cfg.getPasswordCifrado()); } catch (Exception e2) { return ""; }
        }
    }

    /** Cifra para guardar en BD con Tier2 (install master). Fallback Tier1 si master no existe (compat). */
    public static String cifrarParaGuardar(String plano) {
        if (plano == null || plano.isEmpty()) return "";
        try {
            return com.vendex.util.SecureConfigStore.cifrarConMasterKey(plano);
        } catch (Exception e) {
            try { return Cifrado.encriptar(plano); } catch (Exception e2) { throw new RuntimeException(e2); }
        }
    }

    /**
     * Prueba conexión SMTP sin enviar correo, distingue credenciales vs red/host.
     * @return null si ok, mensaje de error descriptivo si falla
     */
    public String probarConexion(ConfiguracionEmail cfg, String passwordPlano) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        String host = cfg.getHostSmtp();
        int puerto = cfg.getPuertoSmtp();
        boolean tls = cfg.isUsarTls();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(puerto));
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        if (puerto == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", String.valueOf(tls));
        }

        Session session = Session.getInstance(props);
        Transport transport = null;
        try {
            transport = session.getTransport("smtp");
            transport.connect(host, puerto, cfg.getUsuarioSmtp(), passwordPlano);
            return null;
        } catch (AuthenticationFailedException e) {
            return "Credenciales incorrectas (usuario/contraseña). Verifique usuario SMTP y contraseña/app-password.";
        } catch (MessagingException e) {
            Throwable cause = e.getCause();
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (cause instanceof UnknownHostException || msg.contains("unknownhost") || msg.contains("no such host")) {
                return "Host SMTP no alcanzable: " + host + " (" + e.getMessage() + ")";
            }
            if (cause instanceof ConnectException || msg.contains("connection refused") || msg.contains("connect") || msg.contains("timeout")) {
                return "No se pudo conectar a " + host + ":" + puerto + " (puerto bloqueado/firewall/timeout). " + e.getMessage();
            }
            return "Error SMTP: " + e.getMessage();
        } catch (Exception e) {
            return "Error inesperado: " + e.getMessage();
        } finally {
            if (transport != null) try { transport.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * Envía un correo de prueba real usando la config dada (sin persistir).
     * @return null si ok, mensaje de error si falla
     */
    public String probarEnvio(ConfiguracionEmail cfg, String passwordPlano, String destinatario) {
        String errConn = probarConexion(cfg, passwordPlano);
        if (errConn != null) return errConn;
        // si connect ok, intentar envío real mínimo
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", cfg.getHostSmtp());
        props.put("mail.smtp.port", String.valueOf(cfg.getPuertoSmtp()));
        props.put("mail.smtp.ssl.trust", cfg.getHostSmtp());
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        if (cfg.getPuertoSmtp() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", String.valueOf(cfg.isUsarTls()));
        }
        jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(cfg.getUsuarioSmtp(), passwordPlano);
            }
        });
        try {
            jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
            msg.setFrom(new jakarta.mail.internet.InternetAddress(cfg.getEmailRemitente(), cfg.getNombreRemitente()));
            msg.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(destinatario));
            if (cfg.getReplyTo() != null && !cfg.getReplyTo().trim().isEmpty()) {
                msg.setReplyTo(jakarta.mail.internet.InternetAddress.parse(cfg.getReplyTo().trim()));
            }
            msg.setSubject("Correo de prueba - Vendex");
            msg.setText("Este es un correo de prueba de Vendex.\n\nSi recibes este mensaje, la configuración SMTP es correcta.\nHost: " + cfg.getHostSmtp() + ":" + cfg.getPuertoSmtp());
            jakarta.mail.Transport.send(msg);
            return null;
        } catch (Exception e) {
            return "Conexión OK pero fallo al enviar: " + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private ConfiguracionEmail mapear(ResultSet rs) throws SQLException {
        ConfiguracionEmail c = new ConfiguracionEmail();
        c.setId(rs.getInt("id"));
        c.setHostSmtp(rs.getString("host_smtp"));
        c.setPuertoSmtp(rs.getInt("puerto_smtp"));
        c.setUsarTls(rs.getBoolean("usar_tls"));
        c.setEmailRemitente(rs.getString("email_remitente"));
        c.setNombreRemitente(rs.getString("nombre_remitente"));
        c.setUsuarioSmtp(rs.getString("usuario_smtp"));
        c.setPasswordCifrado(rs.getString("password_cifrado"));
        c.setReplyTo(rs.getString("reply_to"));
        c.setActivo(rs.getBoolean("activo"));
        Timestamp ts = rs.getTimestamp("actualizado_en");
        if (ts != null) c.setActualizadoEn(ts.toLocalDateTime());
        int ap = rs.getInt("actualizado_por");
        if (!rs.wasNull()) c.setActualizadoPor(ap);
        return c;
    }
}

package com.vendex.backup;

import com.vendex.util.EmailService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notificaciones de backup vía EmailService / ConfiguracionEmail.
 * Destino configurable en backup.properties (backup.email.destino) — por defecto andresrockfull@gmail.com.
 */
public class BackupNotificationService {

    private static final Logger LOG = Logger.getLogger(BackupNotificationService.class.getName());
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void notificarExito(BackupResult result) {
        if (!BackupConfig.isNotificarExito()) {
            LOG.info("Notificación de éxito deshabilitada (backup.notificar.exito=false)");
            return;
        }
        String destino = BackupConfig.getEmailDestino();
        String asunto = "Vendex Backup OK - " + LocalDateTime.now().format(FMT);
        String cuerpo = "Backup completado correctamente.\n"
                + "Archivo: " + (result.archivoLocal != null ? result.archivoLocal.getName() : "N/A") + "\n"
                + "Tamaño: " + formatSize(result.tamanioBytes) + "\n"
                + "Duración: " + (result.duracionMs/1000) + "s\n"
                + "Offsite: " + (result.copiadoOffsite ? "OK -> " + result.offsiteDestino : "NO (" + result.errorOffsite + ")") + "\n"
                + "Dir local: " + BackupConfig.getBackupDir() + "\n"
                + "Offsite path: " + BackupConfig.getOffsitePath() + "\n";
        enviar(destino, asunto, cuerpo, "BACKUP_EXITO");
    }

    public void notificarFalla(String motivo, Throwable ex) { notificarFalla(motivo, ex, null); }
    public void notificarFalla(String motivo, Throwable ex, File logFile) {
        String destino = BackupConfig.getEmailDestino();
        String detalle = ex != null ? (ex.getMessage() != null ? ex.getMessage() : ex.toString()) : "";
        String asunto = "[ALERTA] Vendex Backup FALLÓ - " + LocalDateTime.now().format(FMT);
        String cuerpo = "El backup de Vendex falló.\n\n"
                + "Motivo: " + motivo + "\n"
                + (detalle.isEmpty() ? "" : "Detalle: " + detalle + "\n")
                + "Fecha: " + LocalDateTime.now().format(FMT) + "\n"
                + "Dir local: " + BackupConfig.getBackupDir() + "\n"
                + "Offsite path: " + BackupConfig.getOffsitePath() + "\n"
                + "\nAcción requerida: revisar logs y ejecutar respaldo manual desde Administración → Respaldos.";
        // Enviar sin adjuntos para simplicidad; logFile opcional podría adjuntarse si se desea
        enviar(destino, asunto, cuerpo, "BACKUP_FALLA");
    }

    public void notificarFallaConResultado(BackupResult result) {
        String destino = BackupConfig.getEmailDestino();
        String asunto = "[ALERTA] Vendex Backup FALLÓ - " + LocalDateTime.now().format(FMT);
        String cuerpo = "El backup de Vendex falló.\n\n"
                + "Motivo: " + result.mensaje + "\n"
                + "Offsite error: " + (result.errorOffsite != null ? result.errorOffsite : "n/a") + "\n"
                + "Fecha: " + LocalDateTime.now().format(FMT) + "\n"
                + "Dir local: " + BackupConfig.getBackupDir() + "\n";
        enviar(destino, asunto, cuerpo, "BACKUP_FALLA");
    }

    private void enviar(String destino, String asunto, String cuerpo, String tipo) {
        if (destino == null || destino.isBlank()) {
            LOG.warning("Sin destinatario de notificación backup");
            return;
        }
        // Intentar envío con asunto/cuerpo exactos vía enviarCorreoSimple; fallback a EmailService si falla
        boolean ok = enviarCorreoSimple(destino, asunto, cuerpo);
        if (!ok) {
            try {
                EmailService emailService = new EmailService();
                ok = emailService.enviarCorreoConArchivos(destino, cuerpo, asunto, tipo, null, null);
                if (!ok) {
                    LOG.log(Level.WARNING, "Fallo envío notificación backup a " + destino + ": " + emailService.getUltimoError());
                } else {
                    LOG.info("Notificación backup enviada (fallback EmailService) a " + destino + " tipo=" + tipo);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Excepción enviando notificación backup", e);
            }
        } else {
            LOG.info("Notificación backup enviada a " + destino + " asunto=" + asunto);
        }
    }

    /** Envío con plantilla exacta (asunto/cuerpo controlados) usando JavaMail directo con ConfiguracionEmailDAO. */
    public boolean enviarCorreoSimple(String destino, String subject, String body) {
        if (destino == null || destino.isBlank()) return false;
        try {
            var dao = new com.vendex.dao.ConfiguracionEmailDAOPostgres();
            var opt = dao.obtenerActiva();
            String host; int puerto; boolean tls; String remitente; String nombreRemitente; String usuario; String pass;
            if (opt.isPresent()) {
                var cfg = opt.get();
                host = cfg.getHostSmtp();
                puerto = cfg.getPuertoSmtp();
                tls = cfg.isUsarTls();
                remitente = cfg.getEmailRemitente();
                nombreRemitente = cfg.getNombreRemitente();
                usuario = cfg.getUsuarioSmtp();
                pass = dao.obtenerPasswordPlano(cfg);
            } else {
                // sin configuracion_email, no se puede enviar
                LOG.warning("No hay configuracion_email activa para notificar backup");
                return false;
            }
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(puerto));
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.connectiontimeout", "15000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "15000");
            if (puerto == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
            } else {
                props.put("mail.smtp.starttls.enable", String.valueOf(tls));
            }
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(usuario, pass);
                }
            });
            jakarta.mail.Message msg = new jakarta.mail.internet.MimeMessage(session);
            msg.setFrom(new jakarta.mail.internet.InternetAddress(remitente, nombreRemitente != null ? nombreRemitente : "Vendex"));
            msg.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(destino));
            msg.setSubject(subject);
            msg.setText(body);
            jakarta.mail.Transport.send(msg);
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "enviarCorreoSimple falló", e);
            return false;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024*1024) return String.format("%.1f KB", bytes/1024.0);
        return String.format("%.2f MB", bytes/(1024.0*1024));
    }
}

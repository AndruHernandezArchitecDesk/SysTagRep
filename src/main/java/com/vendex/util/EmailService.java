package com.vendex.util;

import com.vendex.dao.ConfiguracionEmailDAO;
import com.vendex.model.ConfiguracionEmail;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.vendex.dao.ConfiguracionEmailDAOPostgres;

public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private static volatile ConfiguracionEmail cachedConfig;
    private static final Object CACHE_LOCK = new Object();

    // Fallback legacy (solo si no hay fila activa y para compatibilidad tests sin BD)
    private static final String FALLBACK_REMITENTE = "tagrepuestosvick@gmail.com";
    private static final String FALLBACK_PASSWORD = "awnfnmidbtqyyclz";
    private static final String FALLBACK_HOST = "smtp.gmail.com";
    private static final int FALLBACK_PORT = 587;

    private String ultimoError;

    public String getUltimoError() {
        return ultimoError;
    }

    public static void recargarConfiguracion() {
        synchronized (CACHE_LOCK) {
            cachedConfig = null;
        }
    }

    public static void invalidarCache() { recargarConfiguracion(); }

    private ConfiguracionEmail obtenerConfig() {
        ConfiguracionEmail c = cachedConfig;
        if (c != null) return c;
        synchronized (CACHE_LOCK) {
            if (cachedConfig != null) return cachedConfig;
            try {
                Optional<ConfiguracionEmail> opt = new ConfiguracionEmailDAOPostgres().obtenerActiva();
                if (opt.isPresent()) {
                    cachedConfig = opt.get();
                    return cachedConfig;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "No se pudo leer configuracion_email, usando fallback", e);
            }
            return null;
        }
    }

    private String desencriptarPassword(ConfiguracionEmail cfg) {
        if (cfg == null || cfg.getPasswordCifrado() == null) return "";
        try {
            return Cifrado.desencriptar(cfg.getPasswordCifrado());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo desencriptar password SMTP", e);
            return "";
        }
    }

    public boolean enviarCorreoConPDF(String destinatario, String nombreCliente,
                                      String codigoDocumento, String tipoDocumento, File pdfAdjunto) {
        return enviarCorreoConArchivos(destinatario, nombreCliente, codigoDocumento, tipoDocumento,
                pdfAdjunto, null);
    }

    public boolean enviarCorreoConArchivos(String destinatario, String nombreCliente,
                                           String codigoDocumento, String tipoDocumento,
                                           File pdfAdjunto, File xmlAdjunto) {
        if (destinatario == null || destinatario.trim().isEmpty()) {
            ultimoError = "Destinatario vacío";
            return false;
        }

        ConfiguracionEmail cfg = obtenerConfig();
        String host;
        int puerto;
        boolean usarTls;
        String remitente;
        String nombreRemitente;
        String usuario;
        String passwordPlano;
        String replyTo;

        if (cfg != null) {
            host = cfg.getHostSmtp();
            puerto = cfg.getPuertoSmtp();
            usarTls = cfg.isUsarTls();
            remitente = cfg.getEmailRemitente();
            nombreRemitente = cfg.getNombreRemitente();
            usuario = cfg.getUsuarioSmtp();
            passwordPlano = desencriptarPassword(cfg);
            replyTo = cfg.getReplyTo();
            if (host == null || host.trim().isEmpty() || remitente == null || remitente.trim().isEmpty()) {
                ultimoError = "Configuración de correo incompleta. Vaya a Administración → Correo Electrónico y complete los datos.";
                LOGGER.warning("Configuracion email incompleta: host/remitente vacio");
                return false;
            }
            if (passwordPlano == null || passwordPlano.isEmpty()) {
                ultimoError = "Configuración de correo sin contraseña. Configure la contraseña SMTP.";
                LOGGER.warning("Configuracion email sin password descifrable");
                return false;
            }
        } else {
            // Fallback legacy solo si no hay configuración en BD (primer arranque sin migrar o test sin BD)
            LOGGER.warning("Sin configuracion_email activa, usando fallback legacy. Configure el correo en Administración → Correo Electrónico.");
            host = FALLBACK_HOST;
            puerto = FALLBACK_PORT;
            usarTls = true;
            remitente = FALLBACK_REMITENTE;
            nombreRemitente = "Vendex Repuestos";
            usuario = FALLBACK_REMITENTE;
            passwordPlano = FALLBACK_PASSWORD;
            replyTo = null;
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
            props.put("mail.smtp.starttls.enable", String.valueOf(usarTls));
            if (!usarTls) props.put("mail.smtp.ssl.enable", "false");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, passwordPlano);
            }
        });

        try {
            String tipo = (tipoDocumento == null || tipoDocumento.trim().isEmpty()) ? "PROFORMA" : tipoDocumento.toUpperCase();

            Message mensaje = new MimeMessage(session);
            mensaje.setFrom(new InternetAddress(remitente, nombreRemitente != null ? nombreRemitente : "Vendex Repuestos"));
            if (replyTo != null && !replyTo.trim().isEmpty()) {
                mensaje.setReplyTo(InternetAddress.parse(replyTo.trim()));
            }
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            mensaje.setSubject(tipo + " " + codigoDocumento + " - Vendex Repuestos");

            String asunto;
            if ("FACTURA".equals(tipo)) {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "Gracias por su compra en Vendex Repuestos Automotrices.\n\n"
                        + "Le confirmamos que su factura N. " + codigoDocumento + " ha sido emitida exitosamente.\n\n"
                        + "GARANTIA:\n"
                        + "Su compra cuenta con garantia respaldada por la factura N. " + codigoDocumento + ". "
                        + "Conserve este documento como respaldo de su garantia.\n\n"
                        + "REPOSICION DE REPUESTOS:\n"
                        + "En Vendex contamos con una amplia variedad de repuestos automotrices. "
                        + "Si el repuesto que necesita no lo tenemos disponible, lo importamos para usted "
                        + "en el menor tiempo posible.\n\n"
                        + "Si tiene alguna consulta, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "Vendex Repuestos Automotrices";
             } else if ("NOTA_CREDITO".equals(tipo) || "NOTA DE CREDITO".equals(tipo) || "NOTA_CREDITO".equals(tipoDocumento)) {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "Le informamos que se ha emitido la Nota de Crédito N. " + codigoDocumento + " en Vendex Repuestos Automotrices.\n\n"
                        + "Este documento acredita el ajuste/devuelto correspondiente a su factura original. "
                        + "Conserve este comprobante junto a su factura.\n\n"
                        + "Si tiene alguna consulta sobre el motivo del ajuste, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "Vendex Repuestos Automotrices";
            } else if ("NOTA_DEBITO".equals(tipo) || "NOTA DE DEBITO".equals(tipo) || "NOTA_DEBITO".equals(tipoDocumento)) {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "Le informamos que se ha emitido la Nota de Débito N. " + codigoDocumento + " en Vendex Repuestos Automotrices.\n\n"
                        + "Este documento acredita el cargo adicional correspondiente a su factura original. "
                        + "Conserve este comprobante junto a su factura.\n\n"
                        + "Si tiene alguna consulta sobre el motivo del cargo, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "Vendex Repuestos Automotrices";
            } else if ("GUIA_REMISION".equals(tipo) || "GUIA DE REMISION".equals(tipo) || AppConstants.TIPO_DOCUMENTO_GUIA_REMISION.equals(tipoDocumento)) {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "Le informamos que se ha emitido la Guía de Remisión N. " + codigoDocumento + " en Vendex Repuestos Automotrices.\n\n"
                        + "Este documento sustenta el traslado de mercadería. Conserve este comprobante junto a su factura cuando exista documento sustento.\n\n"
                        + "Si tiene alguna consulta sobre el traslado, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "Vendex Repuestos Automotrices";
            } else if ("RETENCION".equals(tipo) || "RETENCIÓN".equals(tipo) || "COMPROBANTE DE RETENCIÓN".equals(tipo) || AppConstants.TIPO_DOCUMENTO_RETENCION.equals(tipoDocumento)) {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "Le informamos que se ha emitido el Comprobante de Retención N. " + codigoDocumento + " en Vendex Repuestos Automotrices.\n\n"
                        + "Este documento acredita las retenciones efectuadas. Conserve este comprobante para su declaración.\n\n"
                        + "Si tiene alguna consulta, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "Vendex Repuestos Automotrices";
            } else if ("CERTIFICADO_POR_EXPIRAR".equals(tipo) || "CERTIFICADO".equals(tipo)) {
                asunto = "⚠ " + codigoDocumento + "\n\n"
                        + "Estimado Administrador,\n\n"
                        + nombreCliente + "\n\n"
                        + "Este es un aviso automático de Vendex: revisa el apartado Firma Electrónica para renovar el certificado antes del vencimiento.\n\n"
                        + "Atentamente,\nVendex Sistema";
            } else {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "En Vendex Repuestos Automotrices le presentamos su PROFORMA N. " + codigoDocumento + ".\n\n"
                        + "Este documento es una cotización informativa de los productos consultados; "
                        + "no representa una venta confirmada ni descuenta existencias de inventario.\n\n"
                        + "DISPONIBILIDAD:\n"
                        + "Contamos con una amplia variedad de repuestos automotrices. "
                        + "Si el repuesto que necesita no lo tenemos disponible, lo importamos para usted "
                        + "en el menor tiempo posible.\n\n"
                        + "Si tiene alguna consulta, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "Vendex Repuestos Automotrices";
            }

            mensaje.setText(asunto);

            java.util.List<File> adjuntos = new java.util.ArrayList<>();
            if (pdfAdjunto != null && pdfAdjunto.exists()) adjuntos.add(pdfAdjunto);
            if (xmlAdjunto != null && xmlAdjunto.exists()) adjuntos.add(xmlAdjunto);

            if (!adjuntos.isEmpty()) {
                MimeBodyPart textoParte = new MimeBodyPart();
                textoParte.setText(asunto);

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textoParte);

                for (File adjunto : adjuntos) {
                    MimeBodyPart adjuntoParte = new MimeBodyPart();
                    adjuntoParte.attachFile(adjunto);
                    adjuntoParte.setFileName(MimeUtility.encodeText(adjunto.getName()));
                    multipart.addBodyPart(adjuntoParte);
                }

                mensaje.setContent(multipart);
            }

            Transport.send(mensaje);
            ultimoError = null;
            return true;
        } catch (Exception e) {
            ultimoError = (e.getMessage() != null ? e.getMessage() : e.toString());
            LOGGER.log(Level.SEVERE, "Error enviando correo via " + host + ":" + puerto, e);
            return false;
        }
    }
}
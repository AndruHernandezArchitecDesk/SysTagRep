package com.tag.sysTagRep.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final String REMITENTE = "tagrepuestosvick@gmail.com";
    private static final String PASSWORD = "awnfnmidbtqyyclz";

    private String ultimoError;

    public String getUltimoError() {
        return ultimoError;
    }

    public boolean enviarCorreoConPDF(String destinatario, String nombreCliente,
                                      String codigoDocumento, String tipoDocumento, File pdfAdjunto) {
        return enviarCorreoConArchivos(destinatario, nombreCliente, codigoDocumento, tipoDocumento,
                pdfAdjunto, null);
    }

    public boolean enviarCorreoConArchivos(String destinatario, String nombreCliente,
                                           String codigoDocumento, String tipoDocumento,
                                           File pdfAdjunto, File xmlAdjunto) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMITENTE, PASSWORD);
            }
        });

        try {
            String tipo = (tipoDocumento == null || tipoDocumento.trim().isEmpty()) ? "PROFORMA" : tipoDocumento.toUpperCase();

            Message mensaje = new MimeMessage(session);
            mensaje.setFrom(new InternetAddress(REMITENTE, "SysTag Repuestos"));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            mensaje.setSubject(tipo + " " + codigoDocumento + " - SysTag Repuestos");

            String asunto;
            if ("FACTURA".equals(tipo)) {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "Gracias por su compra en SysTag Repuestos Automotrices.\n\n"
                        + "Le confirmamos que su factura N. " + codigoDocumento + " ha sido emitida exitosamente.\n\n"
                        + "GARANTIA:\n"
                        + "Su compra cuenta con garantia respaldada por la factura N. " + codigoDocumento + ". "
                        + "Conserve este documento como respaldo de su garantia.\n\n"
                        + "REPOSICION DE REPUESTOS:\n"
                        + "En SysTag contamos con una amplia variedad de repuestos automotrices. "
                        + "Si el repuesto que necesita no lo tenemos disponible, lo importamos para usted "
                        + "en el menor tiempo posible.\n\n"
                        + "Si tiene alguna consulta, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "SysTag Repuestos Automotrices";
            } else {
                asunto = "Estimado/a " + nombreCliente + ",\n\n"
                        + "En SysTag Repuestos Automotrices le presentamos su PROFORMA N. " + codigoDocumento + ".\n\n"
                        + "Este documento es una cotización informativa de los productos consultados; "
                        + "no representa una venta confirmada ni descuenta existencias de inventario.\n\n"
                        + "DISPONIBILIDAD:\n"
                        + "Contamos con una amplia variedad de repuestos automotrices. "
                        + "Si el repuesto que necesita no lo tenemos disponible, lo importamos para usted "
                        + "en el menor tiempo posible.\n\n"
                        + "Si tiene alguna consulta, no dude en contactarnos.\n\n"
                        + "Atentamente,\n"
                        + "SysTag Repuestos Automotrices";
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
            LOGGER.log(Level.SEVERE, "Error enviando correo", e);
            return false;
        }
    }
}

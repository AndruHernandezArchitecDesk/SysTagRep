package com.tag.sysTagRep.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.File;
import java.util.Properties;

public class EmailService {

    private static final String REMITENTE = "tagrepuestosvick@gmail.com";
    private static final String PASSWORD = "scvqccfhctynsphv";

    public boolean enviarCorreoConPDF(String destinatario, String nombreCliente, String codigoNotaVenta, File pdfAdjunto) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMITENTE, PASSWORD);
            }
        });

        try {
            Message mensaje = new MimeMessage(session);
            mensaje.setFrom(new InternetAddress(REMITENTE, "SysTag Repuestos"));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            mensaje.setSubject("Nota de Venta " + codigoNotaVenta + " - SysTag Repuestos");

            String asunto = "Estimado/a " + nombreCliente + ",\n\n"
                    + "Gracias por su compra en SysTag Repuestos Automotrices.\n\n"
                    + "Le confirmamos que su nota de venta N. " + codigoNotaVenta + " ha sido registrada exitosamente.\n\n"
                    + "GARANTIA:\n"
                    + "Su compra cuenta con garantia respaldada por la nota de venta N. " + codigoNotaVenta + ". "
                    + "Conserve este documento como respaldo de su garantia.\n\n"
                    + "REPOSICION DE REPUESTOS:\n"
                    + "En SysTag contamos con una amplia variedad de repuestos automotrices. "
                    + "Si el repuesto que necesita no lo tenemos disponible, lo importamos para usted "
                    + "en el menor tiempo posible.\n\n"
                    + "Si tiene alguna consulta, no dude en contactarnos.\n\n"
                    + "Atentamente,\n"
                    + "SysTag Repuestos Automotrices";

            mensaje.setText(asunto);

            if (pdfAdjunto != null && pdfAdjunto.exists()) {
                MimeBodyPart textoParte = new MimeBodyPart();
                textoParte.setText(asunto);

                MimeBodyPart adjuntoParte = new MimeBodyPart();
                adjuntoParte.attachFile(pdfAdjunto);
                adjuntoParte.setFileName(MimeUtility.encodeText(pdfAdjunto.getName()));

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textoParte);
                multipart.addBodyPart(adjuntoParte);

                mensaje.setContent(multipart);
            }

            Transport.send(mensaje);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

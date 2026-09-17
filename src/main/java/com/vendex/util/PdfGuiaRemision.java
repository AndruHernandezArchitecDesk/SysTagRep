package com.vendex.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;

public class PdfGuiaRemision {

    private static final java.awt.Color GRIS_CLARO = new java.awt.Color(238, 238, 238);
    private static final java.awt.Color GRIS_ENCABEZADO = new java.awt.Color(225, 225, 225);
    private static final java.awt.Color GRIS_LINEA = new java.awt.Color(190, 190, 190);
    private static final java.awt.Color AZUL_TITULO = new java.awt.Color(0, 60, 120);

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONT_NORMAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font FONT_PEQUENA_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

    @SuppressWarnings("unchecked")
    public static void generar(String rutaPdf,
                               String claveAcceso,
                               String numeroAutorizacion,
                               String fechaAutorizacion,
                               String ambiente,
                               String ruc, String razonSocial,
                               String dirEstablecimiento, String telefonoEmpresa, String correoEmpresa,
                               String codEstablecimiento, String codPuntoEmision, int secuencial,
                               String fechaEmision,
                               String dirPartida,
                               String razonSocialTransportista, String tipoIdTransportista, String rucTransportista, String placa,
                               String fechaIniTransporte, String fechaFinTransporte,
                               List<Object[]> destinatarios) {
        try {
            Document doc = new Document(PageSize.A4, 24, 24, 24, 24);
            PdfWriter.getInstance(doc, new FileOutputStream(rutaPdf));
            doc.open();

            PdfPTable encabezado = new PdfPTable(new float[]{45, 55});
            encabezado.setWidthPercentage(100);
            encabezado.setSplitLate(false);

            PdfPCell celdaIzq = new PdfPCell();
            celdaIzq.setBorder(PdfPCell.NO_BORDER);
            celdaIzq.setPadding(0);
            try {
                java.io.InputStream logoStream = PdfGuiaRemision.class.getResourceAsStream("/img/logoVendex.png");
                if (logoStream != null) {
                    Image logo = Image.getInstance(logoStream.readAllBytes());
                    logo.scaleToFit(110, 110);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    celdaIzq.addElement(logo);
                }
            } catch (Exception ignored) {}
            celdaIzq.addElement(new Paragraph(" "));
            PdfPTable cajaEmisor = new PdfPTable(1);
            cajaEmisor.setWidthPercentage(100);
            cajaEmisor.addCell(filaEmisor("Emisor:", razonSocial));
            cajaEmisor.addCell(filaEmisor("RUC:", ruc));
            cajaEmisor.addCell(filaEmisor("Matriz:", dirEstablecimiento));
            cajaEmisor.addCell(filaEmisor("Correo:", correoEmpresa));
            cajaEmisor.addCell(filaEmisor("Teléfono:", telefonoEmpresa));
            celdaIzq.addElement(cajaEmisor);
            encabezado.addCell(celdaIzq);

            PdfPCell celdaDer = new PdfPCell();
            celdaDer.setBorder(PdfPCell.NO_BORDER);
            celdaDer.setPadding(0);
            celdaDer.setVerticalAlignment(Element.ALIGN_TOP);

            Font fTitulo = new Font(Font.HELVETICA, 15, Font.BOLD, AZUL_TITULO);
            PdfPTable filaTitulo = new PdfPTable(2);
            filaTitulo.setWidthPercentage(100);
            PdfPCell cTitulo = new PdfPCell(new Phrase("GUÍA DE REMISIÓN", fTitulo));
            cTitulo.setBorder(PdfPCell.NO_BORDER);
            cTitulo.setPadding(0);
            String numComp = codEstablecimiento + "-" + codPuntoEmision + "-" + String.format("%09d", secuencial);
            PdfPCell cNumero = new PdfPCell(new Phrase(numComp, FONT_NORMAL_BOLD));
            cNumero.setBorder(PdfPCell.NO_BORDER);
            cNumero.setPadding(0);
            cNumero.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cNumero.setVerticalAlignment(Element.ALIGN_MIDDLE);
            filaTitulo.addCell(cTitulo);
            filaTitulo.addCell(cNumero);
            celdaDer.addElement(filaTitulo);

            PdfPTable infoTrans = new PdfPTable(1);
            infoTrans.setWidthPercentage(100);
            PdfPCell infoCell = new PdfPCell(new Phrase("Transporte: " + razonSocialTransportista + " (" + rucTransportista + ")  Placa: " + placa, FONT_PEQUENA_BOLD));
            infoCell.setBackgroundColor(new java.awt.Color(220, 235, 255));
            infoCell.setBorderColor(GRIS_LINEA);
            infoCell.setPadding(4);
            infoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            infoTrans.addCell(infoCell);
            PdfPCell fechasCell = new PdfPCell(new Phrase("Traslado: " + fechaIniTransporte + " al " + fechaFinTransporte + "  |  Partida: " + dirPartida, FONT_PEQUENA));
            fechasCell.setBorderColor(GRIS_LINEA);
            fechasCell.setPadding(4);
            fechasCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            infoTrans.addCell(fechasCell);
            celdaDer.addElement(infoTrans);
            encabezado.addCell(celdaDer);
            doc.add(encabezado);
            doc.add(new Paragraph(" "));

            try {
                String contenidoQR = "CLAVE ACCESO: " + claveAcceso + "\nAUTORIZACIÓN: " + (numeroAutorizacion != null ? numeroAutorizacion : "PENDIENTE");
                BitMatrix matrix = new Code128Writer().encode(contenidoQR, BarcodeFormat.CODE_128, 260, 45);
                BufferedImage bi = MatrixToImageWriter.toBufferedImage(matrix);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(bi, "PNG", baos);
                Image qrImage = Image.getInstance(baos.toByteArray());
                qrImage.scaleToFit(260, 45);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                doc.add(qrImage);
                Paragraph pClave = new Paragraph(claveAcceso != null ? claveAcceso : "", FontFactory.getFont(FontFactory.HELVETICA, 7));
                pClave.setAlignment(Element.ALIGN_CENTER);
                doc.add(pClave);
            } catch (Exception e) { /* QR opcional */ }
            doc.add(new Paragraph(" "));

            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(filaInfoEtiqueta("Fecha emisión:")); info.addCell(filaInfoValor(fechaEmision != null ? fechaEmision : ""));
            info.addCell(filaInfoEtiqueta("RUC Emisor:")); info.addCell(filaInfoValor(ruc));
            info.addCell(filaInfoEtiqueta("Punto partida:")); info.addCell(filaInfoValor(dirPartida != null ? dirPartida : ""));
            doc.add(info);
            doc.add(new Paragraph(" "));

            if (destinatarios != null) {
                int idx = 1;
                for (Object[] dest : destinatarios) {
                    String ident = dest[0] != null ? (String) dest[0] : "";
                    String razon = dest[1] != null ? (String) dest[1] : "";
                    String dir = dest[2] != null ? (String) dest[2] : "";
                    String motivo = dest[3] != null ? (String) dest[3] : "";
                    String codSust = dest.length > 4 && dest[4] != null ? (String) dest[4] : "";
                    String numSust = dest.length > 5 && dest[5] != null ? (String) dest[5] : "";
                    String autSust = dest.length > 6 && dest[6] != null ? (String) dest[6] : "";
                    String fechaSust = dest.length > 7 && dest[7] != null ? (String) dest[7] : "";
                    List<Object[]> detalles = dest.length > 8 && dest[8] instanceof List ? (List<Object[]>) dest[8] : null;

                    PdfPTable destHeader = new PdfPTable(1);
                    destHeader.setWidthPercentage(100);
                    PdfPCell hCell = new PdfPCell(new Phrase("DESTINATARIO " + idx + ": " + razon + " (" + ident + ")", FONT_NORMAL_BOLD));
                    hCell.setBackgroundColor(GRIS_ENCABEZADO); hCell.setBorderColor(GRIS_LINEA); hCell.setPadding(4);
                    destHeader.addCell(hCell);
                    PdfPCell dirCell = new PdfPCell(new Phrase("Dirección: " + dir + "  |  Motivo: " + motivo, FONT_PEQUENA));
                    dirCell.setBorderColor(GRIS_LINEA); dirCell.setPadding(4);
                    destHeader.addCell(dirCell);
                    if (codSust != null && !codSust.trim().isEmpty()) {
                        PdfPCell sustCell = new PdfPCell(new Phrase("Doc. sustento: " + codSust + " " + numSust + "  Aut: " + autSust + "  Fecha: " + fechaSust, FONT_PEQUENA));
                        sustCell.setBorderColor(GRIS_LINEA); sustCell.setPadding(4);
                        destHeader.addCell(sustCell);
                    }
                    doc.add(destHeader);

                    PdfPTable detTabla = new PdfPTable(new float[]{25, 55, 20});
                    detTabla.setWidthPercentage(100);
                    detTabla.addCell(celdaCabecera("Código"));
                    detTabla.addCell(celdaCabecera("Descripción"));
                    detTabla.addCell(celdaCabecera("Cantidad"));
                    if (detalles != null) {
                        for (Object[] det : detalles) {
                            detTabla.addCell(celdaDato((String) det[0], FONT_NORMAL));
                            detTabla.addCell(celdaDato((String) det[1], FONT_NORMAL));
                            detTabla.addCell(celdaDato((String) det[2], FONT_NORMAL));
                        }
                    }
                    doc.add(detTabla);
                    doc.add(new Paragraph(" "));
                    idx++;
                }
            }

            PdfPTable authBox = new PdfPTable(2);
            authBox.setWidthPercentage(100);
            PdfPCell authTitle = new PdfPCell(new Phrase("AUTORIZACIÓN SRI", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            authTitle.setColspan(2); authTitle.setBackgroundColor(GRIS_ENCABEZADO); authTitle.setBorderColor(GRIS_LINEA); authTitle.setPadding(4); authTitle.setHorizontalAlignment(Element.ALIGN_CENTER);
            authBox.addCell(authTitle);
            authBox.addCell(filaInfoEtiqueta("Clave acceso:")); authBox.addCell(filaInfoValor(claveAcceso != null ? claveAcceso : ""));
            authBox.addCell(filaInfoEtiqueta("Número autorización:")); authBox.addCell(filaInfoValor(numeroAutorizacion != null ? numeroAutorizacion : "PENDIENTE"));
            authBox.addCell(filaInfoEtiqueta("Fecha autorización:")); authBox.addCell(filaInfoValor(fechaAutorizacion != null ? fechaAutorizacion : ""));
            authBox.addCell(filaInfoEtiqueta("Ambiente:")); authBox.addCell(filaInfoValor(ambiente != null ? ambiente : ""));
            doc.add(authBox);

            doc.add(new Paragraph(" "));
            PdfPTable pie = new PdfPTable(1);
            PdfPCell pieCell = new PdfPCell(new Phrase("Documento sin valor comercial - sustenta traslado de mercadería", FONT_PEQUENA));
            pieCell.setBorder(PdfPCell.NO_BORDER); pieCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            pie.addCell(pieCell);
            doc.add(pie);

            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PdfPCell filaEmisor(String concepto, String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(concepto + " " + (valor != null ? valor : ""), FONT_NORMAL));
        cell.setBorder(PdfPCell.NO_BORDER); cell.setPadding(1);
        return cell;
    }
    private static PdfPCell filaInfoEtiqueta(String concepto) {
        PdfPCell cell = new PdfPCell(new Phrase(concepto, FONT_NORMAL));
        cell.setBorder(PdfPCell.NO_BORDER); cell.setPadding(1);
        return cell;
    }
    private static PdfPCell filaInfoValor(String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "", FONT_NORMAL));
        cell.setBorder(PdfPCell.NO_BORDER); cell.setPadding(1); cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private static PdfPCell celdaCabecera(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_NORMAL_BOLD));
        cell.setBackgroundColor(GRIS_ENCABEZADO); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f); cell.setPadding(4); cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        return cell;
    }
    private static PdfPCell celdaDato(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        cell.setPadding(4); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f);
        return cell;
    }
}

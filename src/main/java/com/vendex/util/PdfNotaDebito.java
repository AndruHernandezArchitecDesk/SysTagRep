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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PdfNotaDebito {

    private static final java.awt.Color GRIS_CLARO = new java.awt.Color(238, 238, 238);
    private static final java.awt.Color GRIS_ENCABEZADO = new java.awt.Color(225, 225, 225);
    private static final java.awt.Color GRIS_LINEA = new java.awt.Color(190, 190, 190);
    private static final java.awt.Color GRIS_OSCURO = new java.awt.Color(205, 205, 205);
    private static final java.awt.Color ROJO_TITULO = new java.awt.Color(0, 100, 0);

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONT_NORMAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font FONT_PEQUENA_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private static final Font FONT_CLAVE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f);

    public static void generar(String rutaPdf,
                                 String claveAcceso,
                                 String numeroAutorizacion,
                                 String fechaAutorizacion,
                                 String ambiente,
                                 String ruc, String razonSocial,
                                 String dirEstablecimiento, String telefonoEmpresa, String correoEmpresa,
                                 String codEstablecimiento, String codPuntoEmision, int secuencial,
                                 String fechaEmision, String fechaEmisionDocSustento, String numDocModificado,
                                 String tipoIdComprador, String razonSocialComprador,
                                 String idComprador, String dirComprador,
                                 String emailComprador, String telefonoComprador,
                                 String motivo,
                                 List<Object[]> motivos,
                                 BigDecimal totalSinImpuestos, BigDecimal totalIva, BigDecimal valorTotal) {
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
                java.io.InputStream logoStream = PdfNotaDebito.class.getResourceAsStream("/img/logoVendex.jpeg");
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
            cajaEmisor.addCell(filaEmisor("Punto de venta:", codEstablecimiento + "-" + codPuntoEmision));
            cajaEmisor.addCell(filaEmisor("Correo:", correoEmpresa));
            cajaEmisor.addCell(filaEmisor("Teléfono:", telefonoEmpresa));
            celdaIzq.addElement(cajaEmisor);
            encabezado.addCell(celdaIzq);

            PdfPCell celdaDer = new PdfPCell();
            celdaDer.setBorder(PdfPCell.NO_BORDER);
            celdaDer.setPadding(0);
            celdaDer.setVerticalAlignment(Element.ALIGN_TOP);

            Font fTitulo = new Font(Font.HELVETICA, 15, Font.BOLD, ROJO_TITULO);
            PdfPTable filaTitulo = new PdfPTable(2);
            filaTitulo.setWidthPercentage(100);
            PdfPCell cTitulo = new PdfPCell(new Phrase("NOTA DE DÉBITO", fTitulo));
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

            PdfPTable refBox = new PdfPTable(1);
            refBox.setWidthPercentage(100);
            PdfPCell refCell = new PdfPCell(new Phrase("Documento modificado: Factura " + (numDocModificado != null ? numDocModificado : ""), FONT_PEQUENA_BOLD));
            refCell.setBackgroundColor(new java.awt.Color(235, 255, 235));
            refCell.setBorderColor(GRIS_LINEA);
            refCell.setPadding(4);
            refCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            refBox.addCell(refCell);
            celdaDer.addElement(refBox);
            encabezado.addCell(celdaDer);
            doc.add(encabezado);
            doc.add(new Paragraph(" "));

            // RIDE QR
            try {
                String contenidoQR = "CLAVE ACCESO: " + claveAcceso + "\nAUTORIZACIÓN: " + (numeroAutorizacion != null ? numeroAutorizacion : "PENDIENTE") + "\nFECHA: " + (fechaAutorizacion != null ? fechaAutorizacion : "") + "\nTOTAL: " + valorTotal.toPlainString();
                BitMatrix matrix = new Code128Writer().encode(contenidoQR, BarcodeFormat.CODE_128, 200, 100);
                BufferedImage bi = MatrixToImageWriter.toBufferedImage(matrix);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(bi, "PNG", baos);
                Image qrImage = Image.getInstance(baos.toByteArray());
                qrImage.scaleToFit(100, 100);
                qrImage.setAlignment(Element.ALIGN_RIGHT);
                doc.add(qrImage);
            } catch (Exception e) { /* QR opcional */ }

            // Info comprobante
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.addCell(filaInfoEtiqueta("Fecha emisión:")); info.addCell(filaInfoValor(fechaEmision != null ? fechaEmision : ""));
            info.addCell(filaInfoEtiqueta("RUC/Serie:")); info.addCell(filaInfoValor(numComp));
            info.addCell(filaInfoEtiqueta("Contribuyente:")); info.addCell(filaInfoValor(razonSocial));
            info.addCell(filaInfoEtiqueta("Receptor:")); info.addCell(filaInfoValor(razonSocialComprador));
            info.addCell(filaInfoEtiqueta("Dirección receptor:")); info.addCell(filaInfoValor(dirComprador != null ? dirComprador : ""));
            doc.add(info);
            doc.add(new Paragraph(" "));

            // Tabla de motivos
            PdfPTable motivosTabla = new PdfPTable(new float[]{65, 35});
            motivosTabla.setWidthPercentage(100);
            motivosTabla.addCell(celdaCabecera("Motivo"));
            motivosTabla.addCell(celdaCabecera("Valor"));
            if (motivos != null) {
                for (Object[] m : motivos) {
                    motivosTabla.addCell(celdaDato((String) m[0], FONT_NORMAL));
                    motivosTabla.addCell(celdaDato((String) m[1], FONT_NORMAL));
                }
            }
            motivosTabla.addCell(celdaDato("Subtotal sin IVA", FONT_NORMAL_BOLD));
            motivosTabla.addCell(celdaDato(totalSinImpuestos != null ? totalSinImpuestos.toPlainString() : "0.00", FONT_NORMAL_BOLD));
            motivosTabla.addCell(celdaDato("IVA (15%)", FONT_NORMAL_BOLD));
            motivosTabla.addCell(celdaDato(totalIva != null ? totalIva.toPlainString() : "0.00", FONT_NORMAL_BOLD));
            motivosTabla.addCell(celdaDato("TOTAL A PAGAR", FONT_NORMAL_BOLD));
            motivosTabla.addCell(celdaDato(valorTotal != null ? valorTotal.toPlainString() : "0.00", FONT_NORMAL_BOLD));
            doc.add(motivosTabla);
            doc.add(new Paragraph(" "));

            // Bloque autorización
            PdfPTable authBox = new PdfPTable(1);
            authBox.setWidthPercentage(100);
            PdfPCell authCell = new PdfPCell(new Phrase("AUTORIZACIÓN SRI", FONT_SUBTITULO));
            authCell.setBackgroundColor(GRIS_ENCABEZADO); authCell.setBorderColor(GRIS_LINEA); authCell.setPadding(4);
            authBox.addCell(authCell);
            authBox.addCell(filaInfoEtiqueta("Clave acceso:")); authBox.addCell(filaInfoValor(claveAcceso != null ? claveAcceso : ""));
            authBox.addCell(filaInfoEtiqueta("Número autorización:")); authBox.addCell(filaInfoValor(numeroAutorizacion != null ? numeroAutorizacion : "PENDIENTE"));
            authBox.addCell(filaInfoEtiqueta("Fecha autorización:")); authBox.addCell(filaInfoValor(fechaAutorizacion != null ? fechaAutorizacion : ""));
            doc.add(authBox);

            doc.add(new Paragraph(" "));
            PdfPTable pie = new PdfPTable(1);
            PdfPCell pieCell = new PdfPCell(new Phrase("Autorización SRI: " + (numeroAutorizacion != null ? "SÍ" : "NO"), FONT_PEQUENA));
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
        cell.setPadding(4); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f); cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}

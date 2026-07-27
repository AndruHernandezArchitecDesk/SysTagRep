package com.tag.sysTagRep.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera PDF de factura electrónica con formato SRI: QR, clave de acceso, datos tributarios.
 */
public class PdfElectronico {

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_NORMAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font FONT_CLAVE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);

    public static void generar(String rutaPdf,
                                String claveAcceso,
                                String ruc, String razonSocial,
                                String dirEstablecimiento, String contribuyenteEspecial,
                                String obligadoContabilidad,
                                String codEstablecimiento, String codPuntoEmision, int secuencial,
                                String fechaEmision,
                                String tipoIdComprador, String razonSocialComprador,
                                String idComprador, String dirComprador,
                                String formaPago,
                                List<Object[]> detalles,
                                BigDecimal totalSinImpuestos, BigDecimal totalIva, BigDecimal totalConImpuestos) {

        try {
            Document doc = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(rutaPdf));
            doc.open();

            // Encabezado empresa
            Paragraph pRazonSocial = new Paragraph(razonSocial, FONT_TITULO);
            pRazonSocial.setAlignment(Element.ALIGN_CENTER);
            doc.add(pRazonSocial);

            Paragraph pRuc = new Paragraph("RUC: " + ruc, FONT_NORMAL);
            pRuc.setAlignment(Element.ALIGN_CENTER);
            doc.add(pRuc);

            Paragraph pDir = new Paragraph(dirEstablecimiento, FONT_NORMAL);
            pDir.setAlignment(Element.ALIGN_CENTER);
            doc.add(pDir);

            if (contribuyenteEspecial != null && !contribuyenteEspecial.isEmpty()) {
                Paragraph pCE = new Paragraph("Contribuyente Especial N. " + contribuyenteEspecial, FONT_NORMAL);
                pCE.setAlignment(Element.ALIGN_CENTER);
                doc.add(pCE);
            }

            Paragraph pContab = new Paragraph("Obligado a llevar contabilidad: " + (obligadoContabilidad != null ? obligadoContabilidad : "SI"), FONT_NORMAL);
            pContab.setAlignment(Element.ALIGN_CENTER);
            doc.add(pContab);

            doc.add(new Paragraph(" "));

            // Tipo comprobante
            Paragraph pTipo = new Paragraph("FACTURA", FONT_SUBTITULO);
            pTipo.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTipo);

            // Número comprobante
            String numComp = codEstablecimiento + "-" + codPuntoEmision + "-" + String.format("%09d", secuencial);
            Paragraph pNum = new Paragraph("No. " + numComp, FONT_NORMAL_BOLD);
            pNum.setAlignment(Element.ALIGN_CENTER);
            doc.add(pNum);

            Paragraph pFecha = new Paragraph("Fecha Emisión: " + fechaEmision, FONT_NORMAL);
            pFecha.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFecha);

            // QR Code
            try {
                String qrText = "https://celcerce.prib.nubefact.com/cvc-ws/validarcomprobante?claveAcceso=" + claveAcceso;
                QRCodeWriter qrWriter = new QRCodeWriter();
                BitMatrix matrix = qrWriter.encode(qrText, BarcodeFormat.QR_CODE, 120, 120);
                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);

                Image qrImageIT = Image.getInstance(qrImage, null);
                qrImageIT.setAlignment(Element.ALIGN_RIGHT);
                qrImageIT.scaleAbsolute(90, 90);
                doc.add(qrImageIT);
            } catch (Exception e) {
                // QR es opcional
            }

            doc.add(new Paragraph(" "));

            // Datos del comprador
            PdfPTable tablaInfo = new PdfPTable(2);
            tablaInfo.setWidthPercentage(100);
            tablaInfo.setWidths(new float[]{50, 50});

            tablaInfo.addCell(celdaLabel("Razón Social / Nombres:"));
            tablaInfo.addCell(celdaValor(razonSocialComprador));
            tablaInfo.addCell(celdaLabel(tipoIdComprador != null && tipoIdComprador.equals("05") ? "RUC:" : "Cédula:"));
            tablaInfo.addCell(celdaValor(idComprador));
            tablaInfo.addCell(celdaLabel("Dirección:"));
            tablaInfo.addCell(celdaValor(dirComprador));
            tablaInfo.addCell(celdaLabel("Forma de Pago:"));
            tablaInfo.addCell(celdaValor(formaPago));
            tablaInfo.addCell(celdaLabel("Fecha Emisión:"));
            tablaInfo.addCell(celdaValor(fechaEmision));

            doc.add(tablaInfo);
            doc.add(new Paragraph(" "));

            // Tabla de detalles
            PdfPTable tablaDet = new PdfPTable(5);
            tablaDet.setWidthPercentage(100);
            tablaDet.setWidths(new float[]{12, 38, 10, 18, 22});

            tablaDet.addCell(celdaCabecera("Código"));
            tablaDet.addCell(celdaCabecera("Descripción"));
            tablaDet.addCell(celdaCabecera("Cant."));
            tablaDet.addCell(celdaCabecera("P. Unit."));
            tablaDet.addCell(celdaCabecera("Total"));

            for (Object[] det : detalles) {
                tablaDet.addCell(celdaDetalle((String) det[0]));
                tablaDet.addCell(celdaDetalle((String) det[1]));
                tablaDet.addCell(celdaDetalle(String.valueOf(det[2])));
                tablaDet.addCell(celdaDetalle((String) det[3]));
                tablaDet.addCell(celdaDetalle((String) det[5]));
            }

            doc.add(tablaDet);
            doc.add(new Paragraph(" "));

            // Totales
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(60);
            tablaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaTotales.setWidths(new float[]{60, 40});

            tablaTotales.addCell(celdaLabel("Subtotal sin IVA:"));
            tablaTotales.addCell(celdaValor("$ " + totalSinImpuestos.setScale(2, RoundingMode.HALF_UP)));
            tablaTotales.addCell(celdaLabel("IVA 15%:"));
            tablaTotales.addCell(celdaValor("$ " + totalIva.setScale(2, RoundingMode.HALF_UP)));
            tablaTotales.addCell(celdaLabel("TOTAL:"));
            PdfPCell celdaTotal = celdaValor("$ " + totalConImpuestos.setScale(2, RoundingMode.HALF_UP));
            celdaTotal.setColspan(2);
            tablaTotales.addCell(celdaTotal);

            doc.add(tablaTotales);
            doc.add(new Paragraph(" "));

            // Clave de acceso
            Paragraph pClaveLabel = new Paragraph("Clave de Acceso:", FONT_NORMAL_BOLD);
            pClaveLabel.setAlignment(Element.ALIGN_CENTER);
            doc.add(pClaveLabel);

            Paragraph pClave = new Paragraph(claveAcceso, FONT_CLAVE);
            pClave.setAlignment(Element.ALIGN_CENTER);
            doc.add(pClave);

            Paragraph pInfo = new Paragraph("Autorización SRI (Ambiente de PRUEBAS)", FONT_PEQUENA);
            pInfo.setAlignment(Element.ALIGN_CENTER);
            doc.add(pInfo);

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PdfPCell celdaLabel(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_NORMAL_BOLD));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell celdaValor(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_NORMAL));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell celdaCabecera(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_NORMAL_BOLD));
        cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
        cell.setPadding(5);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell celdaDetalle(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_NORMAL));
        cell.setPadding(4);
        return cell;
    }
}

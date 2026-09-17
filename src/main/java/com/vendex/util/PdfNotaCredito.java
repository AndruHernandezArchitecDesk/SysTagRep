package com.vendex.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PdfNotaCredito {

    private static final java.awt.Color GRIS_CLARO = new java.awt.Color(238, 238, 238);
    private static final java.awt.Color GRIS_ENCABEZADO = new java.awt.Color(225, 225, 225);
    private static final java.awt.Color GRIS_LINEA = new java.awt.Color(190, 190, 190);
    private static final java.awt.Color GRIS_OSCURO = new java.awt.Color(205, 205, 205);
    private static final java.awt.Color ROJO_TITULO = new java.awt.Color(192, 0, 0);

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
                               List<Object[]> detalles,
                               BigDecimal totalSinImpuestos, BigDecimal totalIva, BigDecimal valorModificacion) {
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
                java.io.InputStream logoStream = PdfNotaCredito.class.getResourceAsStream("/img/logoVendex.png");
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

            Font fTituloRojo = new Font(Font.HELVETICA, 15, Font.BOLD, ROJO_TITULO);
            PdfPTable filaTitulo = new PdfPTable(2);
            filaTitulo.setWidthPercentage(100);
            PdfPCell cTitulo = new PdfPCell(new Phrase("NOTA DE CRÉDITO", fTituloRojo));
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

            // Referencia visible
            PdfPTable refBox = new PdfPTable(1);
            refBox.setWidthPercentage(100);
            PdfPCell refCell = new PdfPCell(new Phrase("Modifica a: Factura " + (numDocModificado != null ? numDocModificado : "") + " del " + (fechaEmisionDocSustento != null ? fechaEmisionDocSustento : ""), FONT_PEQUENA_BOLD));
            refCell.setBackgroundColor(new java.awt.Color(255, 235, 235));
            refCell.setBorderColor(GRIS_LINEA);
            refCell.setPadding(4);
            refCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            refBox.addCell(refCell);
            // motivo destacado
            PdfPCell motCell = new PdfPCell(new Phrase("Motivo: " + (motivo != null ? motivo : ""), FONT_PEQUENA));
            motCell.setBorderColor(GRIS_LINEA);
            motCell.setPadding(4);
            refBox.addCell(motCell);
            celdaDer.addElement(refBox);
            celdaDer.addElement(new Paragraph(" "));

            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{50, 50});
            datos.addCell(celdaDato("Número de Autorización:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato(numeroAutorizacion != null && !numeroAutorizacion.isEmpty() ? numeroAutorizacion : "—", FONT_PEQUENA));
            datos.addCell(celdaDato("Fecha y hora de Autorización:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato(fechaAutorizacion != null && !fechaAutorizacion.isEmpty() ? fechaAutorizacion : "—", FONT_PEQUENA));
            datos.addCell(celdaDato("Ambiente:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato(ambiente != null ? ambiente : "PRUEBAS", FONT_PEQUENA));
            datos.addCell(celdaDato("Emisión:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato("NORMAL (1)", FONT_PEQUENA));
            datos.addCell(celdaDato("Clave de Acceso:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato("", FONT_PEQUENA));
            PdfPCell celdaClave = new PdfPCell();
            celdaClave.setBorder(PdfPCell.NO_BORDER);
            celdaClave.setColspan(2);
            celdaClave.setPadding(0);
            try {
                BitMatrix matrix = new Code128Writer().encode(claveAcceso, BarcodeFormat.CODE_128, 260, 45);
                BufferedImage bi = MatrixToImageWriter.toBufferedImage(matrix);
                Image bcImage = Image.getInstance(bi, null);
                bcImage.setAlignment(Element.ALIGN_CENTER);
                bcImage.scaleToFit(255, 45);
                celdaClave.addElement(bcImage);
            } catch (Exception ignored) {}
            BaseFont bfClave = FONT_CLAVE.getBaseFont();
            float tamano = 6.5f;
            while (tamano > 5f && bfClave.getWidthPoint(claveAcceso, tamano) > 250f) tamano -= 0.5f;
            Paragraph pClave = new Paragraph(claveAcceso, new Font(bfClave, tamano, Font.BOLD));
            pClave.setAlignment(Element.ALIGN_CENTER);
            celdaClave.addElement(pClave);
            datos.addCell(celdaClave);
            celdaDer.addElement(datos);
            encabezado.addCell(celdaDer);
            doc.add(encabezado);
            doc.add(new Paragraph(" "));

            // Banda cliente
            PdfPTable bandaCliente = new PdfPTable(new float[]{50, 50});
            bandaCliente.setWidthPercentage(100);
            PdfPCell cClienteIzq = new PdfPCell();
            cClienteIzq.setBackgroundColor(GRIS_CLARO);
            cClienteIzq.setBorderColor(GRIS_LINEA);
            cClienteIzq.setPadding(6);
            cClienteIzq.addElement(parejaCliente("Razón Social", razonSocialComprador));
            cClienteIzq.addElement(parejaCliente("Dirección", dirComprador));
            cClienteIzq.addElement(parejaCliente("Fecha de emisión", fechaEmision));
            PdfPCell cClienteDer = new PdfPCell();
            cClienteDer.setBackgroundColor(GRIS_CLARO);
            cClienteDer.setBorderColor(GRIS_LINEA);
            cClienteDer.setPadding(6);
            cClienteDer.addElement(parejaCliente("RUC / CI", idComprador));
            cClienteDer.addElement(parejaCliente("Teléfono", telefonoComprador));
            cClienteDer.addElement(parejaCliente("Correo", emailComprador));
            bandaCliente.addCell(cClienteIzq);
            bandaCliente.addCell(cClienteDer);
            doc.add(bandaCliente);
            doc.add(new Paragraph(" "));

            // Tabla detalle
            PdfPTable tablaDet = new PdfPTable(6);
            tablaDet.setWidthPercentage(100);
            tablaDet.setWidths(new float[]{12, 9, 33, 16, 14, 16});
            tablaDet.addCell(celdaCabecera("Código"));
            tablaDet.addCell(celdaCabecera("Cantidad"));
            tablaDet.addCell(celdaCabecera("Descripción"));
            tablaDet.addCell(celdaCabecera("Precio Unitario"));
            tablaDet.addCell(celdaCabecera("Descuento"));
            tablaDet.addCell(celdaCabecera("Total"));
            for (Object[] det : detalles) {
                BigDecimal descLinea = BigDecimal.ZERO;
                try { descLinea = new BigDecimal((String) det[4]); } catch (Exception ignored) {}
                tablaDet.addCell(celdaDetalle((String) det[0], Element.ALIGN_LEFT));
                tablaDet.addCell(celdaDetalle(String.valueOf(det[2]), Element.ALIGN_CENTER));
                tablaDet.addCell(celdaDetalle((String) det[1], Element.ALIGN_LEFT));
                tablaDet.addCell(celdaDetalle((String) det[3], Element.ALIGN_RIGHT));
                tablaDet.addCell(celdaDetalle(descLinea.compareTo(BigDecimal.ZERO) > 0 ? "- $ " + descLinea.setScale(2, RoundingMode.HALF_UP) : "0.00", Element.ALIGN_RIGHT));
                tablaDet.addCell(celdaDetalle((String) det[5], Element.ALIGN_RIGHT));
            }
            doc.add(tablaDet);
            doc.add(new Paragraph(" "));

            // Pie 2 columnas - solo resumen financiero (no forma pago)
            PdfPTable pie = new PdfPTable(new float[]{45, 55});
            pie.setWidthPercentage(100);
            pie.setSplitLate(false);
            PdfPCell cIzqPie = new PdfPCell();
            cIzqPie.setBorder(PdfPCell.NO_BORDER);
            cIzqPie.setPadding(0);
            // Info adicional vacia
            PdfPTable cajaInfo = new PdfPTable(1);
            cajaInfo.setWidthPercentage(100);
            PdfPCell cInfoTitulo = new PdfPCell(new Phrase("Información Adicional", FONT_NORMAL_BOLD));
            cInfoTitulo.setBackgroundColor(GRIS_CLARO);
            cInfoTitulo.setBorderColor(GRIS_LINEA);
            cInfoTitulo.setPadding(4);
            cajaInfo.addCell(cInfoTitulo);
            PdfPCell cMotivoInfo = new PdfPCell(new Phrase(motivo != null ? motivo : "", FONT_PEQUENA));
            cMotivoInfo.setPadding(4);
            cMotivoInfo.setBorderColor(GRIS_LINEA);
            cajaInfo.addCell(cMotivoInfo);
            cIzqPie.addElement(cajaInfo);
            pie.addCell(cIzqPie);

            PdfPCell cDerPie = new PdfPCell();
            cDerPie.setBorder(PdfPCell.NO_BORDER);
            cDerPie.setPadding(0);
            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(100);
            resumen.setWidths(new float[]{60, 40});
            resumen.addCell(filaResumenEtiqueta("Subtotal sin impuestos"));
            resumen.addCell(filaResumenValor("$ " + totalSinImpuestos.setScale(2, RoundingMode.HALF_UP)));
            resumen.addCell(filaResumenEtiqueta("IVA 15%"));
            resumen.addCell(filaResumenValor("$ " + totalIva.setScale(2, RoundingMode.HALF_UP)));
            resumen.addCell(filaResumenEtiqueta("Valor Modificación", true));
            resumen.addCell(filaResumenValor("$ " + valorModificacion.setScale(2, RoundingMode.HALF_UP), true));
            cDerPie.addElement(resumen);
            pie.addCell(cDerPie);
            doc.add(pie);
            doc.add(new Paragraph(" "));

            PdfPTable footer = new PdfPTable(1);
            footer.setWidthPercentage(100);
            PdfPCell cFooter = new PdfPCell();
            cFooter.setBackgroundColor(GRIS_CLARO);
            cFooter.setBorderColor(GRIS_LINEA);
            cFooter.setPadding(6);
            Paragraph pGracias = new Paragraph("¡Gracias por Preferirnos!", FONT_NORMAL_BOLD);
            pGracias.setAlignment(Element.ALIGN_CENTER);
            cFooter.addElement(pGracias);
            footer.addCell(cFooter);
            doc.add(footer);
            doc.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static PdfPCell filaEmisor(String etiqueta, String valor) {
        Phrase ph = new Phrase(etiqueta + " ", FONT_PEQUENA_BOLD);
        ph.add(new Chunk(valor != null ? valor : "", FONT_PEQUENA));
        PdfPCell cell = new PdfPCell(ph);
        cell.setBackgroundColor(GRIS_CLARO);
        cell.setBorderColor(GRIS_LINEA);
        cell.setBorderWidth(0.5f);
        cell.setPadding(3);
        return cell;
    }
    private static Paragraph parejaCliente(String etiqueta, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(etiqueta + ": ", FONT_PEQUENA_BOLD));
        p.add(new Chunk(valor != null ? valor : "", FONT_PEQUENA));
        p.setLeading(11);
        return p;
    }
    private static PdfPCell filaResumenEtiqueta(String concepto) { return filaResumenEtiqueta(concepto, false); }
    private static PdfPCell filaResumenEtiqueta(String concepto, boolean destacar) {
        PdfPCell cell = new PdfPCell(new Phrase(concepto, destacar ? FONT_NORMAL_BOLD : FONT_NORMAL));
        cell.setPadding(3); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f);
        if (destacar) cell.setBackgroundColor(GRIS_OSCURO);
        return cell;
    }
    private static PdfPCell filaResumenValor(String valor) { return filaResumenValor(valor, false); }
    private static PdfPCell filaResumenValor(String valor, boolean destacar) {
        PdfPCell cell = new PdfPCell(new Phrase(valor, destacar ? FONT_NORMAL_BOLD : FONT_NORMAL));
        cell.setPadding(3); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f); cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (destacar) cell.setBackgroundColor(GRIS_OSCURO);
        return cell;
    }
    private static PdfPCell celdaDato(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        cell.setBorder(PdfPCell.NO_BORDER); cell.setHorizontalAlignment(Element.ALIGN_RIGHT); cell.setVerticalAlignment(Element.ALIGN_MIDDLE); cell.setPadding(1);
        return cell;
    }
    private static PdfPCell celdaCabecera(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_NORMAL_BOLD));
        cell.setBackgroundColor(GRIS_ENCABEZADO); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f); cell.setPadding(4); cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        return cell;
    }
    private static PdfPCell celdaDetalle(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_NORMAL));
        cell.setPadding(4); cell.setBorderColor(GRIS_LINEA); cell.setBorderWidth(0.5f); cell.setHorizontalAlignment(alineacion);
        return cell;
    }
}

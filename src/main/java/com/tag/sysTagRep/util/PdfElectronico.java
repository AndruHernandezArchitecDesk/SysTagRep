package com.tag.sysTagRep.util;

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

/**
 * Genera PDF de factura electrónica con formato SRI:
 * encabezado en 2 columnas (logo + caja gris del emisor | título FACTURA con
 * datos tributarios y código de barras), banda gris del comprador, tabla de
 * detalle, sección inferior en 2 columnas (información adicional + formas de
 * pago | resumen financiero) y pie de página con agradecimiento.
 */
public class PdfElectronico {

    private static final java.awt.Color GRIS_CLARO = new java.awt.Color(238, 238, 238);
    private static final java.awt.Color GRIS_ENCABEZADO = new java.awt.Color(225, 225, 225);
    private static final java.awt.Color GRIS_LINEA = new java.awt.Color(190, 190, 190);
    private static final java.awt.Color GRIS_OSCURO = new java.awt.Color(205, 205, 205);

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
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
                                String contribuyenteEspecial, String obligadoContabilidad,
                                String sucursal, String agenteRetencion, String resolucion,
                                String codEstablecimiento, String codPuntoEmision, int secuencial,
                                String fechaEmision,
                                String tipoIdComprador, String razonSocialComprador,
                                String idComprador, String dirComprador,
                                String emailComprador, String telefonoComprador,
                                String formaPago,
                                List<Object[]> detalles,
                                BigDecimal totalSinImpuestos, BigDecimal descuento,
                                BigDecimal totalIva, BigDecimal totalConImpuestos) {

        try {
            Document doc = new Document(PageSize.A4, 24, 24, 24, 24);
            PdfWriter.getInstance(doc, new FileOutputStream(rutaPdf));
            doc.open();

            // === ENCABEZADO: 2 COLUMNAS ===
            PdfPTable encabezado = new PdfPTable(new float[]{45, 55});
            encabezado.setWidthPercentage(100);
            encabezado.setSplitLate(false);

            // --- Columna 1: logo + caja gris del emisor ---
            PdfPCell celdaIzq = new PdfPCell();
            celdaIzq.setBorder(PdfPCell.NO_BORDER);
            celdaIzq.setPadding(0);

            try {
                java.io.InputStream logoStream = PdfElectronico.class.getResourceAsStream("/img/logoTag.jpeg");
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
            cajaEmisor.addCell(filaEmisor("Matriz:", sucursal != null && !sucursal.isEmpty() ? sucursal : dirEstablecimiento));
            cajaEmisor.addCell(filaEmisor("Punto de venta o establecimiento:",
                    codEstablecimiento + "-" + codPuntoEmision));
            cajaEmisor.addCell(filaEmisor("Correo electrónico:", correoEmpresa));
            cajaEmisor.addCell(filaEmisor("Teléfono:", telefonoEmpresa));
            cajaEmisor.addCell(filaEmisor("Obligado a llevar contabilidad:",
                    obligadoContabilidad != null ? obligadoContabilidad : "NO"));
            if (agenteRetencion != null && !agenteRetencion.isEmpty()) {
                cajaEmisor.addCell(filaEmisor("Agente de retención:", agenteRetencion));
            }
            if (resolucion != null && !resolucion.isEmpty()) {
                cajaEmisor.addCell(filaEmisor("Resolución:", resolucion));
            }
            if (contribuyenteEspecial != null && !contribuyenteEspecial.isEmpty()) {
                cajaEmisor.addCell(filaEmisor("Contribuyente Especial:", "N. " + contribuyenteEspecial));
            }

            celdaIzq.addElement(cajaEmisor);
            encabezado.addCell(celdaIzq);

            // --- Columna 2: título + datos tributarios + barcode ---
            PdfPCell celdaDer = new PdfPCell();
            celdaDer.setBorder(PdfPCell.NO_BORDER);
            celdaDer.setPadding(0);
            celdaDer.setVerticalAlignment(Element.ALIGN_TOP);

            PdfPTable filaTitulo = new PdfPTable(2);
            filaTitulo.setWidthPercentage(100);
            PdfPCell cTitulo = new PdfPCell(new Phrase("FACTURA", FONT_TITULO));
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

            celdaDer.addElement(new Paragraph(" "));

            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{50, 50});

            datos.addCell(celdaDato("Número de Autorización:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato(numeroAutorizacion != null && !numeroAutorizacion.isEmpty()
                    ? numeroAutorizacion : "—", FONT_PEQUENA));

            datos.addCell(celdaDato("Fecha y hora de Autorización:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato(fechaAutorizacion != null && !fechaAutorizacion.isEmpty()
                    ? fechaAutorizacion : "—", FONT_PEQUENA));

            datos.addCell(celdaDato("Ambiente:", FONT_PEQUENA_BOLD));
            datos.addCell(celdaDato(ambiente != null && !ambiente.isEmpty() ? ambiente : "PRUEBAS", FONT_PEQUENA));

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
            float anchoDisponible = 250f;
            float tamano = 6.5f;
            while (tamano > 5f && bfClave.getWidthPoint(claveAcceso, tamano) > anchoDisponible) {
                tamano -= 0.5f;
            }
            Paragraph pClave = new Paragraph(claveAcceso, new Font(bfClave, tamano, Font.BOLD));
            pClave.setAlignment(Element.ALIGN_CENTER);
            celdaClave.addElement(pClave);

            datos.addCell(celdaClave);

            celdaDer.addElement(datos);
            encabezado.addCell(celdaDer);

            doc.add(encabezado);
            doc.add(new Paragraph(" "));

            // === BANDA GRIS DEL COMPRADOR (2 columnas) ===
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
            String tipoId = (tipoIdComprador != null && tipoIdComprador.equals("05")) ? "RUC / CI" : "RUC / CI";
            cClienteDer.addElement(parejaCliente(tipoId, idComprador));
            cClienteDer.addElement(parejaCliente("Teléfono", telefonoComprador));
            cClienteDer.addElement(parejaCliente("Correo electrónico", emailComprador));

            bandaCliente.addCell(cClienteIzq);
            bandaCliente.addCell(cClienteDer);

            doc.add(bandaCliente);
            doc.add(new Paragraph(" "));

            // === TABLA DE PRODUCTOS ===
            PdfPTable tablaDet = new PdfPTable(6);
            tablaDet.setWidthPercentage(100);
            tablaDet.setWidths(new float[]{12, 9, 33, 16, 14, 16});

            tablaDet.addCell(celdaCabecera("Código principal"));
            tablaDet.addCell(celdaCabecera("Cantidad"));
            tablaDet.addCell(celdaCabecera("Descripción"));
            tablaDet.addCell(celdaCabecera("Precio Unitario"));
            tablaDet.addCell(celdaCabecera("Descuento"));
            tablaDet.addCell(celdaCabecera("Total"));

            for (Object[] det : detalles) {
                BigDecimal descLinea = BigDecimal.ZERO;
                try {
                    descLinea = new BigDecimal((String) det[4]);
                } catch (Exception ignored) {}
                tablaDet.addCell(celdaDetalle((String) det[0], Element.ALIGN_LEFT));
                tablaDet.addCell(celdaDetalle(String.valueOf(det[2]), Element.ALIGN_CENTER));
                tablaDet.addCell(celdaDetalle((String) det[1], Element.ALIGN_LEFT));
                tablaDet.addCell(celdaDetalle((String) det[3], Element.ALIGN_RIGHT));
                tablaDet.addCell(celdaDetalle(descLinea.compareTo(BigDecimal.ZERO) > 0
                        ? "- $ " + descLinea.setScale(2, RoundingMode.HALF_UP) : "0.00", Element.ALIGN_RIGHT));
                tablaDet.addCell(celdaDetalle((String) det[5], Element.ALIGN_RIGHT));
            }

            doc.add(tablaDet);
            doc.add(new Paragraph(" "));

            // === PARTE INFERIOR: 2 COLUMNAS ===
            PdfPTable pie = new PdfPTable(new float[]{45, 55});
            pie.setWidthPercentage(100);
            pie.setSplitLate(false);

            // --- Columna izquierda: información adicional + formas de pago ---
            PdfPCell cIzqPie = new PdfPCell();
            cIzqPie.setBorder(PdfPCell.NO_BORDER);
            cIzqPie.setPadding(0);

            PdfPTable cajaInfo = new PdfPTable(1);
            cajaInfo.setWidthPercentage(100);
            PdfPCell cInfoTitulo = new PdfPCell(new Phrase("Información Adicional", FONT_NORMAL_BOLD));
            cInfoTitulo.setBackgroundColor(GRIS_CLARO);
            cInfoTitulo.setBorderColor(GRIS_LINEA);
            cInfoTitulo.setPadding(4);
            cajaInfo.addCell(cInfoTitulo);

            PdfPTable tablaInfo = new PdfPTable(2);
            tablaInfo.setWidthPercentage(100);
            tablaInfo.setWidths(new float[]{50, 50});
            tablaInfo.addCell(celdaCabecera("Descripción"));
            tablaInfo.addCell(celdaCabecera("Valor"));
            tablaInfo.addCell(celdaDetalle("", Element.ALIGN_LEFT));
            tablaInfo.addCell(celdaDetalle("", Element.ALIGN_RIGHT));
            cajaInfo.addCell(tablaInfo);
            cIzqPie.addElement(cajaInfo);

            cIzqPie.addElement(new Paragraph(" "));

            PdfPTable cajaFormaPago = new PdfPTable(1);
            cajaFormaPago.setWidthPercentage(100);
            PdfPCell cFPagoTitulo = new PdfPCell(new Phrase("Formas de pago", FONT_NORMAL_BOLD));
            cFPagoTitulo.setBackgroundColor(GRIS_CLARO);
            cFPagoTitulo.setBorderColor(GRIS_LINEA);
            cFPagoTitulo.setPadding(4);
            cajaFormaPago.addCell(cFPagoTitulo);

            PdfPTable tablaFormaPago = new PdfPTable(4);
            tablaFormaPago.setWidthPercentage(100);
            tablaFormaPago.setWidths(new float[]{35, 25, 20, 20});
            tablaFormaPago.addCell(celdaCabecera("Método de pago"));
            tablaFormaPago.addCell(celdaCabecera("Valor"));
            tablaFormaPago.addCell(celdaCabecera("Plazo"));
            tablaFormaPago.addCell(celdaCabecera("Unidad"));
            tablaFormaPago.addCell(celdaDetalle(formaPago != null ? formaPago : "", Element.ALIGN_LEFT));
            tablaFormaPago.addCell(celdaDetalle("$ " + totalConImpuestos.setScale(2, RoundingMode.HALF_UP), Element.ALIGN_RIGHT));
            tablaFormaPago.addCell(celdaDetalle("—", Element.ALIGN_CENTER));
            tablaFormaPago.addCell(celdaDetalle("días", Element.ALIGN_CENTER));
            cajaFormaPago.addCell(tablaFormaPago);
            cIzqPie.addElement(cajaFormaPago);

            pie.addCell(cIzqPie);

            // --- Columna derecha: resumen financiero ---
            PdfPCell cDerPie = new PdfPCell();
            cDerPie.setBorder(PdfPCell.NO_BORDER);
            cDerPie.setPadding(0);

            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(100);
            resumen.setWidths(new float[]{60, 40});

            resumen.addCell(filaResumenEtiqueta("Subtotal sin impuestos"));
            resumen.addCell(filaResumenValor("$ " + totalSinImpuestos.setScale(2, RoundingMode.HALF_UP)));
            resumen.addCell(filaResumenEtiqueta("Subtotal 15%"));
            resumen.addCell(filaResumenValor("$ " + totalSinImpuestos.setScale(2, RoundingMode.HALF_UP)));
            resumen.addCell(filaResumenEtiqueta("Subtotal 5%"));
            resumen.addCell(filaResumenValor("$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("Subtotal 0%"));
            resumen.addCell(filaResumenValor("$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("Subtotal No Objeto IVA"));
            resumen.addCell(filaResumenValor("$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("Descuentos"));
            resumen.addCell(filaResumenValor((descuento != null && descuento.compareTo(BigDecimal.ZERO) > 0)
                    ? "- $ " + descuento.setScale(2, RoundingMode.HALF_UP) : "$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("ICE"));
            resumen.addCell(filaResumenValor("$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("IVA 15%"));
            resumen.addCell(filaResumenValor("$ " + totalIva.setScale(2, RoundingMode.HALF_UP)));
            resumen.addCell(filaResumenEtiqueta("IVA 5%"));
            resumen.addCell(filaResumenValor("$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("Servicio %"));
            resumen.addCell(filaResumenValor("$ 0.00"));
            resumen.addCell(filaResumenEtiqueta("Valor Total", true));
            resumen.addCell(filaResumenValor("$ " + totalConImpuestos.setScale(2, RoundingMode.HALF_UP), true));

            cDerPie.addElement(resumen);
            pie.addCell(cDerPie);

            doc.add(pie);
            doc.add(new Paragraph(" "));

            // === PIE DE PAGINA ===
            PdfPTable footer = new PdfPTable(1);
            footer.setWidthPercentage(100);
            PdfPCell cFooter = new PdfPCell();
            cFooter.setBackgroundColor(GRIS_CLARO);
            cFooter.setBorderColor(GRIS_LINEA);
            cFooter.setPadding(6);
            Paragraph pGracias = new Paragraph("¡Gracias por Preferirnos!", FONT_NORMAL_BOLD);
            pGracias.setAlignment(Element.ALIGN_CENTER);
            cFooter.addElement(pGracias);
            String invitacion = (correoEmpresa != null && !correoEmpresa.isEmpty())
                    ? "Envía tus comentarios a " + correoEmpresa : "";
            if (!invitacion.isEmpty()) {
                Paragraph pInv = new Paragraph(invitacion, FONT_PEQUENA);
                pInv.setAlignment(Element.ALIGN_CENTER);
                cFooter.addElement(pInv);
            }
            footer.addCell(cFooter);

            doc.add(footer);

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static PdfPCell filaResumenEtiqueta(String concepto) {
        return filaResumenEtiqueta(concepto, false);
    }

    private static PdfPCell filaResumenEtiqueta(String concepto, boolean destacar) {
        PdfPCell cell = new PdfPCell(new Phrase(concepto, destacar ? FONT_NORMAL_BOLD : FONT_NORMAL));
        cell.setPadding(3);
        cell.setBorderColor(GRIS_LINEA);
        cell.setBorderWidth(0.5f);
        if (destacar) cell.setBackgroundColor(GRIS_OSCURO);
        return cell;
    }

    private static PdfPCell filaResumenValor(String valor) {
        return filaResumenValor(valor, false);
    }

    private static PdfPCell filaResumenValor(String valor, boolean destacar) {
        PdfPCell cell = new PdfPCell(new Phrase(valor, destacar ? FONT_NORMAL_BOLD : FONT_NORMAL));
        cell.setPadding(3);
        cell.setBorderColor(GRIS_LINEA);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (destacar) cell.setBackgroundColor(GRIS_OSCURO);
        return cell;
    }

    private static PdfPCell celdaDato(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(1);
        return cell;
    }

    private static PdfPCell celdaCabecera(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_NORMAL_BOLD));
        cell.setBackgroundColor(GRIS_ENCABEZADO);
        cell.setBorderColor(GRIS_LINEA);
        cell.setBorderWidth(0.5f);
        cell.setPadding(4);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell celdaDetalle(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_NORMAL));
        cell.setPadding(4);
        cell.setBorderColor(GRIS_LINEA);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(alineacion);
        return cell;
    }
}

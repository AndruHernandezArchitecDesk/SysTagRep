package com.tag.sysTagRep.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotaVentaPDF {

    public static void generar(String rutaArchivo, String numNota, String fecha,
                               String razonSocial, String ruc, String direccion, String telefono, String correo,
                               String clienteNombre, String clienteIdentificacion, String clienteDireccion, String clienteTelefono,
                               String formaPago,
                               List<String[]> detalles,
                               BigDecimal subtotal, BigDecimal iva, BigDecimal total) {

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(doc, new FileOutputStream(rutaArchivo));
            doc.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font fontNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontPequena = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fontPequenaNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

            // === ENCRESADO EMPRESA ===
            Paragraph pEmpresa = new Paragraph(razonSocial, fontTitulo);
            pEmpresa.setAlignment(Element.ALIGN_CENTER);
            doc.add(pEmpresa);

            Paragraph pRuc = new Paragraph("RUC: " + ruc, fontNormal);
            pRuc.setAlignment(Element.ALIGN_CENTER);
            doc.add(pRuc);

            Paragraph pDireccion = new Paragraph(direccion, fontNormal);
            pDireccion.setAlignment(Element.ALIGN_CENTER);
            doc.add(pDireccion);

            Paragraph pContacto = new Paragraph("Tel: " + telefono + "  |  " + correo, fontPequena);
            pContacto.setAlignment(Element.ALIGN_CENTER);
            doc.add(pContacto);

            doc.add(new Paragraph(" "));

            // === NUMERO DE NOTA ===
            Paragraph pNumNota = new Paragraph("NOTA DE VENTA  N° " + numNota, fontSubtitulo);
            pNumNota.setAlignment(Element.ALIGN_CENTER);
            doc.add(pNumNota);

            Paragraph pFecha = new Paragraph("Fecha: " + fecha, fontNormal);
            pFecha.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFecha);

            doc.add(new Paragraph(" "));

            // === CLIENTE ===
            Paragraph pClienteTitulo = new Paragraph("DATOS DEL CLIENTE", fontNegrita);
            doc.add(pClienteTitulo);

            Paragraph pCliente = new Paragraph(
                    "Nombre: " + clienteNombre + "\n" +
                    "RUC/Cédula: " + clienteIdentificacion + "\n" +
                    "Dirección: " + clienteDireccion + "\n" +
                    "Teléfono: " + clienteTelefono,
                    fontNormal);
            doc.add(pCliente);

            doc.add(new Paragraph(" "));

            // === TABLA DETALLE ===
            PdfPTable tabla = new PdfPTable(new float[]{15, 35, 10, 15, 25});
            tabla.setWidthPercentage(100);

            // Encabezados
            String[] encabezados = {"Código", "Descripción", "Cant.", "P. Unitario", "Total"};
            for (String h : encabezados) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE)));
                cell.setBackgroundColor(new java.awt.Color(52, 73, 94));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                tabla.addCell(cell);
            }

            // Filas
            boolean alternate = false;
            for (String[] fila : detalles) {
                java.awt.Color bgColor = alternate ? new java.awt.Color(240, 240, 240) : java.awt.Color.WHITE;

                for (int i = 0; i < fila.length; i++) {
                    PdfPCell cell = new PdfPCell(new Phrase(fila[i], fontPequena));
                    cell.setBackgroundColor(bgColor);
                    cell.setPadding(4);
                    if (i == 2 || i == 3 || i == 4) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    tabla.addCell(cell);
                }
                alternate = !alternate;
            }

            doc.add(tabla);
            doc.add(new Paragraph(" "));

            // === TOTALES ===
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(50);
            tablaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addFilaTotal(tablaTotales, "Subtotal:", subtotal.setScale(2, RoundingMode.HALF_UP).toString(), fontPequena, fontPequenaNegrita);
            addFilaTotal(tablaTotales, "IVA (15%):", iva.setScale(2, RoundingMode.HALF_UP).toString(), fontPequena, fontPequenaNegrita);
            addFilaTotal(tablaTotales, "TOTAL:", total.setScale(2, RoundingMode.HALF_UP).toString(), fontNegrita, fontNegrita);

            doc.add(tablaTotales);
            doc.add(new Paragraph(" "));

            // === FORMA DE PAGO ===
            Paragraph pFormaPago = new Paragraph("Forma de Pago: " + formaPago, fontNegrita);
            doc.add(pFormaPago);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));

            // === FIRMAS ===
            Paragraph pFirmas = new Paragraph("_________________________                    _________________________", fontNormal);
            pFirmas.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFirmas);

            Paragraph pFirmasLabel = new Paragraph("        Vendedor                                       Cliente", fontPequena);
            pFirmasLabel.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFirmasLabel);

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addFilaTotal(PdfPTable tabla, String etiqueta, String valor, Font fontValor, Font fontEtiqueta) {
        PdfPCell cellEtiqueta = new PdfPCell(new Phrase(etiqueta, fontEtiqueta));
        cellEtiqueta.setBorder(PdfPCell.NO_BORDER);
        cellEtiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellEtiqueta.setPadding(3);
        tabla.addCell(cellEtiqueta);

        PdfPCell cellValor = new PdfPCell(new Phrase(valor, fontValor));
        cellValor.setBorder(PdfPCell.NO_BORDER);
        cellValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValor.setPadding(3);
        tabla.addCell(cellValor);
    }
}

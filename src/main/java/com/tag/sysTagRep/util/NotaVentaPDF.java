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
                               String clienteNombre, String clienteIdentificacion, String clienteDireccion, String clienteTelefono, String clienteCorreo,
                               String formaPago,
                               List<String[]> detalles,
                               BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        generar(rutaArchivo, numNota, fecha, razonSocial, ruc, direccion, telefono, correo,
                clienteNombre, clienteIdentificacion, clienteDireccion, clienteTelefono, clienteCorreo,
                formaPago, detalles, subtotal, iva, BigDecimal.ZERO, total);
    }

    public static void generar(String rutaArchivo, String numNota, String fecha,
                               String razonSocial, String ruc, String direccion, String telefono, String correo,
                               String clienteNombre, String clienteIdentificacion, String clienteDireccion, String clienteTelefono, String clienteCorreo,
                               String formaPago,
                               List<String[]> detalles,
                               BigDecimal subtotal, BigDecimal iva, BigDecimal descuento, BigDecimal total) {

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

            // === ENCABEZADO: logo + datos empresa a la altura del icono ===
            PdfPTable tablaEncabezado = new PdfPTable(new float[]{15, 70, 15});
            tablaEncabezado.setWidthPercentage(100);

            PdfPCell celdaLogo = new PdfPCell();
            celdaLogo.setBorder(PdfPCell.NO_BORDER);
            celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                java.io.InputStream logoStream = NotaVentaPDF.class.getResourceAsStream("/img/logoTag.jpeg");
                if (logoStream != null) {
                    Image logo = Image.getInstance(logoStream.readAllBytes());
                    logo.scaleToFit(70, 70);
                    logo.setAlignment(Element.ALIGN_LEFT);
                    celdaLogo.addElement(logo);
                }
            } catch (Exception ignored) {}
            tablaEncabezado.addCell(celdaLogo);

            PdfPCell celdaEmpresa = new PdfPCell();
            celdaEmpresa.setBorder(PdfPCell.NO_BORDER);
            celdaEmpresa.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph pEmpresa = new Paragraph(razonSocial, fontTitulo);
            pEmpresa.setAlignment(Element.ALIGN_CENTER);
            celdaEmpresa.addElement(pEmpresa);

            Paragraph pRuc = new Paragraph("RUC: " + ruc, fontNormal);
            pRuc.setAlignment(Element.ALIGN_CENTER);
            celdaEmpresa.addElement(pRuc);

            Paragraph pDireccion = new Paragraph(direccion, fontNormal);
            pDireccion.setAlignment(Element.ALIGN_CENTER);
            celdaEmpresa.addElement(pDireccion);

            Paragraph pContacto = new Paragraph("Tel: " + telefono + "  |  " + correo, fontPequena);
            pContacto.setAlignment(Element.ALIGN_CENTER);
            celdaEmpresa.addElement(pContacto);

            tablaEncabezado.addCell(celdaEmpresa);

            PdfPCell celdaVacia = new PdfPCell(new Phrase("", fontNormal));
            celdaVacia.setBorder(PdfPCell.NO_BORDER);
            tablaEncabezado.addCell(celdaVacia);

            doc.add(tablaEncabezado);

            doc.add(new Paragraph(" "));

            // === NUMERO DE NOTA ===
            Paragraph pNumNota = new Paragraph("PROFORMA  N° " + numNota, fontSubtitulo);
            pNumNota.setAlignment(Element.ALIGN_CENTER);
            doc.add(pNumNota);

            doc.add(new Paragraph(" "));

            // === CLIENTE (3 columnas) ===
            Paragraph pClienteTitulo = new Paragraph("DATOS DEL CLIENTE", fontNegrita);
            doc.add(pClienteTitulo);

            PdfPTable tablaCliente = new PdfPTable(3);
            tablaCliente.setWidthPercentage(100);
            tablaCliente.setWidths(new float[]{40, 30, 30});

            tablaCliente.addCell(celdaCliente("Nombre: " + (clienteNombre != null ? clienteNombre : "")));
            tablaCliente.addCell(celdaCliente("RUC/Cédula: " + (clienteIdentificacion != null ? clienteIdentificacion : "")));
            tablaCliente.addCell(celdaCliente("Fecha: " + fecha));

            tablaCliente.addCell(celdaCliente("Dirección: " + (clienteDireccion != null ? clienteDireccion : "")));
            tablaCliente.addCell(celdaCliente("Email: " + (clienteCorreo != null ? clienteCorreo : "")));
            tablaCliente.addCell(celdaCliente("Teléfono: " + (clienteTelefono != null ? clienteTelefono : "")));

            doc.add(tablaCliente);

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

            // === FORMA DE PAGO Y RECUADRO (izquierda) + TOTALES (derecha) ===
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(100);
            tablaTotales.setWidths(new float[]{60, 40});

            addFilaTotal(tablaTotales, "Subtotal:", subtotal.setScale(2, RoundingMode.HALF_UP).toString(), fontPequena, fontPequenaNegrita);
            addFilaTotal(tablaTotales, "IVA (15%):", iva.setScale(2, RoundingMode.HALF_UP).toString(), fontPequena, fontPequenaNegrita);
            if (descuento.compareTo(BigDecimal.ZERO) > 0) {
                addFilaTotal(tablaTotales, "Descuento:", "-" + descuento.setScale(2, RoundingMode.HALF_UP).toString(), fontPequena, fontPequenaNegrita);
            }
            addFilaTotal(tablaTotales, "TOTAL:", total.setScale(2, RoundingMode.HALF_UP).toString(), fontNegrita, fontNegrita);

            PdfPTable tablaIzq = new PdfPTable(1);
            tablaIzq.setWidthPercentage(100);

            PdfPCell celdaIzq = new PdfPCell();
            celdaIzq.setBorder(PdfPCell.NO_BORDER);
            celdaIzq.setVerticalAlignment(Element.ALIGN_TOP);

            Paragraph pFormaPago = new Paragraph("Forma de Pago: " + formaPago, fontNegrita);
            celdaIzq.addElement(pFormaPago);

            celdaIzq.addElement(new Paragraph(" "));

            String textoLegal = "Deberé y pagaré incondicionalmente a la orden de Tag Repuestos Automotrices "
                    + "en el lugar y fecha establecida, el valor expresado en esta factura y el máximo interés "
                    + "por mora más todos los gastos que ocasione su cobro. Renuncio a domicilio y me someto "
                    + "a los jueces competentes del D.M de Quito y al trámite ejecutivo o verbal sumario "
                    + "a elección de Tag Repuestos Automotrices.";

            PdfPTable tablaRecuadro = new PdfPTable(1);
            tablaRecuadro.setWidthPercentage(100);

            PdfPCell celdaRecuadro = new PdfPCell(new Phrase(textoLegal, fontPequena));
            celdaRecuadro.setBorder(PdfPCell.BOX);
            celdaRecuadro.setPadding(6);
            tablaRecuadro.addCell(celdaRecuadro);

            celdaIzq.addElement(tablaRecuadro);
            tablaIzq.addCell(celdaIzq);

            PdfPCell celdaDer = new PdfPCell();
            celdaDer.setBorder(PdfPCell.NO_BORDER);
            celdaDer.setVerticalAlignment(Element.ALIGN_TOP);
            celdaDer.addElement(tablaTotales);

            PdfPTable filaPie = new PdfPTable(2);
            filaPie.setWidthPercentage(100);
            filaPie.setWidths(new float[]{60, 40});
            filaPie.addCell(celdaIzq);
            filaPie.addCell(celdaDer);

            doc.add(filaPie);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));

            // === FIRMAS ===
            Paragraph pFirmas = new Paragraph("_________________________                    _________________________", fontNormal);
            pFirmas.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFirmas);

            Paragraph pFirmasLabel = new Paragraph("    Firma autorizada                         Firma Cliente     ", fontPequena);
            pFirmasLabel.setAlignment(Element.ALIGN_CENTER);
            doc.add(pFirmasLabel);

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PdfPCell celdaCliente(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fontPequena()));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private static Font fontPequena() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9);
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

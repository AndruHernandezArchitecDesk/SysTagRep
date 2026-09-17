package com.vendex.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;

public class PdfRetencion {

    private static final java.awt.Color GRIS_CLARO = new java.awt.Color(238, 238, 238);
    private static final java.awt.Color GRIS_ENCABEZADO = new java.awt.Color(225, 225, 225);
    private static final java.awt.Color GRIS_LINEA = new java.awt.Color(190, 190, 190);
    private static final java.awt.Color MORADO_TITULO = new java.awt.Color(90, 0, 90);

    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font FONT_NORMAL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font FONT_PEQUENA = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

    @SuppressWarnings("unchecked")
    public static void generar(String rutaPdf,
                               String claveAcceso, String numeroAutorizacion, String fechaAutorizacion,
                               String ambiente, String ruc, String razonSocial, String dirMatriz,
                               String codEst, String codPtoEmi, int secuencial,
                               String fechaEmision, String periodoFiscal,
                               String tipoIdSujeto, String razonSujeto, String identSujeto,
                               List<Object[]> docsSustento) {
        try {
            Document doc = new Document(PageSize.A4, 24, 24, 24, 24);
            PdfWriter.getInstance(doc, new FileOutputStream(rutaPdf));
            doc.open();

            PdfPTable enc = new PdfPTable(new float[]{45, 55});
            enc.setWidthPercentage(100);
            PdfPCell cIzq = new PdfPCell(); cIzq.setBorder(PdfPCell.NO_BORDER);
            try {
                java.io.InputStream is = PdfRetencion.class.getResourceAsStream("/img/logoVendex.jpeg");
                if (is != null) { Image logo = Image.getInstance(is.readAllBytes()); logo.scaleToFit(110,110); logo.setAlignment(Element.ALIGN_CENTER); cIzq.addElement(logo); }
            } catch (Exception ignored) {}
            cIzq.addElement(new Paragraph(" "));
            PdfPTable caja = new PdfPTable(1); caja.setWidthPercentage(100);
            caja.addCell(filaEmisor("Emisor:", razonSocial));
            caja.addCell(filaEmisor("RUC:", ruc));
            caja.addCell(filaEmisor("Matriz:", dirMatriz));
            cIzq.addElement(caja);
            enc.addCell(cIzq);

            PdfPCell cDer = new PdfPCell(); cDer.setBorder(PdfPCell.NO_BORDER); cDer.setVerticalAlignment(Element.ALIGN_TOP);
            Font fTit = new Font(Font.HELVETICA, 15, Font.BOLD, MORADO_TITULO);
            PdfPTable tTit = new PdfPTable(2); tTit.setWidthPercentage(100);
            PdfPCell ct = new PdfPCell(new Phrase("COMPROBANTE DE RETENCIÓN", fTit)); ct.setBorder(PdfPCell.NO_BORDER);
            String num = codEst+"-"+codPtoEmi+"-"+String.format("%09d", secuencial);
            PdfPCell cn = new PdfPCell(new Phrase(num, FONT_NORMAL_BOLD)); cn.setBorder(PdfPCell.NO_BORDER); cn.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tTit.addCell(ct); tTit.addCell(cn);
            cDer.addElement(tTit);
            PdfPTable box = new PdfPTable(1); box.setWidthPercentage(100);
            PdfPCell b1 = new PdfPCell(new Phrase("Periodo Fiscal: " + periodoFiscal, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9))); b1.setBackgroundColor(new java.awt.Color(240,230,250)); b1.setBorderColor(GRIS_LINEA); b1.setPadding(4); b1.setHorizontalAlignment(Element.ALIGN_CENTER); box.addCell(b1);
            cDer.addElement(box);
            enc.addCell(cDer);
            doc.add(enc);
            doc.add(new Paragraph(" "));

            try {
                String qrContent = "CLAVE ACCESO: " + claveAcceso + "\nAUTORIZACIÓN: " + (numeroAutorizacion!=null?numeroAutorizacion:"PENDIENTE");
                com.google.zxing.common.BitMatrix m = new com.google.zxing.oned.Code128Writer().encode(qrContent, com.google.zxing.BarcodeFormat.CODE_128, 260, 45);
                BufferedImage bi = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(m);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(bi,"PNG",baos);
                Image img = Image.getInstance(baos.toByteArray()); img.scaleToFit(260,45); img.setAlignment(Element.ALIGN_CENTER); doc.add(img);
                Paragraph pc = new Paragraph(claveAcceso!=null?claveAcceso:"", FontFactory.getFont(FontFactory.HELVETICA,7)); pc.setAlignment(Element.ALIGN_CENTER); doc.add(pc);
            } catch (Exception e) {}

            PdfPTable info = new PdfPTable(2); info.setWidthPercentage(100);
            info.addCell(filaEtiqueta("Fecha emisión:")); info.addCell(filaValor(fechaEmision));
            info.addCell(filaEtiqueta("Sujeto retenido:")); info.addCell(filaValor(razonSujeto+" ("+identSujeto+")"));
            info.addCell(filaEtiqueta("Tipo ID:")); info.addCell(filaValor(tipoIdSujeto));
            info.addCell(filaEtiqueta("Periodo:")); info.addCell(filaValor(periodoFiscal));
            doc.add(info);
            doc.add(new Paragraph(" "));

            if (docsSustento != null) {
                int idx=1;
                for (Object[] ds : docsSustento) {
                    String codSust = (String) ds[0];
                    String codDoc = (String) ds[1];
                    String numDoc = (String) ds[2];
                    String fecha = (String) ds[3];
                    String total = (String) ds[4];
                    List<Object[]> rets = ds.length>6 ? (List<Object[]>) ds[6] : null;
                    PdfPTable h = new PdfPTable(1); h.setWidthPercentage(100);
                    PdfPCell hc = new PdfPCell(new Phrase("DOCUMENTO SUSTENTO " + idx + ": " + numDoc + " ("+codDoc+")  Fecha: "+fecha+"  Total: "+total, FONT_NORMAL_BOLD));
                    hc.setBackgroundColor(GRIS_ENCABEZADO); hc.setBorderColor(GRIS_LINEA); hc.setPadding(4); h.addCell(hc);
                    doc.add(h);
                    PdfPTable rt = new PdfPTable(new float[]{15,20,25,20,20}); rt.setWidthPercentage(100);
                    rt.addCell(celdaCab("Código")); rt.addCell(celdaCab("Código Ret.")); rt.addCell(celdaCab("Base Imponible")); rt.addCell(celdaCab("% Retener")); rt.addCell(celdaCab("Valor Retenido"));
                    if (rets != null) for (Object[] r : rets) {
                        rt.addCell(celdaDato((String)r[0])); rt.addCell(celdaDato((String)r[1])); rt.addCell(celdaDato((String)r[2])); rt.addCell(celdaDato((String)r[3])); rt.addCell(celdaDato((String)r[4]));
                    }
                    doc.add(rt);
                    doc.add(new Paragraph(" "));
                    idx++;
                }
            }

            PdfPTable auth = new PdfPTable(2); auth.setWidthPercentage(100);
            PdfPCell at = new PdfPCell(new Phrase("AUTORIZACIÓN SRI", FONT_SUBTITULO)); at.setColspan(2); at.setBackgroundColor(GRIS_ENCABEZADO); at.setBorderColor(GRIS_LINEA); at.setPadding(4); at.setHorizontalAlignment(Element.ALIGN_CENTER); auth.addCell(at);
            auth.addCell(filaEtiqueta("Clave acceso:")); auth.addCell(filaValor(claveAcceso));
            auth.addCell(filaEtiqueta("Número autorización:")); auth.addCell(filaValor(numeroAutorizacion!=null?numeroAutorizacion:"PENDIENTE"));
            auth.addCell(filaEtiqueta("Fecha autorización:")); auth.addCell(filaValor(fechaAutorizacion!=null?fechaAutorizacion:""));
            doc.add(auth);
            doc.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static PdfPCell filaEmisor(String c, String v){ PdfPCell cell=new PdfPCell(new Phrase(c+" "+(v!=null?v:""), FONT_NORMAL)); cell.setBorder(PdfPCell.NO_BORDER); cell.setPadding(1); return cell; }
    private static PdfPCell filaEtiqueta(String c){ PdfPCell cell=new PdfPCell(new Phrase(c, FONT_NORMAL)); cell.setBorder(PdfPCell.NO_BORDER); cell.setPadding(1); return cell; }
    private static PdfPCell filaValor(String v){ PdfPCell cell=new PdfPCell(new Phrase(v!=null?v:"", FONT_NORMAL)); cell.setBorder(PdfPCell.NO_BORDER); cell.setPadding(1); cell.setHorizontalAlignment(Element.ALIGN_RIGHT); return cell; }
    private static PdfPCell celdaCab(String t){ PdfPCell c=new PdfPCell(new Phrase(t, FONT_NORMAL_BOLD)); c.setBackgroundColor(GRIS_ENCABEZADO); c.setBorderColor(GRIS_LINEA); c.setPadding(4); c.setHorizontalAlignment(Element.ALIGN_CENTER); return c; }
    private static PdfPCell celdaDato(String t){ PdfPCell c=new PdfPCell(new Phrase(t!=null?t:"", FONT_NORMAL)); c.setBorderColor(GRIS_LINEA); c.setPadding(4); c.setHorizontalAlignment(Element.ALIGN_RIGHT); return c; }
}

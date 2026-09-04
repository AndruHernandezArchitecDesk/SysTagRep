package com.tag.sysTagRep.util;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tag.sysTagRep.dao.UbicacionDetalleDAO;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.model.UbicacionDetalle;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Genera una hoja A4 con todas las etiquetas de una factura agrupadas.
 * Usa el mismo diseño de EtiquetaUtil (480x200, 60x25mm a 203 DPI) pero
 * maquetado en 3 columnas, con copias 1/N según cantidad/stockAsignado.
 */
public class HojaEtiquetasPDF {

    private static final String RUTA_LOGO = "/img/logoTag.jpeg";

    public static File generarHojaA4(List<Inventario> productos, String numeroFactura) throws Exception {
        if (productos == null || productos.isEmpty()) throw new IllegalArgumentException("Lista de productos vacía");

        UbicacionDetalleDAO ubicDAO = new UbicacionDetalleDAO();

        // Directorio etiquetas SYSTAG
        Path dir = EtiquetaUtil.getEtiquetasDirPublic();
        Files.createDirectories(dir);
        String safeFactura = numeroFactura == null ? "S_N" : numeroFactura.replaceAll("[^a-zA-Z0-9]", "_");
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File outFile = new File(dir.toFile(), "hoja_factura_" + safeFactura + "_" + ts + ".pdf");

        Rectangle pageSize = PageSize.A4;
        Document doc = new Document(pageSize, 10, 10, 10, 10);
        PdfWriter.getInstance(doc, java.nio.file.Files.newOutputStream(outFile.toPath()));
        doc.open();

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        // anchos iguales
        table.setWidths(new float[]{1f, 1f, 1f});

        int totalEtiquetas = 0;
        final int CAP = 300;
        outer: for (Inventario inv : productos) {
            // SERVICIO LOGISTICO no debe tener etiqueta
            boolean esServicio = inv.getDescripcion() != null && inv.getDescripcion().trim().equalsIgnoreCase("SERVICIO LOGISTICO")
                    || "000".equalsIgnoreCase(inv.getCodigo());
            if (esServicio) continue;
            List<UbicacionDetalle> ubics = ubicDAO.listarPorProducto(inv.getId());
            if (ubics == null || ubics.isEmpty()) {
                // SIN-UBIC: cantidad copias = inv.cantidad con 1/N (capado)
                int total = inv.getCantidad() > 0 ? inv.getCantidad() : 1;
                if (total > 100) total = 100;
                UbicacionDetalle fake = new UbicacionDetalle();
                fake.setCodigoUbicacion("SIN-UBIC");
                fake.setStockAsignado(total);
                for (int i = 1; i <= total; i++) {
                    if (totalEtiquetas >= CAP) break outer;
                    BufferedImage bi = EtiquetaUtil.renderImagenEtiquetaConIndice(inv, fake, i, total, RUTA_LOGO);
                    addImageCell(table, bi);
                    totalEtiquetas++;
                }
            } else {
                for (UbicacionDetalle u : ubics) {
                    int total = (u.getStockAsignado() != null && u.getStockAsignado() > 0) ? u.getStockAsignado() : 1;
                    if (total > 100) total = 100;
                    for (int i = 1; i <= total; i++) {
                        if (totalEtiquetas >= CAP) break outer;
                        BufferedImage bi = EtiquetaUtil.renderImagenEtiquetaConIndice(inv, u, i, total, RUTA_LOGO);
                        addImageCell(table, bi);
                        totalEtiquetas++;
                    }
                }
            }
        }

        // Rellenar última fila incompleta para no deformar ancho
        int resto = totalEtiquetas % 3;
        if (resto != 0) {
            for (int i = 0; i < 3 - resto; i++) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(PdfPCell.NO_BORDER);
                empty.setFixedHeight(72f);
                table.addCell(empty);
            }
        }

        doc.add(table);
        doc.close();
        return outFile;
    }

    private static void addImageCell(PdfPTable table, BufferedImage bi) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", baos);
        Image img = Image.getInstance(baos.toByteArray());
        // 60x25mm aprox 170x72 pt en A4 (450pt ancho usable /3)
        img.scaleToFit(160f, 68f);
        PdfPCell cell = new PdfPCell(img, true);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(4f);
        cell.setFixedHeight(75f);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}

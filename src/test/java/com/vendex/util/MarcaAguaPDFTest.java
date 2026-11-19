package com.vendex.util;

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfReader;
import com.vendex.model.MarcaAguaPDFConfig;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class MarcaAguaPDFTest {
    @Test
    void watermarkOverlaysOpaqueDetailRowsOnEveryPageOfInvoiceAndProforma() throws Exception {
        byte[] logo = crearLogoDePrueba();
        for (MarcaAguaPDF.TipoDocumento tipo : MarcaAguaPDF.TipoDocumento.values()) {
            for (MarcaAguaPDFConfig.Distribucion distribucion : MarcaAguaPDFConfig.Distribucion.values()) {
                MarcaAguaPDFConfig config = new MarcaAguaPDFConfig(true, true, distribucion, 0.14f, logo);
                File pdf = Files.createTempFile("marca-agua-test-", ".pdf").toFile();
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(pdf)) {
                    Rectangle pageSize = tipo == MarcaAguaPDF.TipoDocumento.PROFORMA
                            ? new Rectangle(PageSize.A4.getWidth(), PageSize.A4.getHeight() / 2) : PageSize.A4;
                    Document document = new Document(pageSize);
                    PdfWriter writer = PdfWriter.getInstance(document, output);
                    assertTrue(MarcaAguaPDF.configurar(writer, tipo, config));
                    document.open();
                    com.lowagie.text.pdf.PdfPTable detalle = new com.lowagie.text.pdf.PdfPTable(1);
                    for (int i = 0; i < 8; i++) {
                        var celda = new com.lowagie.text.pdf.PdfPCell(new Paragraph("Fila de detalle opaca " + i));
                        celda.setBackgroundColor(java.awt.Color.WHITE);
                        celda.setFixedHeight(24);
                        detalle.addCell(celda);
                    }
                    document.add(detalle);
                    for (int i = 0; i < 45; i++) document.add(new Paragraph("Detalle de prueba " + i));
                    document.close();
                    try (PdfReader reader = new PdfReader(pdf.getAbsolutePath())) {
                        assertTrue(reader.getNumberOfPages() > 1, "la muestra debe abarcar varias páginas");
                        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                            var resources = reader.getPageN(page).getAsDict(com.lowagie.text.pdf.PdfName.RESOURCES);
                            assertNotNull(resources.getAsDict(com.lowagie.text.pdf.PdfName.XOBJECT),
                                    "cada página debe incluir el logo como XObject de marca de agua");
                            String stream = new String(reader.getPageContent(page),
                                    java.nio.charset.StandardCharsets.ISO_8859_1);
                            assertTrue(stream.lastIndexOf(" Do") > stream.lastIndexOf("Tj"),
                                    "la marca debe dibujarse después de las filas de detalle opacas en cada página");
                        }
                    }
                } finally {
                    Files.deleteIfExists(pdf.toPath());
                }
            }
        }
    }

    @Test
    void disabledConfigurationDoesNotInstallPageEvent() throws Exception {
        File pdf = Files.createTempFile("marca-agua-disabled-", ".pdf").toFile();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(pdf)) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(PageSize.A4);
            var writer = com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            assertFalse(MarcaAguaPDF.configurar(writer, MarcaAguaPDF.TipoDocumento.FACTURA,
                    MarcaAguaPDFConfig.desactivada()));
            doc.open();
            doc.add(new com.lowagie.text.Paragraph("Factura sin marca de agua"));
            doc.close();
        } finally {
            Files.deleteIfExists(pdf.toPath());
        }
    }

    @Test
    void normalizaLogoGrandeAntesDeUsarloEnLaVistaPrevia() throws Exception {
        BufferedImage highResolution = new BufferedImage(2400, 1600, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = highResolution.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, highResolution.getWidth(), highResolution.getHeight());
            graphics.setColor(java.awt.Color.BLUE);
            graphics.fillOval(100, 100, 1400, 1400);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        ImageIO.write(highResolution, "png", input);

        byte[] normalized = MarcaAguaPDF.normalizarLogo(input.toByteArray());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(normalized));
        assertTrue(decoded.getWidth() <= 1200);
        assertTrue(decoded.getHeight() <= 1200);
        assertTrue(normalized.length <= 5 * 1024 * 1024);
    }

    private static byte[] crearLogoDePrueba() throws Exception {
        BufferedImage image = new BufferedImage(48, 32, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new java.awt.Color(20, 90, 160, 220));
            graphics.fillOval(2, 2, 44, 28);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}

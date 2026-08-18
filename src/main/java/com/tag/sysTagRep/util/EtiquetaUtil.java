package com.tag.sysTagRep.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.tag.sysTagRep.model.Inventario;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

public class EtiquetaUtil {

    private static final int WIDTH_PX = 480;
    private static final int HEIGHT_PX = 280;
    private static final int MARGIN = 8;
    private static final int LOGO_SIZE = 48;
    private static final int DPI = 203;

    private static Path getEtiquetasDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        Path docs;
        if (os.contains("win")) {
            docs = Paths.get(home, "Documents");
        } else {
            docs = Paths.get(home, "Documentos");
        }
        Path dir = docs.resolve("etiquetas SYSTAG");
        return dir;
    }

    public static File generarEtiqueta(Inventario item, String rutaLogo, String razonSocial) {
        BufferedImage img = new BufferedImage(WIDTH_PX, HEIGHT_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH_PX, HEIGHT_PX);

        int y = 6;

        try {
            InputStream logoStream = EtiquetaUtil.class.getResourceAsStream(rutaLogo);
            if (logoStream != null) {
                java.awt.Image logo = ImageIO.read(logoStream).getScaledInstance(LOGO_SIZE, LOGO_SIZE, java.awt.Image.SCALE_SMOOTH);
                g.drawImage(logo, MARGIN, y, null);
            }
        } catch (Exception ignored) {}

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("TAG REPUESTOS AUTOMOTRICES", MARGIN + LOGO_SIZE + 4, y + LOGO_SIZE - 5);

        String codigo = item.getCodigo() != null ? item.getCodigo() : "";
        String tagCodigo = item.getTagCodigo() != null ? item.getTagCodigo() : "";
        String descripcion = item.getDescripcion() != null ? item.getDescripcion() : "";
        String ubicacion = item.getUbicacionPercha() != null ? item.getUbicacionPercha() : "";
        LocalDateTime fechaIngreso = item.getFecha_ingreso();
        String fechaStr = fechaIngreso != null
                ? fechaIngreso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "";

        y += LOGO_SIZE + 6;

        int barcodeWidth = 410;
        int barcodeHeight = 80;
        int barcodeX = MARGIN;

        if (!codigo.trim().isEmpty()) {
            try {
                BitMatrix matrix = new Code128Writer().encode(codigo, BarcodeFormat.CODE_128, barcodeWidth, barcodeHeight);
                BufferedImage barcodeImg = MatrixToImageWriter.toBufferedImage(matrix);
                g.drawImage(barcodeImg, barcodeX, y, null);
            } catch (Exception ignored) {}
        }
        y += barcodeHeight + 16;

        int rightX = WIDTH_PX - MARGIN;

        if (!codigo.trim().isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 23));
            g.drawString(codigo, MARGIN, y + 8);
            y += 30;
        }

        String precioStr = item.getPrecioVenta() != null
                ? cifrarPrecio(item.getPrecioVenta().setScale(2, java.math.RoundingMode.HALF_UP).toString())
                : "";

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        int precioWidth = precioStr.isEmpty() ? 0 : g.getFontMetrics().stringWidth(precioStr);
        int descMaxWidth = rightX - MARGIN - precioWidth - 8;
        List<String> lineas = partirTexto(g, descripcion, descMaxWidth, 3);
        if (lineas.isEmpty()) {
            g.drawString(precioStr, rightX - precioWidth, y + 6);
            y += 27;
        } else {
            for (int i = 0; i < lineas.size(); i++) {
                g.drawString(lineas.get(i), MARGIN, y + 6);
                if (i == lineas.size() - 1) {
                    g.drawString(precioStr, rightX - precioWidth, y + 6);
                }
                y += 27;
            }
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("Fecha: " + fechaStr, MARGIN, y + 5);

        g.setFont(new Font("SansSerif", Font.PLAIN, 19));
        String ubicText = "Ubic: " + ubicacion;
        g.drawString(ubicText, rightX - g.getFontMetrics().stringWidth(ubicText), y + 5);

        g.dispose();

        try {
            Path dir = getEtiquetasDir();
            Files.createDirectories(dir);
            String safeName = codigo.replaceAll("[^a-zA-Z0-9]", "_");
            File outFile = new File(dir.toFile(), "etiqueta_" + safeName + ".jpg");
            escribirJpgConDpi(img, outFile);
            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> partirTexto(Graphics2D g, String texto, int maxWidth, int maxLineas) {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) {
            return lineas;
        }
        FontMetrics fm = g.getFontMetrics();
        String[] palabras = texto.trim().split("\\s+");
        StringBuilder actual = new StringBuilder();
        boolean truncado = false;
        for (String palabra : palabras) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) <= maxWidth) {
                actual.setLength(0);
                actual.append(prueba);
            } else {
                if (actual.length() > 0) {
                    if (lineas.size() == maxLineas - 1) {
                        truncado = true;
                        break;
                    }
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                actual.append(palabra);
            }
        }
        if (truncado) {
            lineas.add(conElipsis(fm, actual.toString(), maxWidth));
        } else if (actual.length() > 0) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static String conElipsis(FontMetrics fm, String texto, int maxWidth) {
        String res = texto;
        while (!res.isEmpty() && fm.stringWidth(res + "...") > maxWidth) {
            res = res.substring(0, res.length() - 1);
        }
        return res.isEmpty() ? "..." : res + "...";
    }

    private static void escribirJpgConDpi(BufferedImage img, File outFile) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.95f);

        IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(img), param);
        Element tree = (Element) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
        Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);
        if (jfif != null) {
            jfif.setAttribute("resUnits", "1");
            jfif.setAttribute("Xdensity", String.valueOf(DPI));
            jfif.setAttribute("Ydensity", String.valueOf(DPI));
            metadata.mergeTree("javax_imageio_jpeg_image_1.0", tree);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, metadata), param);
        } finally {
            writer.dispose();
        }
    }

    public static String cifrarPrecio(String valor) {
        StringBuilder sb = new StringBuilder();
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '0': sb.append('S'); break;
                case '1': sb.append('M'); break;
                case '2': sb.append('O'); break;
                case '3': sb.append('N'); break;
                case '4': sb.append('T'); break;
                case '5': sb.append('A'); break;
                case '6': sb.append('G'); break;
                case '7': sb.append('U'); break;
                case '8': sb.append('I'); break;
                case '9': sb.append('V'); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
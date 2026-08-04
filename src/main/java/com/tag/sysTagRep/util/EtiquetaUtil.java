package com.tag.sysTagRep.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.tag.sysTagRep.model.Inventario;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EtiquetaUtil {

    private static final int WIDTH_PX = 354;
    private static final int HEIGHT_PX = 177;
    private static final int MARGIN = 4;
    private static final int LOGO_SIZE = 35;

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

        int y = 3;

        try {
            InputStream logoStream = EtiquetaUtil.class.getResourceAsStream(rutaLogo);
            if (logoStream != null) {
                java.awt.Image logo = ImageIO.read(logoStream).getScaledInstance(LOGO_SIZE, LOGO_SIZE, java.awt.Image.SCALE_SMOOTH);
                g.drawImage(logo, MARGIN, y, null);
            }
        } catch (Exception ignored) {}

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(razonSocial, MARGIN + LOGO_SIZE + 4, y + LOGO_SIZE - 5);

        String tagCodigo = item.getTagCodigo() != null ? item.getTagCodigo() : "";
        String descripcion = item.getDescripcion() != null ? item.getDescripcion() : "";
        String ubicacion = item.getUbicacionPercha() != null ? item.getUbicacionPercha() : "";
        LocalDateTime fechaIngreso = item.getFecha_ingreso();
        String fechaStr = fechaIngreso != null
                ? fechaIngreso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "";

        y += LOGO_SIZE + 6;

        int barcodeWidth = WIDTH_PX - 2 * MARGIN;
        int barcodeHeight = 65;

        if (!tagCodigo.trim().isEmpty()) {
            try {
                BitMatrix matrix = new Code128Writer().encode(tagCodigo, BarcodeFormat.CODE_128, barcodeWidth, barcodeHeight);
                BufferedImage barcodeImg = MatrixToImageWriter.toBufferedImage(matrix);
                g.drawImage(barcodeImg, MARGIN, y, null);
            } catch (Exception ignored) {}
        }
        y += barcodeHeight + 6;

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(tagCodigo, MARGIN, y + 8);
        y += 13;

        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        String descShort = descripcion.length() > 30 ? descripcion.substring(0, 30) + "..." : descripcion;
        g.drawString(descShort, MARGIN, y + 6);
        y += 11;

        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g.drawString("Fecha: " + fechaStr, MARGIN, y + 5);

        int rightX = WIDTH_PX - MARGIN;
        String precioStr = item.getPrecioVenta() != null
                ? cifrarPrecio(item.getPrecioVenta().setScale(2, java.math.RoundingMode.HALF_UP).toString())
                : "";
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g.drawString(precioStr, rightX - g.getFontMetrics().stringWidth(precioStr), y + 5);

        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        String ubicText = "Ubic: " + ubicacion;
        g.drawString(ubicText, rightX - g.getFontMetrics().stringWidth(ubicText), y + 14);

        g.dispose();

        try {
            Path dir = getEtiquetasDir();
            Files.createDirectories(dir);
            String safeName = tagCodigo.replaceAll("[^a-zA-Z0-9]", "_");
            File outFile = new File(dir.toFile(), "etiqueta_" + safeName + ".jpg");
            ImageIO.write(img, "jpg", outFile);
            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
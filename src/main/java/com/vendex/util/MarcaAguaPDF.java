package com.vendex.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.vendex.dao.MarcaAguaPDFDAOPostgres;
import com.vendex.model.MarcaAguaPDFConfig;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Dibuja el logo como marca de agua debajo del contenido en todas las páginas. */
public final class MarcaAguaPDF {
    private static final Logger LOG = Logger.getLogger(MarcaAguaPDF.class.getName());
    private static final int MAX_LOGO_BYTES = 5 * 1024 * 1024;
    private static final int MAX_LOGO_DIMENSION = 1200;

    public enum TipoDocumento { FACTURA, PROFORMA }

    private MarcaAguaPDF() { }

    /** Lee la configuración única compartida desde la base central. */
    public static boolean configurar(PdfWriter writer, TipoDocumento tipo) {
        try {
            return configurar(writer, tipo, new MarcaAguaPDFDAOPostgres().obtener());
        } catch (Exception e) {
            if (e instanceof java.sql.SQLException sql && "42P01".equals(sql.getSQLState())) {
                LOG.fine("La configuración compartida de marca de agua aún no está migrada; el PDF sale sin marca.");
            } else {
                LOG.log(Level.WARNING, "No se pudo leer la configuración compartida de marca de agua", e);
            }
            return false;
        }
    }

    public static boolean configurar(PdfWriter writer, TipoDocumento tipo, MarcaAguaPDFConfig config) {
        if (writer == null || tipo == null || config == null) return false;
        boolean activa = tipo == TipoDocumento.FACTURA
                ? config.isFacturaActiva() : config.isProformaActiva();
        if (!activa) return false;
        try {
            byte[] logo = obtenerLogo(config);
            if (logo == null || logo.length == 0) return false;
            writer.setPageEvent(new MarcaAguaEvento(logo, config.getDistribucion(), config.getOpacidad()));
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo preparar el logo de marca de agua", e);
            return false;
        }
    }

    /** Genera una muestra temporal con la configuración de diseño seleccionada. */
    public static void generarVistaPrevia(File destino, MarcaAguaPDFConfig config, TipoDocumento tipo) throws Exception {
        Rectangle page = tipo == TipoDocumento.PROFORMA
                ? new Rectangle(PageSize.A4.getWidth(), PageSize.A4.getHeight() / 2f)
                : PageSize.A4;
        Document document = new Document(page, 36, 36, 36, 36);
        try (FileOutputStream output = new FileOutputStream(destino)) {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            configurar(writer, tipo, config);
            document.open();
            Paragraph title = new Paragraph(tipo == TipoDocumento.PROFORMA
                    ? "VISTA PREVIA — PROFORMA" : "VISTA PREVIA — FACTURA",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Marca de agua: logo de empresa"));
            document.add(new Paragraph("La marca se dibuja detrás del contenido en cada página."));
            for (int i = 0; i < 5; i++) document.add(new Paragraph("Muestra de detalle " + (i + 1)));
            document.close();
        }
    }

    private static byte[] obtenerLogo(MarcaAguaPDFConfig config) throws Exception {
        byte[] compartido = config.getLogoBytes();
        if (compartido != null && compartido.length > 0) {
            if (compartido.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("El logo compartido supera 5 MB");
            return normalizarLogo(compartido);
        }
        try (InputStream stream = EmpresaLogo.abrirLogoStream(null)) {
            if (stream == null) return null;
            byte[] local = stream.readNBytes(MAX_LOGO_BYTES + 1);
            if (local.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("El logo supera 5 MB");
            return normalizarLogo(local);
        }
    }

    /** Reduce imágenes de alta resolución antes de previsualizarlas o incrustarlas en los PDF. */
    public static byte[] normalizarLogo(byte[] entrada) throws Exception {
        if (entrada == null || entrada.length == 0) throw new IllegalArgumentException("El logo está vacío");
        if (entrada.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("El logo supera 5 MB");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(entrada))) {
            if (input == null) throw new IllegalArgumentException("No se reconoce el formato del logo");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Formato de logo no compatible");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int sample = Math.max(1, Math.max(
                        (width + MAX_LOGO_DIMENSION - 1) / MAX_LOGO_DIMENSION,
                        (height + MAX_LOGO_DIMENSION - 1) / MAX_LOGO_DIMENSION));
                ImageReadParam params = reader.getDefaultReadParam();
                params.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, params);
                if (decoded == null) throw new IllegalArgumentException("No se pudo decodificar el logo");
                BufferedImage normalized = new BufferedImage(decoded.getWidth(), decoded.getHeight(),
                        decoded.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = normalized.createGraphics();
                try { graphics.drawImage(decoded, 0, 0, null); }
                finally { graphics.dispose(); }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(normalized, "png", output)) throw new IllegalArgumentException("No se pudo codificar el logo");
                byte[] result = output.toByteArray();
                if (result.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("El logo optimizado supera 5 MB");
                return result;
            } finally {
                reader.dispose();
            }
        }
    }

    private static final class MarcaAguaEvento extends PdfPageEventHelper {
        private final byte[] logoBytes;
        private final MarcaAguaPDFConfig.Distribucion distribucion;
        private final float opacidad;

        private MarcaAguaEvento(byte[] logoBytes, MarcaAguaPDFConfig.Distribucion distribucion, float opacidad) {
            this.logoBytes = logoBytes.clone();
            this.distribucion = distribucion;
            this.opacidad = Math.max(0.05f, Math.min(0.40f, opacidad));
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            canvas.saveState();
            try {
                PdfGState transparency = new PdfGState();
                transparency.setFillOpacity(opacidad);
                transparency.setStrokeOpacity(opacidad);
                canvas.setGState(transparency);
                Rectangle page = writer.getPageSize();
                if (distribucion == MarcaAguaPDFConfig.Distribucion.REPETIDA) drawTiled(canvas, page);
                else drawCentered(canvas, page);
            } catch (Exception e) {
                LOG.log(Level.FINE, "No se pudo dibujar la marca de agua", e);
            } finally {
                canvas.restoreState();
            }
        }

        private void drawCentered(PdfContentByte canvas, Rectangle page) throws Exception {
            Image logo = Image.getInstance(logoBytes);
            logo.scaleToFit(page.getWidth() * 0.58f, page.getHeight() * 0.46f);
            logo.setAbsolutePosition((page.getWidth() - logo.getScaledWidth()) / 2f,
                    (page.getHeight() - logo.getScaledHeight()) / 2f);
            canvas.addImage(logo);
        }

        private void drawTiled(PdfContentByte canvas, Rectangle page) throws Exception {
            final int columns = 4;
            final int rows = 5;
            float cellWidth = page.getWidth() / columns;
            float cellHeight = page.getHeight() / rows;
            Image logo = Image.getInstance(logoBytes);
            logo.scaleToFit(cellWidth * 0.55f, cellHeight * 0.42f);
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    float x = column * cellWidth + (cellWidth - logo.getScaledWidth()) / 2f;
                    float y = page.getHeight() - (row + 1) * cellHeight
                            + (cellHeight - logo.getScaledHeight()) / 2f;
                    logo.setAbsolutePosition(x, y);
                    canvas.addImage(logo);
                }
            }
        }
    }
}

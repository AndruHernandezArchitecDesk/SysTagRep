package com.vendex.service;

import com.vendex.dao.MarcaAguaPDFDAO;
import com.vendex.model.MarcaAguaPDFConfig;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MarcaAguaLogoServiceTest {
    @Test
    void sincronizaNuevoLogoSinCambiarPreferenciasCompartidas() throws Exception {
        var actual = new AtomicReference<>(new MarcaAguaPDFConfig(true, false,
                MarcaAguaPDFConfig.Distribucion.REPETIDA, 0.27f, new byte[]{1}));
        MarcaAguaPDFDAO dao = new MarcaAguaPDFDAO() {
            @Override public MarcaAguaPDFConfig obtener() { return actual.get(); }
            @Override public void guardar(MarcaAguaPDFConfig configuracion) { actual.set(configuracion); }
        };
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(10, 10, 0xff336699);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", bytes));

        new MarcaAguaLogoService(dao).sincronizar(bytes.toByteArray());

        MarcaAguaPDFConfig guardada = actual.get();
        assertTrue(guardada.isFacturaActiva());
        assertFalse(guardada.isProformaActiva());
        assertEquals(MarcaAguaPDFConfig.Distribucion.REPETIDA, guardada.getDistribucion());
        assertEquals(0.27f, guardada.getOpacidad());
        assertNotNull(guardada.getLogoBytes());
        assertNotEquals(1, guardada.getLogoBytes().length);
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(guardada.getLogoBytes())));
    }
}

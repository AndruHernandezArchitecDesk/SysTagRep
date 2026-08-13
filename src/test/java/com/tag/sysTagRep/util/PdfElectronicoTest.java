package com.tag.sysTagRep.util;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PdfElectronicoTest {

    private static final String CLAVE = "1234567890123456789012345678901234567890123456789";
    private static final String NUM_AUT = "0987654321098765432109876543210987654321098765432";

    private String generarRide(String ruta, String numeroAutorizacion, String fechaAutorizacion) {
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"001", "Aceite 5W30", "2", "26.96", "0.00", "53.92"}
        );
        PdfElectronico.generar(ruta, CLAVE, numeroAutorizacion, fechaAutorizacion,
                "PRUEBAS", "1790000000001", "TAG REPUESTOS",
                "Av. Principal y Secundaria", "0999999999", "tag@example.com",
                "", "SI", "Sucursal Matriz", "", "",
                "001", "001", 1, "12/08/2026",
                "04", "Cliente Prueba", "1790000000002",
                "Quito", "cliente@example.com", "0988888888",
                "Efectivo", detalles,
                new BigDecimal("53.92"), new BigDecimal("0.00"),
                new BigDecimal("8.09"), new BigDecimal("62.01"));
        return ruta;
    }

    private String extraerTexto(String ruta) throws Exception {
        PdfReader r = new PdfReader(ruta);
        try {
            return new PdfTextExtractor(r).getTextFromPage(1);
        } finally {
            r.close();
        }
    }

    @Test
    void generar_rideConAutorizacion_archivoCreadoYContieneNumeroAutorizacion() throws Exception {
        File temp = File.createTempFile("factura_ride_test_", ".pdf");
        String ruta = generarRide(temp.getAbsolutePath(), NUM_AUT, "2026-08-12T10:00:00-05:00");
        File f = new File(ruta);
        assertTrue(f.exists(), "El RIDE debería existir en " + ruta);
        assertTrue(f.length() > 0, "El RIDE debería tener contenido");
        String txt = extraerTexto(ruta);
        String normalizado = txt.replaceAll("\\s+", "");
        assertTrue(normalizado.contains(NUM_AUT), "El RIDE debe contener el número de autorización");
        assertTrue(normalizado.contains(CLAVE), "El RIDE debe contener la clave de acceso");
        assertTrue(txt.contains("FACTURA"), "El RIDE debe decir FACTURA");
    }

    @Test
    void generar_ridePendienteSinAutorizacion_noLanzaExcepcion() throws Exception {
        File temp = File.createTempFile("factura_ride_pendiente_test_", ".pdf");
        String ruta = generarRide(temp.getAbsolutePath(), null, null);
        File f = new File(ruta);
        assertTrue(f.exists(), "El RIDE pendiente debería generarse sin autorización");
        assertTrue(f.length() > 0, "El RIDE debería tener contenido");
        String txt = extraerTexto(ruta);
        assertTrue(txt.contains("—") || txt.contains("PRUEBAS"),
                "El RIDE sin autorización debe mostrar un marcador o el ambiente");
    }
}

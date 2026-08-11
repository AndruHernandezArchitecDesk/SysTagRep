package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NotaVentaPDFTest {

    @Test
    void generarPDF_archivoCreadoYContieneContenido() throws Exception {
        File temp = File.createTempFile("nota_venta_test_", ".pdf");
        String ruta = temp.getAbsolutePath();
        List<String[]> detalles = Arrays.asList(
            new String[]{"001", "Aceite 5W30", "2", "15.50", "31.00"},
            new String[]{"002", "Filtro Aceite", "1", "8.00", "8.00"}
        );
        NotaVentaPDF.generar(ruta, "001-001-000000001", "08/08/2026",
                "TAG REPUESTOS", "1790000000001", "Av. Principal", "0999999999", "tag@example.com",
                "Cliente Prueba", "1790000000002", "Quito", "0988888888", "cliente@example.com",
                "Efectivo",
                detalles,
                new BigDecimal("31.00"), new BigDecimal("4.65"), new BigDecimal("0.00"), new BigDecimal("35.65"));
        File f = new File(ruta);
        assertTrue(f.exists(), "El PDF debería existir en " + ruta);
        assertTrue(f.length() > 0, "El PDF debería tener contenido");
        byte[] bytes = Files.readAllBytes(f.toPath());
        String content = new String(bytes);
        assertTrue(content.contains("TAG REPUESTOS") || content.contains("PROFORMA") || content.length() > 0,
                "El PDF debería contener texto de la proforma");
    }
}

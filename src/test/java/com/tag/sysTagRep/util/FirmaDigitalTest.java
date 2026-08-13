package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FirmaDigitalTest {

    private String xmlDePrueba() {
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"001", "Aceite 5W30", "1", "26.96", "0.00", "26.96"}
        );
        return XmlSriBuilder.construirFactura(
                "PRUEBAS", "1234567890123456789012345678901234567890123456789", "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal y Secundaria",
                "", "SI", "04", "Cliente Prueba", "1790000000002",
                "Quito", "26.96", "0.00", "4.04", "31.00", "0.00",
                "Efectivo", "12/08/2026", detalles);
    }

    @Test
    void firmarXml_conFirmaConfigurada_agregaBloqueSignature() throws Exception {
        String[] config = ConfigFirma.cargar();
        assumeTrue(config[0] != null && !config[0].isEmpty() && !config[1].isEmpty(),
                "No hay firma electrónica configurada; se omite la prueba de firma");

        FirmaDigital firma = new FirmaDigital();
        assertTrue(firma.cargarCertificado(config[0], config[1]),
                "El certificado .p12 debe cargarse con la ruta y clave guardadas");

        String xmlFirmado = firma.firmarXml(xmlDePrueba());
        assertNotNull(xmlFirmado);
        assertTrue(xmlFirmado.contains("Signature"), "El XML firmado debe contener el bloque ds:Signature");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlFirmado.getBytes("UTF-8")));
        assertTrue(doc.getElementsByTagNameNS("*", "Signature").getLength() > 0,
                "Debe existir un elemento Signature (cualquier prefijo)");
    }
}

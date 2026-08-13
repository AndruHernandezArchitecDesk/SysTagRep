package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlSriBuilderTest {

    private static final String CLAVE = "1234567890123456789012345678901234567890123456789";

    private String construirXml() {
        List<Object[]> detalles = Arrays.asList(
            new Object[]{"001", "Aceite 5W30", "2", "26.96", "0.00", "53.92"},
            new Object[]{"002", "Filtro Aceite", "1", "6.96", "0.00", "6.96"}
        );
        return XmlSriBuilder.construirFactura(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal y Secundaria",
                "", "SI", "04", "Cliente Prueba", "1790000000002",
                "Quito", "60.87", "0.00", "9.13", "70.00", "0.00",
                "Efectivo", "12/08/2026", detalles);
    }

    @Test
    void construirFactura_xmlBienFormadoYContieneDatosEsenciales() throws Exception {
        String xml = construirXml();
        assertNotNull(xml);
        assertTrue(xml.contains("factura"), "Debe contener la raíz factura");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        assertEquals("factura", doc.getDocumentElement().getTagName());
        assertEquals("1.0.0", doc.getDocumentElement().getAttribute("version"));

        assertTrue(xml.contains(CLAVE), "Debe contener la clave de acceso");
        assertTrue(xml.contains("1790000000001"), "Debe contener el RUC");
        assertTrue(xml.contains("TAG REPUESTOS"), "Debe contener la razón social");
        assertTrue(xml.contains("12/08/2026"), "Debe contener la fecha de emisión");
        assertTrue(xml.contains("70.00"), "Debe contener el total con impuestos");
        assertTrue(xml.contains("Aceite 5W30"), "Debe contener la descripción del detalle");
    }

    @Test
    void construirFactura_ambienteProduccion_usaCodigo2() throws Exception {
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"001", "Aceite 5W30", "1", "26.96", "0.00", "26.96"}
        );
        String xml = XmlSriBuilder.construirFactura(
                "PRODUCCION", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal",
                "", "SI", "04", "Cliente Prueba", "1790000000002",
                "Quito", "26.96", "0.00", "4.04", "31.00", "0.00",
                "Efectivo", "12/08/2026", detalles);
        assertTrue(xml.contains("<ambiente>2</ambiente>"), "Producción debe usar ambiente 2");
    }

    @Test
    void construirFactura_contribuyenteEspecialInformativoNoVaEnInfoTributaria() throws Exception {
        String xml = XmlSriBuilder.construirFactura(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal",
                "NO", "SI", "04", "Cliente Prueba", "1790000000002",
                "Quito", "26.96", "0.00", "4.04", "31.00", "0.00",
                "Efectivo", "12/08/2026",
                List.<Object[]>of(new Object[]{"001", "Aceite 5W30", "1", "26.96", "0.00", "26.96"}));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        Element infoTributaria = (Element) doc.getElementsByTagName("infoTributaria").item(0);
        Element infoFactura = (Element) doc.getElementsByTagName("infoFactura").item(0);

        assertNull(elementoHijo(infoTributaria, "contribuyenteEspecial"),
                "infoTributaria no debe contener contribuyenteEspecial");
        assertNull(elementoHijo(infoTributaria, "agenteRetencion"),
                "infoTributaria no debe contener agenteRetencion con valor no numérico");
        assertNull(elementoHijo(infoFactura, "contribuyenteEspecial"),
                "contribuyenteEspecial de menos de 3 caracteres no debe emitirse");
    }

    @Test
    void construirFactura_agenteRetencionNumericoVaEnInfoTributaria() throws Exception {
        String xml = XmlSriBuilder.construirFactura(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal",
                "001", "SI", "04", "Cliente Prueba", "1790000000002",
                "Quito", "26.96", "0.00", "4.04", "31.00", "0.00",
                "Efectivo", "12/08/2026",
                List.<Object[]>of(new Object[]{"001", "Aceite 5W30", "1", "26.96", "0.00", "26.96"}));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        Element infoTributaria = (Element) doc.getElementsByTagName("infoTributaria").item(0);
        Element infoFactura = (Element) doc.getElementsByTagName("infoFactura").item(0);

        assertEquals("001", elementoHijo(infoTributaria, "agenteRetencion").getTextContent(),
                "infoTributaria debe contener agenteRetencion cuando el valor es numérico");
        assertEquals("001", elementoHijo(infoFactura, "contribuyenteEspecial").getTextContent(),
                "infoFactura puede contener contribuyenteEspecial de 3+ caracteres");
    }

    private Element elementoHijo(Element padre, String nombre) {
        for (int i = 0; i < padre.getChildNodes().getLength(); i++) {
            if (padre.getChildNodes().item(i) instanceof Element
                    && nombre.equals(((Element) padre.getChildNodes().item(i)).getTagName())) {
                return (Element) padre.getChildNodes().item(i);
            }
        }
        return null;
    }
}

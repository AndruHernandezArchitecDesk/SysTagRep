package com.vendex.util;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resolución NAC-DGERCGC26-00000027: todo comprobante emitido con software de
 * terceros debe incluir el RUC del proveedor del sistema en infoAdicional.
 */
class XmlRucProveedorTest {

    private static final String CLAVE = "1234567890123456789012345678901234567890123456789";
    private static final String ESPERADO = AppConstants.RUC_PROVEEDOR_SISTEMA;

    @Test
    void factura_incluyeRucProveedor() {
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"001", "Aceite 5W30", "1", "26.96", "0.00", "26.96"}
        );
        String xml = XmlSriBuilder.construirFactura(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal",
                "", "SI", "04", "Cliente Prueba", "1790000000002",
                "Quito", "26.96", "0.00", "4.04", "31.00", "0.00",
                "Efectivo", "12/08/2026", detalles);
        assertEquals(ESPERADO, campoRucProveedor(xml), "Factura debe incluir RUC Proveedor");
    }

    @Test
    void notaCredito_incluyeRucProveedor() {
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"001", "Aceite 5W30", "1", "26.96", "0.00", "26.96"}
        );
        String xml = XmlNotaCreditoBuilder.construirNotaCredito(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal", "Av. Principal",
                "", "SI", "04", "Cliente Prueba", "1790000000002",
                "01", "001-001-000000001", "12/08/2026", "12/08/2026",
                "26.96", "4.04", "31.00", "Devolución", detalles);
        assertEquals(ESPERADO, campoRucProveedor(xml), "Nota de crédito debe incluir RUC Proveedor");
    }

    @Test
    void notaDebito_incluyeRucProveedor() {
        List<Object[]> motivos = List.<Object[]>of(new Object[]{"Interés por mora", "4.04"});
        String xml = XmlNotaDebitoBuilder.construirNotaDebito(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal", "Av. Principal",
                "", "SI", "04", "Cliente Prueba", "1790000000002",
                "01", "001-001-000000001", "12/08/2026", "12/08/2026",
                "26.96", "4.04", "31.00", "Interés", "01", motivos);
        assertEquals(ESPERADO, campoRucProveedor(xml), "Nota de débito debe incluir RUC Proveedor");
    }

    @Test
    void guiaRemision_incluyeRucProveedor() {
        List<Object[]> detalles = List.<Object[]>of(new Object[]{"001", "Aceite 5W30", "2"});
        List<Object[]> destinatarios = List.<Object[]>of(new Object[]{
            "1790000000002", "Cliente Prueba", "Quito", "Venta",
            "01", "001-001-000000001", "1234567890", "12/08/2026", detalles});
        String xml = XmlGuiaRemisionBuilder.construirGuiaRemision(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal", "Av. Principal",
                "Av. Partida", "Transporte SA", "04", "1790000000003", "ABC-1234",
                "12/08/2026", "13/08/2026", destinatarios);
        assertEquals(ESPERADO, campoRucProveedor(xml), "Guía de remisión debe incluir RUC Proveedor");
    }

    @Test
    void retencion_incluyeRucProveedor() {
        List<Object[]> rets = List.<Object[]>of(new Object[]{"1", "312", "100.00", "1.00", "1.00"});
        List<Object[]> docs = List.<Object[]>of(new Object[]{
            "01", "01", "001-001-000000001", "12/08/2026", "100.00", "1234567890", rets});
        String xml = XmlRetencionBuilder.construirRetencion(
                "PRUEBAS", CLAVE, "1790000000001", "TAG REPUESTOS",
                "001", "001", 1, "Av. Principal", "Av. Principal",
                "08/2026", "04", "Proveedor Prueba", "1790000000002", docs);
        assertEquals(ESPERADO, campoRucProveedor(xml), "Retención debe incluir RUC Proveedor");
    }

    @Test
    void constante_noEsPlaceholder() {
        assertNotEquals("1799999999001", ESPERADO, "El RUC proveedor no debe ser el placeholder");
        assertTrue(ESPERADO.matches("\\d{13}"), "El RUC proveedor debe tener 13 dígitos");
    }

    private String campoRucProveedor(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            NodeList infos = doc.getElementsByTagName("infoAdicional");
            assertTrue(infos.getLength() > 0, "Debe existir bloque infoAdicional");
            Element info = (Element) infos.item(0);
            NodeList campos = info.getElementsByTagName("campoAdicional");
            for (int i = 0; i < campos.getLength(); i++) {
                Element campo = (Element) campos.item(i);
                if (XmlInfoAdicional.NOMBRE_CAMPO_RUC_PROVEEDOR.equals(campo.getAttribute("nombre"))) {
                    return campo.getTextContent();
                }
            }
            fail("infoAdicional no contiene campoAdicional nombre=\"RUC Proveedor\"");
            return null;
        } catch (Exception e) {
            throw new RuntimeException("XML no parseable: " + e.getMessage(), e);
        }
    }
}

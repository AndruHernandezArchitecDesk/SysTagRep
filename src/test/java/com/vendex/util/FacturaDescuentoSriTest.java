package com.vendex.util;

import com.vendex.model.FacturaDetalle;
import com.vendex.service.FacturaService;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas del validador SRI para facturas CON descuento, CONVENCION C:
 *  el descuento vive SOLO en cabecera (totalDescuento + descuentoAdicional);
 *  las lineas van en BRUTO (descuento 0.00, neto = base sin IVA).
 *  R1: sub == S netos; R2: desc == S lineas + adic; linea: cant*pu-desc == neto;
 *  iva == base*15%; T: importe == sub - adic + iva.
 * Ejemplo usuario: total 350 - descuento 200 = 150 (IVA sobre base bruta).
 */
class FacturaDescuentoSriTest {

    /** Invoca FacturaService.armarDetalles (package-visible static) por reflexion. */
    @SuppressWarnings("unchecked")
    private static List<Object[]> armar(List<FacturaDetalle> items) throws Exception {
        Method m = FacturaService.class.getDeclaredMethod("armarDetalles", List.class);
        m.setAccessible(true);
        return (List<Object[]>) m.invoke(null, items);
    }

    @Test
    void armarDetalles_lineasEnBruto_descuentoCero() throws Exception {
        // pu con IVA incluido 72.45 => sin IVA 63.00; linea en bruto, sin reparto
        FacturaDetalle d = new FacturaDetalle(1, "4PK1210", "TEST12", 1, new BigDecimal("72.45"));
        List<Object[]> filas = armar(List.of(d));

        assertEquals(1, filas.size());
        Object[] f = filas.get(0);
        assertEquals("63.00", f[3], "precioUnitario sin IVA");
        assertEquals("0.00", f[4], "descuento de linea siempre 0.00 (convencion C)");
        assertEquals("63.00", f[5], "precioTotalSinImpuesto = base bruta");

        BigDecimal cant = new BigDecimal((String) f[2]);
        BigDecimal calc = cant.multiply(new BigDecimal((String) f[3]))
                .subtract(new BigDecimal((String) f[4])).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, calc.compareTo(new BigDecimal((String) f[5])));
    }

    @Test
    void armarDetalles_multiLinea_netosIgualanBases() throws Exception {
        FacturaDetalle a = new FacturaDetalle(1, "4PK1210", "TEST12", 1, new BigDecimal("72.45")); // base 63.00
        FacturaDetalle b = new FacturaDetalle(2, "X2", "OTRO", 2, new BigDecimal("11.50"));       // pu sin iva 10.00 x2
        List<Object[]> filas = armar(List.of(a, b));

        BigDecimal sumaDesc = BigDecimal.ZERO;
        BigDecimal sumaNeto = BigDecimal.ZERO;
        for (Object[] f : filas) {
            BigDecimal desc = new BigDecimal((String) f[4]);
            BigDecimal tot = new BigDecimal((String) f[5]);
            BigDecimal cant = new BigDecimal((String) f[2]);
            BigDecimal pu = new BigDecimal((String) f[3]);
            assertEquals(0, cant.multiply(pu).subtract(desc).setScale(2, RoundingMode.HALF_UP).compareTo(tot),
                    "Cada linea debe cumplir cant*pu-desc==total (regla SRI linea)");
            sumaDesc = sumaDesc.add(desc);
            sumaNeto = sumaNeto.add(tot);
        }
        assertEquals(0, sumaDesc.compareTo(BigDecimal.ZERO), "Sin reparto: suma lineas 0.00");
        assertEquals(0, sumaNeto.compareTo(new BigDecimal("83.00")), "63.00 + 20.00 brutos");
    }

    @Test
    void xml_convencionC_casoReal1643() throws Exception {
        // TAPA RADIADOR 2x56.35 (bases 98.00), desc cabecera 70.00:
        // sub 98.00, desc/adic 70.00, base 98.00, iva 4.20, total 32.20.
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"4PK1210", "TAPA RADIADOR TOYOTA PRIUS/HILUX", "2", "49.00", "0.00", "98.00"}
        );
        String xml = XmlSriBuilder.construirFactura("PRUEBAS", "1310202601171685454000110011000000016438804530017",
                "1716854540001", "TAG Repuestos Automotrices", "001", "100", 1643, "Av. De los Pinos y Av. 6 de Diciembre",
                "", "NO", "07", "Consumidor final", "9999999999999", "NA",
                "98.00", "70.00", "32.20", "0.00", "01", "13/10/2026", detalles);

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals(0, num(doc, "totalSinImpuestos").compareTo(new BigDecimal("98.00")));
        assertEquals(0, num(doc, "totalDescuento").compareTo(new BigDecimal("70.00")));

        NodeList adic = doc.getElementsByTagName("descuentoAdicional");
        assertTrue(adic.getLength() > 0, "Falta <descuentoAdicional> en totalImpuesto");
        assertEquals(0, new BigDecimal(adic.item(0).getTextContent().trim())
                .compareTo(new BigDecimal("70.00")), "descuentoAdicional espeja cabecera");

        assertEquals(0, num(doc, "baseImponible").compareTo(new BigDecimal("98.00")),
                "base = suma de netos en bruto");
        assertEquals(0, num(doc, "baseImponible")
                .subtract(new BigDecimal(adic.item(0).getTextContent().trim()))
                .multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP)
                .compareTo(num(doc, "valor")),
                "Regla SRI ImpuestoTotalizado: valor = 15% * (base - adicional)");

        // Formulas SRI con el elemento presente:
        // R2: desc == lineas + adic  =>  70.00 == 0.00 + 70.00
        // T:  importe == sub - adic + imp  =>  98.00 - 70.00 + 4.20 == 32.20
        BigDecimal calcR2 = new BigDecimal("0.00")
                .add(new BigDecimal(adic.item(0).getTextContent().trim()));
        assertEquals(0, calcR2.compareTo(num(doc, "totalDescuento")), "R2 SRI");
        BigDecimal calcT = num(doc, "totalSinImpuestos")
                .subtract(new BigDecimal(adic.item(0).getTextContent().trim()))
                .add(num(doc, "valor"));
        assertEquals(0, calcT.compareTo(num(doc, "importeTotal")), "T SRI");
    }

    @Test
    void xml_ejemploUsuario_350menos200igual150() throws Exception {
        // bases 350/1.15 = 304.35; desc/adic 200; iva = 15% * 104.35 = 15.65; total 120.00
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"X", "ITEM", "1", "304.35", "0.00", "304.35"}
        );
        String xml = XmlSriBuilder.construirFactura("PRUEBAS", "1310202601171685454000110011000000016438804530017",
                "1716854540001", "TAG Repuestos Automotrices", "001", "100", 1644, "Av. Principal",
                "", "NO", "04", "Cliente", "1790000000002", "Quito",
                "304.35", "200.00", "120.00", "0.00", "01", "13/10/2026", detalles);

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        BigDecimal calcT = num(doc, "totalSinImpuestos")
                .subtract(num(doc, "totalDescuento"))
                .add(num(doc, "valor"));
        assertEquals(0, calcT.compareTo(new BigDecimal("120.00")));
        assertEquals(0, calcT.compareTo(num(doc, "importeTotal")));
    }

    private static BigDecimal num(Document doc, String tag) {
        NodeList n = doc.getElementsByTagName(tag);
        assertTrue(n.getLength() > 0, "Falta elemento <" + tag + "> en el XML");
        return new BigDecimal(n.item(0).getTextContent().trim()).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validar(List<Object[]> filas, String sub, String descH, String adic,
                                String iva, String tot) throws Exception {
        Method m = FacturaService.class.getDeclaredMethod("validarConsistenciaSri",
                List.class, BigDecimal.class, BigDecimal.class, BigDecimal.class,
                BigDecimal.class, BigDecimal.class);
        m.setAccessible(true);
        try {
            m.invoke(null, filas, new BigDecimal(sub), new BigDecimal(descH), new BigDecimal(adic),
                    new BigDecimal(iva), new BigDecimal(tot));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void prevalidacion_casoCorrectoC_pasa() throws Exception {
        // Lineas en bruto + header C: sub 63.00, desc/adic 40.00, iva 3.45, total 26.45
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "0.00", "63.00"});
        assertDoesNotThrow(() -> validar(filas, "63.00", "40.00", "40.00", "3.45", "26.45"));
    }

    @Test
    void prevalidacion_lineaConDescuentoSinAdicional_falla() {
        // Convencion vieja (reparto en lineas, sub bruto): R1 exige sub == suma netos
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "40.00", "23.00"});
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> validar(filas, "72.45", "40.00", "0.00", "3.45", "35.90"));
        assertTrue(e.getMessage().contains("R1"));
    }

    @Test
    void prevalidacion_headerDescuentoCeroConAdicional_falla() {
        // adic sin respaldo en header: R2 exige igualdad exacta
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "0.00", "63.00"});
        assertThrows(IllegalStateException.class,
                () -> validar(filas, "63.00", "0.00", "40.00", "9.45", "32.45"));
    }

    @Test
    void prevalidacion_ivaSobreBaseBruta_falla() {
        // IVA calculado sobre base bruta ignorando el adicional (bug anterior):
        // 9.45 vs 3.45 = 15% * (63.00 - 40.00) esperado por el SRI
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "0.00", "63.00"});
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> validar(filas, "63.00", "40.00", "40.00", "9.45", "32.45"));
        assertTrue(e.getMessage().contains("ImpuestoTotalizado"));
    }

    @Test
    void xml_valorEsTarifaPorBaseMenosAdicional_caso1646() throws Exception {
        // Factura 1646 real: base 126.00, desc/adic 100.00.
        // Regla SRI ImpuestoTotalizado: valor = 15% * (126.00 - 100.00) = 3.90
        // (antes emitiamos 18.90 = 15% * base y el SRI devolvia DIFERENCIAS).
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"4PK1210", "TEST12", "2", "63.00", "0.00", "126.00"}
        );
        String xml = XmlSriBuilder.construirFactura("PRUEBAS", "1310202601171685454000110011000000016462735730414",
                "1716854540001", "TAG Repuestos Automotrices", "001", "100", 1646, "Av. Principal",
                "", "NO", "07", "Consumidor final", "9999999999999", "NA",
                "126.00", "100.00", "29.90", "0.00", "01", "13/10/2026", detalles);

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals(0, num(doc, "valor").compareTo(new BigDecimal("3.90")),
                "valor debe ser 15% * (base - adicional)");
        BigDecimal calcT = num(doc, "totalSinImpuestos")
                .subtract(num(doc, "descuentoAdicional"))
                .add(num(doc, "valor"));
        assertEquals(0, calcT.compareTo(num(doc, "importeTotal")),
                "T SRI: 126.00 - 100.00 + 3.90 == 29.90");
    }

    @Test
    void prevalidacion_sinDescuento_pasa() throws Exception {
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"X", "Y", "2", "10.00", "0.00", "20.00"});
        assertDoesNotThrow(() -> validar(filas, "20.00", "0.00", "0.00", "3.00", "23.00"));
    }
}

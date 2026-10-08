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
 * Reglas del validador SRI para facturas CON descuento (casos reales prod:
 * linea TEST12 4PK1210 1x63.00 con desc 40.00; base 72.45 vs 23.0).
 * Convencion evidenciada en rechazos reales (sin doble IVA):
 *  1. detalle: cantidad*precioUnitario - descuento == precioTotalSinImpuesto (NETO);
 *  2. header totalDescuento == suma de descuentos de lineas;
 *  3. baseImponible == suma de totales NETOS de lineas;
 *  4. impuesto valor == baseImponible * tarifa (15%);
 *  5. importeTotal == totalSinImpuestos - totalDescuento + impuestos.
 * Los precios de UI traen IVA incluido: IVA se calcula sobre base NETA.
 */
class FacturaDescuentoSriTest {

    /** Invoca FacturaService.armarDetalles (package-visible static) por reflexion. */
    @SuppressWarnings("unchecked")
    private static List<Object[]> armar(List<FacturaDetalle> items, BigDecimal desc) throws Exception {
        Method m = FacturaService.class.getDeclaredMethod("armarDetalles", List.class, BigDecimal.class);
        m.setAccessible(true);
        return (List<Object[]>) m.invoke(null, items, desc);
    }

    @Test
    void armarDetalles_lineaConDescuento_totalNetoYDescuentoDistribuido() throws Exception {
        // pu con IVA incluido 72.45 => sin IVA 63.00; desc 40.00 => neto 23.00
        FacturaDetalle d = new FacturaDetalle(1, "4PK1210", "TEST12", 1, new BigDecimal("72.45"));
        List<Object[]> filas = armar(List.of(d), new BigDecimal("40.00"));

        assertEquals(1, filas.size());
        Object[] f = filas.get(0);
        assertEquals("63.00", f[3], "precioUnitario sin IVA");
        assertEquals("40.00", f[4], "descuento de linea distribuido");
        assertEquals("23.00", f[5], "precioTotalSinImpuesto NETO (63-40, regla SRI 1)");

        BigDecimal cant = new BigDecimal((String) f[2]);
        BigDecimal calc = cant.multiply(new BigDecimal((String) f[3]))
                .subtract(new BigDecimal((String) f[4])).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, calc.compareTo(new BigDecimal((String) f[5])));
    }

    @Test
    void armarDetalles_multiLinea_sumaDescuentosIgualHeader() throws Exception {
        FacturaDetalle a = new FacturaDetalle(1, "4PK1210", "TEST12", 1, new BigDecimal("72.45")); // base 63.00
        FacturaDetalle b = new FacturaDetalle(2, "X2", "OTRO", 2, new BigDecimal("11.50"));       // pu sin iva 10.00 x2
        List<Object[]> filas = armar(List.of(a, b), new BigDecimal("40.00"));

        BigDecimal sumaDesc = BigDecimal.ZERO;
        BigDecimal sumaNeto = BigDecimal.ZERO;
        for (Object[] f : filas) {
            BigDecimal desc = new BigDecimal((String) f[4]);
            BigDecimal tot = new BigDecimal((String) f[5]);
            BigDecimal cant = new BigDecimal((String) f[2]);
            BigDecimal pu = new BigDecimal((String) f[3]);
            assertEquals(0, cant.multiply(pu).subtract(desc).setScale(2, RoundingMode.HALF_UP).compareTo(tot),
                    "Cada linea debe cumplir cant*pu-desc==total (regla SRI 1)");
            sumaDesc = sumaDesc.add(desc);
            sumaNeto = sumaNeto.add(tot);
        }
        assertEquals(0, sumaDesc.compareTo(new BigDecimal("40.00")),
                "Suma de descuentos de lineas debe igualar totalDescuento del header (regla SRI 2)");
        assertTrue(sumaNeto.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void xml_baseNetaIvaNetoYTotalCuadran() throws Exception {
        // Caso real prod en convencion corregida (sin doble IVA):
        // linea (63.00, desc 40.00, total 23.00); header subtotal 72.45 (bruto app),
        // desc 40.00, base 23.00, iva 3.45 (15% de 23), total 72.45-40+3.45=35.90.
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"4PK1210", "TEST12", "1", "63.00", "40.00", "23.00"}
        );
        String xml = XmlSriBuilder.construirFactura("PRUEBAS", "0810202601171685454000110011000000015264450584618",
                "1790000000001", "TAG REPUESTOS", "001", "100", 1526, "Av. Principal",
                "", "NO", "04", "Cliente", "1790000000002", "Quito",
                "72.45", "40.00", "3.45", "35.90", "0.00", "01", "08/10/2026", detalles);

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        BigDecimal subtotal = num(doc, "totalSinImpuestos");
        BigDecimal descHeader = num(doc, "totalDescuento");
        BigDecimal base = num(doc, "baseImponible");
        BigDecimal iva = num(doc, "valor");
        BigDecimal total = num(doc, "importeTotal");

        assertEquals(0, base.compareTo(new BigDecimal("23.00")),
                "baseImponible debe igualar suma de netos de lineas (regla SRI 3)");
        assertEquals(0, base.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP).compareTo(iva),
                "IVA debe ser base * 15% sin doble cobro (regla SRI 4)");
        assertEquals(0, subtotal.subtract(descHeader).add(iva).compareTo(total),
                "totalSinImpuestos - totalDescuento + impuestos == importeTotal (regla SRI 5)");
        assertEquals(0, total.compareTo(new BigDecimal("35.90")));
    }

    @Test
    void sinDescuento_nadaCambia() throws Exception {
        // Sin descuento neto==bruto: IVA y total identicos al calculo anterior.
        FacturaDetalle d = new FacturaDetalle(1, "X", "Y", 2, new BigDecimal("11.50")); // base 10.00 x2
        List<Object[]> filas = armar(List.of(d), BigDecimal.ZERO);
        assertEquals("0.00", filas.get(0)[4]);
        assertEquals("20.00", filas.get(0)[5]);
    }

    private static BigDecimal num(Document doc, String tag) {
        NodeList n = doc.getElementsByTagName(tag);
        assertTrue(n.getLength() > 0, "Falta elemento <" + tag + "> en el XML");
        return new BigDecimal(n.item(0).getTextContent().trim()).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validar(List<Object[]> filas, String sub, String descH, String iva, String tot) throws Exception {
        Method m = FacturaService.class.getDeclaredMethod("validarConsistenciaSri",
                List.class, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);
        m.setAccessible(true);
        try {
            m.invoke(null, filas, new BigDecimal(sub), new BigDecimal(descH),
                    new BigDecimal(iva), new BigDecimal(tot));
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void prevalidacion_casoCorrecto_pasa() throws Exception {
        // Linea neta + header consistente + IVA neto + total 35.90
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "40.00", "23.00"});
        assertDoesNotThrow(() -> validar(filas, "72.45", "40.00", "3.45", "35.90"));
    }

    @Test
    void prevalidacion_lineaEnBrutoConDescuento_falla() {
        // Caso produccion original: linea 63.00 con desc 40.00 (debió ser 23.00)
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "40.00", "63.00"});
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> validar(filas, "72.45", "40.00", "10.87", "43.32"));
        assertTrue(e.getMessage().contains("23.00") || e.getMessage().contains("difiere"));
    }

    @Test
    void prevalidacion_headerDescuentoCeroConLineasDescuento_falla() {
        // Caso misterio prod: lineas con 40 pero header en 0.00 y total sin restar
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "40.00", "23.00"});
        assertThrows(IllegalStateException.class,
                () -> validar(filas, "72.45", "0.00", "4.87", "77.32"));
    }

    @Test
    void prevalidacion_ivaDoble_falla() {
        // IVA sobre base bruta con lineas netas: 10.87 vs 3.45 esperado
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"4PK1210", "TEST12", "1", "63.00", "40.00", "23.00"});
        assertThrows(IllegalStateException.class,
                () -> validar(filas, "72.45", "40.00", "10.87", "43.32"));
    }

    @Test
    void prevalidacion_sinDescuento_pasa() throws Exception {
        List<Object[]> filas = List.<Object[]>of(
                new Object[]{"X", "Y", "2", "10.00", "0.00", "20.00"});
        assertDoesNotThrow(() -> validar(filas, "20.00", "0.00", "3.00", "23.00"));
    }
}

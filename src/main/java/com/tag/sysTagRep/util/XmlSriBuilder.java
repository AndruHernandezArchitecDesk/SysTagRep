package com.tag.sysTagRep.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Construye el XML de una factura electrónica conforme al esquema del SRI.
 */
public class XmlSriBuilder {

    public static String construirFactura(String ambiente, String claveAcceso, String ruc, String razonSocial,
                                           String codEstablecimiento, String codPuntoEmision,
                                           int secuencial, String dirEstablecimiento,
                                           String contribuyenteEspecial, String obligadoContabilidad,
                                           String tipoIdentificacionComprador,
                                           String razonSocialComprador, String identificacionComprador,
                                           String direccionComprador, String totalSinImpuestos,
                                           String totalDescuento, String totalImpuesto,
                                           String totalConImpuestos, String propina,
                                           String formaPago, String fechaEmision,
                                           List<Object[]> detalles) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Raíz
            Element factura = doc.createElement("factura");
            factura.setAttribute("id", "comprobante");
            factura.setAttribute("version", "1.0.0");
            doc.appendChild(factura);

            // infoTributaria
            Element infoTributaria = doc.createElement("infoTributaria");
            agregarElemento(infoTributaria, "ambiente", "PRODUCCION".equalsIgnoreCase(ambiente) ? "2" : "1"); // 1=PRUEBAS, 2=PRODUCCION
            agregarElemento(infoTributaria, "tipoEmision", "1");
            agregarElemento(infoTributaria, "razonSocial", razonSocial);
            agregarElemento(infoTributaria, "ruc", ruc);
            agregarElemento(infoTributaria, "claveAcceso", claveAcceso);
            agregarElemento(infoTributaria, "codDoc", "01");
            agregarElemento(infoTributaria, "estab", codEstablecimiento);
            agregarElemento(infoTributaria, "ptoEmi", codPuntoEmision);
            agregarElemento(infoTributaria, "secuencial", String.format("%09d", secuencial));
            agregarElemento(infoTributaria, "dirMatriz", dirEstablecimiento);
            if (esNumeroValido(contribuyenteEspecial, 8)) {
                agregarElemento(infoTributaria, "agenteRetencion", contribuyenteEspecial);
            }
            factura.appendChild(infoTributaria);

            // infoFactura
            Element infoFactura = doc.createElement("infoFactura");
            agregarElemento(infoFactura, "fechaEmision", fechaEmision);
            agregarElemento(infoFactura, "dirEstablecimiento", dirEstablecimiento);
            if (contribuyenteEspecial != null && contribuyenteEspecial.trim().length() >= 3) {
                agregarElemento(infoFactura, "contribuyenteEspecial", contribuyenteEspecial.trim());
            }
            agregarElemento(infoFactura, "obligadoContabilidad", obligadoContabilidad != null ? obligadoContabilidad : "SI");
            agregarElemento(infoFactura, "tipoIdentificacionComprador", tipoIdentificacionComprador);
            agregarElemento(infoFactura, "razonSocialComprador", razonSocialComprador);
            agregarElemento(infoFactura, "identificacionComprador", identificacionComprador);
            if (direccionComprador != null && !direccionComprador.isEmpty()) {
                agregarElemento(infoFactura, "direccionComprador", direccionComprador);
            }
            agregarElemento(infoFactura, "totalSinImpuestos", totalSinImpuestos);
            agregarElemento(infoFactura, "totalDescuento", totalDescuento);

            // totalConImpuestos
            Element totalConImpuestosEl = doc.createElement("totalConImpuestos");
            Element impuesto = doc.createElement("totalImpuesto");
            agregarElemento(impuesto, "codigo", "2"); // 2=IVA
            agregarElemento(impuesto, "codigoPorcentaje", "4"); // 4=IVA 15%
            agregarElemento(impuesto, "baseImponible", totalSinImpuestos);
            agregarElemento(impuesto, "valor", totalImpuesto);
            totalConImpuestosEl.appendChild(impuesto);
            infoFactura.appendChild(totalConImpuestosEl);

            agregarElemento(infoFactura, "propina", propina != null ? propina : "0.00");
            agregarElemento(infoFactura, "importeTotal", totalConImpuestos);

            // pagos
            Element pagosEl = doc.createElement("pagos");
            Element pagoEl = doc.createElement("pago");
            agregarElemento(pagoEl, "formaPago", mapearFormaPago(formaPago));
            agregarElemento(pagoEl, "total", totalConImpuestos);
            agregarElemento(pagoEl, "plazo", "0");
            agregarElemento(pagoEl, "unidadTiempo", "DIAS");
            pagosEl.appendChild(pagoEl);
            infoFactura.appendChild(pagosEl);

            factura.appendChild(infoFactura);

            // detalles
            Element detallesEl = doc.createElement("detalles");
            for (Object[] det : detalles) {
                Element detEl = doc.createElement("detalle");
                agregarElemento(detEl, "codigoPrincipal", (String) det[0]);
                agregarElemento(detEl, "descripcion", (String) det[1]);
                agregarElemento(detEl, "cantidad", String.valueOf(det[2]));
                agregarElemento(detEl, "precioUnitario", (String) det[3]);
                agregarElemento(detEl, "descuento", det[4] != null ? (String) det[4] : "0.00");
                agregarElemento(detEl, "precioTotalSinImpuesto", (String) det[5]);

                Element detImpuestos = doc.createElement("impuestos");
                Element detImp = doc.createElement("impuesto");
                agregarElemento(detImp, "codigo", "2");
                agregarElemento(detImp, "codigoPorcentaje", "4"); // 4=IVA 15%
                agregarElemento(detImp, "tarifa", "15");
                agregarElemento(detImp, "baseImponible", (String) det[5]);
                BigDecimal valorIva = new BigDecimal((String) det[5]).multiply(new BigDecimal("0.15")).setScale(2, java.math.RoundingMode.HALF_UP);
                agregarElemento(detImp, "valor", valorIva.toString());
                detImpuestos.appendChild(detImp);
                detEl.appendChild(detImpuestos);

                detallesEl.appendChild(detEl);
            }
            factura.appendChild(detallesEl);

            // infoAdicional
            Element infoAdicional = doc.createElement("infoAdicional");
            if (direccionComprador != null && !direccionComprador.isEmpty()) {
                Element campoAdic = doc.createElement("campoAdicional");
                campoAdic.setAttribute("nombre", "Direccion");
                campoAdic.setTextContent(direccionComprador);
                infoAdicional.appendChild(campoAdic);
            }
            factura.appendChild(infoAdicional);

            // Transform to String
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void agregarElemento(Element padre, String nombre, String valor) {
        Element el = padre.getOwnerDocument().createElement(nombre);
        el.setTextContent(valor != null ? valor : "");
        padre.appendChild(el);
    }

    private static boolean esNumeroValido(String valor, int maxLength) {
        if (valor == null || valor.trim().isEmpty()) {
            return false;
        }
        return valor.trim().matches("[0-9]+") && valor.trim().length() <= maxLength;
    }

    private static String mapearFormaPago(String formaPago) {
        if (formaPago == null) return "01";
        switch (formaPago) {
            case "Efectivo": return "01";
            case "Tarjeta de Crédito": return "02";
            case "Tarjeta de Débito": return "03";
            case "Transferencia": return "20";
            case "Depósito": return "20";
            case "Cheque": return "04";
            default: return "01";
        }
    }
}

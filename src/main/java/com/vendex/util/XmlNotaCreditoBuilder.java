package com.vendex.util;

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
import java.math.RoundingMode;
import java.util.List;

/**
 * Construye XML notaCredito v1.1.0 SRI (tipo 04).
 */
public class XmlNotaCreditoBuilder {

    public static String construirNotaCredito(String ambiente, String claveAcceso, String ruc, String razonSocial,
                                              String codEstablecimiento, String codPuntoEmision, int secuencial,
                                              String dirMatriz, String dirEstablecimiento,
                                              String contribuyenteEspecial, String obligadoContabilidad,
                                              String tipoIdentificacionComprador, String razonSocialComprador,
                                              String identificacionComprador,
                                              String codDocModificado, String numDocModificado,
                                              String fechaEmisionDocSustento,
                                              String fechaEmision,
                                              String totalSinImpuestos, String valorModificacion,
                                              String totalImpuesto,
                                              String motivo,
                                              List<Object[]> detalles) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element notaCredito = doc.createElement("notaCredito");
            notaCredito.setAttribute("id", "comprobante");
            notaCredito.setAttribute("version", "1.1.0");
            doc.appendChild(notaCredito);

            Element infoTributaria = doc.createElement("infoTributaria");
            agregar(infoTributaria, "ambiente", "PRODUCCION".equalsIgnoreCase(ambiente) ? "2" : "1");
            agregar(infoTributaria, "tipoEmision", "1");
            agregar(infoTributaria, "razonSocial", razonSocial);
            agregar(infoTributaria, "nombreComercial", razonSocial);
            agregar(infoTributaria, "ruc", ruc);
            agregar(infoTributaria, "claveAcceso", claveAcceso);
            agregar(infoTributaria, "codDoc", AppConstants.TIPO_COMPROBANTE_NOTA_CREDITO);
            agregar(infoTributaria, "estab", codEstablecimiento);
            agregar(infoTributaria, "ptoEmi", codPuntoEmision);
            agregar(infoTributaria, "secuencial", String.format("%09d", secuencial));
            agregar(infoTributaria, "dirMatriz", dirMatriz);
            notaCredito.appendChild(infoTributaria);

            Element infoNC = doc.createElement("infoNotaCredito");
            agregar(infoNC, "fechaEmision", fechaEmision);
            agregar(infoNC, "dirEstablecimiento", dirEstablecimiento != null ? dirEstablecimiento : dirMatriz);
            agregar(infoNC, "tipoIdentificacionComprador", tipoIdentificacionComprador);
            agregar(infoNC, "razonSocialComprador", razonSocialComprador);
            agregar(infoNC, "identificacionComprador", identificacionComprador);
            if (contribuyenteEspecial != null && contribuyenteEspecial.trim().length() >= 3) {
                agregar(infoNC, "contribuyenteEspecial", contribuyenteEspecial.trim());
            }
            agregar(infoNC, "obligadoContabilidad", obligadoContabilidad != null ? obligadoContabilidad : "NO");
            agregar(infoNC, "codDocModificado", codDocModificado != null ? codDocModificado : AppConstants.COD_DOC_MODIFICADO_FACTURA);
            agregar(infoNC, "numDocModificado", numDocModificado);
            agregar(infoNC, "fechaEmisionDocSustento", fechaEmisionDocSustento);
            agregar(infoNC, "totalSinImpuestos", totalSinImpuestos);
            agregar(infoNC, "valorModificacion", valorModificacion);
            agregar(infoNC, "moneda", "DOLAR");
            Element totalConImpuestos = doc.createElement("totalConImpuestos");
            Element ti = doc.createElement("totalImpuesto");
            agregar(ti, "codigo", "2");
            agregar(ti, "codigoPorcentaje", "4");
            agregar(ti, "baseImponible", totalSinImpuestos);
            agregar(ti, "valor", totalImpuesto);
            totalConImpuestos.appendChild(ti);
            infoNC.appendChild(totalConImpuestos);
            agregar(infoNC, "motivo", motivo);
            notaCredito.appendChild(infoNC);

            Element detallesEl = doc.createElement("detalles");
            for (Object[] det : detalles) {
                Element detEl = doc.createElement("detalle");
                agregar(detEl, "codigoInterno", (String) det[0]);
                // codigoAdicional opcional omitted
                agregar(detEl, "descripcion", (String) det[1]);
                agregar(detEl, "cantidad", String.valueOf(det[2]));
                agregar(detEl, "precioUnitario", (String) det[3]);
                agregar(detEl, "descuento", det[4] != null ? (String) det[4] : "0.00");
                agregar(detEl, "precioTotalSinImpuesto", (String) det[5]);
                Element detImpuestos = doc.createElement("impuestos");
                Element detImp = doc.createElement("impuesto");
                agregar(detImp, "codigo", "2");
                agregar(detImp, "codigoPorcentaje", "4");
                agregar(detImp, "tarifa", "15");
                agregar(detImp, "baseImponible", (String) det[5]);
                BigDecimal valorIva = new BigDecimal((String) det[5]).multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
                agregar(detImp, "valor", valorIva.toPlainString());
                detImpuestos.appendChild(detImp);
                detEl.appendChild(detImpuestos);
                detallesEl.appendChild(detEl);
            }
            notaCredito.appendChild(detallesEl);

            // infoAdicional vacia o con motivo
            Element infoAdicional = doc.createElement("infoAdicional");
            if (motivo != null && !motivo.isEmpty()) {
                Element campo = doc.createElement("campoAdicional");
                campo.setAttribute("nombre", "Motivo");
                campo.setTextContent(motivo.length() > 300 ? motivo.substring(0,300) : motivo);
                infoAdicional.appendChild(campo);
            }
            notaCredito.appendChild(infoAdicional);

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

    private static void agregar(Element padre, String nombre, String valor) {
        Element el = padre.getOwnerDocument().createElement(nombre);
        el.setTextContent(valor != null ? valor : "");
        padre.appendChild(el);
    }
}

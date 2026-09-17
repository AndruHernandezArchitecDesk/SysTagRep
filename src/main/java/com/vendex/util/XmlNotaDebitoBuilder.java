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

public class XmlNotaDebitoBuilder {

    public static String construirNotaDebito(String ambiente, String claveAcceso, String ruc, String razonSocial,
                                               String codEstablecimiento, String codPuntoEmision, int secuencial,
                                               String dirMatriz, String dirEstablecimiento,
                                               String contribuyenteEspecial, String obligadoContabilidad,
                                               String tipoIdentificacionComprador, String razonSocialComprador,
                                               String identificacionComprador,
                                               String codDocModificado, String numDocModificado,
                                               String fechaEmisionDocSustento,
                                               String fechaEmision,
                                               String totalSinImpuestos, String valorIva, String valorTotal,
                                               String motivoStr,
                                               String formaPago,
                                               List<Object[]> motivos) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element notaDebito = doc.createElement("notaDebito");
            notaDebito.setAttribute("id", "comprobante");
            notaDebito.setAttribute("version", "1.0.0");
            doc.appendChild(notaDebito);

            Element infoTributaria = doc.createElement("infoTributaria");
            agregar(infoTributaria, "ambiente", "PRODUCCION".equalsIgnoreCase(ambiente) ? "2" : "1");
            agregar(infoTributaria, "tipoEmision", "1");
            agregar(infoTributaria, "razonSocial", razonSocial);
            agregar(infoTributaria, "nombreComercial", razonSocial);
            agregar(infoTributaria, "ruc", ruc);
            agregar(infoTributaria, "claveAcceso", claveAcceso);
            agregar(infoTributaria, "codDoc", AppConstants.TIPO_COMPROBANTE_NOTA_DEBITO);
            agregar(infoTributaria, "estab", codEstablecimiento);
            agregar(infoTributaria, "ptoEmi", codPuntoEmision);
            agregar(infoTributaria, "secuencial", String.format("%09d", secuencial));
            agregar(infoTributaria, "dirMatriz", dirMatriz);
            notaDebito.appendChild(infoTributaria);

            Element infoND = doc.createElement("infoNotaDebito");
            agregar(infoND, "fechaEmision", fechaEmision);
            agregar(infoND, "dirEstablecimiento", dirEstablecimiento != null ? dirEstablecimiento : dirMatriz);
            agregar(infoND, "tipoIdentificacionComprador", tipoIdentificacionComprador);
            agregar(infoND, "razonSocialComprador", razonSocialComprador);
            agregar(infoND, "identificacionComprador", identificacionComprador);
            if (contribuyenteEspecial != null && contribuyenteEspecial.trim().length() >= 3) {
                agregar(infoND, "contribuyenteEspecial", contribuyenteEspecial.trim());
            }
            agregar(infoND, "obligadoContabilidad", obligadoContabilidad != null ? obligadoContabilidad : "NO");
            agregar(infoND, "codDocModificado", codDocModificado != null ? codDocModificado : AppConstants.COD_DOC_MODIFICADO_FACTURA);
            agregar(infoND, "numDocModificado", numDocModificado);
            agregar(infoND, "fechaEmisionDocSustento", fechaEmisionDocSustento);
            agregar(infoND, "totalSinImpuestos", totalSinImpuestos);
            Element impuestos = doc.createElement("impuestos");
            if (valorIva != null && new BigDecimal(valorIva).compareTo(BigDecimal.ZERO) > 0) {
                Element imp = doc.createElement("impuesto");
                agregar(imp, "codigo", "2");
                agregar(imp, "codigoPorcentaje", "4");
                agregar(imp, "tarifa", "15");
                agregar(imp, "baseImponible", totalSinImpuestos);
                agregar(imp, "valor", valorIva);
                impuestos.appendChild(imp);
            }
            infoND.appendChild(impuestos);
            agregar(infoND, "valorTotal", valorTotal);
            if (formaPago != null && !formaPago.isEmpty()) {
                Element pagos = doc.createElement("pagos");
                Element pago = doc.createElement("pago");
                agregar(pago, "formaPago", formaPago);
                agregar(pago, "total", valorTotal);
                pagos.appendChild(pago);
                infoND.appendChild(pagos);
            }
            notaDebito.appendChild(infoND);

            Element motivosEl = doc.createElement("motivos");
            for (Object[] mot : motivos) {
                Element motEl = doc.createElement("motivo");
                agregar(motEl, "razon", (String) mot[0]);
                agregar(motEl, "valor", (String) mot[1]);
                motivosEl.appendChild(motEl);
            }
            notaDebito.appendChild(motivosEl);

            Element infoAdicional = doc.createElement("infoAdicional");
            if (motivoStr != null && !motivoStr.isEmpty()) {
                Element campo = doc.createElement("campoAdicional");
                campo.setAttribute("nombre", "Motivos");
                campo.setTextContent(motivoStr.length() > 300 ? motivoStr.substring(0,300) : motivoStr);
                infoAdicional.appendChild(campo);
            }
            {
                Element campoProv = doc.createElement("campoAdicional");
                campoProv.setAttribute("nombre", "RUC Proveedor");
                campoProv.setTextContent(AppConstants.RUC_PROVEEDOR_SISTEMA);
                infoAdicional.appendChild(campoProv);
            }
            notaDebito.appendChild(infoAdicional);

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

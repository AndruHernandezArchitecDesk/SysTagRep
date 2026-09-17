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
import java.util.List;

public class XmlGuiaRemisionBuilder {

    public static String construirGuiaRemision(String ambiente, String claveAcceso, String ruc, String razonSocial,
                                               String codEstablecimiento, String codPuntoEmision, int secuencial,
                                               String dirMatriz, String dirEstablecimiento,
                                               String dirPartida,
                                               String razonSocialTransportista, String tipoIdentificacionTransportista,
                                               String rucTransportista, String placa,
                                               String fechaIniTransporte, String fechaFinTransporte,
                                               List<Object[]> destinatarios) {
        // destinatarios: cada Object[] = {identificacion, razonSocial, direccion, motivoTraslado, codDocSustento, numDocSustento, numAutDocSustento, fechaEmisionDocSustento, List<Object[]> detalles}
        // detalle: cada Object[] = {codigoInterno, descripcion, cantidad}
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element guia = doc.createElement("guiaRemision");
            guia.setAttribute("id", "comprobante");
            guia.setAttribute("version", "1.1.0");
            doc.appendChild(guia);

            Element infoTributaria = doc.createElement("infoTributaria");
            agregar(infoTributaria, "ambiente", "PRODUCCION".equalsIgnoreCase(ambiente) ? "2" : "1");
            agregar(infoTributaria, "tipoEmision", "1");
            agregar(infoTributaria, "razonSocial", razonSocial);
            agregar(infoTributaria, "nombreComercial", razonSocial);
            agregar(infoTributaria, "ruc", ruc);
            agregar(infoTributaria, "claveAcceso", claveAcceso);
            agregar(infoTributaria, "codDoc", AppConstants.TIPO_COMPROBANTE_GUIA_REMISION);
            agregar(infoTributaria, "estab", codEstablecimiento);
            agregar(infoTributaria, "ptoEmi", codPuntoEmision);
            agregar(infoTributaria, "secuencial", String.format("%09d", secuencial));
            agregar(infoTributaria, "dirMatriz", dirMatriz);
            guia.appendChild(infoTributaria);

            Element infoGR = doc.createElement("infoGuiaRemision");
            agregar(infoGR, "dirEstablecimiento", dirEstablecimiento != null ? dirEstablecimiento : dirMatriz);
            agregar(infoGR, "dirPartida", dirPartida);
            agregar(infoGR, "razonSocialTransportista", razonSocialTransportista);
            agregar(infoGR, "tipoIdentificacionTransportista", tipoIdentificacionTransportista);
            agregar(infoGR, "rucTransportista", rucTransportista);
            agregar(infoGR, "obligadoContabilidad", "NO");
            agregar(infoGR, "fechaIniTransporte", fechaIniTransporte);
            agregar(infoGR, "fechaFinTransporte", fechaFinTransporte);
            agregar(infoGR, "placa", placa);
            guia.appendChild(infoGR);

            Element destinatariosEl = doc.createElement("destinatarios");
            if (destinatarios != null) {
                for (Object[] dest : destinatarios) {
                    // dest[0]=identificacion, [1]=razonSocial, [2]=direccion, [3]=motivo, [4]=codDocSust, [5]=numDocSust, [6]=numAut, [7]=fechaEmision, [8]=List detalles
                    Element dEl = doc.createElement("destinatario");
                    agregar(dEl, "identificacionDestinatario", (String) dest[0]);
                    agregar(dEl, "razonSocialDestinatario", (String) dest[1]);
                    agregar(dEl, "dirDestinatario", (String) dest[2]);
                    agregar(dEl, "motivoTraslado", (String) dest[3]);
                    String codDocSust = dest[4] != null ? (String) dest[4] : null;
                    if (codDocSust != null && !codDocSust.trim().isEmpty()) {
                        agregar(dEl, "codDocSustento", codDocSust);
                        agregar(dEl, "numDocSustento", (String) dest[5]);
                        agregar(dEl, "numAutDocSustento", (String) dest[6]);
                        agregar(dEl, "fechaEmisionDocSustento", (String) dest[7]);
                    }
                    @SuppressWarnings("unchecked")
                    List<Object[]> detalles = dest.length > 8 ? (List<Object[]>) dest[8] : null;
                    Element detallesEl = doc.createElement("detalles");
                    if (detalles != null) {
                        for (Object[] det : detalles) {
                            Element detEl = doc.createElement("detalle");
                            agregar(detEl, "codigoInterno", (String) det[0]);
                            agregar(detEl, "descripcion", (String) det[1]);
                            agregar(detEl, "cantidad", (String) det[2]);
                            detallesEl.appendChild(detEl);
                        }
                    }
                    dEl.appendChild(detallesEl);
                    destinatariosEl.appendChild(dEl);
                }
            }
            guia.appendChild(destinatariosEl);

            Element infoAdicional = doc.createElement("infoAdicional");
            {
                Element campoProv = doc.createElement("campoAdicional");
                campoProv.setAttribute("nombre", "RUC Proveedor");
                campoProv.setTextContent(AppConstants.RUC_PROVEEDOR_SISTEMA);
                infoAdicional.appendChild(campoProv);
            }
            guia.appendChild(infoAdicional);

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

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

public class XmlRetencionBuilder {

    // docs: cada Object[] = {codSustento, codDocSustento, numDocSustento, fechaEmisionDocSustento, totalSinImpuestos, numAutDocSustento, List<Object[]> retenciones}
    // retencion: cada Object[] = {codigo, codigoRetencion, baseImponible, porcentajeRetener, valorRetenido}
    public static String construirRetencion(String ambiente, String claveAcceso, String ruc, String razonSocial,
                                            String codEstablecimiento, String codPuntoEmision, int secuencial,
                                            String dirMatriz, String dirEstablecimiento,
                                            String periodoFiscal,
                                            String tipoIdentificacionSujeto, String razonSocialSujeto, String identificacionSujeto,
                                            List<Object[]> docsSustento) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element compRet = doc.createElement("comprobanteRetencion");
            compRet.setAttribute("id", "comprobante");
            compRet.setAttribute("version", "2.0.0");
            doc.appendChild(compRet);

            Element infoTributaria = doc.createElement("infoTributaria");
            agregar(infoTributaria, "ambiente", "PRODUCCION".equalsIgnoreCase(ambiente) ? "2" : "1");
            agregar(infoTributaria, "tipoEmision", "1");
            agregar(infoTributaria, "razonSocial", razonSocial);
            agregar(infoTributaria, "nombreComercial", razonSocial);
            agregar(infoTributaria, "ruc", ruc);
            agregar(infoTributaria, "claveAcceso", claveAcceso);
            agregar(infoTributaria, "codDoc", AppConstants.TIPO_COMPROBANTE_RETENCION);
            agregar(infoTributaria, "estab", codEstablecimiento);
            agregar(infoTributaria, "ptoEmi", codPuntoEmision);
            agregar(infoTributaria, "secuencial", String.format("%09d", secuencial));
            agregar(infoTributaria, "dirMatriz", dirMatriz);
            compRet.appendChild(infoTributaria);

            Element infoComp = doc.createElement("infoCompRetencion");
            agregar(infoComp, "fechaEmision", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)));
            agregar(infoComp, "dirEstablecimiento", dirEstablecimiento != null ? dirEstablecimiento : dirMatriz);
            agregar(infoComp, "obligadoContabilidad", "SI");
            agregar(infoComp, "tipoIdentificacionSujetoRetenido", tipoIdentificacionSujeto);
            agregar(infoComp, "parteRel", "NO");
            agregar(infoComp, "tipoSujetoRetenido", "01");
            agregar(infoComp, "razonSocialSujetoRetenido", razonSocialSujeto);
            agregar(infoComp, "identificacionSujetoRetenido", identificacionSujeto);
            agregar(infoComp, "periodoFiscal", periodoFiscal);
            compRet.appendChild(infoComp);

            Element docsEl = doc.createElement("docsSustento");
            if (docsSustento != null) {
                for (Object[] ds : docsSustento) {
                    Element docEl = doc.createElement("docSustento");
                    agregar(docEl, "codSustento", (String) ds[0]);
                    agregar(docEl, "codDocSustento", (String) ds[1]);
                    agregar(docEl, "numDocSustento", (String) ds[2]);
                    agregar(docEl, "fechaEmisionDocSustento", (String) ds[3]);
                    agregar(docEl, "numAutDocSustento", ds[5] != null ? (String) ds[5] : (String) ds[2]);
                    agregar(docEl, "totalSinImpuestos", (String) ds[4]);
                    agregar(docEl, "totalComprobantesReembolso", "0.00");
                    agregar(docEl, "totalConImpuestos", "0.00");
                    agregar(docEl, "importeTotal", (String) ds[4]);
                    @SuppressWarnings("unchecked")
                    List<Object[]> rets = ds.length > 6 ? (List<Object[]>) ds[6] : null;
                    Element retsEl = doc.createElement("retenciones");
                    if (rets != null) {
                        for (Object[] r : rets) {
                            Element retEl = doc.createElement("retencion");
                            agregar(retEl, "codigo", (String) r[0]);
                            agregar(retEl, "codigoRetencion", (String) r[1]);
                            agregar(retEl, "baseImponible", (String) r[2]);
                            agregar(retEl, "porcentajeRetener", (String) r[3]);
                            agregar(retEl, "valorRetenido", (String) r[4]);
                            retsEl.appendChild(retEl);
                        }
                    }
                    docEl.appendChild(retsEl);
                    // pago opcional según XSD 2.0.0 - agregado por compatibilidad
                    Element pagosEl = doc.createElement("pagos");
                    Element pagoEl = doc.createElement("pago");
                    agregar(pagoEl, "formaPago", "01");
                    agregar(pagoEl, "total", (String) ds[4]);
                    pagosEl.appendChild(pagoEl);
                    docEl.appendChild(pagosEl);
                    docsEl.appendChild(docEl);
                }
            }
            compRet.appendChild(docsEl);

            Element infoAdicional = doc.createElement("infoAdicional");
            {
                Element campoProv = doc.createElement("campoAdicional");
                campoProv.setAttribute("nombre", "RUC Proveedor");
                campoProv.setTextContent(AppConstants.RUC_PROVEEDOR_SISTEMA);
                infoAdicional.appendChild(campoProv);
            }
            compRet.appendChild(infoAdicional);

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

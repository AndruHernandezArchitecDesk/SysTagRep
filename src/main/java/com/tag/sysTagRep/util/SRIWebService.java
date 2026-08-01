package com.tag.sysTagRep.util;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Cliente SOAP 1.1 de los Web Services del SRI (recepción y autorización).
 * Recepción: envía el comprobante XML firmado (Base64).
 * Autorización: consulta el estado por clave de acceso.
 */
public class SRIWebService {

    private static final String URL_RECEPCION_PRUEBAS =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";
    private static final String URL_RECEPCION_PRODUCCION =
            "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";
    private static final String URL_AUTORIZACION_PRUEBAS =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";
    private static final String URL_AUTORIZACION_PRODUCCION =
            "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";

    private static final int TIEMPO_ESPERA_MS = 20000;

    private String ambiente;

    public SRIWebService() {
        this.ambiente = "PRUEBAS";
    }

    public SRIWebService(String ambiente) {
        this.ambiente = ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }

    /**
     * Envía el comprobante al SRI y consulta su autorización.
     * @param xmlFirmado XML firmado digitalmente
     * @param claveAcceso clave de acceso del comprobante
     * @return respuesta del SRI (AUTORIZADO / RECHAZADA / DEVUELTA / PENDIENTE / ERROR)
     */
    public SRIResponse validarComprobante(String xmlFirmado, String claveAcceso) {
        SRIResponse recepcion = enviarComprobante(xmlFirmado);
        String respuestaRecepcion = recepcion.getRespuestaRecepcionXml();
        if (!"RECIBIDA".equals(recepcion.getEstado())) {
            return recepcion;
        }

        for (int intento = 1; intento <= 5; intento++) {
            SRIResponse autorizacion = consultarAutorizacion(claveAcceso);
            autorizacion.setRespuestaRecepcionXml(respuestaRecepcion);
            String estado = autorizacion.getEstado();
            if ("AUTORIZADO".equals(estado) || "RECHAZADA".equals(estado) || "DEVUELTA".equals(estado)) {
                return autorizacion;
            }
            try {
                Thread.sleep(1000L * intento);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        SRIResponse pendiente = new SRIResponse();
        pendiente.setEstado("PENDIENTE");
        pendiente.setMensaje("Comprobante recibido por el SRI; autorización aún pendiente.");
        pendiente.setRespuestaRecepcionXml(respuestaRecepcion);
        return pendiente;
    }

    /**
     * Web service de recepción: validarComprobante.
     */
    public SRIResponse enviarComprobante(String xmlFirmado) {
        String url = "PRUEBAS".equals(ambiente) ? URL_RECEPCION_PRUEBAS : URL_RECEPCION_PRODUCCION;
        String xmlBase64 = Base64.getEncoder().encodeToString(
                xmlFirmado.getBytes(StandardCharsets.UTF_8));

        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:rece=\"http://ec.gob.sri.ws.recepcion\">"
                + "<soapenv:Body>"
                + "<rece:validarComprobante>"
                + "<xml>" + xmlBase64 + "</xml>"
                + "</rece:validarComprobante>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        String respuestaXml = postSoap(url, soap);
        if (respuestaXml == null) {
            SRIResponse r = new SRIResponse();
            r.setEstado("ERROR");
            r.setMensaje("No hubo respuesta del SRI (recepción).");
            return r;
        }
        return parsearRecepcion(respuestaXml, "RECIBIDA");
    }

    /**
     * Web service de autorización: autorizacionComprobante.
     */
    public SRIResponse consultarAutorizacion(String claveAcceso) {
        String url = "PRUEBAS".equals(ambiente) ? URL_AUTORIZACION_PRUEBAS : URL_AUTORIZACION_PRODUCCION;

        String soap = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">"
                + "<soapenv:Body>"
                + "<aut:autorizacionComprobante>"
                + "<claveAccesoComprobante>" + claveAcceso + "</claveAccesoComprobante>"
                + "</aut:autorizacionComprobante>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        String respuestaXml = postSoap(url, soap);
        if (respuestaXml == null) {
            SRIResponse r = new SRIResponse();
            r.setEstado("ERROR");
            r.setMensaje("No hubo respuesta del SRI (autorización).");
            return r;
        }
        return parsearAutorizacion(respuestaXml);
    }

    private String postSoap(String url, String soapBody) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIEMPO_ESPERA_MS);
            conn.setReadTimeout(TIEMPO_ESPERA_MS);
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "");
            conn.setRequestProperty("Accept", "text/xml");

            byte[] bytes = soapBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int codigo = conn.getResponseCode();
            byte[] respuesta;
            if (codigo >= 200 && codigo < 300) {
                respuesta = conn.getInputStream().readAllBytes();
            } else {
                respuesta = conn.getErrorStream() != null ? conn.getErrorStream().readAllBytes() : new byte[0];
            }
            return new String(respuesta, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private SRIResponse parsearRecepcion(String respuestaXml, String estadoEsperado) {
        SRIResponse r = new SRIResponse();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(respuestaXml.getBytes(StandardCharsets.UTF_8)));
            r.setEstado(valorPrimero(doc, "estado", estadoEsperado));
            r.setMensaje(concatMensajes(doc));
            r.setNumeroAutorizacion(null);
            r.setFechaAutorizacion(null);
            r.setRespuestaRecepcionXml(respuestaXml);
        } catch (Exception e) {
            r.setEstado("ERROR");
            r.setMensaje("No se pudo interpretar la respuesta del SRI (recepción): " + e.getMessage());
        }
        return r;
    }

    private SRIResponse parsearAutorizacion(String respuestaXml) {
        SRIResponse r = new SRIResponse();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(respuestaXml.getBytes(StandardCharsets.UTF_8)));
            r.setEstado(valorPrimero(doc, "estado", "PENDIENTE"));
            r.setMensaje(concatMensajes(doc));
            r.setNumeroAutorizacion(valorPrimero(doc, "numeroAutorizacion", null));
            r.setFechaAutorizacion(valorPrimero(doc, "fechaAutorizacion", null));
            r.setRespuestaAutorizacionXml(respuestaXml);
        } catch (Exception e) {
            r.setEstado("ERROR");
            r.setMensaje("No se pudo interpretar la respuesta del SRI (autorización): " + e.getMessage());
        }
        return r;
    }

    private String valorPrimero(Document doc, String nombreLocal, String porDefecto) {
        NodeList lista = doc.getElementsByTagNameNS("*", nombreLocal);
        if (lista.getLength() > 0 && lista.item(0).getTextContent() != null) {
            String valor = lista.item(0).getTextContent().trim();
            if (!valor.isEmpty()) return valor;
        }
        return porDefecto;
    }

    private String concatMensajes(Document doc) {
        NodeList lista = doc.getElementsByTagNameNS("*", "mensaje");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.getLength(); i++) {
            Element m = (Element) lista.item(i);
            String texto = textoDeHijo(m, "mensaje");
            String tipo = textoDeHijo(m, "tipo");
            if (texto == null) texto = m.getTextContent();
            if (texto == null || texto.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(tipo != null && !tipo.isEmpty() ? "[" + tipo + "] " : "").append(texto.trim());
        }
        return sb.toString();
    }

    private String textoDeHijo(Element padre, String nombreLocal) {
        NodeList hijos = padre.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node n = hijos.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && nombreLocal.equals(n.getLocalName())) {
                return n.getTextContent();
            }
        }
        return null;
    }

    public static class SRIResponse {
        private String estado;
        private String mensaje;
        private String numeroAutorizacion;
        private String fechaAutorizacion;
        private String respuestaRecepcionXml;
        private String respuestaAutorizacionXml;

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        public String getNumeroAutorizacion() { return numeroAutorizacion; }
        public void setNumeroAutorizacion(String n) { this.numeroAutorizacion = n; }
        public String getFechaAutorizacion() { return fechaAutorizacion; }
        public void setFechaAutorizacion(String f) { this.fechaAutorizacion = f; }
        public String getRespuestaRecepcionXml() { return respuestaRecepcionXml; }
        public void setRespuestaRecepcionXml(String r) { this.respuestaRecepcionXml = r; }
        public String getRespuestaAutorizacionXml() { return respuestaAutorizacionXml; }
        public void setRespuestaAutorizacionXml(String r) { this.respuestaAutorizacionXml = r; }
    }
}

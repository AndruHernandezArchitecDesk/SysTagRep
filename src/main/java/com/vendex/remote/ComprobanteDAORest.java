package com.vendex.remote;

import com.vendex.dao.ComprobanteDAO;

import java.sql.Connection;
import java.util.Map;

public class ComprobanteDAORest implements ComprobanteDAO {
    private final RestClient rest;

    public ComprobanteDAORest(RestClient rest) {
        this.rest = rest;
    }

    @Override
    public int obtenerSecuencial(String tipoComprobante) {
        try {
            Map<String, Object> resp = rest.get("/api/comprobantes/secuencial/" + tipoComprobante, Map.class);
            if (resp != null && resp.get("secuencial") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado) {
        insertar(null, claveAcceso, idRelacionado, numeroComprobante, ambiente, xmlGenerado, "FACTURA");
    }

    @Override
    public void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado, String tipoComprobante) {
        insertar(null, claveAcceso, idRelacionado, numeroComprobante, ambiente, xmlGenerado, tipoComprobante);
    }

    @Override
    public void insertar(Connection con, String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado, String tipoComprobante) {
        try {
            rest.post("/api/comprobantes", Map.of(
                    "claveAcceso", claveAcceso,
                    "idRelacionado", idRelacionado,
                    "numeroComprobante", numeroComprobante,
                    "ambiente", ambiente,
                    "xmlGenerado", xmlGenerado,
                    "tipoComprobante", tipoComprobante
            ), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error insertando comprobante vía REST", e);
        }
    }

    @Override
    public void insertar(Connection con, String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado) {
        insertar(con, claveAcceso, idRelacionado, numeroComprobante, ambiente, xmlGenerado, "FACTURA");
    }

    @Override
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion) {
        actualizarEstado(null, claveAcceso, estado, mensaje, xmlAutorizado, numeroAutorizacion, fechaAutorizacion);
    }

    @Override
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion) {
        try {
            rest.put("/api/comprobantes/estado", Map.of(
                    "claveAcceso", claveAcceso,
                    "estado", estado,
                    "mensaje", mensaje,
                    "xmlAutorizado", xmlAutorizado,
                    "numeroAutorizacion", numeroAutorizacion,
                    "fechaAutorizacion", fechaAutorizacion
            ), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando estado comprobante vía REST", e);
        }
    }

    @Override
    public void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        guardarEnvio(null, claveAcceso, numeroComprobante, ambiente, xmlEnviado, respuestaRecepcion, respuestaAutorizacion, estado, mensaje, numeroAutorizacion, fechaAutorizacion, "FACTURA");
    }

    @Override
    public void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion, String tipoComprobante) {
        guardarEnvio(null, claveAcceso, numeroComprobante, ambiente, xmlEnviado, respuestaRecepcion, respuestaAutorizacion, estado, mensaje, numeroAutorizacion, fechaAutorizacion, tipoComprobante);
    }

    @Override
    public void guardarEnvio(Connection con, String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion, String tipoComprobante) {
        try {
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("claveAcceso", claveAcceso);
            body.put("numeroComprobante", numeroComprobante);
            body.put("ambiente", ambiente);
            body.put("xmlEnviado", xmlEnviado);
            body.put("respuestaRecepcion", respuestaRecepcion);
            body.put("respuestaAutorizacion", respuestaAutorizacion);
            body.put("estado", estado);
            body.put("mensaje", mensaje);
            body.put("numeroAutorizacion", numeroAutorizacion);
            body.put("fechaAutorizacion", fechaAutorizacion);
            body.put("tipoComprobante", tipoComprobante);
            rest.post("/api/comprobantes/envio", body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando envío comprobante vía REST", e);
        }
    }

    @Override
    public void guardarEnvio(Connection con, String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        guardarEnvio(con, claveAcceso, numeroComprobante, ambiente, xmlEnviado, respuestaRecepcion, respuestaAutorizacion, estado, mensaje, numeroAutorizacion, fechaAutorizacion, "FACTURA");
    }

    @Override
    public int consultarSecuencial(String tipoComprobante) {
        throw new UnsupportedOperationException("Comprobante.consultarSecuencial rest");
    }
}

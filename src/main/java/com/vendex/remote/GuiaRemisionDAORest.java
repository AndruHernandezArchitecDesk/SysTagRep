package com.vendex.remote;

import com.vendex.dao.GuiaRemisionRegistroDAO;
import com.vendex.model.GuiaRemisionRegistro;
import com.vendex.util.SesionActual;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class GuiaRemisionDAORest extends RestDao<GuiaRemisionRegistro> implements GuiaRemisionRegistroDAO {

    public GuiaRemisionDAORest(RestClient rest) {
        super(rest, "/guia-remision", GuiaRemisionRegistro.class);
    }

    @Override
    public int insertar(GuiaRemisionRegistro r) {
        try {
            Map<String, Object> body = Map.of(
                    "claveAcceso", r.getClaveAcceso(),
                    "establecimiento", r.getEstablecimiento(),
                    "puntoEmision", r.getPuntoEmision(),
                    "secuencial", r.getSecuencial(),
                    "fechaEmision", r.getFechaEmision() != null ? r.getFechaEmision().toString() : null,
                    "estadoSri", r.getEstadoSri(),
                    "sucursalId", SesionActual.getSucursalId()
            );
            Map<String, Object> resp = rest.post("/api/guia-remision", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error insertando guía vía REST", e);
        }
    }

    @Override
    public int insertar(Connection con, GuiaRemisionRegistro r) {
        return insertar(r);
    }

    @Override
    public GuiaRemisionRegistro obtenerPorClave(String clave) {
        try {
            String path = "/api/guia-remision/clave?claveAcceso=" + URLEncoder.encode(clave, StandardCharsets.UTF_8);
            return rest.get(path, GuiaRemisionRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public GuiaRemisionRegistro obtenerPorId(int id) {
        try {
            return rest.get("/api/guia-remision/" + id, GuiaRemisionRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<GuiaRemisionRegistro> listarPendientesSri() {
        try {
            return rest.get("/api/guia-remision/pendientes-sri", new com.fasterxml.jackson.core.type.TypeReference<List<GuiaRemisionRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        try {
            rest.put("/api/guia-remision/estado", Map.of(
                    "claveAcceso", claveAcceso,
                    "estado", estado,
                    "mensaje", mensaje,
                    "numeroAutorizacion", numeroAutorizacion,
                    "fechaAutorizacion", fechaAutorizacion
            ), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando estado guía vía REST", e);
        }
    }

    @Override
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        actualizarEstado(claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion);
    }
}

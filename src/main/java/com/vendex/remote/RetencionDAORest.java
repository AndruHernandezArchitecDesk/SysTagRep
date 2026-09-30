package com.vendex.remote;

import com.vendex.dao.RetencionRegistroDAO;
import com.vendex.model.RetencionRegistro;
import com.vendex.util.SesionActual;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class RetencionDAORest extends RestDao<RetencionRegistro> implements RetencionRegistroDAO {

    public RetencionDAORest(RestClient rest) {
        super(rest, "/retencion", RetencionRegistro.class);
    }

    @Override
    public int insertar(RetencionRegistro r) {
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
            Map<String, Object> resp = rest.post("/api/retencion", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error insertando retención vía REST", e);
        }
    }

    @Override
    public int insertar(Connection con, RetencionRegistro r) {
        return insertar(r);
    }

    @Override
    public RetencionRegistro obtenerPorClave(String clave) {
        try {
            String path = "/api/retencion/clave?claveAcceso=" + URLEncoder.encode(clave, StandardCharsets.UTF_8);
            return rest.get(path, RetencionRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public RetencionRegistro obtenerPorId(int id) {
        try {
            return rest.get("/api/retencion/" + id, RetencionRegistro.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<RetencionRegistro> listarPendientesSri() {
        try {
            return rest.get("/api/retencion/pendientes-sri", new com.fasterxml.jackson.core.type.TypeReference<List<RetencionRegistro>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        try {
            rest.put("/api/retencion/estado", Map.of(
                    "claveAcceso", claveAcceso,
                    "estado", estado,
                    "mensaje", mensaje,
                    "numeroAutorizacion", numeroAutorizacion,
                    "fechaAutorizacion", fechaAutorizacion
            ), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando estado retención vía REST", e);
        }
    }

    @Override
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        actualizarEstado(claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion);
    }
}

package com.vendex.remote;

import com.vendex.dao.CajaSesionDAO;
import com.vendex.model.CajaSesion;
import com.vendex.util.SesionActual;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CajaSesionDAORest extends RestDao<CajaSesion> implements CajaSesionDAO {

    public CajaSesionDAORest(RestClient rest) {
        super(rest, "/caja-sesion", CajaSesion.class);
    }

    @Override
    public int abrir(CajaSesion s) {
        try {
            Map<String, Object> body = Map.of(
                    "montoInicial", s.getMontoInicial() != null ? s.getMontoInicial().toString() : "0",
                    "observaciones", s.getObservaciones() != null ? s.getObservaciones() : "",
                    "usuarioId", SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : 1
            );
            Map<String, Object> resp = rest.post("/api/caja/abrir", body, Map.class);
            if (resp != null && resp.get("id") instanceof Number n) return n.intValue();
            return -1;
        } catch (Exception e) {
            throw new RuntimeException("Error abriendo caja vía REST", e);
        }
    }

    @Override
    public CajaSesion obtenerAbierta() {
        try {
            return rest.get("/api/caja/abierta", CajaSesion.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public CajaSesion obtenerPorId(int id) {
        try {
            return rest.get("/api/caja/sesiones/" + id, CajaSesion.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<CajaSesion> listarPorFecha(LocalDate desde, LocalDate hasta) {
        try {
            String path = "/api/caja/sesiones?desde=" + desde.toString() + "&hasta=" + hasta.toString();
            return rest.get(path, new com.fasterxml.jackson.core.type.TypeReference<List<CajaSesion>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean cerrar(int id, BigDecimal montoFisico, BigDecimal diferencia, String observaciones) {
        try {
            Map<String, Object> body = Map.of(
                    "sesionId", id,
                    "montoFisico", montoFisico != null ? montoFisico.toString() : "0",
                    "diferencia", diferencia != null ? diferencia.toString() : "0",
                    "observaciones", observaciones != null ? observaciones : ""
            );
            Map<String, Object> resp = rest.post("/api/caja/cerrar", body, Map.class);
            return resp != null && Boolean.TRUE.equals(resp.get("cerrado"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public BigDecimal calcularTotalMovimientos(int sesionId) {
        try {
            Map<String, Object> resp = rest.get("/api/caja/movimientos/" + sesionId + "/total", Map.class);
            if (resp != null && resp.get("total") instanceof String s) return new BigDecimal(s);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

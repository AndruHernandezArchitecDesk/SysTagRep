package com.vendex.remote;

import com.vendex.dao.SecuenciaBloqueDAO;
import com.vendex.model.SecuenciaBloque;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SecuenciaBloqueDAORest extends RestDao<SecuenciaBloque> implements SecuenciaBloqueDAO {

    public SecuenciaBloqueDAORest(RestClient rest) {
        super(rest, "/secuencia-bloque", SecuenciaBloque.class);
    }

    @Override
    public SecuenciaBloque obtenerBloqueDisponible(int puntoEmisionId, String tipo) {
        try {
            String path = "/api/secuencia-documento/bloque-disponible?puntoEmisionId=" + puntoEmisionId + "&tipo=" + URLEncoder.encode(tipo, StandardCharsets.UTF_8);
            return rest.get(path, SecuenciaBloque.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public SecuenciaBloque obtenerBloqueDisponible(java.sql.Connection con, int puntoEmisionId, String tipo) {
        return obtenerBloqueDisponible(puntoEmisionId, tipo);
    }

    @Override
    public SecuenciaBloque reservarBloque(int puntoEmisionId, String tipo, int tamano) {
        try {
            Map<String, Object> body = Map.of(
                    "puntoEmisionId", puntoEmisionId,
                    "tipo", tipo,
                    "tamano", tamano
            );
            return rest.post("/api/secuencia-documento/bloque", body, SecuenciaBloque.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public SecuenciaBloque reservarBloque(java.sql.Connection con, int puntoEmisionId, String tipo, int tamano) {
        return reservarBloque(puntoEmisionId, tipo, tamano);
    }

    @Override
    public void marcarUsado(SecuenciaBloque bloque, int numeroUsado) {
        try {
            rest.put("/api/secuencia-documento/bloque/" + bloque.getId() + "/usado", Map.of("numeroUsado", numeroUsado), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error marcando usado bloque vía REST", e);
        }
    }

    @Override
    public void marcarUsado(java.sql.Connection con, SecuenciaBloque bloque, int numeroUsado) {
        marcarUsado(bloque, numeroUsado);
    }

    @Override
    public List<SecuenciaBloque> listarPorPuntoEmision(int puntoEmisionId) {
        try {
            return rest.get("/api/secuencia-documento/bloques?puntoEmisionId=" + puntoEmisionId, new com.fasterxml.jackson.core.type.TypeReference<List<SecuenciaBloque>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void liberarBloquesExpirados() {
        try {
            rest.post("/api/secuencia-documento/bloques/liberar-expirados", Map.of(), Map.class);
        } catch (Exception e) {
            // ignorar
        }
    }
}

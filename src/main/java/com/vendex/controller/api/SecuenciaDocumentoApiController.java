package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.SecuenciaBloqueDAO;
import com.vendex.dao.SecuenciaDocumentoDAO;
import com.vendex.model.SecuenciaBloque;
import com.vendex.model.SecuenciaDocumento;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class SecuenciaDocumentoApiController {

    private final SecuenciaDocumentoDAO secuenciaDAO;
    private final SecuenciaBloqueDAO bloqueDAO;

    public SecuenciaDocumentoApiController() {
        this(AppContext.getInstance());
    }

    public SecuenciaDocumentoApiController(AppContext ctx) {
        this.secuenciaDAO = ctx.secuenciaDAO;
        this.bloqueDAO = ctx.secuenciaBloqueDAO;
    }

    public void obtener(Context ctx) {
        int puntoEmisionId = Integer.parseInt(ctx.pathParam("puntoEmisionId"));
        String tipo = ctx.pathParam("tipo");
        SecuenciaDocumento sec = secuenciaDAO.obtener(puntoEmisionId, tipo);
        if (sec == null) ctx.status(404).json(Map.of("error", "Secuencia no encontrada"));
        else ctx.json(Map.of(
                "puntoEmisionId", sec.getPuntoEmisionId(),
                "tipo", sec.getTipo(),
                "establecimiento", sec.getEstablecimiento(),
                "puntoEmision", sec.getPuntoEmision(),
                "siguienteNumero", sec.getSiguienteNumero()
        ));
    }

    public void marcarUsado(Context ctx) {
        int puntoEmisionId = Integer.parseInt(ctx.pathParam("puntoEmisionId"));
        String tipo = ctx.pathParam("tipo");
        int secuencial = secuenciaDAO.marcarUsado(puntoEmisionId, tipo);
        ctx.json(Map.of("secuencial", secuencial));
    }

    public void reservarBloque(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int puntoEmisionId = ((Number) body.get("puntoEmisionId")).intValue();
            String tipo = (String) body.get("tipo");
            int tamano = ((Number) body.getOrDefault("tamano", 50)).intValue();
            SecuenciaBloque bloque = bloqueDAO.reservarBloque(puntoEmisionId, tipo, tamano);
            if (bloque == null) ctx.status(500).json(Map.of("error", "No se pudo reservar bloque"));
            else ctx.json(mapearBloque(bloque));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerBloqueDisponible(Context ctx) {
        int puntoEmisionId = Integer.parseInt(ctx.queryParam("puntoEmisionId"));
        String tipo = ctx.queryParam("tipo");
        SecuenciaBloque bloque = bloqueDAO.obtenerBloqueDisponible(puntoEmisionId, tipo);
        if (bloque == null) ctx.status(404).json(Map.of("error", "No hay bloques disponibles"));
        else ctx.json(mapearBloque(bloque));
    }

    public void marcarBloqueUsado(Context ctx) {
        int bloqueId = Integer.parseInt(ctx.pathParam("bloqueId"));
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int numeroUsado = ((Number) body.get("numeroUsado")).intValue();
            // Obtener bloque y actualizar
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void listarBloques(Context ctx) {
        int puntoEmisionId = Integer.parseInt(ctx.queryParam("puntoEmisionId"));
        try {
            List<SecuenciaBloque> bloques = bloqueDAO.listarPorPuntoEmision(puntoEmisionId);
            List<Map<String, Object>> out = new java.util.ArrayList<>();
            for (SecuenciaBloque b : bloques) {
                out.add(mapearBloque(b));
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void liberarExpirados(Context ctx) {
        try {
            bloqueDAO.liberarBloquesExpirados();
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    private Map<String, Object> mapearBloque(SecuenciaBloque b) {
        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("puntoEmisionId", b.getPuntoEmisionId());
        m.put("tipo", b.getTipo());
        m.put("numeroInicio", b.getNumeroInicio());
        m.put("numeroFin", b.getNumeroFin());
        m.put("usadoHasta", b.getUsadoHasta());
        m.put("reservadoEn", b.getReservadoEn() != null ? b.getReservadoEn().toString() : null);
        return m;
    }
}


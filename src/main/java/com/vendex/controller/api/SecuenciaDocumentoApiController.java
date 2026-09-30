package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.SecuenciaDocumentoDAO;
import com.vendex.model.SecuenciaDocumento;
import io.javalin.http.Context;

import java.util.Map;

public class SecuenciaDocumentoApiController {

    private final SecuenciaDocumentoDAO dao;

    public SecuenciaDocumentoApiController() {
        this(AppContext.getInstance());
    }

    public SecuenciaDocumentoApiController(AppContext ctx) {
        this.dao = ctx.secuenciaDAO;
    }

    public void obtener(Context ctx) {
        int puntoEmisionId = Integer.parseInt(ctx.pathParam("puntoEmisionId"));
        String tipo = ctx.pathParam("tipo");
        SecuenciaDocumento sec = dao.obtener(puntoEmisionId, tipo);
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
        int secuencial = dao.marcarUsado(puntoEmisionId, tipo);
        ctx.json(Map.of("secuencial", secuencial));
    }
}

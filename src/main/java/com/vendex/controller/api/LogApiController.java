package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.LogDAO;
import io.javalin.http.Context;

import java.util.Map;

public class LogApiController {

    private final LogDAO dao;

    public LogApiController() {
        this(AppContext.getInstance());
    }

    public LogApiController(AppContext ctx) {
        this.dao = ctx.logDAO;
    }

    public void guardar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String controlador = (String) body.get("controlador");
            String metodo = (String) body.get("metodo");
            String mensaje = (String) body.get("mensaje");
            Exception ex = null;
            if (body.get("exception") instanceof Map<?, ?> exMap) {
                ex = new Exception((String) exMap.get("message"));
            }
            if (ex != null) {
                dao.guardar(controlador, metodo, mensaje, ex);
            } else {
                dao.guardar(controlador, metodo, mensaje);
            }
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

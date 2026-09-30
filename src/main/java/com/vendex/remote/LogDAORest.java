package com.vendex.remote;

import com.vendex.dao.LogDAO;

import java.util.Map;

public class LogDAORest implements LogDAO {
    private final RestClient rest;

    public LogDAORest(RestClient rest) {
        this.rest = rest;
    }

    @Override
    public void guardar(String controlador, String metodo, String mensaje, Exception ex) {
        try {
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("controlador", controlador);
            body.put("metodo", metodo);
            body.put("mensaje", mensaje);
            if (ex != null) {
                java.util.Map<String, Object> exMap = new java.util.LinkedHashMap<>();
                exMap.put("message", ex.getMessage());
                exMap.put("class", ex.getClass().getName());
                body.put("exception", exMap);
            }
            rest.post("/api/logs", body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando log vía REST", e);
        }
    }

    @Override
    public void guardar(String controlador, String metodo, String mensaje) {
        guardar(controlador, metodo, mensaje, null);
    }
}

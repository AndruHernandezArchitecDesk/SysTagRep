package com.vendex.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.vendex.dao.VendexiConsumoDAO;
import com.vendex.remote.VendexiConsumoDAORest;
import io.javalin.http.Context;
import java.time.LocalDateTime;
import java.util.Map;

public class ConsumoIaApiController {
    private final VendexiConsumoDAO dao;
    public ConsumoIaApiController(VendexiConsumoDAO dao) { this.dao = dao; }
    public void ejecutar(Context c) {
        JsonNode n = c.bodyAsClass(JsonNode.class);
        Object respuesta = Map.of("ok", true);
        switch (c.pathParam("accion")) {
            case "registrar" -> dao.insertar(VendexiConsumoDAORest.leer(n));
            case "tokens" -> respuesta = Map.of("valor", dao.sumarTokensDesde(LocalDateTime.parse(n.path("desde").asText())));
            case "costo" -> respuesta = Map.of("valor", dao.sumarCostoDesde(LocalDateTime.parse(n.path("desde").asText())));
            case "reciente" -> respuesta = dao.listarReciente(Math.max(1, Math.min(200, n.path("limite").asInt(200))))
                    .stream().map(VendexiConsumoDAORest::datos).toList();
            case "periodo" -> { var f = dao.obtenerInicioPeriodo(); respuesta = Map.of("valor", f == null ? "" : f.toString()); }
            case "renovar" -> dao.actualizarInicioPeriodo(LocalDateTime.now());
            case "sesion" -> respuesta = Map.of("valor", dao.iniciarSesion(n.path("modulo").asText(), n.path("tipo").asText()));
            case "fin-sesion" -> dao.finalizarSesion(n.path("id").asLong());
            default -> throw new IllegalArgumentException("Operación de consumo desconocida");
        }
        c.json(respuesta);
    }
}

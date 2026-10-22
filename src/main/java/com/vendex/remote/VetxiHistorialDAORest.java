package com.vendex.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vendex.dao.VetxiHistorialDAO;
import com.vendex.model.VetxiHistorial.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VetxiHistorialDAORest implements VetxiHistorialDAO {
    private static final java.time.Duration TIMEOUT = java.time.Duration.ofSeconds(15);
    private final RestClient cliente;
    public VetxiHistorialDAORest(RestClient cliente) { this.cliente = cliente; }

    private String identidad(Ambito a) { return "?usuarioId=" + a.usuarioId() + "&sucursalId=" + a.sucursalId(); }

    @Override public void guardar(Ambito a, Mensaje mensaje) throws Exception {
        cliente.post("/api/vetxi/mensajes", Map.of("ambito", a, "mensaje", mensaje), Map.class, TIMEOUT);
    }
    @Override public List<Conversacion> conversaciones(Ambito a, int limite) throws Exception {
        return cliente.get("/api/vetxi/conversaciones" + identidad(a) + "&limite=" + Math.max(1, Math.min(50, limite)),
                new TypeReference<List<Conversacion>>() {}, TIMEOUT);
    }
    @Override public List<Mensaje> mensajes(Ambito a, String id, int limite) throws Exception {
        UUID.fromString(id);
        return cliente.get("/api/vetxi/conversaciones/" + id + "/mensajes" + identidad(a)
                + "&limite=" + Math.max(1, Math.min(200, limite)), new TypeReference<List<Mensaje>>() {}, TIMEOUT);
    }
}

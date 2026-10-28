package com.vendex.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.vendex.dao.VendexiConsumoDAO;
import com.vendex.model.VendexiConsumo;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Fechas ISO explícitas para no depender del módulo Jackson JavaTime del cliente. */
public class VendexiConsumoDAORest implements VendexiConsumoDAO {
    private final RestClient client;
    public VendexiConsumoDAORest(RestClient client) { this.client = client; }
    private JsonNode llamar(String accion, Object datos) {
        try { return client.post("/api/ia/consumo/" + accion, datos, JsonNode.class, Duration.ofSeconds(15)); }
        catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("Medición central no disponible", e);
        }
    }
    public static Map<String, Object> datos(VendexiConsumo c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId()); m.put("fecha", c.getFecha() == null ? LocalDateTime.now().toString() : c.getFecha().toString());
        m.put("modulo", c.getModulo()); m.put("operacion", c.getOperacion()); m.put("modelo", c.getModelo());
        m.put("entrada", c.getTokensEntrada()); m.put("salida", c.getTokensSalida()); m.put("costo", c.getCostoEstimado());
        return m;
    }
    public static VendexiConsumo leer(JsonNode n) {
        var c = new VendexiConsumo(); c.setId(n.path("id").asLong());
        c.setFecha(LocalDateTime.parse(n.path("fecha").asText()));
        c.setModulo(n.path("modulo").asText()); c.setOperacion(n.path("operacion").asText()); c.setModelo(n.path("modelo").asText());
        c.setTokensEntrada(Math.max(0, n.path("entrada").asLong())); c.setTokensSalida(Math.max(0, n.path("salida").asLong()));
        c.setCostoEstimado(new BigDecimal(n.path("costo").asText("0"))); return c;
    }
    public void insertar(VendexiConsumo c) { llamar("registrar", datos(c)); }
    public long sumarTokensDesde(LocalDateTime d) { return llamar("tokens", Map.of("desde", d.toString())).path("valor").asLong(); }
    public BigDecimal sumarCostoDesde(LocalDateTime d) { return new BigDecimal(llamar("costo", Map.of("desde", d.toString())).path("valor").asText()); }
    public List<VendexiConsumo> listarReciente(int limite) {
        List<VendexiConsumo> out = new ArrayList<>();
        for (JsonNode n : llamar("reciente", Map.of("limite", limite))) out.add(leer(n)); return out;
    }
    public LocalDateTime obtenerInicioPeriodo() {
        String fecha = llamar("periodo", Map.of()).path("valor").asText("");
        return fecha.isBlank() ? null : LocalDateTime.parse(fecha);
    }
    public void actualizarInicioPeriodo(LocalDateTime f) { llamar("renovar", Map.of("fecha", f.toString())); }
    public long iniciarSesion(String modulo, String tipo) { return llamar("sesion", Map.of("modulo", modulo, "tipo", tipo)).path("valor").asLong(); }
    public void finalizarSesion(long id) { llamar("fin-sesion", Map.of("id", id)); }
}

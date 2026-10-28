package com.vendex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vendex.dao.*;
import com.vendex.model.CuotaIA.*;
import com.vendex.remote.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Logger;

/** Punto común para todas las solicitudes Gemini, con reserva central previa al envío. */
public class GeminiCuotas {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final CuotaIADAO dao;
    public GeminiCuotas(CuotaIADAO dao) { this.dao = dao; }
    public static CuotaIADAO dao() {
        return ApiConfig.isModoRemoto() ? new CuotaIADAORest(ApiConfig.client()) : new CuotaIADAOPostgres();
    }
    public static class Limite extends Exception {
        public Limite(String mensaje) { super(mensaje); }
    }
    public record Respuesta(HttpResponse<String> http, boolean ahorro) {}

    public Respuesta enviar(HttpClient cliente, URI uri, ObjectNode body, Duration timeout,
                            String modelo, String operacion) throws Exception {
        String id = UUID.randomUUID().toString();
        Permiso permiso;
        try {
            // Cota conservadora por bytes UTF-8 del prompt/herramientas; no es tokenización oficial.
            long estimados = JSON.writeValueAsBytes(body).length;
            permiso = dao.reservar(new Reserva(id, modelo, operacion, estimados));
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new Limite("No se pudo consultar el control central de cuotas. Búsqueda local disponible.");
        }
        if (!permiso.permitido()) throw new Limite(permiso.mensaje());
        body.with("generationConfig").put("maxOutputTokens", permiso.maxSalida());
        HttpRequest req = HttpRequest.newBuilder(uri).timeout(timeout).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
        long inicio = System.nanoTime();
        HttpResponse<String> resp;
        try { resp = cliente.send(req, HttpResponse.BodyHandlers.ofString()); }
        catch (Exception ex) {
            completar(new Fin(id, 0, (System.nanoTime() - inicio) / 1000000, 0, 0, 0, 0, 0, "Sin respuesta HTTP"));
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw ex;
        }
        JsonNode raiz;
        try { raiz = JSON.readTree(resp.body()); } catch (Exception ex) { raiz = JSON.createObjectNode(); }
        if (raiz == null) raiz = JSON.createObjectNode();
        JsonNode uso = raiz.path("usageMetadata");
        long entrada = Math.max(0, uso.path("promptTokenCount").asLong());
        long salida = Math.max(0, uso.path("candidatesTokenCount").asLong());
        long pensamiento = Math.max(0, uso.path("thoughtsTokenCount").asLong());
        long total = Math.max(0, uso.path("totalTokenCount").asLong(entrada + salida + pensamiento));
        String detalle = resp.statusCode() == 429 ? "Cuota Google: límite no especificado; revisar AI Studio"
                : resp.statusCode() == 200 ? (uso.isMissingNode() ? "Sin metadatos de uso" : "") : "HTTP " + resp.statusCode();
        // Guardar solo identificadores de cuota, nunca prompts, claves ni cuerpos de errores completos.
        for (JsonNode d : raiz.path("error").path("details")) {
            for (JsonNode v : d.path("violations")) {
                if (v.hasNonNull("quotaId")) detalle = "Cuota Google: " + v.path("quotaId").asText();
            }
        }
        completar(new Fin(id, resp.statusCode(), (System.nanoTime() - inicio) / 1000000,
                entrada, salida, pensamiento, total,
                resp.statusCode() == 429 ? espera(resp.headers().firstValue("Retry-After").orElse(""), raiz, Instant.now()) : 0, detalle));
        if (resp.statusCode() == 429) throw new Limite("Google ha limitado este modelo (429). Pausa compartida activada; revisa Cuotas y consumo de IA.");
        return new Respuesta(resp, permiso.ahorro());
    }
    private void completar(Fin fin) throws Limite {
        try { dao.finalizar(fin); }
        catch (Exception e) {
            Logger.getLogger(GeminiCuotas.class.getName()).warning("No se pudo persistir resultado de solicitud IA " + fin.id());
            // No continuar con otra llamada cuando la pausa compartida no pudo guardarse.
            throw new Limite("No se pudo registrar el resultado en el control central de cuotas.");
        }
    }
    public static long espera(String header, JsonNode raiz, Instant ahora) {
        long ms = 60000;
        try { ms = Math.max(ms, (long) Math.ceil(Double.parseDouble(header) * 1000)); }
        catch (Exception ex) {
            try { ms = Math.max(ms, Duration.between(ahora, ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis()); }
            catch (Exception ignored) {}
        }
        for (JsonNode d : raiz.path("error").path("details")) {
            String delay = d.path("retryDelay").asText("");
            try { if (delay.endsWith("s")) ms = Math.max(ms, (long) Math.ceil(Double.parseDouble(delay.substring(0, delay.length() - 1)) * 1000)); }
            catch (NumberFormatException ignored) {}
        }
        return Math.min(ms, 7 * 86400000L);
    }
    /** Compatibilidad con la capacidad existente: incorpora pensamiento sin duplicar el total. */
    public static long salidaContabilizada(JsonNode uso) {
        long entrada = Math.max(0, uso.path("promptTokenCount").asLong());
        long salida = Math.max(0, uso.path("candidatesTokenCount").asLong()) + Math.max(0, uso.path("thoughtsTokenCount").asLong());
        return Math.max(0, uso.path("totalTokenCount").asLong(entrada + salida) - entrada);
    }
}

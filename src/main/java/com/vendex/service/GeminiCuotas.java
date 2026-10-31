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
        String detalle = resp.statusCode() == 429 ? detalleCuota(raiz)
                : resp.statusCode() == 200 ? (uso.isMissingNode() ? "Sin metadatos de uso" : "") : "HTTP " + resp.statusCode();
        Instant ahora = Instant.now();
        long pausaMs = resp.statusCode() == 429 ? espera(resp.headers().firstValue("Retry-After").orElse(""), raiz, ahora) : 0;
        completar(new Fin(id, resp.statusCode(), (System.nanoTime() - inicio) / 1000000,
                entrada, salida, pensamiento, total, pausaMs, detalle));
        if (resp.statusCode() == 429) throw new Limite("Google rechazó la solicitud del modelo " + modelo + " (429). "
                + detalle + ". No es un límite local de Vendex. "
                + "Reintento no antes de " + ahora.plusMillis(pausaMs) + " (UTC; espera " + ((pausaMs + 999) / 1000) + " s). "
                + "La pausa se comparte entre equipos. "
                + (cuotaDiaria(detalle) ? "La cuota diaria puede seguir agotada después de esa espera. " : "La espera no garantiza que Google haya liberado cuota. ")
                + "Consulta la cuota y facturación del proyecto en Google AI Studio.");
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
        long ms = -1;
        try { ms = segundos(header); }
        catch (Exception ex) {
            try { ms = Math.max(0, Duration.between(ahora, ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis()); }
            catch (Exception ignored) {}
        }
        for (JsonNode d : raiz.path("error").path("details")) {
            String delay = d.path("retryDelay").asText("");
            try { if (delay.endsWith("s")) ms = Math.max(ms, segundos(delay.substring(0, delay.length() - 1))); }
            catch (NumberFormatException ignored) {}
        }
        return ms < 0 ? 60000 : Math.max(1000, Math.min(ms, 7 * 86400000L));
    }

    private static long segundos(String valor) {
        double segundos = Double.parseDouble(valor);
        if (!Double.isFinite(segundos) || segundos < 0) throw new NumberFormatException("Espera inválida");
        return (long) Math.ceil(segundos * 1000);
    }

    /** Solo campos estructurados de cuota; nunca copiar mensajes completos, prompts o claves. */
    static String detalleCuota(JsonNode raiz) {
        var cuotas = new java.util.LinkedHashSet<String>();
        for (JsonNode d : raiz.path("error").path("details")) {
            for (JsonNode v : d.path("violations")) {
                String id = identificador(v.path("quotaId").asText());
                String metrica = identificador(v.path("quotaMetric").asText());
                String valor = v.path("quotaValue").asText("");
                String cuota = !id.isEmpty() ? id : metrica;
                if (!id.isEmpty() && !metrica.isEmpty()) cuota += " [" + metrica + "]";
                if (!cuota.isEmpty()) {
                    if (valor.matches("\\d{1,18}")) cuota += " (valor " + valor + ")";
                    cuotas.add(cuota);
                }
                if (cuotas.size() >= 3) break;
            }
            if (cuotas.size() >= 3) break;
        }
        String detalle = cuotas.isEmpty() ? "Cuota Google: no especificada; revisar AI Studio" : "Cuota Google: " + String.join("; ", cuotas);
        return detalle.length() > 300 ? detalle.substring(0, 297) + "..." : detalle;
    }

    private static String identificador(String valor) {
        return valor.length() <= 160 && valor.matches("[A-Za-z0-9_./:-]+") ? valor : "";
    }

    private static boolean cuotaDiaria(String detalle) {
        String t = detalle.toLowerCase(java.util.Locale.ROOT);
        return t.contains("perday") || t.contains("per_day") || t.contains("daily");
    }
    /** Compatibilidad con la capacidad existente: incorpora pensamiento sin duplicar el total. */
    public static long salidaContabilizada(JsonNode uso) {
        long entrada = Math.max(0, uso.path("promptTokenCount").asLong());
        long salida = Math.max(0, uso.path("candidatesTokenCount").asLong()) + Math.max(0, uso.path("thoughtsTokenCount").asLong());
        return Math.max(0, uso.path("totalTokenCount").asLong(entrada + salida) - entrada);
    }
}

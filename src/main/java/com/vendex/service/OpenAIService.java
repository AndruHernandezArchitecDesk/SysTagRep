package com.vendex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vendex.config.OpenAIConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Responses API client used by Vetxi for text generation and hosted web search. */
public class OpenAIService {
    private static final Logger LOGGER = Logger.getLogger(OpenAIService.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final String CHAT_MODEL = "gpt-6-luna";
    private static final String WEB_MODEL = "gpt-6-luna";

    public record Fuente(String titulo, String uri) { }
    public record Respuesta(String texto, List<Fuente> fuentes, long tokensEntrada, long tokensSalida, String modelo) {
        public Respuesta {
            texto = texto == null ? "" : texto;
            fuentes = fuentes == null ? List.of() : List.copyOf(fuentes);
        }
    }

    private final HttpClient http;
    private final URI endpoint;
    private final VendexiConsumoService consumo;

    public OpenAIService() { this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), RESPONSES_URI, null); }

    OpenAIService(HttpClient http, URI endpoint, VendexiConsumoService consumo) {
        this.http = http;
        this.endpoint = endpoint;
        this.consumo = consumo;
    }

    public String completar(String prompt, String operacion) throws Exception {
        return completar(prompt, operacion, OpenAIConfig.obtenerApiKey());
    }

    public String completar(String prompt, String operacion, String apiKey) throws Exception {
        Respuesta respuesta = solicitar(prompt, apiKey, CHAT_MODEL, false, false, operacion);
        return respuesta.texto();
    }

    public String completarJson(String prompt, String operacion, String apiKey) throws Exception {
        Respuesta respuesta = solicitar(prompt, apiKey, CHAT_MODEL, false, true, operacion);
        return respuesta.texto();
    }

    public Respuesta investigarWeb(String prompt) throws Exception {
        return investigarWeb(prompt, OpenAIConfig.obtenerApiKey());
    }

    public Respuesta investigarWeb(String prompt, String apiKey) throws Exception {
        return solicitar(prompt, apiKey, WEB_MODEL, true, false, "web.investigacion");
    }

    private Respuesta solicitar(String prompt, String apiKey, String modelo, boolean web,
                                boolean jsonOutput, String operacion) throws Exception {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("API key de OpenAI no configurada");
        ObjectNode body = JSON.createObjectNode();
        body.put("model", modelo);
        body.put("input", prompt == null ? "" : prompt);
        if (web) {
            ArrayNode tools = body.putArray("tools");
            tools.addObject().put("type", "web_search").put("search_context_size", "medium");
            body.put("tool_choice", "required");
            body.putArray("include").add("web_search_call.action.sources");
        }
        if (jsonOutput) body.putObject("text").putObject("format").put("type", "json_object");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IllegalStateException("Timeout de OpenAI: " + e.getMessage(), e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Sin conexión con OpenAI: " + e.getMessage(), e);
        }
        int status = response.statusCode();
        if (status != 200) {
            String bodyText = response.body() == null ? "" : response.body();
            LOGGER.log(Level.FINE, "OpenAI HTTP {0}: {1}", new Object[]{status, abbreviate(bodyText, 500)});
            throw new IllegalStateException("HTTP " + status + " OpenAI: " + abbreviate(bodyText, 240));
        }
        JsonNode root = JSON.readTree(response.body());
        String text = extractText(root);
        if (text.isBlank()) throw new IllegalStateException("OpenAI devolvió una respuesta vacía");
        JsonNode usage = root.path("usage");
        long input = usage.path("input_tokens").asLong(0);
        long output = usage.path("output_tokens").asLong(0);
        registrarConsumo(operacion, modelo, input, output);
        return new Respuesta(text, web ? extractSources(root) : List.of(), input, output, modelo);
    }

    static String extractText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        JsonNode output = root == null ? null : root.path("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                if (!"message".equals(item.path("type").asText())) continue;
                JsonNode content = item.path("content");
                if (!content.isArray()) continue;
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText())) text.append(part.path("text").asText(""));
                }
            }
        }
        return text.toString().trim();
    }

    static List<Fuente> extractSources(JsonNode root) {
        Map<String, String> titlesByUrl = new LinkedHashMap<>();
        JsonNode output = root == null ? null : root.path("output");
        if (output == null || !output.isArray()) return List.of();
        for (JsonNode item : output) {
            if ("message".equals(item.path("type").asText())) {
                for (JsonNode part : item.path("content")) {
                    for (JsonNode annotation : part.path("annotations")) {
                        if (!"url_citation".equals(annotation.path("type").asText())) continue;
                        String url = annotation.path("url").asText("").trim();
                        if (isHttp(url)) {
                            String title = annotation.path("title").asText("");
                            titlesByUrl.merge(url, title, (oldTitle, newTitle) -> oldTitle.isBlank() ? newTitle : oldTitle);
                        }
                    }
                }
            }
            if ("web_search_call".equals(item.path("type").asText())) {
                for (JsonNode source : item.path("action").path("sources")) {
                    String url = source.path("url").asText("").trim();
                    if (isHttp(url)) titlesByUrl.putIfAbsent(url, "");
                }
            }
        }
        return titlesByUrl.entrySet().stream().limit(8)
                .map(e -> new Fuente(e.getValue().isBlank() ? e.getKey() : e.getValue(), e.getKey())).toList();
    }

    private static boolean isHttp(String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    private void registrarConsumo(String operacion, String modelo, long entrada, long salida) {
        try {
            VendexiConsumoService servicio = consumo;
            if (servicio == null) servicio = com.vendex.config.AppContext.getInstance().vendexiConsumoService;
            servicio.registrarTokens("openai", operacion, modelo, entrada, salida);
        } catch (Exception ignored) { }
    }

    private static String abbreviate(String text, int limit) {
        if (text == null) return "";
        return text.length() <= limit ? text : text.substring(0, limit) + "…";
    }
}

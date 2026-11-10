package com.vendex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.vendex.dao.VendexiConsumoDAO;
import com.vendex.model.VendexiConsumo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> response = new AtomicReference<>();
    private final AtomicReference<JsonNode> request = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private OpenAIService service;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            try (exchange) {
                request.set(JSON.readTree(exchange.getRequestBody().readAllBytes()));
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        VendexiConsumoService consumo = new VendexiConsumoService(new NoOpConsumoDao());
        service = new OpenAIService(java.net.http.HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses"), consumo);
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void completionUsesResponsesApiAndExtractsText() throws Exception {
        response.set("""
                {"output":[{"type":"message","content":[{"type":"output_text","text":"Respuesta Vetxi"}]}],
                 "usage":{"input_tokens":21,"output_tokens":8}}
                """);

        assertEquals("Respuesta Vetxi", service.completar("hola", "chat.redaccion", "sk-test"));
        assertEquals("Bearer sk-test", authorization.get());
        assertEquals("gpt-6-luna", request.get().path("model").asText());
        assertEquals("hola", request.get().path("input").asText());
        assertTrue(request.get().path("tools").isMissingNode());
    }

    @Test
    void structuredCompletionRequestsJsonModeForVetxiIntentClassification() throws Exception {
        response.set("""
                {"output":[{"type":"message","content":[{"type":"output_text","text":"{\\\"accion\\\":\\\"GENERAL\\\"}"}]}],
                 "usage":{"input_tokens":12,"output_tokens":6}}
                """);

        assertTrue(service.completarJson("clasifica como JSON", "chat.clasificacion", "sk-test")
                .contains("GENERAL"));
        assertEquals("json_object", request.get().path("text").path("format").path("type").asText());
    }

    @Test
    void webSearchRequestsHostedToolAndExtractsCitationsAndSources() throws Exception {
        response.set("""
                {"output":[
                  {"type":"web_search_call","action":{"sources":[
                    {"type":"url","url":"https://parts.example/oem"},
                    {"type":"url","url":"https://catalog.example/fitment"}]}},
                  {"type":"message","content":[{"type":"output_text","text":"OEM 123; fuente oficial.",
                    "annotations":[{"type":"url_citation","url":"https://parts.example/oem","title":"Catálogo OEM"}]}]}
                ],"usage":{"input_tokens":50,"output_tokens":18}}
                """);

        OpenAIService.Respuesta result = service.investigarWeb("Investiga el OEM", "sk-test");
        assertEquals("OEM 123; fuente oficial.", result.texto());
        assertEquals(2, result.fuentes().size());
        assertEquals("Catálogo OEM", result.fuentes().get(0).titulo());
        assertEquals("https://parts.example/oem", result.fuentes().get(0).uri());
        assertEquals("gpt-6-luna", request.get().path("model").asText());
        assertEquals("web_search", request.get().path("tools").get(0).path("type").asText());
        assertEquals("required", request.get().path("tool_choice").asText());
        assertTrue(request.get().path("include").toString().contains("web_search_call.action.sources"));
    }

    private static final class NoOpConsumoDao implements VendexiConsumoDAO {
        @Override public void insertar(VendexiConsumo consumo) { }
        @Override public long sumarTokensDesde(LocalDateTime desde) { return 0; }
        @Override public BigDecimal sumarCostoDesde(LocalDateTime desde) { return BigDecimal.ZERO; }
        @Override public List<VendexiConsumo> listarReciente(int limite) { return List.of(); }
        @Override public LocalDateTime obtenerInicioPeriodo() { return LocalDateTime.now(); }
        @Override public void actualizarInicioPeriodo(LocalDateTime inicio) { }
        @Override public long iniciarSesion(String modulo, String tipo) { return 1; }
        @Override public void finalizarSesion(long idSesion) { }
    }
}

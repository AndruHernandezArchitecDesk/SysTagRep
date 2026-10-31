package com.vendex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.vendex.config.DatabaseConnection;
import com.vendex.dao.CuotaIADAOPostgres;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.HttpClient;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GeminiCuotasTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    @AfterEach void cerrar() { if (server != null) server.stop(0); DatabaseConnection.resetToDefault(); }
    @Test void transporteReal429_noVuelveAEnviarDesdeOtroCliente() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:transporte_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        CuotaIADAOPostgres.prepararEsquema();
        AtomicInteger llamadas = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                var body = json.readTree(exchange.getRequestBody());
                assertEquals(2048, body.path("generationConfig").path("maxOutputTokens").asInt());
                llamadas.incrementAndGet();
                byte[] datos = ("{\"error\":{\"message\":\"secreto-no-registrar\",\"details\":[{\"retryDelay\":\"120s\"},"
                        + "{\"violations\":[{\"quotaId\":\"GenerateRequestsPerDayPerProjectPerModel-FreeTier\","
                        + "\"quotaMetric\":\"generativelanguage.googleapis.com/generate_content_free_tier_requests\",\"quotaValue\":\"20\"}]}]}}").getBytes();
                exchange.getResponseHeaders().set("Retry-After", "90");
                exchange.sendResponseHeaders(429, datos.length); exchange.getResponseBody().write(datos);
            }
        }); server.start();
        var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        for (int i = 0; i < 2; i++) {
            var gateway = new GeminiCuotas(new CuotaIADAOPostgres());
            var error = assertThrows(GeminiCuotas.Limite.class, () -> gateway.enviar(HttpClient.newHttpClient(), uri,
                    json.createObjectNode().put("prompt", "prueba"), Duration.ofSeconds(5), "gemini-2.5-flash", "web.investigacion"));
            assertTrue(error.getMessage().contains("GenerateRequestsPerDayPerProjectPerModel-FreeTier"));
            assertTrue(error.getMessage().contains("UTC"));
            assertTrue(error.getMessage().contains("No es un límite local"));
            assertFalse(error.getMessage().contains("secreto-no-registrar"));
            if (i == 0) {
                assertTrue(error.getMessage().contains("espera 120 s"));
                assertTrue(error.getMessage().contains("cuota diaria"));
            }
        }
        assertEquals(1, llamadas.get());
        assertEquals(1, new CuotaIADAOPostgres().panel().intentos().size());
        String detalle = new CuotaIADAOPostgres().panel().intentos().get(0).detalle();
        assertTrue(detalle.contains("valor 20")); assertFalse(detalle.contains("secreto-no-registrar"));
    }
    @Test void retryAfterRespetaSegundosFechaYDetalle() throws Exception {
        var body = json.readTree("{\"error\":{\"details\":[{\"retryDelay\":\"120.5s\"}]}}");
        assertEquals(120500, GeminiCuotas.espera("90", body, Instant.EPOCH));
        assertEquals(180000, GeminiCuotas.espera("Thu, 1 Jan 1970 00:03:00 GMT", body, Instant.EPOCH));
        assertEquals(60000, GeminiCuotas.espera("inválido", json.createObjectNode(), Instant.EPOCH));
        assertEquals(12500, GeminiCuotas.espera("12.5", json.createObjectNode(), Instant.EPOCH));
        assertEquals(5000, GeminiCuotas.espera("", json.readTree("{\"error\":{\"details\":[{\"retryDelay\":\"5s\"}]}}"), Instant.EPOCH));
        assertEquals(60000, GeminiCuotas.espera("NaN", json.createObjectNode(), Instant.EPOCH));
        assertEquals(60000, GeminiCuotas.espera("-5", json.createObjectNode(), Instant.EPOCH));
    }
    @Test void tokensTotalIncluyePensamientoSinDuplicarlo() throws Exception {
        assertEquals(100, GeminiCuotas.salidaContabilizada(json.readTree("{\"promptTokenCount\":100,\"candidatesTokenCount\":30,\"thoughtsTokenCount\":70,\"totalTokenCount\":200}")));
        assertEquals(100, GeminiCuotas.salidaContabilizada(json.readTree("{\"promptTokenCount\":100,\"candidatesTokenCount\":30,\"thoughtsTokenCount\":70}")));
    }
}

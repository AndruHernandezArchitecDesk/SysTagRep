package com.vendex.chatbot;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Ejerce el transporte HTTP real contra un servidor local; nunca usa Google ni claves reales. */
class GeminiChatbotServiceDisponibilidadTest {

    private static final String PRINCIPAL = "gemini-3.1-flash-lite";
    private static final String RESPALDO = "gemini-flash-latest";
    private static final String ULTIMO = "gemini-flash-lite-latest";
    private final Map<String, Integer> estados = new ConcurrentHashMap<>();
    private final List<String> llamadas = new CopyOnWriteArrayList<>();
    private final RelojMutable reloj = new RelojMutable();
    private HttpServer servidor;
    private GeminiChatbotService servicio;
    private String respuesta = "ok";

    private static class RelojMutable extends Clock {
        private Instant ahora = Instant.parse("2026-10-22T00:00:00Z");
        void avanzar(long segundos) { ahora = ahora.plusSeconds(segundos); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @BeforeEach
    void iniciar() throws Exception {
        com.vendex.config.DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:cuotas_chat_" + java.util.UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        com.vendex.dao.CuotaIADAOPostgres.prepararEsquema();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/models/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                String modelo = exchange.getRequestURI().getPath().substring("/models/".length()).split(":")[0];
                llamadas.add(modelo);
                int estado = estados.getOrDefault(modelo, 200);
                String body = estado == 200
                        ? "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" +
                        new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(respuesta) + "}]}}]}"
                        : "{\"error\":{\"code\":" + estado + ",\"message\":\"UNAVAILABLE 503 en mensaje\"}}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(estado, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        servidor.start();
        servicio = new GeminiChatbotService((d, m, mo, a) -> GeminiChatbotService.ResultadoBusqueda.noEncontrado(),
                null, URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/models/"), reloj);
    }

    @AfterEach
    void cerrar() {
        if (servidor != null) servidor.stop(0);
        com.vendex.config.DatabaseConnection.resetToDefault();
    }

    @Test
    void error503_usaRespaldo_yNoRepitePrincipalEnRedaccion() throws Exception {
        estados.put(PRINCIPAL, 503);
        respuesta = "{\"accion\":\"GENERAL\"}";
        var resultado = servicio.preguntarConGeminiRespuesta("hola", "clave-falsa", null, null, null);
        assertNotNull(resultado.texto);
        assertEquals(List.of(PRINCIPAL, RESPALDO, RESPALDO), llamadas,
                "La redacción debe omitir el principal que acaba de devolver 503");
    }

    @Test
    void dosModelosSaturados_reutilizaTercerModelo() throws Exception {
        estados.put(PRINCIPAL, 503);
        estados.put(RESPALDO, 503);
        assertEquals("ok", servicio.llamarGemini("consulta", "clave-falsa"));
        assertEquals("ok", servicio.llamarGemini("otra", "clave-falsa"));
        assertEquals(List.of(PRINCIPAL, RESPALDO, ULTIMO, ULTIMO), llamadas);
    }

    @Test
    void todosSaturados_siguientesConsultasFallanRapido_sinMasPeticiones() {
        estados.put(PRINCIPAL, 503);
        estados.put(RESPALDO, 503);
        estados.put(ULTIMO, 503);
        assertThrows(Exception.class, () -> servicio.llamarGemini("consulta", "clave-falsa"));
        Exception error = assertThrows(Exception.class, () -> servicio.llamarGemini("otra", "clave-falsa"));
        assertTrue(error.getMessage().contains("60s"), error.getMessage());
        assertEquals(List.of(PRINCIPAL, RESPALDO, ULTIMO), llamadas);
    }

    @Test
    void principalRecuperado_seReintentaDespuesDeLaPausa() throws Exception {
        estados.put(PRINCIPAL, 503);
        servicio.llamarGemini("consulta", "clave-falsa");
        estados.put(PRINCIPAL, 200);
        reloj.avanzar(59);
        servicio.llamarGemini("antes", "clave-falsa");
        reloj.avanzar(1);
        servicio.llamarGemini("despues", "clave-falsa");
        assertEquals(List.of(PRINCIPAL, RESPALDO, RESPALDO, PRINCIPAL), llamadas);
    }

    @Test
    void modelo404_seOmiteDuranteDiezMinutos() throws Exception {
        estados.put(PRINCIPAL, 404);
        servicio.llamarGemini("consulta", "clave-falsa");
        reloj.avanzar(60);
        servicio.llamarGemini("otra", "clave-falsa");
        estados.put(PRINCIPAL, 200);
        reloj.avanzar(540);
        servicio.llamarGemini("recuperado", "clave-falsa");
        assertEquals(List.of(PRINCIPAL, RESPALDO, RESPALDO, PRINCIPAL), llamadas);
    }

    @Test
    void erroresDeCuotaKeyYPayload_noSeConfundenCon503DelTexto() {
        for (int estado : List.of(401, 403, 400, 429)) {
            llamadas.clear();
            estados.put(PRINCIPAL, estado);
            Exception error = assertThrows(Exception.class, () -> servicio.llamarGemini("consulta", "clave-falsa"));
            assertTrue(error.getMessage().contains(Integer.toString(estado)), error.getMessage());
            assertEquals(List.of(PRINCIPAL), llamadas, "No cambiar modelo para HTTP " + estado);
        }
    }

    @Test
    void otrosErroresTransitorios500502504_tambienPermitenRespaldo() throws Exception {
        estados.put(PRINCIPAL, 500);
        estados.put(RESPALDO, 502);
        estados.put(ULTIMO, 504);
        assertThrows(Exception.class, () -> servicio.llamarGemini("consulta", "clave-falsa"));
        assertEquals(List.of(PRINCIPAL, RESPALDO, ULTIMO), llamadas);
        reloj.avanzar(60);
        estados.clear();
        assertEquals("ok", servicio.llamarGemini("recuperado", "clave-falsa"));
        assertEquals(PRINCIPAL, llamadas.get(3));
    }
}

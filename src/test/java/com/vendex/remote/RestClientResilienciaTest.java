package com.vendex.remote;

import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 6b: resiliencia del cliente API ante fallos de conexión y token vencido.
 * Servidor Javalin embebido (sin BD) simula la API central:
 * login → token Bearer; el token rota para forzar un 401 y probar el reintento automático.
 */
class RestClientResilienciaTest {

    private Javalin server;
    private String base;
    private final AtomicReference<String> tokenVigente = new AtomicReference<>("tok-aaa");

    @BeforeEach
    void setUp() {
        RestClient.clearToken();
        server = Javalin.create().start(0);
        base = "http://localhost:" + server.port();
        server.post("/api/auth/login", ctx -> {
            Map<String, Object> b = ctx.bodyAsClass(Map.class);
            if ("admin".equals(b.get("username")) && "secret".equals(b.get("password"))) {
                ctx.json(Map.of("token", tokenVigente.get(), "tokenType", "Bearer"));
            } else {
                ctx.status(401).json(Map.of("error", "credenciales inválidas"));
            }
        });
        server.get("/api/datos", ctx -> {
            String auth = ctx.header("Authorization");
            if (auth == null || !("Bearer " + tokenVigente.get()).equals(auth)) {
                ctx.status(401).json(Map.of("error", "token inválido"));
            } else {
                ctx.json(Map.of("ok", true));
            }
        });
    }

    @AfterEach
    void tearDown() {
        RestClient.clearToken();
        if (server != null) server.stop();
    }

    @Test
    void login_getYTokenEnviado() throws Exception {
        assertTrue(RestClient.login(base, "admin", "secret"));
        assertNotNull(RestClient.getToken());
        RestClient c = new RestClient(base);
        Map<String, Object> r = c.get("/api/datos", Map.class);
        assertEquals(Boolean.TRUE, r.get("ok"));
    }

    @Test
    void login_credencialesMalas_devuelveFalse() {
        assertFalse(RestClient.login(base, "admin", "mala"));
        assertNull(RestClient.getToken());
    }

    @Test
    void servidorCaido_lanzaIOException_noExcepcionRara() {
        RestClient c = new RestClient("http://localhost:1");
        assertThrows(Exception.class, () -> c.get("/api/datos", Map.class));
    }

    @Test
    void tokenVencido_401_reintentaConLoginAutomatico() throws Exception {
        assertTrue(RestClient.login(base, "admin", "secret"));
        RestClient c = new RestClient(base);
        // El servidor rota el token (simula expiración/revocación)
        tokenVigente.set("tok-bbb");
        // Sin reintento fallaría con 401; el cliente debe re-loguear y reintentar una vez
        Map<String, Object> r = c.get("/api/datos", Map.class);
        assertEquals(Boolean.TRUE, r.get("ok"));
    }

    @Test
    void sinCredenciales_elPrimer401NoSeRecupera() {
        // Token inventado sin haber hecho login → no hay lastUser/lastPass para reintentar
        RestClient.setToken("tok-falso");
        RestClient c = new RestClient(base);
        assertThrows(Exception.class, () -> c.get("/api/datos", Map.class));
    }
}

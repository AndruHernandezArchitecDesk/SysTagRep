package com.vendex.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static com.vendex.remote.ApiConfig.DEFAULT_HOST;

public class RestClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Fase 6: token Bearer compartido por todos los clientes *DAORest (una sesión por proceso)
    private static volatile String token;
    private static volatile String lastUser;
    private static volatile String lastPass;
    private static volatile String lastBase;

    public static void setToken(String t) { token = t; }
    public static String getToken() { return token; }
    public static void clearToken() { token = null; }

    private static void auth(HttpRequest.Builder b) {
        String t = token;
        if (t != null && !t.isBlank()) b.header("Authorization", "Bearer " + t);
    }

    /** Login contra /api/auth/login y guarda el token para el resto de llamadas. */
    public static boolean login(String baseUrl, String username, String password) {
        try {
            String body = MAPPER.writeValueAsString(java.util.Map.of(
                    "username", username, "password", password));
            String host = baseUrl != null && !baseUrl.isBlank() ? baseUrl : DEFAULT_HOST;
            String base = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return false;
            com.fasterxml.jackson.databind.JsonNode n = MAPPER.readTree(resp.body());
            String t = n.path("token").asText("");
            if (t.isBlank()) return false;
            token = t;
            lastUser = username;
            lastPass = password;
            lastBase = base;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Auto-login con las últimas credenciales conocidas (refresco tras 401). */
    private static boolean refrescarToken() {
        String u = lastUser, p = lastPass, b = lastBase;
        if (u == null || u.isBlank()) return false;
        return login(b, u, p);
    }

    private final String baseUrl;

    public RestClient(String baseUrl) {
        this.baseUrl = (baseUrl != null ? baseUrl : DEFAULT_HOST).endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : (baseUrl != null ? baseUrl : DEFAULT_HOST);
    }

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").GET();
        auth(b);
        return execute(b.build(), type);
    }

    public <T> T get(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").GET();
        auth(b);
        return execute(b.build(), typeRef);
    }

    public <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        byte[] json = MAPPER.writeValueAsBytes(body);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json));
        auth(b);
        return execute(b.build(), type);
    }

    public <T> T put(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        byte[] json = MAPPER.writeValueAsBytes(body);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(json));
        auth(b);
        return execute(b.build(), type);
    }

    public void delete(String path) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").DELETE();
        auth(b);
        HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401 && refrescarToken()) {
            auth(b);
            resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        }
        if (resp.statusCode() >= 400) throw new IOException("DELETE " + path + " -> " + resp.statusCode());
    }

    public int status(String path) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").GET();
        auth(b);
        return CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    public String postString(String path, String body, String idempotencyKey) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        auth(builder);
        HttpResponse<String> resp = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code >= 500 && code < 600) {
            throw new IOException("Servidor en mantenimiento: " + path + " -> " + code);
        }
        return resp.body();
    }

    private <T> T execute(HttpRequest req, Class<T> type) throws IOException, InterruptedException {
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401 && refrescarToken()) {
            resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        }
        if (resp.statusCode() >= 400) throw new IOException(req.uri() + " -> " + resp.statusCode() + ": " + resp.body());
        return resp.body().isEmpty() ? null : MAPPER.readValue(resp.body(), type);
    }

    private <T> T execute(HttpRequest req, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401 && refrescarToken()) {
            resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        }
        if (resp.statusCode() >= 400) throw new IOException(req.uri() + " -> " + resp.statusCode() + ": " + resp.body());
        return MAPPER.readValue(resp.body(), typeRef);
    }
}

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

    private final String baseUrl;

    public RestClient(String baseUrl) {
        this.baseUrl = (baseUrl != null ? baseUrl : DEFAULT_HOST).endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : (baseUrl != null ? baseUrl : DEFAULT_HOST);
    }

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").GET().build();
        return execute(req, type);
    }

    public <T> T get(String path, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").GET().build();
        return execute(req, typeRef);
    }

    public <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        byte[] json = MAPPER.writeValueAsBytes(body);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json)).build();
        return execute(req, type);
    }

    public <T> T put(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        byte[] json = MAPPER.writeValueAsBytes(body);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(json)).build();
        return execute(req, type);
    }

    public void delete(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").DELETE().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new IOException("DELETE " + path + " -> " + resp.statusCode());
    }

    public int status(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json").GET().build();
        return CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private <T> T execute(HttpRequest req, Class<T> type) throws IOException, InterruptedException {
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new IOException(req.uri() + " -> " + resp.statusCode() + ": " + resp.body());
        return resp.body().isEmpty() ? null : MAPPER.readValue(resp.body(), type);
    }

    private <T> T execute(HttpRequest req, TypeReference<T> typeRef) throws IOException, InterruptedException {
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new IOException(req.uri() + " -> " + resp.statusCode() + ": " + resp.body());
        return MAPPER.readValue(resp.body(), typeRef);
    }
}

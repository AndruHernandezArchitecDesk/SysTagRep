package com.vendex.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Base para DAOs REST: traduce operaciones a llamadas HTTP contra el backend.
 * Cada {@code XxxDAORest} extiende esto e implementa su interfaz de DAO.
 */
public abstract class RestDao<T> {

    protected final RestClient rest;
    protected final String basePath;
    protected final Class<T> type;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected RestDao(RestClient rest, String basePath, Class<T> type) {
        this.rest = rest;
        this.basePath = basePath;
        this.type = type;
    }

    protected T getOne(String path, Class<T> t) throws IOException, InterruptedException {
        return rest.get(path, t);
    }

    protected List<T> getAll(String path, TypeReference<List<T>> ref) throws IOException, InterruptedException {
        return rest.get(path, ref);
    }

    protected List<T> getAll(String path) throws IOException, InterruptedException {
        return rest.get(path, new TypeReference<List<T>>() {});
    }

    protected T save(String path, T entity) throws IOException, InterruptedException {
        return rest.post(path, entity, type);
    }

    protected T update(String path, T entity) throws IOException, InterruptedException {
        return rest.put(path, entity, type);
    }

    protected void remove(String path) throws IOException, InterruptedException {
        rest.delete(path);
    }

    protected int status(String path) throws IOException, InterruptedException {
        return rest.status(path);
    }
}

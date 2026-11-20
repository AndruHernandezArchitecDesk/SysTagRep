package com.vendex.controller.api;

import com.vendex.dao.EmpresaDAO;
import com.vendex.model.Empresa;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaLogoApiTest {
    @Test
    void actualizaYDevuelveElLogoBinarioCompartidoComoBase64() throws Exception {
        Empresa inicial = new Empresa();
        inicial.setId(1); inicial.setRuc("1790000000001"); inicial.setRazonSocial("TAG"); inicial.setEstado(true);
        var guardada = new AtomicReference<>(inicial);
        EmpresaDAO dao = new EmpresaDAO() {
            @Override public Empresa obtenerPorId(int id) { return id == 1 ? guardada.get() : null; }
            @Override public List<Empresa> listar() { return List.of(guardada.get()); }
            @Override public boolean actualizar(Empresa empresa) { guardada.set(empresa); return true; }
            @Override public Empresa crear(Empresa empresa) { empresa.setId(1); guardada.set(empresa); return empresa; }
        };
        var api = new EmpresaApiController(dao);
        var app = Javalin.create(c -> c.showJavalinBanner = false);
        app.get("/empresa", api::listar);
        app.put("/empresa", api::actualizar);
        app.start(0);
        byte[] logo = {4, 3, 2, 1, 0, -1};
        String base64 = java.util.Base64.getEncoder().encodeToString(logo);
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var put = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + app.port() + "/empresa"))
                    .header("Content-Type", "application/json")
                    .PUT(java.net.http.HttpRequest.BodyPublishers.ofString("""
                            {"id":1,"logoBytes":"%s"}
                            """.formatted(base64))).build();
            var updated = client.send(put, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, updated.statusCode(), updated.body());
            assertArrayEquals(logo, guardada.get().getLogoBytes());

            var get = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + app.port() + "/empresa"))
                    .GET().build();
            var listed = client.send(get, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, listed.statusCode());
            assertTrue(listed.body().contains("\"logoBytes\":\"" + base64 + "\""), listed.body());
        } finally { app.stop(); }
    }
}

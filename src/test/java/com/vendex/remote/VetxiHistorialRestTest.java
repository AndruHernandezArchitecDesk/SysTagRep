package com.vendex.remote;

import com.vendex.api.ApiAuthException;
import com.vendex.config.DatabaseConnection;
import com.vendex.controller.api.VetxiHistorialApiController;
import com.vendex.dao.VetxiHistorialDAOPostgres;
import com.vendex.model.VetxiHistorial.*;
import com.vendex.service.VetxiHistorialService;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Controlador y DAO REST reales contra HTTP local y SQL aislado, sin servidor productivo. */
class VetxiHistorialRestTest {
    private final ThreadLocal<Ambito> identidad = new ThreadLocal<>();
    private final Ambito ana = new Ambito(10, 1), juan = new Ambito(20, 1);
    private Javalin app;
    private String tokenAnterior, base;
    private VetxiHistorialDAORest dao;

    @BeforeEach void iniciar() {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:vetxi_rest_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        DatabaseConnection.ensureVetxiHistorialSchema();
        var ctrl = new VetxiHistorialApiController(new VetxiHistorialService(new VetxiHistorialDAOPostgres(), identidad::get));
        app = Javalin.create(cfg -> cfg.jetty.defaultHost = "127.0.0.1");
        // Equivalente al ámbito ya autenticado que ApiServer instala por petición.
        app.before(c -> identidad.set(switch (String.valueOf(c.header("Authorization"))) {
            case "Bearer ana" -> ana;
            case "Bearer juan" -> juan;
            default -> null;
        }));
        app.after(c -> identidad.remove());
        app.exception(ApiAuthException.class, (e, c) -> c.status(e.status).json(java.util.Map.of("error", e.getMessage())));
        app.exception(IllegalArgumentException.class, (e, c) -> c.status(400));
        app.get("/api/vetxi/conversaciones", ctrl::conversaciones);
        app.get("/api/vetxi/conversaciones/{id}/mensajes", ctrl::mensajes);
        app.post("/api/vetxi/mensajes", ctrl::guardar);
        app.start(0);
        base = "http://127.0.0.1:" + app.port();
        tokenAnterior = RestClient.getToken(); RestClient.setToken("ana");
        dao = new VetxiHistorialDAORest(new RestClient(base));
    }

    @AfterEach void cerrar() {
        if (app != null) app.stop();
        RestClient.setToken(tokenAnterior);
        DatabaseConnection.resetToDefault();
    }

    private Mensaje mensaje(String id) {
        return new Mensaje(UUID.randomUUID().toString(), id, "Filtro aceite", false, "Código F-1, stock 4",
                false, "INVENTARIO", List.of(), List.of(new Fuente("Catálogo", "https://example.com/filtro")), null);
    }

    @Test void guardaYRecuperaMetadatosPorRest_sinDuplicarReintentos() throws Exception {
        String id = UUID.randomUUID().toString(); var mensaje = mensaje(id);
        dao.guardar(ana, mensaje); dao.guardar(ana, mensaje);
        assertEquals(id, dao.conversaciones(ana, 20).get(0).id());
        var mensajes = dao.mensajes(ana, id, 100);
        assertEquals(1, mensajes.size());
        assertEquals("INVENTARIO", mensajes.get(0).rolIA());
        assertEquals(mensaje.fuentes(), mensajes.get(0).fuentes());
        dao.guardar(ana, new Mensaje(UUID.randomUUID().toString(), id, mensaje.titulo(), true, "Segunda consulta",
                false, null, List.of(), List.of(), null));
        assertEquals("Segunda consulta", dao.mensajes(ana, id, 1).get(0).texto());
        dao.guardar(ana, mensaje(UUID.randomUUID().toString()));
        assertEquals(1, dao.conversaciones(ana, 1).size());
    }

    @Test void otroUsuarioNoPuedeLeerConversacionNiSuplantarPropietarioEnLaEscritura() throws Exception {
        String id = UUID.randomUUID().toString(); dao.guardar(ana, mensaje(id));
        RestClient.setToken("juan");
        assertTrue(dao.mensajes(juan, id, 100).isEmpty());
        assertTrue(dao.conversaciones(juan, 20).isEmpty());
        var error = assertThrows(java.io.IOException.class, () -> dao.guardar(ana, mensaje(id)));
        assertTrue(error.getMessage().contains("403"));
        assertThrows(java.io.IOException.class, () -> dao.mensajes(ana, id, 100));
        assertThrows(java.io.IOException.class, () -> dao.conversaciones(new Ambito(20, 2), 20));
    }

    @Test void sinSesionNoHayAccesoAlHistorial() throws Exception {
        var respuesta = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/api/vetxi/conversaciones"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, respuesta.statusCode());
    }
}

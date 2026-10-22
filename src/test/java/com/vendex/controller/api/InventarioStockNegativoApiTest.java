package com.vendex.controller.api;

import com.vendex.dao.InventarioDAO;
import com.vendex.model.Inventario;
import com.vendex.model.StockNegativoItem;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InventarioStockNegativoApiTest {
    private Javalin api;
    private AtomicInteger sucursalConsultada;
    private String base;

    @BeforeEach void iniciar() {
        sucursalConsultada = new AtomicInteger();
        InventarioDAO dao = (InventarioDAO) java.lang.reflect.Proxy.newProxyInstance(InventarioDAO.class.getClassLoader(),
                new Class[]{InventarioDAO.class}, (proxy, metodo, args) -> {
                    if (metodo.getName().equals("listarStockNegativoPorFecha")) {
                        sucursalConsultada.set((int) args[0]);
                        return List.of(new StockNegativoItem(4, "TMP-1", "Temporal", -3, "2026-10-22T10:00:00"));
                    }
                    if (metodo.getReturnType() == List.class) return List.of();
                    return null;
                });
        var controller = new InventarioApiController(dao);
        api = Javalin.create(cfg -> cfg.jetty.defaultHost = "127.0.0.1");
        api.exception(com.vendex.api.ApiAuthException.class,
                (e, ctx) -> ctx.status(e.status).json(java.util.Map.of("error", e.getMessage())));
        api.exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).json(java.util.Map.of("error", e.getMessage())));
        api.get("/stock-negativo", controller::stockNegativo);
        api.start(0); base = "http://127.0.0.1:" + api.port();
    }

    @AfterEach void cerrar() { if (api != null) api.stop(); SesionActual.cerrar(); }

    private HttpResponse<String> get(String query) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/stock-negativo" + query)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test void usuarioAutenticadoConsultaSoloSuSucursal() throws Exception {
        Usuario usuario = new Usuario(); usuario.setId(9); usuario.setRol("VENDEDOR");
        SesionActual.setUsuarioForTest(usuario); SesionActual.setSucursalIdForTest(7);
        var respuesta = get("");
        assertEquals(200, respuesta.statusCode());
        assertEquals(7, sucursalConsultada.get());
        assertTrue(respuesta.body().contains("TMP-1"));
        assertTrue(respuesta.body().contains("-3"));
    }

    @Test void clienteNoPuedeSolicitarOtraSucursalNiUnaSucursalMalformada() throws Exception {
        Usuario usuario = new Usuario(); usuario.setId(9); usuario.setRol("VENDEDOR");
        SesionActual.setUsuarioForTest(usuario); SesionActual.setSucursalIdForTest(7);
        assertEquals(403, get("?sucursalId=8").statusCode());
        assertEquals(400, get("?sucursalId=no-es-numero").statusCode());
        assertEquals(0, sucursalConsultada.get());
    }

    @Test void sesionAusente_noDevuelveStock() throws Exception {
        assertEquals(401, get("").statusCode());
        assertEquals(0, sucursalConsultada.get());
    }
}

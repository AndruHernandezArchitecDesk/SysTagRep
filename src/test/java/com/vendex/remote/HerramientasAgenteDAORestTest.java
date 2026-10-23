package com.vendex.remote;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class HerramientasAgenteDAORestTest {
    private HttpServer server;
    private final List<String> rutas = new CopyOnWriteArrayList<>();
    private HerramientasAgenteDAORest dao;

    @BeforeEach void iniciarApiFalsa() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/vetxi/herramientas", e -> {
            String ruta = e.getRequestURI().toString();
            rutas.add(ruta);
            String json;
            if (ruta.contains("/productos?")) json = """
                    [{"codigo":"FIL-1","descripcion":"FILTRO","stock":4,"precioVenta":15.50,"costoSinIva":9.00,"margenPct":41.9,"proveedor":"Proveedor A"}]
                    """;
            else if (ruta.contains("/compatibles?")) json = """
                    [{"codigo":"BUJ-18","descripcion":"BUJIA COROLLA","stock":3,"precioVenta":8.00,"costoSinIva":4.00,"margenPct":50.0,"proveedor":"Bodega"}]
                    """;
            else if (ruta.contains("/complementarios?")) json = """
                    [{"id":9,"codigo":"MANG-1","descripcion":"MANGUERA","cantidad":5,"precioVenta":4.5,"grupo":"MOTOR","marca":"A"}]
                    """;
            else if (ruta.contains("/clientes?")) json = "[{\"id\":7,\"nombre\":\"Ana\",\"identificacion\":\"099\"}]";
            else if (ruta.endsWith("/ventas/7")) json = "{\"facturas\":2,\"total\":80.00,\"ultimaFecha\":\"2026-10-22\"}";
            else if (ruta.endsWith("/saldo/7")) json = "{\"saldo\":12.30}";
            else if (ruta.contains("/mas-vendidos?")) json = "[{\"codigo\":\"A\",\"descripcion\":\"REPUESTO\",\"cantidad\":2,\"monto\":10.00,\"margenPct\":null}]";
            else json = "[]";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(200, bytes.length);
            try (var out = e.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        dao = new HerramientasAgenteDAORest(new RestClient("http://localhost:" + server.getAddress().getPort()));
    }

    @AfterEach void cerrarApiFalsa() { if (server != null) server.stop(0); }

    @Test void todasLasHerramientasRemotasDeserializanRespuestaYCodificanParametros() {
        var productos = dao.productos("filtro aceite", 1000);
        assertEquals("Proveedor A", productos.get(0).proveedor());
        assertEquals(20, queryLimit("productos"));
        assertEquals("MANG-1", dao.complementarios("FIL-1", 2).get(0).getCodigo());
        var compatibles = dao.compatibles("bujías", "Toyota", "Corolla", 2015, "1.8", 3);
        assertEquals("BUJ-18", compatibles.get(0).codigo());
        assertTrue(rutas.stream().anyMatch(p -> p.contains("descripcion=buj%C3%ADas")
                && p.contains("marca=Toyota") && p.contains("modelo=Corolla") && p.contains("anio=2015") && p.contains("motor=1.8")), rutas.toString());
        assertEquals("Ana", dao.clientes("Ana", 5).get(0).nombre());
        assertEquals(2, dao.ventas(7).facturas());
        assertEquals("12.30", dao.saldo(7).toPlainString());
        assertEquals("A", dao.masVendidos(5).get(0).codigo());
        assertTrue(dao.mejorMargen(5).isEmpty());
    }

    @Test void errorHttpNoSeConfundeConListaValidaVacia() {
        var sinServidor = new HerramientasAgenteDAORest(new RestClient("http://localhost:1"));
        assertThrows(IllegalStateException.class, () -> sinServidor.productos("filtro", 5));
    }

    private int queryLimit(String operation) {
        String ruta = rutas.stream().filter(p -> p.contains("/" + operation + "?")).findFirst().orElseThrow();
        String value = ruta.substring(ruta.lastIndexOf("limite=") + 7);
        return Integer.parseInt(value);
    }
}

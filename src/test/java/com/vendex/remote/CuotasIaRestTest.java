package com.vendex.remote;

import com.vendex.api.ApiAuthService;
import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.CuotaIA.*;
import com.vendex.model.VendexiConsumo;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CuotasIaRestTest {
    private Javalin app;
    private String tokenAnterior;
    private CuotaIADAORest pc1, pc2;
    private RestClient client;
    @BeforeEach void iniciar() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:cuotas_rest_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        CuotaIADAOPostgres.prepararEsquema(); DatabaseConnection.ensureVendexiSchema();
        var dao = new CuotaIADAOPostgres();
        app = Javalin.create(cfg -> cfg.jetty.defaultHost = "127.0.0.1");
        app.before(c -> {
            String token = c.header("Authorization");
            if (token == null) throw new io.javalin.http.UnauthorizedResponse();
            if (ApiAuthService.permisoRequerido(c.method().name(), c.path()) != null && !"Bearer admin".equals(token))
                throw new io.javalin.http.ForbiddenResponse();
        });
        app.post("/api/ia/cuotas/reservar", c -> c.json(dao.reservar(c.bodyAsClass(Reserva.class))));
        app.post("/api/ia/cuotas/finalizar", c -> { dao.finalizar(c.bodyAsClass(Fin.class)); c.json(Map.of("ok", true)); });
        app.get("/api/ia/cuotas/panel", c -> c.json(dao.panel()));
        app.post("/api/ia/cuotas/config", c -> { dao.guardar(c.bodyAsClass(Config.class)); c.json(Map.of("ok", true)); });
        var consumo = new com.vendex.controller.api.ConsumoIaApiController(new VendexiConsumoDAOPostgres());
        app.post("/api/ia/consumo/{accion}", consumo::ejecutar);
        app.start(0); tokenAnterior = RestClient.getToken(); RestClient.setToken("admin");
        client = new RestClient("http://127.0.0.1:" + app.port());
        pc1 = new CuotaIADAORest(client); pc2 = new CuotaIADAORest(client);
    }
    @AfterEach void cerrar() {
        if (app != null) app.stop(); RestClient.setToken(tokenAnterior); DatabaseConnection.resetToDefault();
    }
    @Test void configuracionYReservasCompartidasPorRest() throws Exception {
        String modelo = "gemini-2.5-flash";
        pc1.guardar(new Config(modelo, "Gratuito", true, 1, 0, 0, 1024, true, null, null));
        String id = UUID.randomUUID().toString();
        assertTrue(pc1.reservar(new Reserva(id, modelo, "web", 100)).permitido());
        assertFalse(pc2.reservar(new Reserva(UUID.randomUUID().toString(), modelo, "web", 100)).permitido());
        pc1.finalizar(new Fin(id, 200, 5, 100, 20, 30, 150, 0, ""));
        var panel = pc2.panel(); assertEquals(150, panel.intentos().get(0).total());
        assertEquals(4, panel.modelos().size());
        var cfg = panel.modelos().stream().filter(e -> e.config().modelo().equals(modelo)).findFirst().orElseThrow().config();
        pc2.guardar(cfg.quitarLimitesLocales());
        assertTrue(pc1.reservar(new Reserva(UUID.randomUUID().toString(), modelo, "web", 100000)).permitido());
        assertTrue(pc1.panel().modelos().stream().filter(e -> e.config().modelo().equals(modelo)).findFirst().orElseThrow().config().sinLimitesLocales());
    }
    @Test void usuarioOperativoPuedeReservar_peroNoAdministrarNiLeerPanel() throws Exception {
        RestClient.setToken("operador");
        assertTrue(pc1.reservar(new Reserva(UUID.randomUUID().toString(), "gemini-2.5-flash", "web", 10)).permitido());
        assertThrows(java.io.IOException.class, pc1::panel);
        assertThrows(java.io.IOException.class, () -> pc1.guardar(new Config("gemini-2.5-flash", "Pago", false, 0, 0, 0, 256, false, null, null)));
        RestClient.setToken(null);
        assertThrows(java.io.IOException.class, () -> pc1.reservar(new Reserva(UUID.randomUUID().toString(), "gemini-2.5-flash", "web", 10)));
        assertEquals("USUARIO_GESTIONAR", ApiAuthService.permisoRequerido("POST", "/api/ia/consumo/renovar"));
    }
    @Test void consumoHistoricoSeRegistraEnServidor_yFechasViajanPorRest() {
        var remoto = new VendexiConsumoDAORest(client);
        var c = new VendexiConsumo();
        c.setFecha(java.time.LocalDateTime.now()); c.setModulo("web"); c.setOperacion("investigacion");
        c.setModelo("gemini-2.5-flash"); c.setTokensEntrada(100); c.setTokensSalida(50); c.setCostoEstimado(java.math.BigDecimal.ZERO);
        remoto.insertar(c);
        var desde = c.getFecha().minusDays(1);
        assertEquals(150, remoto.sumarTokensDesde(desde));
        assertEquals(1, remoto.listarReciente(200).size());
        assertEquals("web", remoto.listarReciente(1).get(0).getModulo());
        remoto.actualizarInicioPeriodo(java.time.LocalDateTime.now());
        assertNotNull(remoto.obtenerInicioPeriodo());
    }
}

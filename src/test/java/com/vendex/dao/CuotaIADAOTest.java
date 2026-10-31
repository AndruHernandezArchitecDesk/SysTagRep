package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CuotaIA.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CuotaIADAOTest {
    private static final String MODELO = "gemini-2.5-flash";
    private final CuotaIADAOPostgres dao = new CuotaIADAOPostgres();
    @BeforeEach void iniciar() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:cuotas_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        CuotaIADAOPostgres.prepararEsquema();
    }
    @AfterEach void cerrar() { DatabaseConnection.resetToDefault(); }
    private Reserva reserva(long tokens) { return new Reserva(UUID.randomUUID().toString(), MODELO, "web.investigacion", tokens); }
    private void configurar(long rpm, long tpm, long rpd) throws Exception {
        dao.guardar(new Config(MODELO, "Gratuito", true, rpm, tpm, rpd, 512, true,
                new java.math.BigDecimal("1"), new java.math.BigDecimal("2")));
    }
    @Test void multiplesEquipos_reservanAtomicamente_yPersisteTrasRecrearDAO() throws Exception {
        configurar(3, 0, 0);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> tareas = new ArrayList<>();
            for (int i = 0; i < 12; i++) tareas.add(() -> new CuotaIADAOPostgres().reservar(reserva(10)).permitido());
            long permitidos = 0;
            for (Future<Boolean> f : pool.invokeAll(tareas)) if (f.get(10, TimeUnit.SECONDS)) permitidos++;
            assertEquals(3, permitidos);
            assertEquals(3, new CuotaIADAOPostgres().panel().intentos().size());
        } finally { pool.shutdownNow(); }
    }
    @Test void error429_pausaOtrosEquipos_yFinalizarEsIdempotente() throws Exception {
        var r = reserva(100); assertTrue(dao.reservar(r).permitido());
        dao.finalizar(new Fin(r.id(), 429, 50, 0, 0, 0, 0, 120000, "RPM"));
        assertFalse(new CuotaIADAOPostgres().reservar(reserva(100)).permitido());
        long pausa = dao.panel().modelos().stream().filter(e -> e.config().modelo().equals(MODELO)).findFirst().orElseThrow().pausaHasta();
        dao.finalizar(new Fin(r.id(), 200, 20, 100, 100, 100, 300, 0, ""));
        var panel = dao.panel(); assertEquals(429, panel.intentos().get(0).http());
        assertEquals(pausa, panel.modelos().stream().filter(e -> e.config().modelo().equals(MODELO)).findFirst().orElseThrow().pausaHasta());
        configurar(100, 0, 0);
        assertFalse(dao.reservar(reserva(1)).permitido(), "Guardar configuración no borra pausa");
    }
    @Test void limitesTokensYDia_cuentanReservasInclusoSinRespuesta() throws Exception {
        configurar(0, 100, 0);
        var r = reserva(60); assertTrue(dao.reservar(r).permitido());
        assertFalse(dao.reservar(reserva(41)).permitido());
        assertFalse(dao.reservar(r).permitido(), "Reserva duplicada no autoriza envío extra");
        configurar(0, 0, 1);
        assertFalse(dao.reservar(reserva(1)).permitido());
    }
    @Test void sinLimitesLocales_sigueMidiendoYRespetaLaEsperaDeGoogle() throws Exception {
        configurar(1, 10, 1);
        var primera = reserva(10); assertTrue(dao.reservar(primera).permitido());
        assertFalse(dao.reservar(reserva(100)).permitido());
        var anterior = dao.panel().modelos().stream().filter(e -> e.config().modelo().equals(MODELO)).findFirst().orElseThrow().config();
        dao.guardar(anterior.quitarLimitesLocales());
        assertTrue(new CuotaIADAOPostgres().reservar(reserva(1000000)).permitido());
        assertEquals(2, dao.panel().intentos().size(), "Sin límites también registra cada intento");
        long antes = System.currentTimeMillis();
        dao.finalizar(new Fin(primera.id(), 429, 1, 0, 0, 0, 0, 12000, "Cuota Google: RPM"));
        var estado = dao.panel().modelos().stream().filter(e -> e.config().modelo().equals(MODELO)).findFirst().orElseThrow();
        assertTrue(estado.config().sinLimitesLocales());
        assertTrue(estado.pausaHasta() >= antes + 11000);
        assertTrue(estado.pausaHasta() < antes + 30000, "No debe imponer 60 s si Google indicó 12 s");
        assertEquals(anterior.maxSalida(), estado.config().maxSalida());
        assertEquals(anterior.precioEntrada(), estado.config().precioEntrada());
        assertFalse(new CuotaIADAOPostgres().reservar(reserva(1)).permitido());
        dao.guardar(estado.config().quitarLimitesLocales());
        assertFalse(dao.reservar(reserva(1)).permitido(), "Quitar límites locales no borra la pausa de Google");
    }
    @Test void mideDesgloseYCosto_sinSumarTotalOtraVez() throws Exception {
        configurar(0, 0, 0);
        var r = reserva(500); dao.reservar(r);
        dao.finalizar(new Fin(r.id(), 200, 123, 100, 30, 70, 200, 0, ""));
        var i = dao.panel().intentos().get(0);
        assertEquals(200, i.total()); assertEquals(70, i.pensamiento());
        assertEquals(0, new java.math.BigDecimal("0.0003").compareTo(i.costo()));
        assertEquals(512, dao.reservar(reserva(10)).maxSalida());
    }

    @Test void pausaExpiradaYVentanaAnterior_permitenNuevaSolicitud() throws Exception {
        configurar(1, 0, 0);
        var r = reserva(10); dao.reservar(r);
        dao.finalizar(new Fin(r.id(), 429, 1, 0, 0, 0, 0, 60000, ""));
        assertFalse(dao.reservar(reserva(10)).permitido());
        // Simula paso del tiempo sin esperar ni modificar el reloj del equipo.
        try (var c = DatabaseConnection.getConnection(); var s = c.createStatement()) {
            s.executeUpdate("UPDATE ia_cuota SET pausa_hasta=1");
            s.executeUpdate("UPDATE ia_solicitud SET fecha=1");
        }
        assertTrue(new CuotaIADAOPostgres().reservar(reserva(10)).permitido());
    }
}

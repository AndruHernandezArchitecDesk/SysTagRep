package com.vendex.service;

import com.vendex.model.Inventario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Reproduce la cancelación del hilo contextual sin claves reales ni llamadas de red. */
class VendexiSugerenciaCancelacionTest {
    private final Logger logger = Logger.getLogger(VendexiSugerenciaService.class.getName());
    private final List<LogRecord> registros = new CopyOnWriteArrayList<>();
    private final AtomicInteger llamadas = new AtomicInteger();
    private final AtomicInteger consultasCapacidad = new AtomicInteger();
    private final VendexiConsumoService consumo = new VendexiConsumoService(null) {
        @Override public boolean hayCapacidadDisponible() { consultasCapacidad.incrementAndGet(); return true; }
    };
    private final Handler handler = new Handler() {
        @Override public void publish(LogRecord record) { registros.add(record); }
        @Override public void flush() {}
        @Override public void close() {}
    };
    private static final String JSON = "{\"sugerencias\":[{\"codigo\":\"COMP-2\",\"motivo\":\"Complemento real\"}]}";

    @FunctionalInterface private interface Proveedor { String responder() throws Exception; }

    @BeforeEach void preparar() {
        logger.addHandler(handler);
    }

    @AfterEach void limpiar() { logger.removeHandler(handler); }

    private VendexiSugerenciaService servicio(Proveedor proveedor) {
        return new VendexiSugerenciaService(consumo) {
            @Override public boolean esIaDisponible() { return true; }
            @Override protected String obtenerApiKey() { return "clave-de-prueba"; }
            @Override protected String llamarGemini(String prompt, String apiKey) throws Exception {
                assertEquals("clave-de-prueba", apiKey);
                llamadas.incrementAndGet();
                return proveedor.responder();
            }
        };
    }

    private Inventario producto(int id) {
        var i = new Inventario();
        i.setId(id); i.setCodigo("COMP-" + id); i.setDescripcion("FRENO " + id);
        i.setGrupo("FRENOS"); i.setCantidad(5); i.setPrecioVenta(BigDecimal.TEN);
        return i;
    }

    private List<VendexiSugerenciaService.Sugerencia> consultar(VendexiSugerenciaService svc) {
        return svc.sugerenciasComplementarias(producto(1), List.of(producto(2)), Set.of(1), null);
    }

    private List<LogRecord> advertencias() {
        return registros.stream().filter(r -> r.getLevel().intValue() >= Level.WARNING.intValue()).toList();
    }

    @Test void interrumpirConsultaEnCurso_noAdvierte_yPermiteLaSiguienteConsulta() throws Exception {
        var inicio = new CountDownLatch(1);
        var esperar = new CountDownLatch(1);
        var primera = new AtomicBoolean(true);
        var svc = servicio(() -> {
            if (primera.getAndSet(false)) { inicio.countDown(); esperar.await(); }
            return JSON;
        });
        var resultado = new AtomicReference<List<VendexiSugerenciaService.Sugerencia>>();
        var interrumpido = new AtomicBoolean();
        var error = new AtomicReference<Throwable>();
        Thread hilo = new Thread(() -> {
            try {
                resultado.set(consultar(svc));
                interrumpido.set(Thread.currentThread().isInterrupted());
            } catch (Throwable t) { error.set(t); }
        });
        hilo.setDaemon(true);
        hilo.start();
        try {
            assertTrue(inicio.await(10, TimeUnit.SECONDS));
            hilo.interrupt();
            hilo.join(10000);
            assertFalse(hilo.isAlive());
            assertNull(error.get());
            assertTrue(resultado.get().isEmpty());
            assertTrue(interrumpido.get(), "Debe conservar la señal de cancelación");
            assertTrue(advertencias().isEmpty(), "Una cancelación normal no debe generar WARNING");
            var siguiente = consultar(svc);
            assertEquals(1, siguiente.size());
            assertEquals("COMP-2", siguiente.get(0).producto.getCodigo());
            assertEquals(2, llamadas.get());
        } finally { esperar.countDown(); hilo.interrupt(); }
    }

    @Test void hiloYaCancelado_noConsultaProveedorNiCapacidad() throws Exception {
        var svc = servicio(() -> { fail("No llamar al proveedor tras cancelar"); return JSON; });
        var error = new AtomicReference<Throwable>();
        Thread hilo = new Thread(() -> {
            try {
                Thread.currentThread().interrupt();
                assertTrue(consultar(svc).isEmpty());
                assertTrue(Thread.currentThread().isInterrupted());
            } catch (Throwable t) { error.set(t); }
        });
        hilo.start(); hilo.join(10000);
        assertFalse(hilo.isAlive());
        assertNull(error.get());
        assertEquals(0, llamadas.get());
        assertEquals(0, consultasCapacidad.get());
        assertTrue(advertencias().isEmpty());
    }

    @Test void cancelacionSinMensaje_noSeRegistraComoFalloDeIa() {
        var svc = servicio(() -> { throw new java.util.concurrent.CancellationException(); });
        assertTrue(consultar(svc).isEmpty());
        assertTrue(advertencias().isEmpty());
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test void cancelacionDuranteRespuesta_descartaElResultado_yConservaLaInterrupcion() {
        var svc = servicio(() -> { Thread.currentThread().interrupt(); return JSON; });
        try {
            assertTrue(consultar(svc).isEmpty());
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(advertencias().isEmpty());
        } finally { Thread.interrupted(); }
    }

    @Test void falloRealSinMensaje_registraTipoYCausa_enLugarDeNull() {
        var fallo = new IllegalStateException();
        var svc = servicio(() -> { throw fallo; });
        assertTrue(consultar(svc).isEmpty());
        assertEquals(1, advertencias().size());
        var registro = advertencias().get(0);
        assertTrue(registro.getMessage().contains("Vetxi"));
        assertTrue(registro.getMessage().contains("IllegalStateException"));
        assertFalse(registro.getMessage().contains(": null"));
        assertSame(fallo, registro.getThrown());
    }

    @Test void falloRealConMensaje_conservaElDetalleTecnico() {
        var fallo = new java.io.IOException("Sin conexión de prueba");
        var svc = servicio(() -> { throw fallo; });
        assertTrue(consultar(svc).isEmpty());
        assertEquals(1, advertencias().size());
        assertTrue(advertencias().get(0).getMessage().contains("IOException"));
        assertTrue(advertencias().get(0).getMessage().contains("Sin conexión de prueba"));
        assertSame(fallo, advertencias().get(0).getThrown());
    }
}

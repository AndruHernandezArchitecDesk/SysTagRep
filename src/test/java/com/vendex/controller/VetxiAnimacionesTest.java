package com.vendex.controller;

import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VetxiAnimacionesTest {
    @BeforeAll static void iniciarFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @FunctionalInterface private interface Accion { void ejecutar() throws Exception; }
    private void fx(Accion accion) throws Exception {
        var error = new AtomicReference<Throwable>();
        var fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { accion.ejecutar(); } catch (Throwable t) { error.set(t); } finally { fin.countDown(); }
        });
        assertTrue(fin.await(10, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Animación de Vetxi inválida", error.get());
    }

    private static final class Escenario implements AutoCloseable {
        final AtomicLong reloj = new AtomicLong();
        final AtomicBoolean libre = new AtomicBoolean(true);
        final Group dibujo;
        final Region mascota = new Pane();
        final Pane superficie = new Pane();
        final VetxiAnimaciones motor;
        Escenario() throws Exception {
            dibujo = FXMLLoader.load(getClass().getResource("/view/Capibara.fxml"));
            superficie.resize(900, 650); mascota.resize(104, 108); mascota.relocate(200, 200);
            motor = new VetxiAnimaciones(dibujo, mascota, superficie, libre::get, mascota::relocate, reloj::get);
            motor.setActivo(true);
        }
        Object campo(String nombre) throws Exception {
            var f = VetxiAnimaciones.class.getDeclaredField(nombre); f.setAccessible(true); return f.get(motor);
        }
        void segundos(long s) { reloj.set(java.time.Duration.ofSeconds(s).toNanos()); }
        @Override public void close() { motor.setActivo(false); }
    }

    @Test void modoDocumento_poneLentesYMueveLapiz_yLosRetiraAlSalir() throws Exception {
        fx(() -> {
            try (var e = new Escenario()) {
                e.motor.setTrabajando(true);
                assertEquals(VetxiAnimaciones.Estado.ANOTANDO, e.motor.estado());
                assertTrue(e.dibujo.lookup("#lentes").isVisible());
                assertTrue(e.dibujo.lookup("#libreta").isVisible());
                Animation escritura = (Animation) e.campo("escritura");
                assertEquals(Animation.Status.RUNNING, escritura.getStatus());
                escritura.jumpTo(Duration.millis(225));
                var lapiz = e.dibujo.lookup("#lapiz");
                assertTrue(Math.abs(lapiz.getTranslateX()) > 0.1 || Math.abs(lapiz.getRotate()) > 0.1);
                e.motor.rascar();
                assertEquals(VetxiAnimaciones.Estado.ANOTANDO, e.motor.estado(), "No mezclar rascado y escritura");
                e.motor.setTrabajando(false);
                assertFalse(e.dibujo.lookup("#lentes").isVisible());
                assertFalse(e.dibujo.lookup("#libreta").isVisible());
                assertFalse(lapiz.isVisible());
            }
        });
    }

    @Test void rascado_muevePataYMuestraPulgas_ySeInterrumpeConActividad() throws Exception {
        fx(() -> {
            try (var e = new Escenario()) {
                e.motor.rascar();
                assertEquals(VetxiAnimaciones.Estado.RASCANDO, e.motor.estado());
                assertTrue(e.dibujo.lookup("#pulgas").isVisible());
                assertFalse(e.dibujo.lookup("#pataTrasera").isVisible());
                ((Animation) e.campo("accion")).jumpTo(Duration.millis(405));
                assertNotEquals(0, e.dibujo.lookup("#pataRascado").getRotate());
                e.motor.registrarActividad();
                assertEquals(VetxiAnimaciones.Estado.REPOSO, e.motor.estado());
                assertFalse(e.dibujo.lookup("#pulgas").isVisible());
                assertFalse(e.dibujo.lookup("#pataRascado").isVisible());
                assertTrue(e.dibujo.lookup("#pataTrasera").isVisible());
                assertEquals(0, e.dibujo.getRotate());
            }
        });
    }

    @Test void paseo_soloTrasUnMinuto_yActividadReanudaNotas() throws Exception {
        fx(() -> {
            try (var e = new Escenario()) {
                e.motor.setTrabajando(true);
                e.segundos(59); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.ANOTANDO, e.motor.estado());
                e.segundos(60); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.PASEANDO, e.motor.estado());
                assertFalse(e.dibujo.lookup("#lentes").isVisible());
                e.motor.registrarActividad();
                assertEquals(VetxiAnimaciones.Estado.ANOTANDO, e.motor.estado());
                assertTrue(e.dibujo.lookup("#lentes").isVisible());
                e.segundos(119); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.ANOTANDO, e.motor.estado());
            }
        });
    }

    @Test void paseo_mueveDentroDeLaVentana_ySeDetieneSinSaltar() throws Exception {
        fx(() -> {
            try (var e = new Escenario()) {
                e.segundos(61); e.motor.comprobarInactividad();
                Animation paseo = (Animation) e.campo("paseo");
                paseo.jumpTo(paseo.getTotalDuration().divide(2));
                double x = e.mascota.getLayoutX(), y = e.mascota.getLayoutY();
                assertTrue(Math.hypot(x - 200, y - 200) > 1);
                assertTrue(x >= 0 && x + 104 <= 900);
                assertTrue(y >= 0 && y + 108 <= 650);
                e.motor.registrarActividad();
                assertEquals(x, e.mascota.getLayoutX(), 0.01);
                assertEquals(y, e.mascota.getLayoutY(), 0.01);
                assertEquals(Animation.Status.STOPPED, paseo.getStatus());
            }
        });
    }

    @Test void chatAvisosYArrastre_bloqueanPaseo() throws Exception {
        fx(() -> {
            try (var e = new Escenario()) {
                e.libre.set(false);
                e.segundos(90); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.REPOSO, e.motor.estado());
                e.libre.set(true); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.PASEANDO, e.motor.estado());
                e.libre.set(false); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.REPOSO, e.motor.estado());
                assertNull(e.campo("paseo"));
            }
        });
    }

    @Test void ventanaInactiva_detieneTodo_yAlVolverReiniciaElTiempoDeEspera() throws Exception {
        fx(() -> {
            try (var e = new Escenario()) {
                e.motor.setTrabajando(true);
                e.segundos(61); e.motor.comprobarInactividad();
                e.motor.setActivo(false);
                assertNull(e.campo("paseo"));
                assertNull(e.campo("escritura"));
                assertEquals(Animation.Status.STOPPED, ((Animation) e.campo("vigilancia")).getStatus());
                e.segundos(1000); e.motor.comprobarInactividad();
                assertNotEquals(VetxiAnimaciones.Estado.PASEANDO, e.motor.estado());
                e.motor.setActivo(true); e.motor.comprobarInactividad();
                assertEquals(VetxiAnimaciones.Estado.ANOTANDO, e.motor.estado());
            }
        });
    }
}

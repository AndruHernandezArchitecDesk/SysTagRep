package com.vendex.controller;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test: carga los FXML modificados con su controller real para detectar
 * fx:id/onAction inexistentes o errores de contexto JavaFX en tiempo de ejecución.
 */
class FxmlSmokeTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // toolkit ya inicializado
        }
        Platform.setImplicitExit(false);
    }

    private Object cargar(String ruta) throws Exception {
        AtomicReference<Object> ctrl = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(ruta));
                loader.load();
                ctrl.set(loader.getController());
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout cargando " + ruta);
        if (err.get() != null) {
            if (err.get() instanceof Exception e) throw e;
            throw new RuntimeException(err.get());
        }
        return ctrl.get();
    }

    @Test
    void cargaInventarioViewConFiltrosVehiculo() throws Exception {
        Object controller = cargar("/view/InventarioView.fxml");
        assertNotNull(controller, "controller InventarioController");
        assertNotNull(controller.getClass().getDeclaredField("cmbVehMarca"), "campo cmbVehMarca");
        assertNotNull(controller.getClass().getDeclaredField("cmbVehModelo"), "campo cmbVehModelo");
        assertNotNull(controller.getClass().getDeclaredField("cmbVehAnio"), "campo cmbVehAnio");
    }

    @Test
    void cargaChatWidgetConFiltrosVehiculo() throws Exception {
        Object controller = cargar("/view/ChatWidget.fxml");
        assertNotNull(controller, "controller ChatWidgetController");
        assertNotNull(controller.getClass().getDeclaredField("cmbMarca"), "campo cmbMarca");
        assertNotNull(controller.getClass().getDeclaredField("cmbModelo"), "campo cmbModelo");
        assertNotNull(controller.getClass().getDeclaredField("cmbAnio"), "campo cmbAnio");
    }

    @Test
    void cargaNotaVentaViewConFiltrosVehiculo() throws Exception {
        Object controller = cargar("/view/NotaVentaView.fxml");
        assertNotNull(controller, "controller NotaVentaController");
        assertNotNull(controller.getClass().getDeclaredField("cmbProdVehMarca"), "campo cmbProdVehMarca");
        assertNotNull(controller.getClass().getDeclaredField("cmbProdVehModelo"), "campo cmbProdVehModelo");
        assertNotNull(controller.getClass().getDeclaredField("cmbProdVehAnio"), "campo cmbProdVehAnio");
    }
}

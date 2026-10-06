package com.vendex.controller;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test: carga los FXML modificados con su controller real para detectar
 * fx:id/onAction inexistentes o errores de contexto JavaFX en tiempo de ejecución.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @Order(2)
    void cargaInventarioViewConFiltrosVehiculo() throws Exception {
        Object controller = cargar("/view/InventarioView.fxml");
        assertNotNull(controller, "controller InventarioController");
        assertNotNull(controller.getClass().getDeclaredField("cmbVehMarca"), "campo cmbVehMarca");
        assertNotNull(controller.getClass().getDeclaredField("cmbVehModelo"), "campo cmbVehModelo");
        assertNotNull(controller.getClass().getDeclaredField("cmbVehAnio"), "campo cmbVehAnio");
    }

    @Test
    @Order(3)
    void cargaChatWidgetConFiltrosVehiculo() throws Exception {
        Object controller = cargar("/view/ChatWidget.fxml");
        assertNotNull(controller, "controller ChatWidgetController");
        assertNotNull(controller.getClass().getDeclaredField("cmbMarca"), "campo cmbMarca");
        assertNotNull(controller.getClass().getDeclaredField("cmbModelo"), "campo cmbModelo");
        assertNotNull(controller.getClass().getDeclaredField("cmbAnio"), "campo cmbAnio");
    }

    @Test
    @Order(4)
    void cargaNotaVentaViewConFiltrosVehiculo() throws Exception {
        Object controller = cargar("/view/NotaVentaView.fxml");
        assertNotNull(controller, "controller NotaVentaController");
        assertNotNull(controller.getClass().getDeclaredField("cmbProdVehMarca"), "campo cmbProdVehMarca");
        assertNotNull(controller.getClass().getDeclaredField("cmbProdVehModelo"), "campo cmbProdVehModelo");
        assertNotNull(controller.getClass().getDeclaredField("cmbProdVehAnio"), "campo cmbProdVehAnio");
    }

    @Test
    @Order(1)
    void cargaMainViewConQuickAccessBar() throws Exception {
        Object controller = cargar("/view/MainView.fxml");
        assertNotNull(controller, "controller MainController");
        assertNotNull(controller.getClass().getDeclaredField("quickAccessBar"), "campo quickAccessBar");
    }

    @Test
    @Order(6)
    void recolectaBotonesSidebarSinLookup() throws Exception {
        // Sintético y rápido: no carga FXML ni toca BD.
        javafx.scene.layout.VBox raiz = new javafx.scene.layout.VBox();
        javafx.scene.control.Button arrastrable = new javafx.scene.control.Button("Facturas de Venta");
        arrastrable.getStyleClass().add("nav-item");
        javafx.scene.control.Button grupo = new javafx.scene.control.Button("Crédito");
        grupo.getStyleClass().add("nav-group");
        javafx.scene.control.Button noArrastrable = new javafx.scene.control.Button("Cerrar Sesión");
        noArrastrable.getStyleClass().add("nav-item");
        javafx.scene.control.Button sinEstilo = new javafx.scene.control.Button("Facturas de Venta");
        javafx.scene.layout.VBox anidado = new javafx.scene.layout.VBox(grupo, noArrastrable);
        raiz.getChildren().addAll(arrastrable, sinEstilo, anidado);
        java.util.List<javafx.scene.control.Button> botones =
                MainController.recolectarBotonesSidebar(raiz);
        org.junit.jupiter.api.Assertions.assertEquals(2, botones.size(), "solo arrastrables con estilo");
        org.junit.jupiter.api.Assertions.assertTrue(botones.contains(arrastrable));
        org.junit.jupiter.api.Assertions.assertTrue(botones.contains(grupo));
    }

    @Test
    @Order(5)
    void catalogoVistasResuelveRutasArrastrables() {
        org.junit.jupiter.api.Assertions.assertEquals("/view/FacturaView.fxml",
                com.vendex.util.CatalogoVistas.rutaDe("Facturas de Venta"));
        org.junit.jupiter.api.Assertions.assertEquals("/view/NotaVentaView.fxml",
                com.vendex.util.CatalogoVistas.rutaDe("Proformas"));
        org.junit.jupiter.api.Assertions.assertNull(com.vendex.util.CatalogoVistas.rutaDe("Cerrar Sesión"));
        org.junit.jupiter.api.Assertions.assertNull(com.vendex.util.CatalogoVistas.rutaDe("Cola Offline"));
    }
}

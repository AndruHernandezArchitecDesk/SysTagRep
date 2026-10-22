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
        if (!latch.await(120, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout cargando " + ruta);
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
    @Order(7)
    void cargaSeguimientoSriViewConColumnaAcciones() throws Exception {
        Object controller = cargar("/view/SeguimientoSriView.fxml");
        assertNotNull(controller, "controller SeguimientoSriController");
        assertNotNull(controller.getClass().getDeclaredField("colAcciones"), "campo colAcciones");
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("colAcciones");
        f.setAccessible(true);
        assertNotNull(f.get(controller), "colAcciones cableada desde FXML");
    }

    @Test
    @Order(1)
    void cargaMainViewConQuickAccessBar() throws Exception {
        // MainView exige sesion con permiso (igual que en produccion tras login);
        // sin esto el gate muestra un modal y la carga se cuelga. Se cierra al final
        // para no fugar sesion a otros tests del mismo JVM.
        com.vendex.model.Usuario u = new com.vendex.model.Usuario();
        u.setId(1);
        u.setRol("ADMINISTRADOR");
        com.vendex.util.SesionActual.setUsuarioForTest(u);
        com.vendex.util.SesionActual.setPermisosForTest(java.util.Set.of("REPORTE_VER"));
        try {
            Object controller = cargar("/view/MainView.fxml");
            assertNotNull(controller, "controller MainController");
            assertNotNull(controller.getClass().getDeclaredField("quickAccessBar"), "campo quickAccessBar");
            java.lang.reflect.Field asistente = controller.getClass().getDeclaredField("asistenteController");
            asistente.setAccessible(true);
            assertNotNull(asistente.get(controller), "asistente único inyectado desde fx:include");
        } finally {
            com.vendex.util.SesionActual.cerrar();
        }
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
        org.junit.jupiter.api.Assertions.assertEquals("/view/EmpresaView.fxml",
                com.vendex.util.CatalogoVistas.rutaDe("Mi Empresa"));
        org.junit.jupiter.api.Assertions.assertNull(com.vendex.util.CatalogoVistas.rutaDe("Cerrar Sesión"));
        org.junit.jupiter.api.Assertions.assertNull(com.vendex.util.CatalogoVistas.rutaDe("Cola Offline"));
    }

        @Test
    @Order(8)
    void cargaMiEmpresaViewConCamposSriYLogo() throws Exception {

        Object controller = cargar("/view/EmpresaView.fxml");
        assertNotNull(controller, "controller EmpresaController");
        for (String campo : new String[]{"txtRuc", "txtRazonSocial", "txtTelefono", "cmbObligado",
                "txtAgenteRetencion", "imgLogo", "btnGuardar", "btnCancelar"}) {
            java.lang.reflect.Field f = controller.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            assertNotNull(f.get(controller), campo + " cableado desde FXML");
        }
    }

    @Test
    @Order(9)
    void cargaFacturaViewSinErroresExpresion() throws Exception {
        // Regresion: promptText="$ ..." rompia el FXMLLoader (Invalid path en KeyPath).
        Object controller = cargar("/view/FacturaView.fxml");
        assertNotNull(controller, "controller FacturaController");
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("txtDescuento");
        f.setAccessible(true);
        assertNotNull(f.get(controller), "txtDescuento cableado desde FXML");
    }

    @Test
    @Order(10)
    void cargaNotaVentaViewSinErroresExpresion() throws Exception {
        Object controller = cargar("/view/NotaVentaView.fxml");
        assertNotNull(controller, "controller NotaVentaController");
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("txtDescuento");
        f.setAccessible(true);
        assertNotNull(f.get(controller), "txtDescuento cableado desde FXML");
    }

    @Test
    @Order(11)
    void cargaCajaViewConColumnaAccionesHistorial() throws Exception {
        Object controller = cargar("/view/CajaView.fxml");
        assertNotNull(controller, "controller CajaController");
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("colHistAcciones");
        f.setAccessible(true);
        assertNotNull(f.get(controller), "colHistAcciones cableada desde FXML");
    }

    @Test
    @Order(12)
    void cargaPrestamoViewConTooltipsYEstilo() throws Exception {
        Object controller = cargar("/view/PrestamoView.fxml");
        assertNotNull(controller, "controller PrestamoController");
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("tblPrestamos");
        f.setAccessible(true);
        assertNotNull(f.get(controller), "tblPrestamos cableada desde FXML");
        java.lang.reflect.Field f2 = controller.getClass().getDeclaredField("lblResumen");
        f2.setAccessible(true);
        assertNotNull(f2.get(controller), "lblResumen cableada desde FXML");
        java.lang.reflect.Field f3 = controller.getClass().getDeclaredField("tblIngresos");
        f3.setAccessible(true);
        assertNotNull(f3.get(controller), "tblIngresos cableada desde FXML");
        java.lang.reflect.Field f4 = controller.getClass().getDeclaredField("colIngMonto");
        f4.setAccessible(true);
        assertNotNull(f4.get(controller), "colIngMonto cableada desde FXML");
        java.lang.reflect.Field f5 = controller.getClass().getDeclaredField("paneIngresos");
        f5.setAccessible(true);
        assertNotNull(f5.get(controller), "paneIngresos cableada desde FXML");
        java.lang.reflect.Field f6 = controller.getClass().getDeclaredField("btnToggleIngresos");
        f6.setAccessible(true);
        assertNotNull(f6.get(controller), "btnToggleIngresos cableada desde FXML");
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var btn = (javafx.scene.control.Button) f6.get(controller);
                var pane = (javafx.scene.control.TitledPane) f5.get(controller);
                org.junit.jupiter.api.Assertions.assertEquals("Ingresos recientes", btn.getText(),
                        "rótulo del botón de ingresos recientes");
                org.junit.jupiter.api.Assertions.assertNotNull(btn.getTooltip(),
                        "tooltip instalado desde FXML (patrón <tooltip>)");
                org.junit.jupiter.api.Assertions.assertFalse(btn.getText().contains("javafx.scene"),
                        "el botón no debe mostrar la clase del tooltip");
                btn.fire();
                org.junit.jupiter.api.Assertions.assertFalse(pane.isVisible(), "panel oculto tras toggle");
                org.junit.jupiter.api.Assertions.assertFalse(pane.isManaged(), "panel no ocupa espacio tras toggle");
                btn.fire();
                org.junit.jupiter.api.Assertions.assertTrue(pane.isVisible(), "panel visible tras segundo toggle");
                org.junit.jupiter.api.Assertions.assertTrue(pane.isManaged(), "panel ocupa espacio tras segundo toggle");
            } catch (Throwable t) { err.set(t); } finally { fin.countDown(); }
        });
        org.junit.jupiter.api.Assertions.assertTrue(fin.await(60, TimeUnit.SECONDS));
        if (err.get() != null) throw new AssertionError("Toggle de ingresos recientes inválido", err.get());
    }

    @Test
    @Order(16)
    void factura_productoTemporalSeAgregaAlBorradorConPrecioIvaIncluido() throws Exception {
        var controller = (FacturaController) cargar("/view/FacturaView.fxml");
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var boton = FacturaController.class.getDeclaredField("btnProductoTemporal"); boton.setAccessible(true);
                var accion = (javafx.scene.control.Button) boton.get(controller);
                assertNotNull(accion.getParent(), "Botón montado junto al buscador");
                String codigo = "TMP-" + java.util.UUID.randomUUID().toString();
                var d = controller.agregarTemporalFactura(codigo, "REPUESTO DE PRUEBA", 3, new java.math.BigDecimal("11.50"));
                org.junit.jupiter.api.Assertions.assertEquals(0, d.getInventarioId());
                var contexto = controller.contextoVendexi();
                org.junit.jupiter.api.Assertions.assertEquals(1, contexto.lineas.size());
                org.junit.jupiter.api.Assertions.assertEquals(3, contexto.lineas.get(0).cantidad);
                org.junit.jupiter.api.Assertions.assertFalse(contexto.lineas.get(0).codigo.contains("TEMPORAL"));
                var total = FacturaController.class.getDeclaredField("lblTotal"); total.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertEquals("34.50", ((javafx.scene.control.Label) total.get(controller)).getText());
                org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                        () -> controller.agregarTemporalFactura(codigo.toLowerCase(), "OTRO", 1, java.math.BigDecimal.TEN));
            } catch (Throwable t) { error.set(t); } finally { fin.countDown(); }
        });
        org.junit.jupiter.api.Assertions.assertTrue(fin.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Producto temporal de factura inválido", error.get());
    }

    /** Simula el commit del doble clic en la celda de precio unitario (TableCell.commitEdit). */
    private static <S> void dispararPrecioCommit(javafx.scene.control.TableColumn<S, java.math.BigDecimal> col,
            javafx.scene.control.TableView<S> tabla, java.math.BigDecimal nuevo) {
        var posicion = new javafx.scene.control.TablePosition<S, java.math.BigDecimal>(tabla, 0, col);
        var evento = new javafx.scene.control.TableColumn.CellEditEvent<S, java.math.BigDecimal>(tabla, posicion,
                javafx.scene.control.TableColumn.<S, java.math.BigDecimal>editCommitEvent(), nuevo);
        col.getOnEditCommit().handle(evento);
    }

    @Test
    @Order(16)
    void factura_precioUnitarioEditableConDobleClicRecalculaTotales() throws Exception {
        var controller = (FacturaController) cargar("/view/FacturaView.fxml");
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var converter = new com.vendex.util.PrecioCeldaConverter();
                org.junit.jupiter.api.Assertions.assertNull(converter.fromString("abc"),
                        "texto no numérico no lanza excepción: devuelve null");
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("11.50")
                        .compareTo(converter.fromString("11,50")), "acepta coma decimal");
                org.junit.jupiter.api.Assertions.assertEquals("12.00", converter.toString(new java.math.BigDecimal("12")),
                        "la celda muestra 2 decimales");
                org.junit.jupiter.api.Assertions.assertTrue(
                        com.vendex.service.ProductoTemporalFactura.precioValido(new java.math.BigDecimal("10.500")));
                org.junit.jupiter.api.Assertions.assertFalse(
                        com.vendex.service.ProductoTemporalFactura.precioValido(new java.math.BigDecimal("10.555")));
                org.junit.jupiter.api.Assertions.assertFalse(
                        com.vendex.service.ProductoTemporalFactura.precioValido(java.math.BigDecimal.ZERO));

                String codigo = "TMP-" + java.util.UUID.randomUUID().toString();
                var d = controller.agregarTemporalFactura(codigo, "REPUESTO DE PRUEBA", 3, new java.math.BigDecimal("11.50"));

                var fTabla = FacturaController.class.getDeclaredField("tblDetalle"); fTabla.setAccessible(true);
                var tabla = (javafx.scene.control.TableView<com.vendex.model.FacturaDetalle>) fTabla.get(controller);
                var fCol = FacturaController.class.getDeclaredField("colPrecioUnitario"); fCol.setAccessible(true);
                var col = (javafx.scene.control.TableColumn<com.vendex.model.FacturaDetalle, java.math.BigDecimal>) fCol.get(controller);
                var fCant = FacturaController.class.getDeclaredField("colCantidad"); fCant.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertTrue(tabla.isEditable(), "tabla habilitada para edición");
                org.junit.jupiter.api.Assertions.assertNotNull(col.getOnEditCommit(), "editor de precio unitario cableado");
                org.junit.jupiter.api.Assertions.assertFalse(((javafx.scene.control.TableColumn<?, ?>) fCant.get(controller)).isEditable(),
                        "la columna cantidad no entra en modo edición");

                // 11.50 → 12.00: base 31.29, IVA 4.69, total 35.98 (Convención C, igual que el XML)
                dispararPrecioCommit(col, tabla, new java.math.BigDecimal("12.00"));
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("12.00").compareTo(d.getPrecioUnitario()),
                        "precio unitario actualizado");
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("36.00").compareTo(d.getPrecioTotal()),
                        "precio total = 3 × 12.00");
                var fTotal = FacturaController.class.getDeclaredField("lblTotal"); fTotal.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertEquals("35.98",
                        ((javafx.scene.control.Label) fTotal.get(controller)).getText(), "total recalculado");
                var fIva = FacturaController.class.getDeclaredField("lblIva"); fIva.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertEquals("4.69",
                        ((javafx.scene.control.Label) fIva.get(controller)).getText(), "IVA 15% sobre la nueva base");

                // Precios inválidos: revertir silencioso (el modelo no cambia)
                dispararPrecioCommit(col, tabla, java.math.BigDecimal.ZERO);
                dispararPrecioCommit(col, tabla, null);
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("12.00").compareTo(d.getPrecioUnitario()),
                        "precio inválido no modifica el modelo");
            } catch (Throwable t) { error.set(t); } finally { fin.countDown(); }
        });
        org.junit.jupiter.api.Assertions.assertTrue(fin.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Edición de precio unitario en factura inválida", error.get());
    }

    @Test
    @Order(16)
    void proforma_precioUnitarioEditableConDobleClicRecalculaTotales() throws Exception {
        var controller = (NotaVentaController) cargar("/view/NotaVentaView.fxml");
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var fItems = NotaVentaController.class.getDeclaredField("itemsDetalle"); fItems.setAccessible(true);
                @SuppressWarnings("unchecked")
                var items = (javafx.collections.ObservableList<com.vendex.model.DetalleVenta>) fItems.get(controller);
                var d = new com.vendex.model.DetalleVenta(1, "P-1", "PIEZA DE PRUEBA", 2, new java.math.BigDecimal("10.00"));
                items.add(d);

                var fTabla = NotaVentaController.class.getDeclaredField("tblDetalle"); fTabla.setAccessible(true);
                var tabla = (javafx.scene.control.TableView<com.vendex.model.DetalleVenta>) fTabla.get(controller);
                var fCol = NotaVentaController.class.getDeclaredField("colPrecioUnitario"); fCol.setAccessible(true);
                var col = (javafx.scene.control.TableColumn<com.vendex.model.DetalleVenta, java.math.BigDecimal>) fCol.get(controller);
                var fCant = NotaVentaController.class.getDeclaredField("colCantidad"); fCant.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertTrue(tabla.isEditable(), "tabla habilitada para edición");
                org.junit.jupiter.api.Assertions.assertNotNull(col.getOnEditCommit(), "editor de precio unitario cableado");
                org.junit.jupiter.api.Assertions.assertFalse(((javafx.scene.control.TableColumn<?, ?>) fCant.get(controller)).isEditable(),
                        "la columna cantidad no entra en modo edición");

                // 10.00 → 12.50 sin IVA: subtotal y total = 25.00
                dispararPrecioCommit(col, tabla, new java.math.BigDecimal("12.50"));
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("12.50").compareTo(d.getPrecioUnitario()),
                        "precio unitario actualizado");
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("25.00").compareTo(d.getPrecioTotal()),
                        "precio total = 2 × 12.50");
                var fSub = NotaVentaController.class.getDeclaredField("lblSubtotal"); fSub.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertEquals("25.00",
                        ((javafx.scene.control.Label) fSub.get(controller)).getText(), "subtotal recalculado");
                var fTotal = NotaVentaController.class.getDeclaredField("lblTotal"); fTotal.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertEquals("25.00",
                        ((javafx.scene.control.Label) fTotal.get(controller)).getText(), "total sin IVA");

                // Con IVA 15%: iva 3.75, total 28.75
                var fChk = NotaVentaController.class.getDeclaredField("chkIva"); fChk.setAccessible(true);
                var chk = (javafx.scene.control.CheckBox) fChk.get(controller);
                chk.setSelected(true);
                chk.getOnAction().handle(new javafx.event.ActionEvent());
                var fIva = NotaVentaController.class.getDeclaredField("lblIva"); fIva.setAccessible(true);
                org.junit.jupiter.api.Assertions.assertEquals("3.75",
                        ((javafx.scene.control.Label) fIva.get(controller)).getText(), "IVA 15% recalculado");
                org.junit.jupiter.api.Assertions.assertEquals("28.75",
                        ((javafx.scene.control.Label) fTotal.get(controller)).getText(), "total con IVA");

                // Precio inválido: revertir silencioso
                dispararPrecioCommit(col, tabla, java.math.BigDecimal.ZERO);
                dispararPrecioCommit(col, tabla, null);
                org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("12.50").compareTo(d.getPrecioUnitario()),
                        "precio inválido no modifica el modelo");
            } catch (Throwable t) { error.set(t); } finally { fin.countDown(); }
        });
        org.junit.jupiter.api.Assertions.assertTrue(fin.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Edición de precio unitario en proforma inválida", error.get());
    }

    @Test
    @Order(16)
    void ingresoMercaderia_montaPanelStockNegativoEnLateralDerecho() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        AtomicReference<javafx.stage.Stage> stageRef = new AtomicReference<>();
        AtomicReference<Object> controllerRef = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/IngresoMercaderiaView.fxml"));
                javafx.scene.Parent root = loader.load();
                controllerRef.set(loader.getController());
                var stage = new javafx.stage.Stage(); stage.setScene(new javafx.scene.Scene(root, 1100, 720));
                stageRef.set(stage); stage.show();
            } catch (Throwable t) { error.set(t); fin.countDown(); return; }
            Platform.runLater(() -> Platform.runLater(() -> {
                try {
                    Object controller = controllerRef.get();
                    stageRef.get().getScene().getRoot().applyCss();
                    stageRef.get().getScene().getRoot().layout();
                    java.lang.reflect.Field f = controller.getClass().getDeclaredField("stockNegativo");
                    f.setAccessible(true);
                    StockNegativoPane panel = (StockNegativoPane) f.get(controller);
                    assertNotNull(panel, "panel instanciado");
                    java.lang.reflect.Field div = controller.getClass().getDeclaredField("divisorioStock"); div.setAccessible(true);
                    var divisor = (javafx.scene.control.SplitPane) div.get(controller);
                    org.junit.jupiter.api.Assertions.assertSame(panel, divisor.getItems().get(1), "panel a la derecha del formulario");
                    org.junit.jupiter.api.Assertions.assertTrue(divisor.getDividerPositions()[0] > 0.5);
                    controller.getClass().getMethod("setCerrarAlGuardar", boolean.class).invoke(controller, true);
                    org.junit.jupiter.api.Assertions.assertEquals(1, divisor.getItems().size(), "Edición modal no muestra el panel global");
                    controller.getClass().getMethod("setCerrarAlGuardar", boolean.class).invoke(controller, false);
                    org.junit.jupiter.api.Assertions.assertEquals(2, divisor.getItems().size(), "Formulario principal recupera el panel");
                    java.lang.reflect.Field btnF = controller.getClass().getDeclaredField("btnTogglePanelStock");
                    btnF.setAccessible(true);
                    var btnToggle = (javafx.scene.control.Button) btnF.get(controller);
                    assertNotNull(btnToggle.getParent(), "botón de ocultar panel montado en el encabezado");
                    org.junit.jupiter.api.Assertions.assertEquals("Stock negativo", btnToggle.getText(),
                            "rótulo del botón de stock negativo");
                    org.junit.jupiter.api.Assertions.assertNotNull(btnToggle.getTooltip(),
                            "tooltip instalado desde FXML (patrón <tooltip>)");
                    org.junit.jupiter.api.Assertions.assertFalse(btnToggle.getText().contains("javafx.scene"),
                            "el botón no debe mostrar la clase del tooltip");
                    btnToggle.fire();
                    org.junit.jupiter.api.Assertions.assertEquals(1, divisor.getItems().size(), "toggle oculta el panel de stock negativo");
                    btnToggle.fire();
                    org.junit.jupiter.api.Assertions.assertEquals(2, divisor.getItems().size(), "segundo toggle vuelve a mostrar el panel");
                    org.junit.jupiter.api.Assertions.assertTrue(divisor.getDividerPositions()[0] > 0.5, "divider restablecido al mostrar");
                } catch (Throwable t) { error.set(t); }
                finally { stageRef.get().close(); fin.countDown(); }
            }));
        });
        org.junit.jupiter.api.Assertions.assertTrue(fin.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Panel lateral de stock negativo inválido", error.get());
    }

    @Test
    @Order(15)
    void chatWidget_muestraRolYBloqueoPorRespuesta() throws Exception {
        ChatWidgetController controller = (ChatWidgetController) cargar("/view/ChatWidget.fxml");
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                var estado = com.vendex.chatbot.GeminiChatbotService.EstadoIA.IA_ACTIVA;
                controller.agregarRespuesta(new com.vendex.chatbot.GeminiChatbotService.RespuestaChat(
                        "Stock real", estado, false, "VENTAS", java.util.List.of()));
                controller.agregarRespuesta(new com.vendex.chatbot.GeminiChatbotService.RespuestaChat(
                        "Licencia sin acceso", estado, false, null, java.util.List.of("FINANCIERO")));
                controller.agregarRespuesta(new com.vendex.chatbot.GeminiChatbotService.RespuestaChat(
                        "Hola", estado, false));
                controller.agregarRespuesta(new com.vendex.chatbot.GeminiChatbotService.RespuestaChat(
                        "Búsqueda local", com.vendex.chatbot.GeminiChatbotService.EstadoIA.SIN_API_KEY, true));
                java.lang.reflect.Field campo = ChatWidgetController.class.getDeclaredField("mensajesBox");
                campo.setAccessible(true);
                var mensajes = ((javafx.scene.layout.VBox) campo.get(controller)).getChildren();
                org.junit.jupiter.api.Assertions.assertEquals(4, mensajes.size());
                var venta = (javafx.scene.layout.VBox) ((javafx.scene.layout.HBox) mensajes.get(0)).getChildren().get(0);
                var bloqueo = (javafx.scene.layout.VBox) ((javafx.scene.layout.HBox) mensajes.get(1)).getChildren().get(0);
                org.junit.jupiter.api.Assertions.assertEquals("Rol IA: VENTAS",
                        ((javafx.scene.control.Label) venta.getChildren().get(0)).getText());
                org.junit.jupiter.api.Assertions.assertTrue(bloqueo.getChildren().get(0).getStyleClass().contains("mensaje-rol-bloqueado"));
                org.junit.jupiter.api.Assertions.assertTrue(((javafx.scene.control.Label) bloqueo.getChildren().get(0)).getText().contains("FINANCIERO"));
                for (int i = 2; i < 4; i++) {
                    var cuerpo = (javafx.scene.layout.VBox) ((javafx.scene.layout.HBox) mensajes.get(i)).getChildren().get(0);
                    org.junit.jupiter.api.Assertions.assertEquals(1, cuerpo.getChildren().size(), "Sin etiqueta de rol heredada");
                }
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        org.junit.jupiter.api.Assertions.assertTrue(latch.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Indicador de rol inválido", error.get());
    }

    @Test
    @Order(14)
    void cargaNotaCreditoSinPanelYConContextoParaMascota() throws Exception {
        Object controller = cargar("/view/NotaCreditoView.fxml");
        java.lang.reflect.Field f = controller.getClass().getDeclaredField("vendexi");
        f.setAccessible(true);
        org.junit.jupiter.api.Assertions.assertNull(f.get(controller), "Sin panel ni asistente propio en el formulario");
        DocumentoVendexi documento = (DocumentoVendexi) controller;
        org.junit.jupiter.api.Assertions.assertNull(documento.accionAgregarVendexi(), "NC no agrega referencias ajenas");
        AtomicReference<com.vendex.service.VendexiSugerenciaService.Contexto> ctx = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                ctx.set(documento.contextoVendexi());
                org.junit.jupiter.api.Assertions.assertFalse(ctx.get().clienteSeleccionado);
                java.lang.reflect.Field detalles = controller.getClass().getDeclaredField("detallesNc");
                detalles.setAccessible(true);
                @SuppressWarnings("unchecked")
                var lineas = (java.util.List<com.vendex.service.NotaCreditoService.DetalleNCInput>) detalles.get(controller);
                lineas.add(new com.vendex.service.NotaCreditoService.DetalleNCInput(7, 9, "NC-7", "FILTRO",
                        new java.math.BigDecimal("2"), new java.math.BigDecimal("12.50")));
                ctx.set(documento.contextoVendexi());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        org.junit.jupiter.api.Assertions.assertTrue(latch.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Contexto NC inválido", error.get());
        org.junit.jupiter.api.Assertions.assertEquals("nota_credito", ctx.get().modulo);
        org.junit.jupiter.api.Assertions.assertEquals(1, ctx.get().lineas.size());
        org.junit.jupiter.api.Assertions.assertEquals(7, ctx.get().lineas.get(0).inventarioId);
        org.junit.jupiter.api.Assertions.assertEquals(2, ctx.get().lineas.get(0).cantidad);
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("12.50"), ctx.get().lineas.get(0).precio);
    }

    @Test
    @Order(13)
    void formulariosSinPanelesVendexiIncrustados() throws Exception {
        AtomicReference<Throwable> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                for (String ruta : new String[]{"/view/FacturaView.fxml", "/view/NotaVentaView.fxml", "/view/NotaCreditoView.fxml"}) {
                    var loader = new javafx.fxml.FXMLLoader(getClass().getResource(ruta));
                    javafx.scene.Parent raiz = loader.load();
                    org.junit.jupiter.api.Assertions.assertTrue(loader.getController() instanceof DocumentoVendexi);
                    org.junit.jupiter.api.Assertions.assertFalse(contienePanelVendexi(raiz), ruta);
                }
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        org.junit.jupiter.api.Assertions.assertTrue(latch.await(60, TimeUnit.SECONDS), "timeout cargando formularios");
        if (err.get() != null) {
            throw new AssertionError("Formulario sin panel inválido", err.get());
        }
    }

    private boolean contienePanelVendexi(javafx.scene.Node nodo) {
        if (nodo.getStyleClass().contains("vendexi-panel")) return true;
        if (nodo instanceof javafx.scene.control.ScrollPane scroll && scroll.getContent() != null
                && contienePanelVendexi(scroll.getContent())) return true;
        if (nodo instanceof javafx.scene.control.TitledPane titulo && titulo.getContent() != null
                && contienePanelVendexi(titulo.getContent())) return true;
        if (nodo instanceof javafx.scene.Parent padre) {
            for (var hijo : padre.getChildrenUnmodifiable()) if (contienePanelVendexi(hijo)) return true;
        }
        return false;
    }
}

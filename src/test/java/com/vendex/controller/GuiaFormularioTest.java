package com.vendex.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GuiaFormularioTest {
    @BeforeAll static void iniciarFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    private void fx(Runnable tarea) throws Exception {
        var futuro = new FutureTask<Void>(() -> { tarea.run(); return null; });
        Platform.runLater(futuro); futuro.get(20, TimeUnit.SECONDS);
    }

    @Test void cubreCamposEtiquetasYColumnasDelFormularioReal() throws Exception {
        var catalogo = new AyudaFacturaCatalogo();
        try (var recurso = getClass().getResourceAsStream("/view/FacturaView.fxml")) {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(recurso);
            var nodos = doc.getElementsByTagName("*");
            for (int i = 0; i < nodos.getLength(); i++) {
                var e = (Element) nodos.item(i);
                String clave = e.getAttribute("fx:id");
                if (e.getTagName().equals("TableColumn")) {
                    org.w3c.dom.Node padre = e.getParentNode();
                    while (!(padre instanceof Element p && p.getTagName().equals("TableView"))) padre = padre.getParentNode();
                    clave = ((Element) padre).getAttribute("fx:id") + ":" + e.getAttribute("text");
                } else if (clave.isBlank() && (e.getTagName().equals("Label") || e.getTagName().equals("TitledPane")))
                    clave = e.getAttribute("text");
                if (!clave.isBlank()) {
                    var ayuda = catalogo.buscar(clave);
                    assertNotNull(ayuda, "Falta ayuda para " + clave);
                    assertFalse(ayuda.ejemplo().isBlank(), clave);
                }
            }
        }
        assertNotNull(catalogo.buscar("btnProductoTemporal"));
    }

    @Test void clicYTecladoConsultanAyudaSinModificarYSalenConEscape() throws Exception {
        fx(() -> {
            var nombre = new TextField(); nombre.setId("txtNombre");
            var guardar = new Button("Generar"); guardar.setId("btnGuardar");
            var formulario = new VBox(nombre, guardar);
            var overlay = new StackPane(); overlay.setPickOnBounds(false);
            var dibujo = dibujo("/view/Capibara.fxml"); overlay.getChildren().add(dibujo);
            var escena = new Scene(new StackPane(formulario, overlay), 900, 650);
            escena.getRoot().applyCss(); escena.getRoot().layout();
            var recibidos = new AtomicInteger();
            guardar.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> recibidos.incrementAndGet());
            nombre.addEventHandler(KeyEvent.KEY_TYPED, e -> recibidos.incrementAndGet());
            var cerrados = new AtomicInteger();
            var guia = new GuiaFormulario(overlay, formulario, dibujo, cerrados::incrementAndGet);
            guia.iniciar();
            overlay.applyCss(); overlay.layout();
            try {
                assertTrue(guia.activa());
                nombre.fireEvent(raton(MouseEvent.MOUSE_PRESSED, nombre));
                assertTrue(overlay.lookupAll(".label").stream().filter(Label.class::isInstance)
                        .map(Label.class::cast).anyMatch(l -> l.getText().equals("Nombre del cliente")));
                guardar.fireEvent(raton(MouseEvent.MOUSE_PRESSED, guardar));
                nombre.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "a", "a", KeyCode.UNDEFINED, false, false, false, false));
                assertEquals(0, recibidos.get(), "La guía debe interceptar antes de los handlers del formulario");
                guardar.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
                assertFalse(guia.activa()); assertEquals(1, cerrados.get());
                assertNull(overlay.lookup("#guiaBurbuja")); assertNull(overlay.lookup("#guiaLaser"));
                guardar.fireEvent(raton(MouseEvent.MOUSE_PRESSED, guardar));
                assertEquals(1, recibidos.get(), "Al salir se restablecen los eventos originales");
                assertTrue(dibujo.getTransforms().isEmpty());
            } finally { guia.cerrar(); }
        });
    }

    @Test void celdaYPapeleraSeResuelvenPorColumnaNoPorTextoDinamico() throws Exception {
        fx(() -> {
            var tabla = new TableView<String>(); tabla.setId("tblDetalle");
            var columna = new TableColumn<String, String>("Eliminar"); tabla.getColumns().add(columna);
            var celda = new TableCell<String, String>(); celda.updateTableView(tabla); celda.updateTableColumn(columna);
            var boton = new Button("Nombre:"); celda.setGraphic(boton);
            var formulario = new VBox(tabla, celda);
            var overlay = new StackPane(); var dibujo = dibujo("/view/RobotVendexi.fxml"); overlay.getChildren().add(dibujo);
            new Scene(new StackPane(formulario, overlay), 900, 650).getRoot().applyCss();
            var guia = new GuiaFormulario(overlay, formulario, dibujo, () -> {});
            assertEquals("Quitar producto", guia.resolver(boton).ayuda().titulo());
            assertSame(celda, guia.resolver(boton).nodo());
        });
    }

    @Test void detectaEtiquetaTransparenteYRespetaElRecorteDelScroll() throws Exception {
        fx(() -> {
            var etiqueta = new Label("Nombre:"); etiqueta.setMouseTransparent(true);
            var fueraDeVista = new Label("Subtotal:");
            var espacio = new Region(); espacio.setMinHeight(500);
            var contenido = new VBox(etiqueta, espacio, fueraDeVista);
            var scroll = new ScrollPane(contenido); scroll.setPrefSize(300, 150); scroll.setMaxSize(300, 150);
            var raiz = new StackPane(scroll); new Scene(raiz, 400, 250);
            raiz.resize(400, 250); raiz.applyCss(); raiz.layout();
            var p = etiqueta.localToScene(5, 5);
            Node seleccionado = GuiaFormulario.bajoPuntero(scroll, p.getX(), p.getY());
            assertTrue(GuiaFormulario.dentro(seleccionado, etiqueta));
            var q = fueraDeVista.localToScene(5, 5);
            assertNull(GuiaFormulario.bajoPuntero(scroll, q.getX(), q.getY()));
        });
    }

    @Test void laserParteDeLosOjosYSigueElPunteroEnAmbasMascotas() throws Exception {
        for (String recurso : new String[]{"/view/Capibara.fxml", "/view/RobotVendexi.fxml"}) {
            var fin = new CompletableFuture<Void>();
            fx(() -> {
                var campo = new TextField(); campo.setId("txtNombre");
                var formulario = new VBox(campo);
                var overlay = new StackPane(); overlay.setPickOnBounds(false);
                var dibujo = dibujo(recurso); overlay.getChildren().add(dibujo);
                var root = new StackPane(formulario, overlay); new Scene(root, 900, 650);
                root.resize(900, 650); root.applyCss(); root.layout();
                var guia = new GuiaFormulario(overlay, formulario, dibujo, () -> {}); guia.iniciar();
                campo.fireEvent(raton(MouseEvent.MOUSE_MOVED, campo));
                var espera = new javafx.animation.PauseTransition(javafx.util.Duration.millis(850));
                espera.setOnFinished(e -> {
                    try {
                        var capa = (Pane) overlay.lookup("#guiaLaser"); assertTrue(capa.isVisible());
                        var lineas = capa.getChildren().stream().filter(Line.class::isInstance).map(Line.class::cast).toList();
                        assertEquals(2, lineas.size());
                        for (int i = 0; i < 2; i++) {
                            assertTrue(dibujo.lookup("#guiaEspalda").isVisible(), "Debe permanecer de espaldas mientras usa el láser");
                            assertTrue(dibujo.getChildren().stream().filter(n -> !"guiaEspalda".equals(n.getId()))
                                    .noneMatch(Node::isVisible), "No debe mostrar el rostro frontal");
                            var ojo = dibujo.lookup(i == 0 ? "#salidaLaserIzquierda" : "#salidaLaserDerecha");
                            var b = ojo.getBoundsInLocal();
                            var origen = overlay.sceneToLocal(ojo.localToScene(b.getCenterX(), b.getCenterY()));
                            assertEquals(origen.getX(), lineas.get(i).getStartX(), 0.01);
                            assertEquals(origen.getY(), lineas.get(i).getStartY(), 0.01);
                            var destino = overlay.sceneToLocal(campo.localToScene(10, 10));
                            assertEquals(destino.getX(), lineas.get(i).getEndX(), 0.01);
                            assertEquals(destino.getY(), lineas.get(i).getEndY(), 0.01);
                        }
                        guia.cerrar();
                        assertNull(dibujo.lookup("#guiaEspalda"));
                        assertTrue(dibujo.lookup("#ojoIzquierdo").isVisible());
                        fin.complete(null);
                    } catch (Throwable ex) { fin.completeExceptionally(ex); }
                    finally { guia.cerrar(); }
                });
                espera.play();
            });
            fin.get(10, TimeUnit.SECONDS);
        }
    }

    private static Group dibujo(String recurso) {
        try { return FXMLLoader.load(GuiaFormularioTest.class.getResource(recurso)); }
        catch (Exception ex) { throw new AssertionError(ex); }
    }

    private static MouseEvent raton(javafx.event.EventType<MouseEvent> tipo, Node nodo) {
        var p = nodo.localToScene(10, 10);
        return new MouseEvent(tipo, p.getX(), p.getY(), p.getX(), p.getY(), MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, true,
                new PickResult(nodo, p.getX(), p.getY()));
    }
}

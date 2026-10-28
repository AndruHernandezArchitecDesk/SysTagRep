package com.vendex.controller;

import javafx.animation.*;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/** Sesión de inspección local. Los filtros se retiran al salir o cambiar de vista. Hilo FX. */
final class GuiaFormulario {
    private final StackPane superficie;
    private final Parent formulario;
    private final Group dibujo;
    private final Group espalda;
    private final java.util.Map<Node, Boolean> visibilidadFrontal = new java.util.IdentityHashMap<>();
    private final Runnable alCerrar;
    private final AyudaFacturaCatalogo catalogo = new AyudaFacturaCatalogo();
    private final Pane capa = new Pane();
    private final VBox burbuja = new VBox(8);
    private final ScrollPane scroll = new ScrollPane();
    private final Label titulo = new Label(), descripcion = new Label(), ejemplo = new Label();
    private final Rectangle resalte = new Rectangle();
    private final Line[] haces = {new Line(), new Line()};
    private final Circle punto = new Circle(4, Color.RED);
    private final Rotate vuelta = new Rotate(0, 50, 41, 0, Rotate.Y_AXIS);
    private final Timeline giro;
    private final AnimationTimer seguimiento;
    private Scene escena;
    private boolean activa, ayudaIzquierda;
    private double mouseX, mouseY;
    private Node objetivo;
    private final EventHandler<MouseEvent> raton = this::procesarRaton;
    private final EventHandler<KeyEvent> teclado = this::procesarTeclado;
    private final EventHandler<ContextMenuEvent> menu = this::procesarMenu;

    private void procesarMenu(ContextMenuEvent e) {
        if (!dentro(e.getTarget() instanceof Node n ? n : null, superficie)) e.consume();
    }

    private void procesarTeclado(KeyEvent e) {
        if (e.getCode() == KeyCode.ESCAPE) { e.consume(); cerrar(); }
        else if (!dentro(e.getTarget() instanceof Node n ? n : null, burbuja)) e.consume();
    }

    GuiaFormulario(StackPane superficie, Parent formulario, Group dibujo, Runnable alCerrar) {
        this.superficie = superficie; this.formulario = formulario; this.dibujo = dibujo; this.alCerrar = alCerrar;
        try {
            espalda = javafx.fxml.FXMLLoader.load(getClass().getResource(dibujo.lookup("#ojos") != null
                    ? "/view/RobotVendexiEspaldas.fxml" : "/view/CapibaraEspaldas.fxml"));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo cargar la vista posterior de Vetxi", ex);
        }
        espalda.setId("guiaEspalda"); espalda.setVisible(false);
        capa.setManaged(false); capa.setMouseTransparent(true); capa.setId("guiaLaser");
        resalte.setFill(Color.web("#ff2020", 0.08)); resalte.setStroke(Color.RED);
        resalte.setStrokeWidth(2); resalte.getStrokeDashArray().addAll(6.0, 4.0);
        resalte.setVisible(false);
        capa.getChildren().add(resalte);
        for (Line haz : haces) {
            haz.setStroke(Color.web("#ff2020")); haz.setStrokeWidth(2);
            haz.setEffect(new javafx.scene.effect.DropShadow(7, Color.RED));
            capa.getChildren().add(haz);
        }
        capa.getChildren().add(punto);
        burbuja.setId("guiaBurbuja"); burbuja.setManaged(false);
        burbuja.getStyleClass().add("capibara-globo");
        titulo.getStyleClass().add("capibara-globo-titulo");
        descripcion.getStyleClass().add("capibara-detalle"); ejemplo.getStyleClass().add("capibara-detalle");
        for (Label label : new Label[]{titulo, descripcion, ejemplo}) { label.setWrapText(true); label.setMinHeight(Region.USE_PREF_SIZE); }
        var contenido = new VBox(8, titulo, descripcion, ejemplo);
        scroll.setContent(contenido); scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); scroll.setMinHeight(0);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        var terminar = new Button("Terminar guía (Esc)"); terminar.setOnAction(e -> cerrar());
        terminar.getStyleClass().add("capibara-accion");
        var cabecera = new Label("Vetxi · Guía de factura"); cabecera.getStyleClass().add("capibara-globo-cabecera");
        var mover = new Button("⇆"); mover.setAccessibleText("Mover ayuda al otro lado");
        mover.setTooltip(new Tooltip("Mover ayuda al otro lado para descubrir campos tapados"));
        mover.setOnAction(e -> ayudaIzquierda = !ayudaIzquierda);
        var fila = new HBox(8, cabecera, mover); HBox.setHgrow(cabecera, Priority.ALWAYS); cabecera.setMaxWidth(Double.MAX_VALUE);
        burbuja.getChildren().addAll(fila, scroll, terminar);
        giro = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(vuelta.angleProperty(), 0)),
                new KeyFrame(Duration.millis(275), e -> mostrarEspalda(), new KeyValue(vuelta.angleProperty(), 90, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(550), new KeyValue(vuelta.angleProperty(), 180, Interpolator.EASE_OUT)));
        seguimiento = new AnimationTimer() {
            @Override public void handle(long ahora) { dibujar(); }
        };
    }

    void iniciar() {
        if (activa || superficie.getScene() == null || formulario.getScene() != superficie.getScene()) return;
        activa = true; escena = superficie.getScene();
        superficie.getChildren().add(0, capa); superficie.getChildren().add(burbuja);
        visibilidadFrontal.clear();
        for (Node pieza : dibujo.getChildren()) visibilidadFrontal.put(pieza, pieza.isVisible());
        dibujo.getChildren().add(espalda);
        dibujo.getTransforms().add(vuelta);
        mostrar(new AyudaFacturaCatalogo.Ayuda("Apunta y haz clic", "El láser sigue tu puntero. Haz clic sobre un campo, etiqueta, botón o columna de la factura para conocer su uso. Durante la guía los clics consultan ayuda y no modifican el formulario. Puedes desplazar la página para ver más campos.", "Empieza por Seleccionar Cliente. Pulsa Esc para terminar."));
        var centro = dibujo.localToScene(50, 33); mouseX = centro.getX(); mouseY = centro.getY();
        escena.addEventFilter(MouseEvent.ANY, raton);
        escena.addEventFilter(KeyEvent.ANY, teclado);
        escena.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, menu);
        giro.playFromStart(); seguimiento.start();
    }

    boolean activa() { return activa; }

    void cerrar() {
        if (!activa) return;
        activa = false;
        escena.removeEventFilter(MouseEvent.ANY, raton);
        escena.removeEventFilter(KeyEvent.ANY, teclado);
        escena.removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, menu);
        seguimiento.stop(); giro.stop(); dibujo.getTransforms().remove(vuelta);
        dibujo.getChildren().remove(espalda);
        visibilidadFrontal.forEach(Node::setVisible); visibilidadFrontal.clear();
        espalda.setVisible(false);
        superficie.getChildren().removeAll(capa, burbuja);
        alCerrar.run();
    }

    private void mostrarEspalda() {
        visibilidadFrontal.keySet().forEach(n -> n.setVisible(false));
        espalda.setVisible(true);
    }

    private void procesarRaton(MouseEvent e) {
        Node nodo = e.getTarget() instanceof Node n ? n : null;
        Node apuntado = dentro(nodo, superficie) ? null : bajoPuntero(formulario, e.getSceneX(), e.getSceneY());
        if (e.getEventType() == MouseEvent.MOUSE_MOVED || e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            mouseX = e.getSceneX(); mouseY = e.getSceneY();
            objetivo = apuntado != null ? resolver(apuntado).nodo() : null;
        }
        if (dentro(nodo, superficie)) return;
        // Interceptar también liberación/clic: ningún skin del formulario debe activar acciones.
        if (e.getEventType() == MouseEvent.MOUSE_PRESSED || e.getEventType() == MouseEvent.MOUSE_RELEASED
                || e.getEventType() == MouseEvent.MOUSE_CLICKED || e.getEventType() == MouseEvent.DRAG_DETECTED
                || e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            if (e.getEventType() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseButton.PRIMARY) {
                if (apuntado != null) {
                    var resultado = resolver(apuntado); objetivo = resultado.nodo();
                    mostrar(resultado.ayuda() != null ? resultado.ayuda() : new AyudaFacturaCatalogo.Ayuda(
                            "Elemento del formulario", "No tengo una explicación específica para este elemento. Apunta al campo, etiqueta o encabezado de columna más cercano.", "Prueba con Seleccionar Cliente o una columna del detalle."));
                } else mostrar(new AyudaFacturaCatalogo.Ayuda("Guía de factura de venta", "Selecciona un componente dentro de la factura. Para navegar a otro módulo, termina primero la guía.", "Pulsa Esc para salir."));
            }
            e.consume();
        }
    }

    record Resultado(Node nodo, AyudaFacturaCatalogo.Ayuda ayuda) {}

    Resultado resolver(Node nodo) {
        // Las columnas mandan sobre el texto dinámico o botones contenidos en sus celdas.
        for (Node n = nodo; n != null && n != formulario; n = n.getParent()) {
            if (n instanceof TableCell<?, ?> celda && celda.getTableView() != null && celda.getTableColumn() != null)
                return new Resultado(n, catalogo.buscar(celda.getTableView().getId() + ":" + celda.getTableColumn().getText()));
            if (n instanceof TableColumnHeader cabecera && cabecera.getTableColumn() instanceof TableColumn<?, ?> columna
                    && columna.getTableView() != null)
                return new Resultado(n, catalogo.buscar(columna.getTableView().getId() + ":" + columna.getText()));
        }
        for (Node n = nodo; n != null; n = n.getParent()) {
            var ayuda = catalogo.buscar(n.getId());
            if (ayuda == null && n instanceof Labeled l) ayuda = catalogo.buscar(l.getText());
            if (ayuda == null && n instanceof TitledPane t) ayuda = catalogo.buscar(t.getText());
            if (ayuda != null) return new Resultado(n, ayuda);
            if (n == formulario) break;
        }
        return new Resultado(nodo, null);
    }

    static boolean dentro(Node nodo, Node padre) {
        for (Node n = nodo; n != null; n = n.getParent()) if (n == padre) return true;
        return false;
    }

    /** Incluye etiquetas mouseTransparent y controles deshabilitados, respetando el recorte del scroll. */
    static Node bajoPuntero(Node nodo, double sceneX, double sceneY) {
        if (!nodo.isVisible()) return null;
        var punto = nodo.sceneToLocal(sceneX, sceneY);
        if (punto == null || !nodo.contains(punto)) return null;
        if (nodo.getClip() != null && !nodo.getClip().contains(nodo.getClip().parentToLocal(punto))) return null;
        if (nodo instanceof Parent padre) {
            var hijos = padre.getChildrenUnmodifiable();
            for (int i = hijos.size() - 1; i >= 0; i--) {
                Node encontrado = bajoPuntero(hijos.get(i), sceneX, sceneY);
                if (encontrado != null) return encontrado;
            }
        }
        return nodo;
    }

    private void mostrar(AyudaFacturaCatalogo.Ayuda ayuda) {
        titulo.setText(ayuda.titulo()); descripcion.setText(ayuda.descripcion());
        ejemplo.setText("Ejemplo: " + ayuda.ejemplo());
        scroll.setVvalue(0);
    }

    private void dibujar() {
        double ancho = Math.max(0, superficie.getWidth() - 16), alto = Math.max(0, superficie.getHeight() - 16);
        double w = Math.min(350, ancho), h = Math.min(340, alto);
        burbuja.resizeRelocate(ayudaIzquierda ? 8 : Math.max(8, superficie.getWidth() - w - 8), 8, w, h);
        Point2D destino = superficie.sceneToLocal(mouseX, mouseY);
        for (int i = 0; i < haces.length; i++) {
            Node ojo = espalda.lookup(i == 0 ? "#salidaLaserIzquierda" : "#salidaLaserDerecha");
            if (ojo == null) continue;
            var b = ojo.getBoundsInLocal();
            Point2D origen = superficie.sceneToLocal(ojo.localToScene(b.getCenterX(), b.getCenterY()));
            haces[i].setStartX(origen.getX()); haces[i].setStartY(origen.getY());
            haces[i].setEndX(destino.getX()); haces[i].setEndY(destino.getY());
        }
        punto.setCenterX(destino.getX()); punto.setCenterY(destino.getY());
        capa.setVisible(giro.getStatus() != Animation.Status.RUNNING);
        resalte.setVisible(objetivo != null && objetivo.getScene() == escena && objetivo.isVisible());
        if (resalte.isVisible()) {
            var b = superficie.sceneToLocal(objetivo.localToScene(objetivo.getBoundsInLocal()));
            resalte.setX(b.getMinX()); resalte.setY(b.getMinY()); resalte.setWidth(b.getWidth()); resalte.setHeight(b.getHeight());
        }
    }
}

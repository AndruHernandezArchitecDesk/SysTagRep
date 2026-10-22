package com.vendex.controller;

import com.vendex.model.StockNegativoItem;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Panel de consulta independiente del formulario. No altera existencias ni precarga productos. */
final class StockNegativoPane extends VBox {
    @FunctionalInterface interface Consulta { List<StockNegativoItem> listar(int sucursal) throws Exception; }
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Comparator<StockNegativoItem> ORDEN = Comparator.comparing(StockNegativoItem::fecha,
            Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Comparator.comparingInt(StockNegativoItem::id).reversed());

    private final Consulta consulta;
    private final IntSupplier sucursal;
    private final Supplier<Integer> usuario;
    private final TableView<StockNegativoItem> tabla = new TableView<>();
    private final Label estado = new Label("Pendiente de actualizar");
    private final Button actualizar = new Button("Actualizar");
    private Task<List<StockNegativoItem>> carga;
    private long version;
    private final javafx.beans.value.ChangeListener<Boolean> mostrando = (o, antes, visible) -> {
        if (visible) actualizar(); else cancelar();
    };
    private final javafx.beans.value.ChangeListener<Window> ventana = (o, antes, actual) -> vincularVentana(antes, actual);

    StockNegativoPane(Consulta consulta, IntSupplier sucursal, Supplier<Integer> usuario) {
        super(10);
        this.consulta = consulta; this.sucursal = sucursal; this.usuario = usuario;
        setId("panelStockNegativo"); getStyleClass().add("card");
        setMinWidth(280); setPrefWidth(400); setMaxWidth(600); setMinHeight(180);
        Label titulo = new Label("Stock negativo"); titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Region espacio = new Region(); HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, titulo, espacio, actualizar);
        Label criterio = new Label("Fecha de alta · más recientes primero");
        criterio.setWrapText(true); criterio.setStyle("-fx-font-size: 11px; -fx-opacity: 0.8;");
        estado.setWrapText(true); estado.setStyle("-fx-font-size: 11px;");

        TableColumn<StockNegativoItem, String> codigo = new TableColumn<>("Código");
        codigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigo())); codigo.setPrefWidth(85);
        TableColumn<StockNegativoItem, String> descripcion = new TableColumn<>("Descripción");
        descripcion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().descripcion())); descripcion.setPrefWidth(150);
        descripcion.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String texto, boolean vacio) {
                super.updateItem(texto, vacio); setText(vacio ? null : texto);
                setTooltip(vacio || texto == null ? null : new Tooltip(texto));
            }
        });
        TableColumn<StockNegativoItem, Integer> stock = new TableColumn<>("Stock");
        stock.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cantidad())); stock.setPrefWidth(60);
        stock.setStyle("-fx-alignment: CENTER_RIGHT; -fx-text-fill: #b00020; -fx-font-weight: bold;");
        TableColumn<StockNegativoItem, LocalDateTime> fecha = new TableColumn<>("Fecha de alta ↓");
        fecha.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fecha())); fecha.setPrefWidth(130);
        fecha.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime valor, boolean vacio) {
                super.updateItem(valor, vacio); setText(vacio ? null : valor == null ? "Sin fecha" : valor.format(FECHA));
            }
        });
        tabla.getColumns().setAll(codigo, descripcion, stock, fecha);
        tabla.getColumns().forEach(c -> c.setSortable(false));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(new Label("Sin productos con stock negativo"));
        tabla.setMinHeight(100); VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(cabecera, criterio, tabla, estado);
        actualizar.setOnAction(e -> actualizar());
        sceneProperty().addListener((o, antes, actual) -> {
            if (antes != null) { antes.windowProperty().removeListener(ventana); vincularVentana(antes.getWindow(), null); }
            if (actual != null) {
                actual.windowProperty().addListener(ventana); vincularVentana(null, actual.getWindow());
            } else cancelar();
        });
    }

    TableView<StockNegativoItem> tabla() { return tabla; }
    Button botonActualizar() { return actualizar; }
    String estado() { return estado.getText(); }

    private void vincularVentana(Window anterior, Window actual) {
        if (anterior != null) anterior.showingProperty().removeListener(mostrando);
        if (actual != null) {
            actual.showingProperty().addListener(mostrando);
            if (actual.isShowing()) actualizar();
        }
        if (anterior != null && actual == null) cancelar();
    }

    void cancelar() {
        version++;
        if (carga != null) { carga.cancel(true); carga = null; }
        tabla.getItems().clear(); actualizar.setDisable(false); estado.setText("Pendiente de actualizar");
    }

    private boolean vigente(long solicitud, int sucursalId, Integer usuarioId) {
        Scene scene = getScene();
        return solicitud == version && scene != null && isVisible()
                && (scene.getWindow() == null || scene.getWindow().isShowing())
                && sucursalId == sucursal.getAsInt() && Objects.equals(usuarioId, usuario.get());
    }

    void actualizar() {
        if (getScene() == null || !isVisible() || getScene().getWindow() != null && !getScene().getWindow().isShowing()) return;
        cancelar();
        long solicitud = version;
        int sucursalId = sucursal.getAsInt(); Integer usuarioId = usuario.get();
        estado.setText("Cargando stock negativo de la sucursal " + sucursalId + "…");
        tabla.setPlaceholder(new Label("Cargando…")); actualizar.setDisable(true);
        Task<List<StockNegativoItem>> tarea = new Task<>() {
            @Override protected List<StockNegativoItem> call() throws Exception {
                return consulta.listar(sucursalId).stream().filter(i -> i != null && i.cantidad() < 0).sorted(ORDEN).toList();
            }
        };
        carga = tarea;
        tarea.setOnSucceeded(e -> {
            if (!vigente(solicitud, sucursalId, usuarioId)) return;
            carga = null; tabla.getItems().setAll(tarea.getValue()); actualizar.setDisable(false);
            tabla.setPlaceholder(new Label("Sin productos con stock negativo"));
            estado.setText(tarea.getValue().size() + " producto(s) · sucursal " + sucursalId);
        });
        tarea.setOnFailed(e -> {
            if (!vigente(solicitud, sucursalId, usuarioId)) return;
            carga = null; actualizar.setDisable(false);
            tabla.setPlaceholder(new Label("No se pudo cargar el stock negativo"));
            estado.setText("No se pudo actualizar. Pulsa Actualizar para reintentar.");
            Logger.getLogger(StockNegativoPane.class.getName()).log(Level.WARNING, "Error al cargar stock negativo", tarea.getException());
        });
        Thread hilo = new Thread(tarea, "ingreso-stock-negativo"); hilo.setDaemon(true); hilo.start();
    }
}

package com.vendex.controller;

import com.vendex.offline.LocalOperationQueue;
import com.vendex.offline.OperacionOffline;
import com.vendex.offline.SyncService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ColaOfflineController implements Initializable {

    @FXML private TableView<OperacionOffline> tabla;
    @FXML private TableColumn<OperacionOffline, Number> colId;
    @FXML private TableColumn<OperacionOffline, String> colTipo;
    @FXML private TableColumn<OperacionOffline, String> colEstado;
    @FXML private TableColumn<OperacionOffline, String> colCreado;
    @FXML private TableColumn<OperacionOffline, String> colError;
    @FXML private Label lblPendientes;
    @FXML private Button btnSincronizar;
    @FXML private Button btnCerrar;
    @FXML private Button btnReencolar;
    @FXML private Button btnDescartar;

    private final ObservableList<OperacionOffline> items = FXCollections.observableArrayList();
    private final LocalOperationQueue cola = new LocalOperationQueue();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()));
        colTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTipo()));
        colEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado() != null ? c.getValue().getEstado().name() : ""));
        colCreado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCreadoEn() != null ? c.getValue().getCreadoEn().toString() : ""));
        colError.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getError() != null ? c.getValue().getError() : ""));
        tabla.setItems(items);
        cargar();
        btnSincronizar.setOnAction(e -> sincronizar());
        btnCerrar.setOnAction(e -> cerrar());
        // Fase 7: resolución de conflictos — reencolar o descartar la operación seleccionada
        btnReencolar.setOnAction(e -> reencolarSeleccionada());
        btnDescartar.setOnAction(e -> descartarSeleccionada());
    }

    public void cargar() {
        // Fase 7: mostrar TODAS las operaciones (pendientes, en conflicto y fallidas)
        items.setAll(cola.listarTodas());
        long pend = items.stream().filter(o -> o.getEstado() == OperacionOffline.Estado.PENDIENTE).count();
        long conflicto = items.stream().filter(o -> o.getEstado() == OperacionOffline.Estado.CONFLICTO).count();
        long fallida = items.stream().filter(o -> o.getEstado() == OperacionOffline.Estado.FALLIDA).count();
        lblPendientes.setText("Pendientes: " + pend + " | En conflicto: " + conflicto + " | Fallidas: " + fallida);
    }

    private void reencolarSeleccionada() {
        OperacionOffline sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { aviso("Seleccione una operación de la tabla."); return; }
        if (sel.getEstado() == OperacionOffline.Estado.ENVIADA) { aviso("La operación ya fue sincronizada."); return; }
        cola.reencolar(sel.getId());
        cargar();
    }

    private void descartarSeleccionada() {
        OperacionOffline sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { aviso("Seleccione una operación de la tabla."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Descartar la operación #" + sel.getId() + " (" + sel.getTipo() + ")?\nNo volverá a enviarse al servidor.",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            cola.descartar(sel.getId());
            cargar();
        }
    }

    private void aviso(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private void sincronizar() {
        btnSincronizar.setDisable(true);
        lblPendientes.setText("Sincronizando...");
        new Thread(() -> {
            try { SyncService.INSTANCE.intentarSincronizacion(); } catch (Exception ignore) {}
            javafx.application.Platform.runLater(() -> {
                cargar();
                btnSincronizar.setDisable(false);
            });
        }, "SyncManual-Queue").start();
    }

    private void cerrar() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }
}

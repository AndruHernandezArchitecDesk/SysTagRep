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
    }

    public void cargar() {
        items.setAll(cola.listarPendientes());
        lblPendientes.setText("Pendientes: " + items.size());
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

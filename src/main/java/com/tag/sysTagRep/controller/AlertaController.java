package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.AlertaDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.model.Alerta;
import com.tag.sysTagRep.service.AlertaService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AlertaController implements Initializable {

    @FXML private TableView<Alerta> tblAlertas;
    @FXML private TableColumn<Alerta, String> colTipo;
    @FXML private TableColumn<Alerta, String> colMensaje;
    @FXML private TableColumn<Alerta, String> colReferencia;
    @FXML private TableColumn<Alerta, String> colFecha;
    @FXML private TableColumn<Alerta, String> colEstado;
    @FXML private TableColumn<Alerta, Void> colAcciones;

    @FXML private ComboBox<String> cmbFiltro;
    @FXML private Label lblResumen;

    private final AlertaService alertaService = new AlertaService();
    private final AlertaDAO alertaDAO = new AlertaDAO();
    private final LogDAO logDAO = new LogDAO();
    private final ObservableList<Alerta> listaAlertas = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbFiltro.getItems().addAll("Todas", "No leídas", "Leídas");
        cmbFiltro.setValue("Todas");
        configurarColumnas();
        configurarAcciones();
        cmbFiltro.setOnAction(e -> cargarDatos());
        cargarDatos();
    }

    private void configurarColumnas() {
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMensaje.setCellValueFactory(new PropertyValueFactory<>("mensaje"));
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referenciaTipo"));

        colFecha.setCellValueFactory(cellData -> {
            if (cellData.getValue() == null || cellData.getValue().getFechaCreacion() == null) {
                return new ReadOnlyObjectWrapper<>("");
            }
            String text = cellData.getValue().getFechaCreacion().toString().replace(".0", "");
            return new ReadOnlyObjectWrapper<>(text);
        });

        colEstado.setCellValueFactory(cellData -> {
            Alerta a = cellData.getValue();
            if (a == null) return new ReadOnlyObjectWrapper<>("");
            return new ReadOnlyObjectWrapper<>(a.isLeida() ? "Leída" : "No leída");
        });
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Leída".equals(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblAlertas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configurarAcciones() {
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnLeer = new Button();
            private final Button btnEliminar = new Button();
            private final HBox hbox = new HBox(8);
            {
                FontIcon iconLeer = new FontIcon(FontAwesomeSolid.CHECK);
                iconLeer.setIconSize(14);
                iconLeer.setIconColor(javafx.scene.paint.Color.web("#27ae60"));
                btnLeer.setGraphic(iconLeer);
                btnLeer.setTooltip(new Tooltip("Marcar como leída"));
                btnLeer.setStyle("-fx-background-color: transparent;");

                FontIcon iconEliminar = new FontIcon(FontAwesomeSolid.TRASH);
                iconEliminar.setIconSize(14);
                iconEliminar.setIconColor(javafx.scene.paint.Color.RED);
                btnEliminar.setGraphic(iconEliminar);
                btnEliminar.setTooltip(new Tooltip("Eliminar"));
                btnEliminar.setStyle("-fx-background-color: transparent;");

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnLeer, btnEliminar);

                btnLeer.setOnAction(e -> {
                    Alerta a = getTableView().getItems().get(getIndex());
                    alertaService.marcarComoLeida(a.getId());
                    cargarDatos();
                });
                btnEliminar.setOnAction(e -> {
                    Alerta a = getTableView().getItems().get(getIndex());
                    alertaService.eliminar(a.getId());
                    cargarDatos();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Alerta a = getTableView().getItems().get(getIndex());
                    btnLeer.setVisible(!a.isLeida());
                    btnLeer.setManaged(!a.isLeida());
                    setGraphic(hbox);
                }
            }
        });
    }

    @FXML
    private void actualizar() {
        alertaService.regenerarAlertas();
        cargarDatos();
    }

    @FXML
    private void marcarTodasLeidas() {
        alertaService.marcarTodasComoLeidas();
        cargarDatos();
    }

    @FXML
    private void eliminarLeidas() {
        alertaService.eliminarLeidas();
        cargarDatos();
    }

    private void cargarDatos() {
        String filtro = cmbFiltro.getValue();
        List<Alerta> fuente = alertaService.obtenerTodas();
        List<Alerta> filtradas;
        if ("No leídas".equals(filtro)) {
            filtradas = fuente.stream().filter(a -> !a.isLeida()).collect(Collectors.toList());
        } else if ("Leídas".equals(filtro)) {
            filtradas = fuente.stream().filter(Alerta::isLeida).collect(Collectors.toList());
        } else {
            filtradas = fuente;
        }
        listaAlertas.setAll(filtradas);
        tblAlertas.setItems(listaAlertas);
        int total = fuente.size();
        long noLeidas = fuente.stream().filter(a -> !a.isLeida()).count();
        lblResumen.setText("Total: " + total + " alertas — " + noLeidas + " no leídas");
    }
}

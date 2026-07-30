package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.VentaResumenDAO;
import com.tag.sysTagRep.model.VentaResumen;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class HistorialVentaController implements Initializable {

    @FXML private TextField txtBuscar;
    @FXML private DatePicker dpFecha;
    @FXML private TableView<VentaResumen> tblVentas;
    @FXML private PieChart pieClientes;
    @FXML private TableColumn<VentaResumen, String> colTipo;
    @FXML private TableColumn<VentaResumen, String> colCodigo;
    @FXML private TableColumn<VentaResumen, LocalDateTime> colFecha;
    @FXML private TableColumn<VentaResumen, String> colCliente;
    @FXML private TableColumn<VentaResumen, String> colFormaPago;
    @FXML private TableColumn<VentaResumen, Integer> colItems;
    @FXML private TableColumn<VentaResumen, BigDecimal> colTotal;

    private final VentaResumenDAO dao = new VentaResumenDAO();
    private ObservableList<VentaResumen> lista = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        tblVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dpFecha.setValue(LocalDate.now());
        dpFecha.setOnAction(e -> cargarDatos());

        cargarDatos();
        filtroBusqueda();
    }

    private void cargarDatos() {
        lista.setAll(dao.listarPorFecha(dpFecha.getValue()));
        tblVentas.setItems(lista);
        actualizarGrafico();
    }

    private void actualizarGrafico() {
        Map<String, BigDecimal> ventasPorCliente = new LinkedHashMap<>();
        for (VentaResumen v : lista) {
            ventasPorCliente.merge(v.getCliente(), v.getTotal(), BigDecimal::add);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, BigDecimal> e : ventasPorCliente.entrySet()) {
            double valor = e.getValue().doubleValue();
            if (valor > 0) {
                pieData.add(new PieChart.Data(e.getKey() + " ($" + e.getValue() + ")", valor));
            }
        }
        pieClientes.setData(pieData);
        pieClientes.setTitle("Clientes");
        pieClientes.setLabelsVisible(true);
    }

    private void filtroBusqueda() {
        FilteredList<VentaResumen> filtrados = new FilteredList<>(lista, p -> true);
        tblVentas.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtrados.setPredicate(v -> (v.getTipo() + " " + v.getCodigo() + " " +
                        v.getCliente() + " " + v.getFormaPago()).toLowerCase().contains(texto));
            }
        });
    }
}

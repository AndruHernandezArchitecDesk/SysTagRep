package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.FacturaProveedorDAO;
import com.tag.sysTagRep.model.CompraResumen;
import com.tag.sysTagRep.util.SortTable;
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

public class HistorialCompraController implements Initializable {

    @FXML private TextField txtBuscar;
    @FXML private DatePicker dpFecha;
    @FXML private TableView<CompraResumen> tblCompras;
    @FXML private PieChart pieProveedores;
    @FXML private TableColumn<CompraResumen, String> colFactura;
    @FXML private TableColumn<CompraResumen, String> colProveedor;
    @FXML private TableColumn<CompraResumen, LocalDateTime> colFecha;
    @FXML private TableColumn<CompraResumen, Integer> colItems;
    @FXML private TableColumn<CompraResumen, BigDecimal> colTotal;

    private final FacturaProveedorDAO dao = new FacturaProveedorDAO();
    private ObservableList<CompraResumen> lista = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colFactura.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        tblCompras.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblCompras);

        dpFecha.setValue(LocalDate.now());
        dpFecha.setOnAction(e -> cargarDatos());

        cargarDatos();
        filtroBusqueda();
    }

    private void cargarDatos() {
        lista.setAll(dao.listarComprasPorFecha(dpFecha.getValue()));
        tblCompras.setItems(lista);
        actualizarGrafico();
    }

    private void actualizarGrafico() {
        Map<String, BigDecimal> comprasPorProveedor = new LinkedHashMap<>();
        for (CompraResumen c : lista) {
            comprasPorProveedor.merge(c.getProveedor(), c.getTotal(), BigDecimal::add);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, BigDecimal> e : comprasPorProveedor.entrySet()) {
            double valor = e.getValue().doubleValue();
            if (valor > 0) {
                pieData.add(new PieChart.Data(e.getKey() + " ($" + e.getValue() + ")", valor));
            }
        }
        pieProveedores.setData(pieData);
        pieProveedores.setTitle("Proveedores");
        pieProveedores.setLabelsVisible(true);
    }

    private void filtroBusqueda() {
        FilteredList<CompraResumen> filtrados = new FilteredList<>(lista, p -> true);
        tblCompras.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtrados.setPredicate(c -> (c.getNumeroFactura() + " " + c.getProveedor())
                        .toLowerCase().contains(texto));
            }
        });
    }
}

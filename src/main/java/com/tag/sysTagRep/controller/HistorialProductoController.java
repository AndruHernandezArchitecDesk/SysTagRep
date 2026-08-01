package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.HistorialProductoDAO;
import com.tag.sysTagRep.model.HistorialProducto;
import com.tag.sysTagRep.util.SortTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HistorialProductoController implements Initializable {

    @FXML private DatePicker dpFecha;
    @FXML private TextField txtBuscar;
    @FXML private TableView<HistorialProducto> tblHistorial;
    @FXML private TableColumn<HistorialProducto, Integer> colId;
    @FXML private TableColumn<HistorialProducto, String> colCodigo;
    @FXML private TableColumn<HistorialProducto, String> colDescripcion;
    @FXML private TableColumn<HistorialProducto, Integer> colCantidad;
    @FXML private TableColumn<HistorialProducto, BigDecimal> colPrecio;
    @FXML private TableColumn<HistorialProducto, String> colTipo;
    @FXML private TableColumn<HistorialProducto, String> colComprobante;
    @FXML private TableColumn<HistorialProducto, String> colCliente;
    @FXML private TableColumn<HistorialProducto, String> colProveedor;
    @FXML private TableColumn<HistorialProducto, LocalDateTime> colFecha;
    @FXML private PieChart pieProductos;

    private final HistorialProductoDAO dao = new HistorialProductoDAO();
    private ObservableList<HistorialProducto> lista = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("productoCodigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("productoDescripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoComprobante"));
        colComprobante.setCellValueFactory(new PropertyValueFactory<>("codigoComprobante"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedorNombre"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaVenta"));

        tblHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblHistorial);

        dpFecha.setValue(LocalDate.now());
        dpFecha.setOnAction(e -> cargarDatos());

        cargarDatos();
        filtroBusqueda();
    }

    private void cargarDatos() {
        lista.setAll(dao.listarPorFecha(dpFecha.getValue()));
        tblHistorial.setItems(lista);
        actualizarGrafico();
    }

    private void actualizarGrafico() {
        Map<String, Integer> ventasPorProd = new LinkedHashMap<>();
        for (HistorialProducto h : lista) {
            ventasPorProd.merge(h.getProductoDescripcion(), h.getCantidad(), Integer::sum);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> e : ventasPorProd.entrySet()) {
            pieData.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }
        pieProductos.setData(pieData);
        pieProductos.setTitle("Productos más vendidos");
        pieProductos.setLabelsVisible(true);
    }

    private void filtroBusqueda() {
        FilteredList<HistorialProducto> filtrados = new FilteredList<>(lista, p -> true);
        tblHistorial.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtrados.setPredicate(h -> {
                    String searchStr = (h.getProductoCodigo() + " " + h.getProductoDescripcion() + " " +
                            h.getTipoComprobante() + " " + h.getCodigoComprobante() + " " +
                            h.getClienteNombre() + " " + h.getProveedorNombre()).toLowerCase();
                    return searchStr.contains(texto);
                });
            }
        });
    }
}

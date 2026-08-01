package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.VentaResumenDAO;
import com.tag.sysTagRep.model.DetalleVentaReporte;
import com.tag.sysTagRep.model.VentaResumen;
import com.tag.sysTagRep.util.SortTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ComprobanteVentaReporteController implements Initializable {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<VentaResumen> tblReporte;
    @FXML private TableColumn<VentaResumen, String> colTipo;
    @FXML private TableColumn<VentaResumen, String> colCodigo;
    @FXML private TableColumn<VentaResumen, LocalDateTime> colFecha;
    @FXML private TableColumn<VentaResumen, String> colCliente;
    @FXML private TableColumn<VentaResumen, String> colFormaPago;
    @FXML private TableColumn<VentaResumen, Integer> colItems;
    @FXML private TableColumn<VentaResumen, BigDecimal> colTotal;
    @FXML private TableView<DetalleVentaReporte> tblDetalle;
    @FXML private TableColumn<DetalleVentaReporte, String> colDetCodigo;
    @FXML private TableColumn<DetalleVentaReporte, String> colDetDescripcion;
    @FXML private TableColumn<DetalleVentaReporte, Integer> colDetCantidad;
    @FXML private TableColumn<DetalleVentaReporte, BigDecimal> colDetPrecio;
    @FXML private TableColumn<DetalleVentaReporte, BigDecimal> colDetIva;
    @FXML private TableColumn<DetalleVentaReporte, BigDecimal> colDetTotal;
    @FXML private Label lblTotal;

    private final VentaResumenDAO dao = new VentaResumenDAO();
    private ObservableList<VentaResumen> lista = FXCollections.observableArrayList();
    private final ObservableList<DetalleVentaReporte> detalle = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colFecha.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? "" : f.toLocalDate().format(FMT_FECHA));
                setStyle("-fx-alignment: CENTER;");
            }
        });
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : "$ " + b.setScale(2, RoundingMode.HALF_UP));
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
            }
        });
        colItems.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : String.valueOf(n));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colDetCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoProducto"));
        colDetDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colDetPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colDetIva.setCellValueFactory(new PropertyValueFactory<>("iva"));
        colDetTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colDetCantidad.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : String.valueOf(n));
                setStyle("-fx-alignment: CENTER;");
            }
        });
        colDetPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : "$ " + b.setScale(2, RoundingMode.HALF_UP));
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colDetIva.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : "$ " + b.setScale(2, RoundingMode.HALF_UP));
                setStyle("-fx-alignment: CENTER_RIGHT;");
            }
        });
        colDetTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : "$ " + b.setScale(2, RoundingMode.HALF_UP));
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
            }
        });

        tblReporte.setItems(lista);
        tblDetalle.setItems(detalle);

        SortTable.agregarBotones(tblReporte);
        SortTable.agregarBotones(tblDetalle);

        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());

        dpDesde.setOnAction(e -> cargar());
        dpHasta.setOnAction(e -> cargar());

        tblReporte.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) cargarDetalle(sel.getTipo(), sel.getId());
        });

        configurarBusqueda();

        cargar();
    }

    @FXML
    private void cargar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null || hasta == null) return;
        lista.setAll(dao.listarPorRango(desde, hasta));
        detalle.clear();
        lblTotal.setText("");

        BigDecimal total = lista.stream()
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total: $" + total.setScale(2, RoundingMode.HALF_UP));

        if (!lista.isEmpty()) {
            tblReporte.getSelectionModel().selectFirst();
        }
    }

    private void cargarDetalle(String tipo, int id) {
        if (tipo == null || tipo.isEmpty() || id <= 0) return;
        detalle.setAll(dao.listarDetalle(tipo, id));
    }

    private void configurarBusqueda() {
        FilteredList<DetalleVentaReporte> filtrados = new FilteredList<>(detalle, p -> true);
        tblDetalle.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtrados.setPredicate(d -> (d.getCodigoProducto() + " " + d.getDescripcion()).toLowerCase().contains(texto));
            }
        });
    }

    @FXML
    private void exportarExcel() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte");
        fc.setInitialFileName("Comprobantes_Venta_" + desde + "_" + hasta + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File archivo = fc.showSaveDialog(tblReporte.getScene().getWindow());
        if (archivo == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            List<DetalleVentaReporte> lineas = dao.listarDetallePorRango(desde, hasta);

            Sheet shComp = wb.createSheet("Comprobantes");
            String[] h1 = {"Tipo", "Código", "Fecha", "Cliente", "Forma de Pago", "Items", "Total"};
            Row hr1 = shComp.createRow(0);
            for (int i = 0; i < h1.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hr1.createCell(i);
                c.setCellValue(h1[i]);
                c.setCellStyle(bold);
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowIdx = 1;
            for (VentaResumen v : lista) {
                Row row = shComp.createRow(rowIdx++);
                row.createCell(0).setCellValue(v.getTipo());
                row.createCell(1).setCellValue(v.getCodigo());
                row.createCell(2).setCellValue(v.getFecha() != null ? v.getFecha().format(fmt) : "");
                row.createCell(3).setCellValue(v.getCliente());
                row.createCell(4).setCellValue(v.getFormaPago());
                row.createCell(5).setCellValue(v.getItems());
                row.createCell(6).setCellValue(v.getTotal() != null ? v.getTotal().doubleValue() : 0);
            }
            for (int i = 0; i < h1.length; i++) shComp.autoSizeColumn(i);

            Sheet shDetalle = wb.createSheet("Productos");
            String[] h2 = {"Tipo", "Nº Comprobante", "Cliente", "Código", "Producto", "Cantidad", "P. Unitario", "IVA", "Total"};
            Row hr2 = shDetalle.createRow(0);
            for (int i = 0; i < h2.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hr2.createCell(i);
                c.setCellValue(h2[i]);
                c.setCellStyle(bold);
            }
            rowIdx = 1;
            for (DetalleVentaReporte d : lineas) {
                Row row = shDetalle.createRow(rowIdx++);
                row.createCell(0).setCellValue(d.getTipo());
                row.createCell(1).setCellValue(d.getCodigoComprobante());
                row.createCell(2).setCellValue(d.getCliente());
                row.createCell(3).setCellValue(d.getCodigoProducto());
                row.createCell(4).setCellValue(d.getDescripcion());
                row.createCell(5).setCellValue(d.getCantidad());
                row.createCell(6).setCellValue(d.getPrecioUnitario() != null ? d.getPrecioUnitario().doubleValue() : 0);
                row.createCell(7).setCellValue(d.getIva() != null ? d.getIva().doubleValue() : 0);
                row.createCell(8).setCellValue(d.getTotal() != null ? d.getTotal().doubleValue() : 0);
            }
            for (int i = 0; i < h2.length; i++) shDetalle.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                wb.write(fos);
            }

            new Alert(Alert.AlertType.INFORMATION, "Reporte exportado correctamente.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al exportar: " + e.getMessage()).show();
        }
    }
}

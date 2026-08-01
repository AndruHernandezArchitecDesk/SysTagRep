package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.FacturaProveedorDAO;
import com.tag.sysTagRep.model.FacturaProveedor;
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

public class CompraReporteController implements Initializable {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<FacturaProveedor> tblFacturas;
    @FXML private TableColumn<FacturaProveedor, String> colFactura;
    @FXML private TableColumn<FacturaProveedor, String> colFacturaProveedor;
    @FXML private TableColumn<FacturaProveedor, LocalDateTime> colFacturaFecha;
    @FXML private TableColumn<FacturaProveedor, BigDecimal> colFacturaTotal;
    @FXML private TableView<FacturaProveedor> tblDetalle;
    @FXML private TableColumn<FacturaProveedor, String> colDetCodigo;
    @FXML private TableColumn<FacturaProveedor, String> colDetDescripcion;
    @FXML private TableColumn<FacturaProveedor, Integer> colDetCantidad;
    @FXML private TableColumn<FacturaProveedor, BigDecimal> colDetCosto;
    @FXML private TableColumn<FacturaProveedor, BigDecimal> colDetIva;
    @FXML private TableColumn<FacturaProveedor, BigDecimal> colDetTotal;
    @FXML private Label lblTotal;

    private final FacturaProveedorDAO dao = new FacturaProveedorDAO();
    private ObservableList<FacturaProveedor> facturas = FXCollections.observableArrayList();
    private final ObservableList<FacturaProveedor> detalle = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colFactura.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colFacturaProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colFacturaTotal.setCellValueFactory(new PropertyValueFactory<>("totalLinea"));

        colFacturaFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFacturaFecha.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? "" : f.toLocalDate().format(FMT_FECHA));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colFacturaTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : "$ " + b.setScale(2, RoundingMode.HALF_UP));
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold;");
            }
        });

        colDetCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDetDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colDetCosto.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colDetIva.setCellValueFactory(new PropertyValueFactory<>("iva"));
        colDetTotal.setCellValueFactory(new PropertyValueFactory<>("totalLinea"));

        colDetCantidad.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "" : String.valueOf(n));
                setStyle("-fx-alignment: CENTER;");
            }
        });
        colDetCosto.setCellFactory(c -> new TableCell<>() {
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

        tblFacturas.setItems(facturas);
        tblDetalle.setItems(detalle);

        SortTable.agregarBotones(tblFacturas);
        SortTable.agregarBotones(tblDetalle);

        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());

        dpDesde.setOnAction(e -> cargar());
        dpHasta.setOnAction(e -> cargar());

        tblFacturas.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) cargarDetalle(sel.getNumeroFactura());
        });

        configurarBusqueda();

        cargar();
    }

    @FXML
    private void cargar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null || hasta == null) return;
        facturas.setAll(dao.listarFacturas(desde, hasta));
        detalle.clear();
        lblTotal.setText("");

        BigDecimal total = facturas.stream()
                .map(f -> f.getTotalLinea() != null ? f.getTotalLinea() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total: $" + total.setScale(2, RoundingMode.HALF_UP));

        if (!facturas.isEmpty()) {
            tblFacturas.getSelectionModel().selectFirst();
        }
    }

    private void cargarDetalle(String numeroFactura) {
        if (numeroFactura == null || numeroFactura.isEmpty()) return;
        detalle.setAll(dao.listarPorFactura(numeroFactura));
    }

    private void configurarBusqueda() {
        FilteredList<FacturaProveedor> filtrados = new FilteredList<>(detalle, p -> true);
        tblDetalle.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtrados.setPredicate(l -> (l.getCodigo() + " " + l.getDescripcion()).toLowerCase().contains(texto));
            }
        });
    }

    @FXML
    private void exportarExcel() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte");
        fc.setInitialFileName("Comprobantes_Compra_" + desde + "_" + hasta + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File archivo = fc.showSaveDialog(tblFacturas.getScene().getWindow());
        if (archivo == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            List<FacturaProveedor> lineas = dao.listarDetallePorRango(desde, hasta);

            Sheet shFacturas = wb.createSheet("Facturas");
            String[] h1 = {"Nº Factura", "Proveedor", "Fecha", "Total"};
            Row hr1 = shFacturas.createRow(0);
            for (int i = 0; i < h1.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hr1.createCell(i);
                c.setCellValue(h1[i]);
                c.setCellStyle(bold);
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int rowIdx = 1;
            for (FacturaProveedor f : facturas) {
                Row row = shFacturas.createRow(rowIdx++);
                row.createCell(0).setCellValue(f.getNumeroFactura());
                row.createCell(1).setCellValue(f.getProveedor());
                row.createCell(2).setCellValue(f.getFecha() != null ? f.getFecha().format(fmt) : "");
                row.createCell(3).setCellValue(f.getTotalLinea() != null ? f.getTotalLinea().doubleValue() : 0);
            }
            for (int i = 0; i < h1.length; i++) shFacturas.autoSizeColumn(i);

            Sheet shDetalle = wb.createSheet("Productos");
            String[] h2 = {"Nº Factura", "Proveedor", "Código", "Producto", "Cantidad", "Costo s/IVA", "IVA", "Total"};
            Row hr2 = shDetalle.createRow(0);
            for (int i = 0; i < h2.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hr2.createCell(i);
                c.setCellValue(h2[i]);
                c.setCellStyle(bold);
            }
            rowIdx = 1;
            for (FacturaProveedor l : lineas) {
                Row row = shDetalle.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getNumeroFactura());
                row.createCell(1).setCellValue(l.getProveedor());
                row.createCell(2).setCellValue(l.getCodigo());
                row.createCell(3).setCellValue(l.getDescripcion());
                row.createCell(4).setCellValue(l.getCantidad());
                row.createCell(5).setCellValue(l.getCostoSinIVA() != null ? l.getCostoSinIVA().doubleValue() : 0);
                row.createCell(6).setCellValue(l.getIva() != null ? l.getIva().doubleValue() : 0);
                row.createCell(7).setCellValue(l.getTotalLinea() != null ? l.getTotalLinea().doubleValue() : 0);
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

package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.VentaResumenDAO;
import com.tag.sysTagRep.model.VentaResumen;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @FXML private Label lblTotal;

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

        tblReporte.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());

        dpDesde.setOnAction(e -> cargar());
        dpHasta.setOnAction(e -> cargar());

        cargar();
        filtroBusqueda();
    }

    @FXML
    private void cargar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        if (desde == null || hasta == null) return;
        lista.setAll(dao.listarPorRango(desde, hasta));
        tblReporte.setItems(lista);

        BigDecimal suma = lista.stream()
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total: $" + suma);
    }

    @FXML
    private void exportarExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte");
        fc.setInitialFileName("Comprobantes_" + dpDesde.getValue() + "_" + dpHasta.getValue() + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File archivo = fc.showSaveDialog(tblReporte.getScene().getWindow());
        if (archivo == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Comprobantes");
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            String[] headers = {"Tipo", "Código", "Fecha", "Cliente", "Forma de Pago", "Items", "Total"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(bold);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowIdx = 1;
            for (VentaResumen v : lista) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(v.getTipo());
                row.createCell(1).setCellValue(v.getCodigo());
                row.createCell(2).setCellValue(v.getFecha() != null ? v.getFecha().format(fmt) : "");
                row.createCell(3).setCellValue(v.getCliente());
                row.createCell(4).setCellValue(v.getFormaPago());
                row.createCell(5).setCellValue(v.getItems());
                row.createCell(6).setCellValue(v.getTotal() != null ? v.getTotal().doubleValue() : 0);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                wb.write(fos);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Reporte exportado correctamente.");
            alert.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al exportar: " + e.getMessage());
            alert.show();
        }
    }

    private void filtroBusqueda() {
        FilteredList<VentaResumen> filtrados = new FilteredList<>(lista, p -> true);
        tblReporte.setItems(filtrados);
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

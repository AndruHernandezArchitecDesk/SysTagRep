package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.model.Inventario;
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

public class CompraReporteController implements Initializable {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Inventario> tblReporte;
    @FXML private TableColumn<Inventario, String> colCodigo;
    @FXML private TableColumn<Inventario, String> colDescripcion;
    @FXML private TableColumn<Inventario, String> colProveedor;
    @FXML private TableColumn<Inventario, BigDecimal> colCosto;
    @FXML private TableColumn<Inventario, Integer> colCantidad;
    @FXML private TableColumn<Inventario, BigDecimal> colTotal;
    @FXML private TableColumn<Inventario, String> colFormaPago;
    @FXML private TableColumn<Inventario, LocalDateTime> colFecha;
    @FXML private Label lblTotal;

    private final InventarioDAO dao = new InventarioDAO();
    private ObservableList<Inventario> lista = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoSinIVA"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colTotal.setCellValueFactory(cellData -> {
            Inventario i = cellData.getValue();
            BigDecimal total = i.getCostoSinIVA() != null
                    ? i.getCostoSinIVA().multiply(BigDecimal.valueOf(i.getCantidad()))
                    : BigDecimal.ZERO;
            return new javafx.beans.property.SimpleObjectProperty<>(total);
        });
        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha_ingreso"));

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
                .map(i -> {
                    BigDecimal c = i.getCostoSinIVA() != null ? i.getCostoSinIVA() : BigDecimal.ZERO;
                    return c.multiply(BigDecimal.valueOf(i.getCantidad()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText("Total: $" + suma);
    }

    @FXML
    private void exportarExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte");
        fc.setInitialFileName("Compras_" + dpDesde.getValue() + "_" + dpHasta.getValue() + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File archivo = fc.showSaveDialog(tblReporte.getScene().getWindow());
        if (archivo == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Compras");
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            String[] headers = {"Código", "Producto", "Proveedor", "Costo s/IVA", "Cantidad", "Total", "Forma de Pago", "Fecha"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(bold);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowIdx = 1;
            for (Inventario inv : lista) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(inv.getCodigo());
                row.createCell(1).setCellValue(inv.getDescripcion());
                row.createCell(2).setCellValue(inv.getProveedor());
                row.createCell(3).setCellValue(inv.getCostoSinIVA() != null ? inv.getCostoSinIVA().doubleValue() : 0);
                row.createCell(4).setCellValue(inv.getCantidad());
                BigDecimal total = inv.getCostoSinIVA() != null
                        ? inv.getCostoSinIVA().multiply(BigDecimal.valueOf(inv.getCantidad()))
                        : BigDecimal.ZERO;
                row.createCell(5).setCellValue(total.doubleValue());
                row.createCell(6).setCellValue(inv.getFormaPago());
                row.createCell(7).setCellValue(inv.getFecha_ingreso() != null ? inv.getFecha_ingreso().format(fmt) : "");
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
        FilteredList<Inventario> filtrados = new FilteredList<>(lista, p -> true);
        tblReporte.setItems(filtrados);
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtrados.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtrados.setPredicate(i -> (i.getCodigo() + " " + i.getDescripcion() + " " +
                        i.getProveedor() + " " + i.getFormaPago()).toLowerCase().contains(texto));
            }
        });
    }
}

package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.CuentaPorPagarDAO;
import com.tag.sysTagRep.dao.FacturaDetalleDAO;
import com.tag.sysTagRep.dao.FacturaProveedorDAO;
import com.tag.sysTagRep.dao.HistorialProductoDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.UbicacionDetalleDAO;
import com.tag.sysTagRep.model.FacturaProveedor;
import com.tag.sysTagRep.model.Inventario;
import com.tag.sysTagRep.util.SortTable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FacturasIngresadasController implements Initializable {

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
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final CuentaPorPagarDAO cuentaPorPagarDAO = new CuentaPorPagarDAO();
    private final FacturaDetalleDAO facturaDetalleDAO = new FacturaDetalleDAO();
    private final HistorialProductoDAO historialProductoDAO = new HistorialProductoDAO();
    private final UbicacionDetalleDAO ubicacionDAO = new UbicacionDetalleDAO();
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
        fc.setInitialFileName("Facturas_Ingresadas_" + desde + "_" + hasta + ".xlsx");
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

    @FXML
    private void editarFactura() {
        FacturaProveedor sel = tblFacturas.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getNumeroFactura() == null || sel.getNumeroFactura().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una factura para editar.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/IngresoMercaderiaView.fxml"));
            Parent vista = loader.load();
            IngresoMercaderiaController controller = loader.getController();
            controller.setCerrarAlGuardar(true);
            controller.cargarFacturaParaEdicion(sel.getNumeroFactura(), sel.getProveedorId());
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Editar Factura Ingresada");
            modal.setScene(new Scene(vista, 820, 720));
            modal.showAndWait();
            cargar();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al abrir edición: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void eliminarFactura() {
        FacturaProveedor sel = tblFacturas.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getNumeroFactura() == null || sel.getNumeroFactura().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una factura para eliminar.").showAndWait();
            return;
        }
        String numero = sel.getNumeroFactura();
        int proveedorId = sel.getProveedorId();

        List<Inventario> inventarios = inventarioDAO.listarPorNumeroFactura(numero, proveedorId);
        List<Integer> ids = new ArrayList<>();
        for (Inventario inv : inventarios) ids.add(inv.getId());

        if (!ids.isEmpty() && (facturaDetalleDAO.existeVentaPorInventarioIds(ids)
                || historialProductoDAO.existeVentaPorInventarioIds(ids))) {
            new Alert(Alert.AlertType.WARNING,
                    "No se puede eliminar la factura: uno de sus productos ya fue vendido.").showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la factura " + numero + "?\nSe borrará el comprobante, los productos del inventario "
                        + (ids.isEmpty() ? "" : "y el crédito pendiente ") + "asociados.",
                ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            inventarioDAO.eliminarFacturaConDependencias(numero, proveedorId);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Error al eliminar la factura: " + e.getMessage() + "\nNo se modificó ningún dato.").showAndWait();
            return;
        }

        new Alert(Alert.AlertType.INFORMATION, "Factura eliminada correctamente.").showAndWait();
        cargar();
    }
}

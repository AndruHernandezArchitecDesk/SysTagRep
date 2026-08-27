package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ClienteDAO;
import com.tag.sysTagRep.dao.ComprobanteTempDAO;
import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.util.EmailService;
import com.tag.sysTagRep.dao.CuentaPorCobrarDAO;
import com.tag.sysTagRep.dao.HistorialProductoDAO;
import com.tag.sysTagRep.dao.NotaVentaDetalleDAO;
import com.tag.sysTagRep.dao.NotaVentaRegistroDAO;
import com.tag.sysTagRep.dao.SecuenciaDocumentoDAO;
import com.tag.sysTagRep.model.*;
import com.tag.sysTagRep.util.NotaVentaPDF;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class NotaVentaController implements Initializable {

    @FXML private ImageView imgLogo;
    @FXML private Label lblRazonSocial, lblRuc, lblDireccion, lblCorreo, lblTelefono, lblNumNotaVenta;
    
    // Cliente
    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private Button btnAgregarCliente;
    @FXML private TextField txtNombre, txtIdentificacion, txtDireccion, txtCorreo, txtTelefono;
    @FXML private DatePicker dpFechaNotaVenta;
    
    // Tabla Busqueda Inventario
    @FXML private TextField txtBuscarProducto;
    @FXML private TableView<Inventario> tblInventarioBusqueda;
    @FXML private TableColumn<Inventario, String> colInvCodigo, colInvDescripcion;
    @FXML private TableColumn<Inventario, String> colInvMarca;
    @FXML private TableColumn<Inventario, Integer> colInvStock;
    @FXML private TableColumn<Inventario, BigDecimal> colInvPrecio;
    @FXML private TableColumn<Inventario, String> colInvUbicacion;
    @FXML private Button btnProductoTemporal;

    // Tabla Detalle
    @FXML private TableView<DetalleVenta> tblDetalle;
    @FXML private TableColumn<DetalleVenta, String> colCodigo, colDescripcion;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colPrecioUnitario, colPrecioTotal;
    @FXML private TableColumn<DetalleVenta, Void> colAcciones;

    // Totales
    @FXML private Label lblSubtotal, lblIva, lblDescuento, lblTotal;
    @FXML private TextField txtDescuento;
    @FXML private ComboBox<String> cmbFormaPago;
    @FXML private ComboBox<String> cmbDescuento;
    @FXML private HBox pnlCredito;
    @FXML private ComboBox<Integer> cmbMesesPlazo;
    @FXML private ComboBox<String> cmbInteres;

    private final EmpresaDAO daoEmpresa = new EmpresaDAO();
    private final ClienteDAO daoCliente = new ClienteDAO();
    private final InventarioDAO daoInventario = new InventarioDAO();
    private final NotaVentaRegistroDAO daoNotaVentaRegistro = new NotaVentaRegistroDAO();
    private final SecuenciaDocumentoDAO secuenciaDAO = new SecuenciaDocumentoDAO();
    private final NotaVentaDetalleDAO daoNotaVentaDetalle = new NotaVentaDetalleDAO();
    private final ComprobanteTempDAO daoComprobanteTemp = new ComprobanteTempDAO();
    private final CuentaPorCobrarDAO daoCuentaPorCobrar = new CuentaPorCobrarDAO();
    private final LogDAO logDAO = new LogDAO();
    private final HistorialProductoDAO historialProductoDAO = new HistorialProductoDAO();

    private Empresa empresaActual;

    private ObservableList<Cliente> clientes;
    private ObservableList<Inventario> listaInventario = FXCollections.observableArrayList();
    private ObservableList<DetalleVenta> itemsDetalle = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaNotaVenta.setValue(LocalDate.now());
        cargarLogo();
        obtenerDatosEmpresa();
        obtenerNumNotaVenta();
        
        cargarListaClientes();
        cargarFormasPago();
        cargarCredito();
        cargarDescuento();
        
        iniciarTablaInventario();
        iniciarTablaDetalle();

        tblInventarioBusqueda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblDetalle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblInventarioBusqueda);
        SortTable.agregarBotones(tblDetalle);
    }

    private void cargarFormasPago() {
        ComboFilter.habilitar(cmbFormaPago, FXCollections.observableArrayList(
            "Efectivo", "Tarjeta de Crédito", "Tarjeta de Débito",
            "Transferencia", "Depósito", "Cheque", "TAG Crédito"
        ));
        cmbFormaPago.getSelectionModel().selectFirst();
    }

    private void cargarCredito() {
        ComboFilter.habilitarEnteros(cmbMesesPlazo, FXCollections.observableArrayList(5, 10, 15, 20, 25, 30));
        cmbMesesPlazo.getSelectionModel().selectFirst();

        ComboFilter.habilitar(cmbInteres, FXCollections.observableArrayList("0", "3", "6", "9", "12", "15"));
        cmbInteres.getSelectionModel().selectFirst();

        cmbFormaPago.valueProperty().addListener((obs, old, valor) -> {
            boolean esCredito = "TAG Crédito".equals(valor);
            pnlCredito.setVisible(esCredito);
            pnlCredito.setManaged(esCredito);
        });
    }

    private void cargarDescuento() {
        ObservableList<String> descuentos = FXCollections.observableArrayList();
        for (int i = 0; i <= 100; i += 5) descuentos.add(String.valueOf(i));
        ComboFilter.habilitar(cmbDescuento, descuentos);
        cmbDescuento.setValue("0");
        cmbDescuento.getEditor().setPromptText("0.00 valor fijo");
        cmbDescuento.valueProperty().addListener((obs, old, val) -> calcularTotales());
        cmbDescuento.getEditor().textProperty().addListener((obs, old, val) -> calcularTotales());
        cmbDescuento.getEditor().textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("\\d*\\.?\\d*")) cmbDescuento.getEditor().setText(old);
        });
        if (txtDescuento != null) {
            txtDescuento.setText("0.00");
            txtDescuento.textProperty().addListener((obs, old, val) -> {
                if (val != null && !val.matches("\\d*\\.?\\d*")) { txtDescuento.setText(old); return; }
                calcularTotales();
            });
        }
    }

    private void cargarListaClientes() {
        clientes = FXCollections.observableArrayList(daoCliente.obtenerListaClientes());
        ComboFilter.habilitar(cmbCliente, clientes, new StringConverter<Cliente>() {
            @Override public String toString(Cliente c) { return c == null ? "" : c.getNombre() + " - " + c.getIdentificacion(); }
            @Override public Cliente fromString(String s) { return null; }
        });

        cmbCliente.valueProperty().addListener((obs, old, cliente) -> {
            if (cliente != null) {
                txtNombre.setText(cliente.getNombre());
                txtIdentificacion.setText(cliente.getIdentificacion());
                txtDireccion.setText(cliente.getDireccion());
                txtCorreo.setText(cliente.getCorreo());
                txtTelefono.setText(cliente.getTelefono() + " - " + (cliente.getCelular() != null && !cliente.getCelular().isEmpty() ? cliente.getCelular() : ""));
            }
        });
    }

    @FXML
    private void abrirCRUDClientes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ClienteView.fxml"));
            Parent vista = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Administración de Clientes");
            stage.setScene(new Scene(vista, 900, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cargarListaClientes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarTablaInventario() {
        colInvCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colInvDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colInvMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colInvStock.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colInvPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colInvUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacionPercha"));

        listaInventario.setAll(daoInventario.listar());
        FilteredList<Inventario> filtradosProd = new FilteredList<>(listaInventario, p -> true);
        
        txtBuscarProducto.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                filtradosProd.setPredicate(p -> true);
            } else {
                String texto = val.toLowerCase();
                filtradosProd.setPredicate(inv -> {
                    String searchStr = (inv.getCodigo() + " " + inv.getDescripcion() + " " + inv.getGrupo() + " " + inv.getMarca()).toLowerCase();
                    return searchStr.contains(texto);
                });
            }
        });
        
        tblInventarioBusqueda.setItems(filtradosProd);
    }

    private void iniciarTablaDetalle() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecioUnitario.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colPrecioTotal.setCellValueFactory(new PropertyValueFactory<>("precioTotal"));

        // Indicador temporal en codigo y descripcion
        colCodigo.setCellFactory(col -> new TableCell<DetalleVenta, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    DetalleVenta d = getTableView().getItems().get(getIndex());
                    if (d.getProductoId() == 0) {
                        setText(item + " \u2022 TEMP");
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    } else { setText(item); setStyle(""); }
                }
            }
        });
        colDescripcion.setCellFactory(col -> new TableCell<DetalleVenta, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    DetalleVenta d = getTableView().getItems().get(getIndex());
                    if (d.getProductoId() == 0) {
                        setText(item + " [TEMPORAL]");
                        setStyle("-fx-text-fill: #e67e22;");
                    } else { setText(item); setStyle(""); }
                }
            }
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setGraphic(new FontIcon(FontAwesomeSolid.TRASH));
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btn.setOnAction(e -> { itemsDetalle.remove(getTableView().getItems().get(getIndex())); calcularTotales(); });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else { setGraphic(btn); setAlignment(Pos.CENTER); }
            }
        });
        colAcciones.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));
        tblDetalle.setItems(itemsDetalle);
        tblDetalle.setRowFactory(tv -> new TableRow<DetalleVenta>() {
            @Override protected void updateItem(DetalleVenta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else if (item.getProductoId() == 0) setStyle("-fx-background-color: #fef9e7;");
                else setStyle("");
            }
        });
    }

    @FXML
    private void agregarProducto() {
        Inventario i = tblInventarioBusqueda.getSelectionModel().getSelectedItem();
        if (i == null) return;

        int stockDisponible = calcularStockDisponible(i);
        if (stockDisponible <= 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Stock insuficiente");
            alert.setHeaderText(null);
            alert.setContentText("No hay stock disponible para: " + i.getDescripcion());
            alert.showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Cantidad");
        dialog.setHeaderText(i.getDescripcion());
        dialog.setContentText("Stock disponible: " + stockDisponible + "\nCantidad a vender:");

        dialog.showAndWait().ifPresent(cantStr -> {
            try {
                int cant = Integer.parseInt(cantStr);
                if (cant <= 0) return;

                if (cant > stockDisponible) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock insuficiente");
                    alert.setHeaderText(null);
                    alert.setContentText("La cantidad ingresada (" + cant + ") excede el stock disponible (" + stockDisponible + ").");
                    alert.showAndWait();
                    return;
                }

                boolean existe = false;
                for (DetalleVenta d : itemsDetalle) {
                    if (d.getProductoId() == i.getId()) {
                        d.setCantidad(d.getCantidad() + cant);
                        existe = true;
                        break;
                    }
                }

                if (!existe) {
                    itemsDetalle.add(new DetalleVenta(i.getId(), i.getCodigo(), i.getDescripcion(), cant, i.getPrecioVenta()));
                }

                tblDetalle.refresh();
                calcularTotales();
            } catch (NumberFormatException ignored) {}
        });
    }

    private int calcularStockDisponible(Inventario inventario) {
        int stockTotal = inventario.getCantidad();
        int cantidadEnDetalle = 0;
        for (DetalleVenta d : itemsDetalle) {
            if (d.getProductoId() == inventario.getId()) {
                cantidadEnDetalle += d.getCantidad();
            }
        }
        return stockTotal - cantidadEnDetalle;
    }

    @FXML
    private void mostrarModalProductoTemporal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Producto Temporal (Fantasma)");
        dialog.setHeaderText("Registrar producto que no está en inventario");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 15;");

        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Ej: TMP-001");
        txtCodigo.textProperty().addListener((obs, old, val) -> { if (val != null && !val.equals(val.toUpperCase())) { txtCodigo.setText(val.toUpperCase()); }});
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Descripción");
        txtDesc.textProperty().addListener((obs, old, val) -> { if (val != null && !val.equals(val.toUpperCase())) { txtDesc.setText(val.toUpperCase()); }});
        TextField txtCant = new TextField("1");
        txtCant.setPromptText("1");
        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("0.00");

        grid.add(new Label("Código*:"), 0, 0); grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Descripción*:"), 0, 1); grid.add(txtDesc, 1, 1);
        grid.add(new Label("Cantidad*:"), 0, 2); grid.add(txtCant, 1, 2);
        grid.add(new Label("P. Unitario*:"), 0, 3); grid.add(txtPrecio, 1, 3);
        grid.getColumnConstraints().addAll(new javafx.scene.layout.ColumnConstraints(110), new javafx.scene.layout.ColumnConstraints(260));

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Añadir");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().clear();

        // Simple validación en OK: campos obligatorios (ingreso manual)
        dialog.setResultConverter(btn -> btn);

        java.util.Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String codigo = txtCodigo.getText() != null ? txtCodigo.getText().trim().toUpperCase() : "";
            String desc = txtDesc.getText() != null ? txtDesc.getText().trim().toUpperCase() : "";
            String sCant = txtCant.getText() != null ? txtCant.getText().trim() : "";
            String sPrecio = txtPrecio.getText() != null ? txtPrecio.getText().trim() : "";

            if (codigo.isEmpty() || desc.isEmpty() || sCant.isEmpty() || sPrecio.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Todos los campos son obligatorios (código, descripción, cantidad, p. unitario).").showAndWait();
                return;
            }
            int cant;
            BigDecimal pUnit;
            try {
                cant = Integer.parseInt(sCant);
                pUnit = new BigDecimal(sPrecio);
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.WARNING, "Cantidad debe ser entero y P. Unitario numérico.").showAndWait();
                return;
            }
            if (cant <= 0 || pUnit.compareTo(BigDecimal.ZERO) <= 0) {
                new Alert(Alert.AlertType.WARNING, "Cantidad y P. Unitario deben ser mayores a 0.").showAndWait();
                return;
            }
            // productoId = 0 indica temporal (fantasma) -> indicador visual TEMPORAL
            DetalleVenta temp = new DetalleVenta(0, codigo, desc, cant, pUnit);
            itemsDetalle.add(temp);
            tblDetalle.refresh();
            calcularTotales();
        }
    }

    private void calcularTotales() {
        BigDecimal subtotal = itemsDetalle.stream().map(DetalleVenta::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBruto = subtotal.add(iva);
        BigDecimal descuento = calcularDescuento(totalBruto);
        BigDecimal total = totalBruto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        
        lblSubtotal.setText(subtotal.setScale(2, RoundingMode.HALF_UP).toString());
        lblIva.setText(iva.toString());
        lblDescuento.setText(descuento.toString());
        lblTotal.setText(total.toString());
    }

    private BigDecimal calcularDescuento(BigDecimal totalBruto) {
        BigDecimal fijo = BigDecimal.ZERO;
        try {
            String txt = null;
            if (txtDescuento != null && txtDescuento.getText() != null && !txtDescuento.getText().trim().isEmpty()) txt = txtDescuento.getText().trim();
            else if (cmbDescuento.getEditor() != null) txt = cmbDescuento.getEditor().getText();
            if (txt != null && !txt.trim().isEmpty()) fijo = new BigDecimal(txt.trim());
            else if (cmbDescuento.getValue() != null && !cmbDescuento.getValue().trim().isEmpty()) fijo = new BigDecimal(cmbDescuento.getValue().trim());
        } catch (NumberFormatException ignored) {}
        if (fijo.compareTo(BigDecimal.ZERO) < 0) fijo = BigDecimal.ZERO;
        if (fijo.compareTo(totalBruto) > 0) fijo = totalBruto;
        return fijo.setScale(2, RoundingMode.HALF_UP);
    }

    private void cargarLogo() {
        try { imgLogo.setImage(new Image(getClass().getResourceAsStream("/img/logoTag.jpeg"))); } catch (Exception ignored) {}
    }

    private void obtenerDatosEmpresa() {
        List<Empresa> empresas = daoEmpresa.listar();
        if (!empresas.isEmpty()) {
            empresaActual = empresas.get(0);
            Empresa e = empresaActual;
            lblRazonSocial.setText(e.getRazonSocial());
            lblDireccion.setText(e.getDireccionCallePrincipal() + " y " + e.getDireccionCalleSecundaria());
            lblRuc.setText(e.getRuc());
            lblCorreo.setText(e.getCorreo());
            lblTelefono.setText(e.getTelefono() + " / " + e.getCelular());
        }
    }

    private void obtenerNumNotaVenta() {
        SecuenciaDocumento sec = secuenciaDAO.obtener("PROFORMA");
        lblNumNotaVenta.setText(sec.getProximoCodigo());
    }

    @FXML
    private void guardar() {
    try {
        if (cmbCliente.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "Debe seleccionar un cliente.").showAndWait();
            return;
        }
        if (itemsDetalle.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Debe agregar al menos un producto.").showAndWait();
            return;
        }
        if (empresaActual == null) {
            new Alert(Alert.AlertType.WARNING, "No se encontraron datos de la empresa.").showAndWait();
            return;
        }

        int clienteId = cmbCliente.getValue().getId();
        int empresaId = empresaActual.getId();
        String codigo = lblNumNotaVenta.getText();
        LocalDateTime ahora = LocalDateTime.now();

        if (secuenciaDAO.existeCodigoNotaVenta(codigo)) {
            new Alert(Alert.AlertType.ERROR, "El número " + codigo + " ya existe en la base de datos, no se puede repetir.").showAndWait();
            obtenerNumNotaVenta();
            return;
        }

        NotaVentaRegistro nvr = new NotaVentaRegistro(empresaId, clienteId, ahora, codigo, cmbFormaPago.getValue(), ahora);
        int notaVentaId = daoNotaVentaRegistro.insertar(nvr);

        if (notaVentaId == -1) {
            new Alert(Alert.AlertType.ERROR, "Error al registrar la proforma.").showAndWait();
            return;
        }

        secuenciaDAO.marcarUsado("PROFORMA");

        daoNotaVentaDetalle.insertarDetalle(notaVentaId, itemsDetalle);

        // Guardar productos temporales (fantasma) en comprobante_temp — campos resumen venta
        for (DetalleVenta d : itemsDetalle) {
            if (d.getProductoId() == 0) {
                daoComprobanteTemp.insertar(notaVentaId, d.getCodigo(), d.getDescripcion(),
                        d.getCantidad(), d.getPrecioUnitario(), d.getPrecioTotal());
            }
        }

        String clienteNombre = cmbCliente.getValue().getNombre();
        List<HistorialProducto> historial = new ArrayList<>();
        for (DetalleVenta d : itemsDetalle) {
            if (d.getProductoId() == 0) continue; // temporales no tienen inventario -> sin historial
            String provNombre = daoInventario.obtenerProveedorNombre(d.getProductoId());
            historial.add(new HistorialProducto(d.getProductoId(), d.getCodigo(), d.getDescripcion(),
                    d.getCantidad(), d.getPrecioUnitario(), "PROFORMA", codigo,
                    clienteNombre, provNombre, ahora));
        }
        if (!historial.isEmpty()) historialProductoDAO.insertar(historial);

        listaInventario.setAll(daoInventario.listar());

        // Generar PDF
        BigDecimal sub = itemsDetalle.stream().map(DetalleVenta::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ivaCalc = sub.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBruto = sub.add(ivaCalc);
        BigDecimal descCalc = calcularDescuento(totalBruto);
        BigDecimal totCalc = totalBruto.subtract(descCalc).setScale(2, RoundingMode.HALF_UP);

        // Guardar crédito si aplica
        if ("TAG Crédito".equals(cmbFormaPago.getValue())) {
            int dias = cmbMesesPlazo.getValue();
            BigDecimal tasaInteres = new BigDecimal(cmbInteres.getValue()).divide(new BigDecimal("100"));
            BigDecimal totalConInteres = totCalc.multiply(BigDecimal.ONE.add(tasaInteres)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cuotaMensual = totalConInteres.divide(new BigDecimal(dias), 2, RoundingMode.HALF_UP);

            CuentaPorCobrar cpc = new CuentaPorCobrar(notaVentaId, clienteId, totCalc, dias,
                    new BigDecimal(cmbInteres.getValue()), cuotaMensual);
            daoCuentaPorCobrar.insertar(cpc);
        }

        // --- Generar PDF Proforma ---
        String rutaPDF = System.getProperty("java.io.tmpdir") + File.separator
                + "Proforma_" + codigo.replace("-", "") + ".pdf";

        List<String[]> detallesPDF = new ArrayList<>();
        for (DetalleVenta d : itemsDetalle) {
            detallesPDF.add(new String[]{
                d.getCodigo(),
                d.getDescripcion(),
                String.valueOf(d.getCantidad()),
                "$" + d.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP),
                "$" + d.getPrecioTotal().setScale(2, RoundingMode.HALF_UP)
            });
        }

        String fechaStr = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        NotaVentaPDF.generar(rutaPDF, codigo, fechaStr,
                empresaActual.getRazonSocial(), empresaActual.getRuc(),
                empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria(),
                empresaActual.getTelefono() + " / " + empresaActual.getCelular(), empresaActual.getCorreo(),
                cmbCliente.getValue().getNombre(), cmbCliente.getValue().getIdentificacion(),
                txtDireccion.getText(), txtTelefono.getText(), txtCorreo.getText(),
                cmbFormaPago.getValue(), detallesPDF, sub, ivaCalc, descCalc, totCalc);

        Alert alertExito = new Alert(Alert.AlertType.INFORMATION);
        alertExito.setTitle("Proforma registrada");
        alertExito.setHeaderText(null);
        alertExito.setContentText("Proforma " + codigo + " registrada exitosamente.\nPDF: " + rutaPDF);
        alertExito.showAndWait();

        // Abrir PDF
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", rutaPDF});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", rutaPDF});
            }
        } catch (Exception ignored) {}

        // Enviar correo al cliente
        String correoCliente = txtCorreo.getText();
        if (correoCliente != null && !correoCliente.trim().isEmpty()) {
            try {
                EmailService emailService = new EmailService();
                boolean enviado = emailService.enviarCorreoConPDF(correoCliente.trim(), cmbCliente.getValue().getNombre(), codigo, "PROFORMA", new File(rutaPDF));
                if (enviado) {
                    new Alert(Alert.AlertType.INFORMATION, "Correo enviado exitosamente a " + correoCliente).showAndWait();
                } else {
                    new Alert(Alert.AlertType.WARNING, "No se pudo enviar el correo a " + correoCliente + ". Verifique la direccion de correo.").showAndWait();
                }
            } catch (Exception e) {
                logDAO.guardar("NotaVentaController", "enviarCorreo", e.getMessage(), e);
                new Alert(Alert.AlertType.WARNING, "Error al enviar correo: " + e.getMessage()).showAndWait();
            }
        }

        itemsDetalle.clear();
        tblDetalle.refresh();
        if (txtDescuento != null) txtDescuento.setText("0.00");
        cmbDescuento.setValue("0");
        calcularTotales();
        cmbCliente.getSelectionModel().clearSelection();
        cmbCliente.getEditor().clear();
        txtNombre.clear();
        txtIdentificacion.clear();
        txtDireccion.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtBuscarProducto.clear();
        obtenerNumNotaVenta();
    } catch (Exception e) {
        logDAO.guardar("NotaVentaController", "guardar", e.getMessage(), e);
        new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
    }
    }
}

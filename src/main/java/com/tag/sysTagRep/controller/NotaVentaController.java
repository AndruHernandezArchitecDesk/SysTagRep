package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ClienteDAO;
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
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
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
    @FXML private TextField txtNombre, txtIdentificacion, txtDireccion, txtCorreo, txtTelefono;
    @FXML private DatePicker dpFechaNotaVenta;
    
    // Tabla Busqueda Inventario
    @FXML private TextField txtBuscarProducto;
    @FXML private TableView<Inventario> tblInventarioBusqueda;
    @FXML private TableColumn<Inventario, String> colInvCodigo, colInvDescripcion;
    @FXML private TableColumn<Inventario, Integer> colInvStock;
    @FXML private TableColumn<Inventario, BigDecimal> colInvPrecio;

    // Tabla Detalle
    @FXML private TableView<DetalleVenta> tblDetalle;
    @FXML private TableColumn<DetalleVenta, String> colCodigo, colDescripcion;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colPrecioUnitario, colPrecioTotal;
    @FXML private TableColumn<DetalleVenta, Void> colAcciones;

    // Totales
    @FXML private Label lblSubtotal, lblIva, lblDescuento, lblTotal;
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
        cmbDescuento.valueProperty().addListener((obs, old, val) -> calcularTotales());
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

    private void iniciarTablaInventario() {
        colInvCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colInvDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colInvStock.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colInvPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));

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
        BigDecimal pct = BigDecimal.ZERO;
        try {
            pct = new BigDecimal(cmbDescuento.getValue() != null ? cmbDescuento.getValue() : "0");
        } catch (NumberFormatException ignored) {}
        return totalBruto.multiply(pct).divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
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

        String clienteNombre = cmbCliente.getValue().getNombre();
        List<HistorialProducto> historial = new ArrayList<>();
        for (DetalleVenta d : itemsDetalle) {
            String provNombre = daoInventario.obtenerProveedorNombre(d.getProductoId());
            historial.add(new HistorialProducto(d.getProductoId(), d.getCodigo(), d.getDescripcion(),
                    d.getCantidad(), d.getPrecioUnitario(), "PROFORMA", codigo,
                    clienteNombre, provNombre, ahora));
        }
        historialProductoDAO.insertar(historial);

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

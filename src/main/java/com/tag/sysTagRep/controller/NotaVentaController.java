package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ClienteDAO;
import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.NotaVentaDetalleDAO;
import com.tag.sysTagRep.dao.NotaVentaRegistroDAO;
import com.tag.sysTagRep.model.*;
import com.tag.sysTagRep.util.NotaVentaPDF;
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
import java.awt.Desktop;
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
    @FXML private TextField txtBuscarCliente;
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
    @FXML private Label lblSubtotal, lblIva, lblTotal;
    @FXML private ComboBox<String> cmbFormaPago;

    private final EmpresaDAO daoEmpresa = new EmpresaDAO();
    private final ClienteDAO daoCliente = new ClienteDAO();
    private final InventarioDAO daoInventario = new InventarioDAO();
    private final NotaVentaRegistroDAO daoNotaVentaRegistro = new NotaVentaRegistroDAO();
    private final NotaVentaDetalleDAO daoNotaVentaDetalle = new NotaVentaDetalleDAO();

    private Empresa empresaActual;

    private ObservableList<Cliente> clientes;
    private FilteredList<Cliente> clientesFiltrados;
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
        
        iniciarTablaInventario();
        iniciarTablaDetalle();

        tblInventarioBusqueda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblDetalle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarFormasPago() {
        cmbFormaPago.setItems(FXCollections.observableArrayList(
            "Efectivo", "Tarjeta de Crédito", "Tarjeta de Débito",
            "Transferencia", "Depósito", "Cheque"
        ));
        cmbFormaPago.getSelectionModel().selectFirst();
    }

    private void cargarListaClientes() {
        clientes = FXCollections.observableArrayList(daoCliente.obtenerListaClientes());
        clientesFiltrados = new FilteredList<>(clientes, p -> true);
        cmbCliente.setItems(clientesFiltrados);
        cmbCliente.setConverter(new StringConverter<Cliente>() {
            @Override public String toString(Cliente c) { return c == null ? "" : c.getNombre() + " - " + c.getIdentificacion(); }
            @Override public Cliente fromString(String s) { return null; }
        });

        txtBuscarCliente.textProperty().addListener((obs, oldText, newText) -> {
            clientesFiltrados.setPredicate(cliente -> {
                if (newText == null || newText.isEmpty()) return true;
                String filtro = newText.toLowerCase();
                return cliente.getNombre().toLowerCase().contains(filtro) || cliente.getIdentificacion().toLowerCase().contains(filtro);
            });
            if (!cmbCliente.isShowing()) cmbCliente.show();
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
        FilteredList<Inventario> filtradosProd = new FilteredList<>(listaInventario, p -> false);
        
        txtBuscarProducto.textProperty().addListener((obs, old, val) -> {
            filtradosProd.setPredicate(i -> val == null || val.isEmpty() ? false : 
                i.getDescripcion().toLowerCase().contains(val.toLowerCase()) || 
                i.getCodigo().toLowerCase().contains(val.toLowerCase()));
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
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);
        
        lblSubtotal.setText(subtotal.setScale(2, RoundingMode.HALF_UP).toString());
        lblIva.setText(iva.toString());
        lblTotal.setText(total.toString());
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
        List<NotaVentaRegistro> nvr = daoNotaVentaRegistro.obtenerNumNotaVenta();
        int nextId = 1;
        for (NotaVentaRegistro r : nvr) {
            try {
                int num = Integer.parseInt(r.getCodigo().replaceAll("\\D+", ""));
                if (num >= nextId) nextId = num + 1;
            } catch (Exception ignored) {}
        }
        lblNumNotaVenta.setText("TAGVIC-" + String.format("%06d", nextId));
    }

    @FXML
    private void guardar() {
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

        NotaVentaRegistro nvr = new NotaVentaRegistro(empresaId, clienteId, ahora, codigo, cmbFormaPago.getValue(), ahora);
        int notaVentaId = daoNotaVentaRegistro.insertar(nvr);

        if (notaVentaId == -1) {
            new Alert(Alert.AlertType.ERROR, "Error al registrar la nota de venta.").showAndWait();
            return;
        }

        daoNotaVentaDetalle.insertarDetalle(notaVentaId, itemsDetalle);

        for (DetalleVenta d : itemsDetalle) {
            daoInventario.descontarStock(d.getProductoId(), d.getCantidad());
        }

        listaInventario.setAll(daoInventario.listar());

        // Generar PDF
        BigDecimal sub = itemsDetalle.stream().map(DetalleVenta::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ivaCalc = sub.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totCalc = sub.add(ivaCalc).setScale(2, RoundingMode.HALF_UP);

        List<String[]> filasPDF = new ArrayList<>();
        for (DetalleVenta d : itemsDetalle) {
            filasPDF.add(new String[]{
                    d.getCodigo(),
                    d.getDescripcion(),
                    String.valueOf(d.getCantidad()),
                    d.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP).toString(),
                    d.getPrecioTotal().setScale(2, RoundingMode.HALF_UP).toString()
            });
        }

        String fechaStr = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String nombreCliente = cmbCliente.getValue().getNombre() + " - " + cmbCliente.getValue().getIdentificacion();

        String rutaPDF = System.getProperty("user.home") + File.separator + "Desktop" + File.separator
                + "NotaVenta_" + codigo.replace(" ", "_") + ".pdf";

        NotaVentaPDF.generar(rutaPDF, codigo, fechaStr,
                empresaActual.getRazonSocial(), empresaActual.getRuc(),
                empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria(),
                empresaActual.getTelefono() + " / " + empresaActual.getCelular(),
                empresaActual.getCorreo(),
                cmbCliente.getValue().getNombre(),
                cmbCliente.getValue().getIdentificacion(),
                txtDireccion.getText(),
                txtTelefono.getText(),
                cmbFormaPago.getValue(),
                filasPDF, sub, ivaCalc, totCalc);

        Alert alertExito = new Alert(Alert.AlertType.INFORMATION);
        alertExito.setTitle("Nota de venta registrada");
        alertExito.setHeaderText(null);
        alertExito.setContentText("Nota de venta " + codigo + " registrada exitosamente.\nPDF guardado en: " + rutaPDF);
        alertExito.showAndWait();

        // Abrir PDF automaticamente
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(rutaPDF));
            }
        } catch (Exception ignored) {}

        itemsDetalle.clear();
        tblDetalle.refresh();
        calcularTotales();
        cmbCliente.getSelectionModel().clearSelection();
        txtBuscarCliente.clear();
        txtNombre.clear();
        txtIdentificacion.clear();
        txtDireccion.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtBuscarProducto.clear();
        obtenerNumNotaVenta();
    }
}

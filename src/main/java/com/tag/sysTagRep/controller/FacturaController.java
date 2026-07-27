package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ClienteDAO;
import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.util.EmailService;
import com.tag.sysTagRep.dao.CuentaPorCobrarDAO;
import com.tag.sysTagRep.dao.ComprobanteDAO;
import com.tag.sysTagRep.dao.FacturaDetalleDAO;
import com.tag.sysTagRep.dao.FacturaRegistroDAO;
import com.tag.sysTagRep.model.*;
import com.tag.sysTagRep.util.NotaVentaPDF;
import com.tag.sysTagRep.util.ClaveAcceso;
import com.tag.sysTagRep.util.XmlSriBuilder;
import com.tag.sysTagRep.util.SRIWebService;
import com.tag.sysTagRep.util.PdfElectronico;
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

public class FacturaController implements Initializable {

    @FXML private ImageView imgLogo;
    @FXML private Label lblRazonSocial, lblRuc, lblDireccion, lblCorreo, lblTelefono, lblNumFactura;

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private TextField txtBuscarCliente;
    @FXML private TextField txtNombre, txtIdentificacion, txtDireccion, txtCorreo, txtTelefono;
    @FXML private DatePicker dpFechaFactura;

    @FXML private TextField txtBuscarProducto;
    @FXML private TableView<Inventario> tblInventarioBusqueda;
    @FXML private TableColumn<Inventario, String> colInvCodigo, colInvDescripcion;
    @FXML private TableColumn<Inventario, Integer> colInvStock;
    @FXML private TableColumn<Inventario, BigDecimal> colInvPrecio;

    @FXML private TableView<FacturaDetalle> tblDetalle;
    @FXML private TableColumn<FacturaDetalle, String> colCodigo, colDescripcion;
    @FXML private TableColumn<FacturaDetalle, Integer> colCantidad;
    @FXML private TableColumn<FacturaDetalle, BigDecimal> colPrecioUnitario, colPrecioTotal;
    @FXML private TableColumn<FacturaDetalle, Void> colAcciones;

    @FXML private Label lblSubtotal, lblIva, lblTotal;
    @FXML private ComboBox<String> cmbFormaPago;
    @FXML private HBox pnlCredito;
    @FXML private ComboBox<Integer> cmbMesesPlazo;
    @FXML private ComboBox<String> cmbInteres;

    private final EmpresaDAO daoEmpresa = new EmpresaDAO();
    private final ClienteDAO daoCliente = new ClienteDAO();
    private final InventarioDAO daoInventario = new InventarioDAO();
    private final FacturaRegistroDAO daoFacturaRegistro = new FacturaRegistroDAO();
    private final FacturaDetalleDAO daoFacturaDetalle = new FacturaDetalleDAO();
    private final ComprobanteDAO daoComprobante = new ComprobanteDAO();
    private final CuentaPorCobrarDAO daoCuentaPorCobrar = new CuentaPorCobrarDAO();
    private final LogDAO logDAO = new LogDAO();

    private Empresa empresaActual;

    private ObservableList<Cliente> clientes;
    private FilteredList<Cliente> clientesFiltrados;
    private ObservableList<Inventario> listaInventario = FXCollections.observableArrayList();
    private ObservableList<FacturaDetalle> itemsDetalle = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaFactura.setValue(LocalDate.now());
        cargarLogo();
        obtenerDatosEmpresa();
        obtenerNumFactura();

        cargarListaClientes();
        cargarFormasPago();
        cargarCredito();

        iniciarTablaInventario();
        iniciarTablaDetalle();

        tblInventarioBusqueda.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblDetalle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarFormasPago() {
        cmbFormaPago.setItems(FXCollections.observableArrayList(
            "Efectivo", "Tarjeta de Crédito", "Tarjeta de Débito",
            "Transferencia", "Depósito", "Cheque", "TAG Crédito"
        ));
        cmbFormaPago.getSelectionModel().selectFirst();
    }

    private void cargarCredito() {
        cmbMesesPlazo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6));
        cmbMesesPlazo.getSelectionModel().selectFirst();

        cmbInteres.setItems(FXCollections.observableArrayList("0", "3", "6", "9", "12", "15"));
        cmbInteres.getSelectionModel().selectFirst();

        cmbFormaPago.valueProperty().addListener((obs, old, valor) -> {
            boolean esCredito = "TAG Crédito".equals(valor);
            pnlCredito.setVisible(esCredito);
            pnlCredito.setManaged(esCredito);
        });
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
            new Alert(Alert.AlertType.WARNING, "No hay stock disponible para: " + i.getDescripcion()).showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Cantidad");
        dialog.setHeaderText(i.getDescripcion());
        dialog.setContentText("Stock disponible: " + stockDisponible + "\nCantidad a facturar:");

        dialog.showAndWait().ifPresent(cantStr -> {
            try {
                int cant = Integer.parseInt(cantStr);
                if (cant <= 0) return;

                if (cant > stockDisponible) {
                    new Alert(Alert.AlertType.WARNING, "La cantidad ingresada (" + cant + ") excede el stock disponible (" + stockDisponible + ").").showAndWait();
                    return;
                }

                boolean existe = false;
                for (FacturaDetalle d : itemsDetalle) {
                    if (d.getInventarioId() == i.getId()) {
                        d.setCantidad(d.getCantidad() + cant);
                        existe = true;
                        break;
                    }
                }

                if (!existe) {
                    itemsDetalle.add(new FacturaDetalle(i.getId(), i.getCodigo(), i.getDescripcion(), cant, i.getPrecioVenta()));
                }

                tblDetalle.refresh();
                calcularTotales();
            } catch (NumberFormatException ignored) {}
        });
    }

    private int calcularStockDisponible(Inventario inventario) {
        int stockTotal = inventario.getCantidad();
        int cantidadEnDetalle = 0;
        for (FacturaDetalle d : itemsDetalle) {
            if (d.getInventarioId() == inventario.getId()) {
                cantidadEnDetalle += d.getCantidad();
            }
        }
        return stockTotal - cantidadEnDetalle;
    }

    private void calcularTotales() {
        BigDecimal subtotal = itemsDetalle.stream().map(FacturaDetalle::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private void obtenerNumFactura() {
        List<FacturaRegistro> lista = daoFacturaRegistro.obtenerNumFactura();
        int nextId = 1;
        for (FacturaRegistro r : lista) {
            try {
                int num = Integer.parseInt(r.getCodigo().replaceAll("\\D+", ""));
                if (num >= nextId) nextId = num + 1;
            } catch (Exception ignored) {}
        }
        lblNumFactura.setText("TAGFAC-" + String.format("%06d", nextId));
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
        String codigo = lblNumFactura.getText();
        LocalDateTime ahora = LocalDateTime.now();

        BigDecimal sub = itemsDetalle.stream().map(FacturaDetalle::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ivaCalc = sub.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totCalc = sub.add(ivaCalc).setScale(2, RoundingMode.HALF_UP);

        String ambienteSri = "PRUEBAS";
        String codEstab = "001";
        String codPtoEmi = "001";
        int secuencialFE = daoComprobante.obtenerSecuencial("01");
        String claveAcceso = ClaveAcceso.generar("01", empresaActual.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialFE);
        String fechaEmisionFE = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + String.format("%09d", secuencialFE);

        FacturaRegistro fr = new FacturaRegistro(empresaId, clienteId, ahora, codigo, cmbFormaPago.getValue(),
                sub, ivaCalc, totCalc, claveAcceso, numComprobante, ambienteSri);
        fr.setEstadoSri("PENDIENTE");
        int facturaId = daoFacturaRegistro.insertar(fr);

        if (facturaId == -1) {
            new Alert(Alert.AlertType.ERROR, "Error al registrar la factura.").showAndWait();
            return;
        }

        List<FacturaDetalle> detallesDb = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            FacturaDetalle fd = new FacturaDetalle(d.getInventarioId(), d.getCodigo(), d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario());
            fd.setFacturaRegistroId(facturaId);
            detallesDb.add(fd);
        }
        daoFacturaDetalle.insertarDetalle(facturaId, detallesDb);

        for (FacturaDetalle d : itemsDetalle) {
            daoInventario.descontarStock(d.getInventarioId(), d.getCantidad());
        }

        listaInventario.setAll(daoInventario.listar());

        if ("TAG Crédito".equals(cmbFormaPago.getValue())) {
            int meses = cmbMesesPlazo.getValue();
            BigDecimal tasaInteres = new BigDecimal(cmbInteres.getValue()).divide(new BigDecimal("100"));
            BigDecimal totalConInteres = totCalc.multiply(BigDecimal.ONE.add(tasaInteres)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cuotaMensual = totalConInteres.divide(new BigDecimal(meses), 2, RoundingMode.HALF_UP);

            CuentaPorCobrar cpc = new CuentaPorCobrar(facturaId, clienteId, totCalc, meses,
                    new BigDecimal(cmbInteres.getValue()), cuotaMensual);
            daoCuentaPorCobrar.insertar(cpc);
        }

        String tipoIdComp = cmbCliente.getValue().getIdentificacion().length() == 13 ? "05" : "04";

        List<Object[]> detallesXml = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            BigDecimal totalDetSinIva = precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            String desc = d.getDescripcion();
            if (desc.length() > 99) desc = desc.substring(0, 99);
            detallesXml.add(new Object[]{d.getCodigo(), desc, String.valueOf(d.getCantidad()),
                    precioSinIva.setScale(2, RoundingMode.HALF_UP).toString(), "0.00", totalDetSinIva.toString()});
        }

        String xmlGenerado = XmlSriBuilder.construirFactura(
                claveAcceso, empresaActual.getRuc(), empresaActual.getRazonSocial(),
                codEstab, codPtoEmi, secuencialFE,
                empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria(),
                "", "SI",
                tipoIdComp, cmbCliente.getValue().getNombre(), cmbCliente.getValue().getIdentificacion(),
                txtDireccion.getText(),
                sub.setScale(2, RoundingMode.HALF_UP).toString(), "0.00",
                ivaCalc.setScale(2, RoundingMode.HALF_UP).toString(), totCalc.setScale(2, RoundingMode.HALF_UP).toString(),
                "0.00", cmbFormaPago.getValue(), fechaEmisionFE, detallesXml);

        daoComprobante.insertar(claveAcceso, facturaId, numComprobante, ambienteSri, xmlGenerado);

        try {
            SRIWebService sriWs = new SRIWebService(ambienteSri);
            SRIWebService.SRIResponse sriResp = sriWs.validarComprobante(xmlGenerado, claveAcceso);
            if ("AUTORIZADO".equals(sriResp.getEstado())) {
                daoComprobante.actualizarEstado(claveAcceso, "AUTORIZADO", sriResp.getMensaje(), xmlGenerado);
                daoFacturaRegistro.actualizarEstado(claveAcceso, "AUTORIZADO");
            }
        } catch (Exception e) {
            logDAO.guardar("FacturaController", "validarSRI", e.getMessage(), e);
        }

        List<Object[]> detallesPDF = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            BigDecimal totalDetSinIva = precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            String desc = d.getDescripcion();
            if (desc.length() > 99) desc = desc.substring(0, 99);
            detallesPDF.add(new Object[]{d.getCodigo(), desc, String.valueOf(d.getCantidad()),
                    precioSinIva.setScale(2, RoundingMode.HALF_UP).toString(), "0.00", totalDetSinIva.toString()});
        }

        String rutaPDF = System.getProperty("user.home") + File.separator + "Desktop" + File.separator
                + "FacturaElectronica_" + numComprobante.replace("-", "") + ".pdf";

        PdfElectronico.generar(rutaPDF, claveAcceso,
                empresaActual.getRuc(), empresaActual.getRazonSocial(),
                empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria(),
                "", "SI", codEstab, codPtoEmi, secuencialFE,
                fechaEmisionFE, tipoIdComp,
                cmbCliente.getValue().getNombre(), cmbCliente.getValue().getIdentificacion(),
                txtDireccion.getText(), cmbFormaPago.getValue(),
                detallesPDF, sub, ivaCalc, totCalc);

        Alert alertExito = new Alert(Alert.AlertType.INFORMATION);
        alertExito.setTitle("Factura Electrónica registrada");
        alertExito.setHeaderText(null);
        alertExito.setContentText("Factura " + numComprobante + " registrada exitosamente.\nClave de acceso: " + claveAcceso + "\nPDF guardado en: " + rutaPDF);
        alertExito.showAndWait();

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(rutaPDF));
            }
        } catch (Exception ignored) {}

        String correoCliente = txtCorreo.getText();
        if (correoCliente != null && !correoCliente.trim().isEmpty()) {
            try {
                EmailService emailService = new EmailService();
                boolean enviado = emailService.enviarCorreoConPDF(correoCliente.trim(), cmbCliente.getValue().getNombre(), codigo, new File(rutaPDF));
                if (enviado) {
                    new Alert(Alert.AlertType.INFORMATION, "Correo enviado exitosamente a " + correoCliente).showAndWait();
                } else {
                    new Alert(Alert.AlertType.WARNING, "No se pudo enviar el correo a " + correoCliente + ". Verifique la dirección de correo.").showAndWait();
                }
            } catch (Exception e) {
                logDAO.guardar("FacturaController", "enviarCorreo", e.getMessage(), e);
                new Alert(Alert.AlertType.WARNING, "Error al enviar correo: " + e.getMessage()).showAndWait();
            }
        }

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
        obtenerNumFactura();
    } catch (Exception e) {
        logDAO.guardar("FacturaController", "guardar", e.getMessage(), e);
        new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
    }
    }
}

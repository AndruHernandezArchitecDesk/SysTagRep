package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.ClienteDAO;
import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.util.EmailService;
import com.tag.sysTagRep.dao.CuentaPorCobrarDAO;
import com.tag.sysTagRep.dao.ComprobanteDAO;
import com.tag.sysTagRep.dao.HistorialProductoDAO;
import com.tag.sysTagRep.dao.FacturaDetalleDAO;
import com.tag.sysTagRep.dao.FacturaRegistroDAO;
import com.tag.sysTagRep.dao.SecuenciaDocumentoDAO;
import com.tag.sysTagRep.model.*;
import com.tag.sysTagRep.util.NotaVentaPDF;
import com.tag.sysTagRep.util.ClaveAcceso;
import com.tag.sysTagRep.util.ConfigFirma;
import com.tag.sysTagRep.util.ConfigAmbiente;
import com.tag.sysTagRep.util.FirmaDigital;
import com.tag.sysTagRep.util.XmlSriBuilder;
import com.tag.sysTagRep.util.SRIWebService;
import com.tag.sysTagRep.util.PdfElectronico;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import javafx.util.StringConverter;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    @FXML private Button btnAgregarCliente;
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

    @FXML private Label lblSubtotal, lblIva, lblDescuento, lblTotal;
    @FXML private ComboBox<String> cmbFormaPago;
    @FXML private ComboBox<String> cmbDescuento;
    @FXML private HBox pnlCredito;
    @FXML private ComboBox<Integer> cmbMesesPlazo;
    @FXML private ComboBox<String> cmbInteres;
    @FXML private ComboBox<String> cmbAmbiente;

    private final EmpresaDAO daoEmpresa = new EmpresaDAO();
    private final ClienteDAO daoCliente = new ClienteDAO();
    private final InventarioDAO daoInventario = new InventarioDAO();
    private final FacturaRegistroDAO daoFacturaRegistro = new FacturaRegistroDAO();
    private final SecuenciaDocumentoDAO secuenciaDAO = new SecuenciaDocumentoDAO();
    private final FacturaDetalleDAO daoFacturaDetalle = new FacturaDetalleDAO();
    private final ComprobanteDAO daoComprobante = new ComprobanteDAO();
    private final CuentaPorCobrarDAO daoCuentaPorCobrar = new CuentaPorCobrarDAO();
    private final LogDAO logDAO = new LogDAO();
    private final HistorialProductoDAO historialProductoDAO = new HistorialProductoDAO();

    private Empresa empresaActual;

    private ObservableList<Cliente> clientes;
    private ObservableList<Inventario> listaInventario = FXCollections.observableArrayList();
    private ObservableList<FacturaDetalle> itemsDetalle = FXCollections.observableArrayList();

    private String rutaP12Config = "";
    private String claveP12Config = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dpFechaFactura.setValue(LocalDate.now());
        cargarLogo();
        obtenerDatosEmpresa();
        obtenerNumFactura();

        String[] firmaConfig = ConfigFirma.cargar();
        rutaP12Config = firmaConfig[0];
        claveP12Config = firmaConfig[1];

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

        cargarAmbiente();
    }

    private void cargarAmbiente() {
        ComboFilter.habilitar(cmbAmbiente, FXCollections.observableArrayList("PRUEBAS", "PRODUCCION"));
        String ambiente = ConfigAmbiente.cargar();
        if (cmbAmbiente.getValue() == null || !cmbAmbiente.getValue().equals(ambiente)) {
            cmbAmbiente.setValue(ambiente);
        }
        cmbAmbiente.valueProperty().addListener((obs, old, val) -> ConfigAmbiente.guardar(val));
    }

    private File obtenerDirectorioEscritorio() {
        File home = new File(System.getProperty("user.home"));
        String[] nombres = {"Desktop", "Escritorio"};
        for (String n : nombres) {
            File d = new File(home, n);
            if (d.exists() && d.isDirectory()) {
                return d;
            }
        }
        File d = new File(home, "Desktop");
        d.mkdirs();
        return d;
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

    private BigDecimal calcularDescuento(BigDecimal totalBruto) {
        BigDecimal pct = BigDecimal.ZERO;
        try {
            pct = new BigDecimal(cmbDescuento.getValue() != null ? cmbDescuento.getValue() : "0");
        } catch (NumberFormatException ignored) {}
        return totalBruto.multiply(pct).divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> distribuirDescuento(BigDecimal descuentoTotal, List<BigDecimal> bases) {
        List<BigDecimal> resultado = new ArrayList<>();
        for (BigDecimal b : bases) resultado.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        if (descuentoTotal == null || descuentoTotal.signum() == 0 || bases.isEmpty()) {
            return resultado;
        }
        BigDecimal sumaBases = BigDecimal.ZERO;
        for (BigDecimal b : bases) sumaBases = sumaBases.add(b);
        if (sumaBases.signum() == 0) return resultado;

        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < bases.size(); i++) {
            if (i == bases.size() - 1) {
                resultado.set(i, descuentoTotal.subtract(acumulado).setScale(2, RoundingMode.HALF_UP));
            } else {
                BigDecimal d = descuentoTotal.multiply(bases.get(i)).divide(sumaBases, 2, RoundingMode.HALF_UP);
                acumulado = acumulado.add(d);
                resultado.set(i, d);
            }
        }
        return resultado;
    }

    private List<Object[]> armarDetalles(BigDecimal descuentoTotal) {
        List<BigDecimal> bases = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            bases.add(precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP));
        }
        List<BigDecimal> descLinea = distribuirDescuento(descuentoTotal, bases);

        List<Object[]> resultado = new ArrayList<>();
        for (int i = 0; i < itemsDetalle.size(); i++) {
            FacturaDetalle d = itemsDetalle.get(i);
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            BigDecimal totalDetSinIva = precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            String desc = d.getDescripcion();
            if (desc.length() > 99) desc = desc.substring(0, 99);
            resultado.add(new Object[]{d.getCodigo(), desc, String.valueOf(d.getCantidad()),
                    precioSinIva.setScale(2, RoundingMode.HALF_UP).toString(),
                    descLinea.get(i).toString(),
                    totalDetSinIva.toString()});
        }
        return resultado;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                filtradosProd.setPredicate(i -> {
                    String searchStr = (i.getCodigo() + " " + i.getDescripcion() + " " + i.getGrupo() + " " + i.getMarca()).toLowerCase();
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
        BigDecimal totalBruto = subtotal.add(iva);
        BigDecimal descuento = calcularDescuento(totalBruto);
        BigDecimal total = totalBruto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);

        lblSubtotal.setText(subtotal.setScale(2, RoundingMode.HALF_UP).toString());
        lblIva.setText(iva.toString());
        lblDescuento.setText("-" + descuento.toString());
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
        SecuenciaDocumento sec = secuenciaDAO.obtener("FACTURA");
        lblNumFactura.setText(sec.getProximoCodigo());
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

        if (secuenciaDAO.existeCodigoFactura(codigo)) {
            new Alert(Alert.AlertType.ERROR, "El número " + codigo + " ya existe en la base de datos, no se puede repetir.").showAndWait();
            obtenerNumFactura();
            return;
        }

        BigDecimal sub = itemsDetalle.stream().map(FacturaDetalle::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ivaCalc = sub.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBrutoCalc = sub.add(ivaCalc);
        BigDecimal descCalc = calcularDescuento(totalBrutoCalc);
        BigDecimal totCalc = totalBrutoCalc.subtract(descCalc).setScale(2, RoundingMode.HALF_UP);

        String ambienteSri = cmbAmbiente.getValue() != null ? cmbAmbiente.getValue() : "PRUEBAS";
        String codEstab = "001";
        String codPtoEmi = "001";
        int secuencialFE = daoComprobante.obtenerSecuencial("01");
        String claveAcceso = ClaveAcceso.generar("01", empresaActual.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialFE);
        String fechaEmisionFE = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + String.format("%07d", secuencialFE);

        FacturaRegistro fr = new FacturaRegistro(empresaId, clienteId, ahora, codigo, cmbFormaPago.getValue(),
                sub, ivaCalc, descCalc, totCalc, claveAcceso, numComprobante, ambienteSri);
        fr.setEstadoSri("PENDIENTE");
        int facturaId = daoFacturaRegistro.insertar(fr);

        if (facturaId == -1) {
            new Alert(Alert.AlertType.ERROR, "Error al registrar la factura.").showAndWait();
            return;
        }

        secuenciaDAO.marcarUsado("FACTURA");

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

        String clienteNombre = cmbCliente.getValue().getNombre();
        List<HistorialProducto> historial = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            String provNombre = daoInventario.obtenerProveedorNombre(d.getInventarioId());
            historial.add(new HistorialProducto(d.getInventarioId(), d.getCodigo(), d.getDescripcion(),
                    d.getCantidad(), d.getPrecioUnitario(), "FACTURA", codigo,
                    clienteNombre, provNombre, ahora));
        }
        historialProductoDAO.insertar(historial);

        listaInventario.setAll(daoInventario.listar());

        if ("TAG Crédito".equals(cmbFormaPago.getValue())) {
            int dias = cmbMesesPlazo.getValue();
            BigDecimal tasaInteres = new BigDecimal(cmbInteres.getValue()).divide(new BigDecimal("100"));
            BigDecimal totalConInteres = totCalc.multiply(BigDecimal.ONE.add(tasaInteres)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cuotaMensual = totalConInteres.divide(new BigDecimal(dias), 2, RoundingMode.HALF_UP);

            CuentaPorCobrar cpc = new CuentaPorCobrar(facturaId, clienteId, totCalc, dias,
                    new BigDecimal(cmbInteres.getValue()), cuotaMensual);
            daoCuentaPorCobrar.insertar(cpc);
        }

        String tipoIdComp = cmbCliente.getValue().getIdentificacion().length() == 13 ? "05" : "04";

        List<Object[]> detallesXml = armarDetalles(descCalc);

        String xmlGenerado = XmlSriBuilder.construirFactura(
                ambienteSri, claveAcceso, empresaActual.getRuc(), empresaActual.getRazonSocial(),
                codEstab, codPtoEmi, secuencialFE,
                empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria(),
                "", "NO",
                tipoIdComp, cmbCliente.getValue().getNombre(), cmbCliente.getValue().getIdentificacion(),
                txtDireccion.getText(),
                sub.setScale(2, RoundingMode.HALF_UP).toString(), descCalc.setScale(2, RoundingMode.HALF_UP).toString(),
                ivaCalc.setScale(2, RoundingMode.HALF_UP).toString(), totCalc.setScale(2, RoundingMode.HALF_UP).toString(),
                "0.00", cmbFormaPago.getValue(), fechaEmisionFE, detallesXml);

        String xmlFirmado = xmlGenerado;
        boolean firmaOk = false;
        String rutaP12 = rutaP12Config;
        String claveP12 = claveP12Config;
        if (rutaP12.isEmpty() || claveP12.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No se configuró la firma electrónica (.p12 y contraseña). "
                    + "La factura se guardará sin enviar al SRI (PENDIENTE).").showAndWait();
        } else {
            try {
                FirmaDigital firma = new FirmaDigital();
                if (!firma.cargarCertificado(rutaP12, claveP12)) {
                    new Alert(Alert.AlertType.ERROR, "No se pudo cargar el certificado. Verifique la ruta y la contraseña.").showAndWait();
                } else {
                    xmlFirmado = firma.firmarXml(xmlGenerado);
                    firmaOk = true;
                }
            } catch (Exception e) {
                logDAO.guardar("FacturaController", "firmarXml", e.getMessage(), e);
                new Alert(Alert.AlertType.ERROR, "Error al firmar el XML: " + e.getMessage()).showAndWait();
            }
        }

        daoComprobante.insertar(claveAcceso, null, numComprobante, ambienteSri, xmlFirmado);

        final String claveFinal = claveAcceso;
        final String xmlFinal = xmlFirmado;
        final String numCompFinal = numComprobante;
        final String ambienteFinal = ambienteSri;
        final boolean firmaOkFinal = firmaOk;
        final BigDecimal subFinal = sub;
        final BigDecimal descFinal = descCalc;
        final BigDecimal ivaFinal = ivaCalc;
        final BigDecimal totFinal = totCalc;

        Task<SRIWebService.SRIResponse> tareaSRI = new Task<>() {
            @Override
            protected SRIWebService.SRIResponse call() {
                try {
                    SRIWebService sriWs = new SRIWebService(ambienteFinal);
                    return sriWs.validarComprobante(xmlFinal, claveFinal);
                } catch (Exception e) {
                    logDAO.guardar("FacturaController", "validarSRI", e.getMessage(), e);
                    SRIWebService.SRIResponse r = new SRIWebService.SRIResponse();
                    r.setEstado("ERROR");
                    r.setMensaje("Error de conexión: " + e.getMessage());
                    return r;
                }
            }
        };

        Alert progreso = new Alert(Alert.AlertType.INFORMATION);
        progreso.setTitle("Factura Electrónica");
        progreso.setHeaderText("Consultando al SRI...");
        progreso.setContentText("Enviando la factura " + numCompFinal + " al SRI.\n"
                + "La ventana permanecerá activa; puede tardar unos segundos.");
        progreso.getButtonTypes().setAll(new ButtonType("Minimizar", ButtonBar.ButtonData.CANCEL_CLOSE));

        tareaSRI.setOnSucceeded(e -> {
            progreso.close();
            finalizarGuardar(tareaSRI.getValue(), claveFinal, numCompFinal, xmlFinal, firmaOkFinal,
                    subFinal, descFinal, ivaFinal, totFinal, ambienteFinal,
                    codEstab, codPtoEmi, secuencialFE, fechaEmisionFE, tipoIdComp, codigo);
        });
        tareaSRI.setOnFailed(e -> {
            progreso.close();
            Throwable exSRI = tareaSRI.getException();
            logDAO.guardar("FacturaController", "validarSRI", String.valueOf(exSRI));
            daoComprobante.actualizarEstado(claveFinal, "PENDIENTE", "Error de conexión", xmlFinal, null, null);
            daoComprobante.guardarEnvio(claveFinal, numCompFinal, ambienteFinal, xmlFinal,
                    null, null, "PENDIENTE", "Error de conexión", null, null);
            new Alert(Alert.AlertType.ERROR, "Error al consultar el SRI: "
                    + (exSRI != null ? exSRI.getMessage() : "desconocido")).showAndWait();
        });

        new Thread(tareaSRI, "Hilo-SRI").start();
        progreso.show();
    } catch (Exception e) {
        logDAO.guardar("FacturaController", "guardar", e.getMessage(), e);
        new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
    }
    }

    private void finalizarGuardar(SRIWebService.SRIResponse sriResp, String claveAcceso, String numComprobante,
                                  String xmlFirmado, boolean firmaOk,
                                  BigDecimal sub, BigDecimal descCalc, BigDecimal ivaCalc, BigDecimal totCalc,
                                  String ambienteSri, String codEstab, String codPtoEmi, int secuencialFE,
                                  String fechaEmisionFE, String tipoIdComp, String codigo) {
        String estadoSri = sriResp.getEstado();
        String numeroAutorizacion = sriResp.getNumeroAutorizacion();
        String fechaAutorizacion = sriResp.getFechaAutorizacion();

        if ("AUTORIZADO".equals(estadoSri)) {
            daoComprobante.actualizarEstado(claveAcceso, "AUTORIZADO", sriResp.getMensaje(), xmlFirmado, numeroAutorizacion, fechaAutorizacion);
            daoFacturaRegistro.actualizarEstado(claveAcceso, "AUTORIZADO");
        } else if ("RECHAZADA".equals(estadoSri) || "DEVUELTA".equals(estadoSri)) {
            daoComprobante.actualizarEstado(claveAcceso, estadoSri, sriResp.getMensaje(), xmlFirmado, numeroAutorizacion, null);
            daoFacturaRegistro.actualizarEstado(claveAcceso, estadoSri);
            new Alert(Alert.AlertType.ERROR, "El SRI " + (estadoSri.equals("RECHAZADA") ? "rechazó" : "devolvió")
                    + " el comprobante:\n" + sriResp.getMensaje()).showAndWait();
        } else if (!firmaOk) {
            daoComprobante.actualizarEstado(claveAcceso, "PENDIENTE", "Sin envío: firma no configurada", xmlFirmado, null, null);
        } else {
            daoComprobante.actualizarEstado(claveAcceso, estadoSri, sriResp.getMensaje(), xmlFirmado, numeroAutorizacion, fechaAutorizacion);
        }

        daoComprobante.guardarEnvio(claveAcceso, numComprobante, ambienteSri, xmlFirmado,
                sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(),
                estadoSri, sriResp.getMensaje(), numeroAutorizacion, fechaAutorizacion);

        List<Object[]> detallesPDF = armarDetalles(descCalc);

        File dirPDF = obtenerDirectorioEscritorio();
        String rutaPDF = dirPDF.getAbsolutePath() + File.separator
                + "FacturaElectronica_" + numComprobante.replace("-", "") + ".pdf";
        final String rutaXML = dirPDF.getAbsolutePath() + File.separator
                + "FacturaElectronica_" + numComprobante.replace("-", "") + ".xml";
        try {
            Files.write(Paths.get(rutaXML), xmlFirmado.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            logDAO.guardar("FacturaController", "guardarXML", ex.getMessage(), ex);
        }

        PdfElectronico.generar(rutaPDF, claveAcceso, numeroAutorizacion, fechaAutorizacion, ambienteSri,
                empresaActual.getRuc(), empresaActual.getRazonSocial(),
                empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria(),
                empresaActual.getTelefono(), empresaActual.getCorreo(),
                "", "NO", empresaActual.getSucursal(),
                empresaActual.getAgenteRetencion(), empresaActual.getResolucion(),
                codEstab, codPtoEmi, secuencialFE,
                fechaEmisionFE, tipoIdComp,
                cmbCliente.getValue().getNombre(), cmbCliente.getValue().getIdentificacion(),
                txtDireccion.getText(), txtCorreo.getText(), txtTelefono.getText(),
                cmbFormaPago.getValue(),
                detallesPDF, sub, descCalc, ivaCalc, totCalc);

        Alert alertExito = new Alert(Alert.AlertType.INFORMATION);
        alertExito.setTitle("Factura Electrónica registrada");
        alertExito.setHeaderText(null);
        String txtAutorizacion = (numeroAutorizacion != null && !numeroAutorizacion.isEmpty())
                ? "\nNúmero de autorización: " + numeroAutorizacion : "";
        alertExito.setContentText("Factura " + numComprobante + " registrada exitosamente.\nClave de acceso: " + claveAcceso
                + txtAutorizacion + "\nPDF guardado en: " + rutaPDF);
        alertExito.showAndWait();

        try {
            if (Desktop.isDesktopSupported()) {
                final File pdfFinal = new File(rutaPDF);
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(pdfFinal);
                    } catch (Exception ignored) {}
                }, "Abrir-PDF").start();
            }
        } catch (Exception ignored) {}

        String correoCliente = txtCorreo.getText();
        if (correoCliente != null && !correoCliente.trim().isEmpty()) {
            final String destCorreo = correoCliente.trim();
            final String nombreClienteCorreo = cmbCliente.getValue().getNombre();
            final String codigoCorreo = codigo;
            logDAO.guardar("FacturaController", "enviarCorreo", "Iniciando envío a " + destCorreo);
            Task<String> tareaCorreo = new Task<>() {
                @Override
                protected String call() {
                    EmailService emailService = new EmailService();
                    boolean enviado = emailService.enviarCorreoConArchivos(destCorreo,
                            nombreClienteCorreo, codigoCorreo, "FACTURA", new File(rutaPDF), new File(rutaXML));
                    if (!enviado) {
                        String error = emailService.getUltimoError();
                        logDAO.guardar("FacturaController", "enviarCorreo",
                                "Fallo SMTP a " + destCorreo + ": " + error);
                        return error;
                    }
                    return null;
                }
            };
            tareaCorreo.setOnSucceeded(ev -> {
                String errorCorreo = tareaCorreo.getValue();
                if (errorCorreo == null) {
                    logDAO.guardar("FacturaController", "enviarCorreo", "Enviado exitosamente a " + destCorreo);
                    new Alert(Alert.AlertType.INFORMATION, "Correo enviado exitosamente a " + destCorreo).showAndWait();
                } else {
                    new Alert(Alert.AlertType.WARNING, "No se pudo enviar el correo a " + destCorreo
                            + ".\nDetalle: " + errorCorreo).showAndWait();
                }
            });
            tareaCorreo.setOnFailed(ev -> {
                Throwable exCorreo = tareaCorreo.getException();
                logDAO.guardar("FacturaController", "enviarCorreo", String.valueOf(exCorreo));
                new Alert(Alert.AlertType.WARNING, "Error al enviar correo: "
                        + (exCorreo != null ? exCorreo.getMessage() : "desconocido")).showAndWait();
            });
            new Thread(tareaCorreo, "Hilo-Correo").start();
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
        obtenerNumFactura();
    }
}

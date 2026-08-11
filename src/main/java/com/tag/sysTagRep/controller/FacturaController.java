package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.dao.ClienteDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.service.FacturaService;
import com.tag.sysTagRep.util.EmailService;
import com.tag.sysTagRep.dao.ComprobanteDAO;
import com.tag.sysTagRep.dao.SecuenciaDocumentoDAO;
import com.tag.sysTagRep.model.*;
import com.tag.sysTagRep.util.ConfigFirma;
import com.tag.sysTagRep.util.ConfigAmbiente;
import com.tag.sysTagRep.util.SRIWebService;
import com.tag.sysTagRep.util.SortTable;
import com.tag.sysTagRep.util.ComboFilter;
import com.tag.sysTagRep.util.AppConstants;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FacturaController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(FacturaController.class.getName());

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
    private final ComprobanteDAO daoComprobante = new ComprobanteDAO();
    private final LogDAO logDAO = new LogDAO();
    private final SecuenciaDocumentoDAO secuenciaDAO = new SecuenciaDocumentoDAO();
    private final FacturaService facturaService = new FacturaService();

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
        ComboFilter.habilitar(cmbAmbiente, FXCollections.observableArrayList(AppConstants.AMBIENTE_PRUEBAS, AppConstants.AMBIENTE_PRODUCCION));
        String ambiente = ConfigAmbiente.cargar();
        if (cmbAmbiente.getValue() == null || !cmbAmbiente.getValue().equals(ambiente)) {
            cmbAmbiente.setValue(ambiente);
        }
        cmbAmbiente.valueProperty().addListener((obs, old, val) -> ConfigAmbiente.guardar(val));
    }

    private File obtenerDirectorioEscritorio() {
        File home = new File(System.getProperty("user.home"));
        for (String n : new String[]{AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT, AppConstants.DIRECTORIO_ESCRITORIO_ALT}) {
            File d = new File(home, n);
            if (d.exists() && d.isDirectory()) {
                return d;
            }
        }
        File d = new File(home, AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT);
        d.mkdirs();
        return d;
    }

    private void cargarFormasPago() {
        ComboFilter.habilitar(cmbFormaPago, FXCollections.observableArrayList(
            "Efectivo", "Tarjeta de Crédito", "Tarjeta de Débito",
            "Transferencia", "Depósito", "Cheque", AppConstants.FORMA_PAGO_CREDITO
        ));
        cmbFormaPago.getSelectionModel().selectFirst();
    }

    private void cargarCredito() {
        ObservableList<Integer> meses = FXCollections.observableArrayList();
        for (int m : AppConstants.MESES_PLAZO) meses.add(m);
        ComboFilter.habilitarEnteros(cmbMesesPlazo, meses);
        cmbMesesPlazo.getSelectionModel().selectFirst();

        ComboFilter.habilitar(cmbInteres, FXCollections.observableArrayList(AppConstants.TASAS_INTERES));
        cmbInteres.getSelectionModel().selectFirst();

        cmbFormaPago.valueProperty().addListener((obs, old, valor) -> {
            boolean esCredito = AppConstants.FORMA_PAGO_CREDITO.equals(valor);
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

    private BigDecimal obtenerDescuentoPct() {
        BigDecimal pct = AppConstants.CERO;
        try {
            pct = new BigDecimal(cmbDescuento.getValue() != null ? cmbDescuento.getValue() : "0");
        } catch (NumberFormatException ignored) {
            LOGGER.log(Level.WARNING, "Descuento invalido, usando 0", ignored);
        }
        return pct;
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
            if (desc.length() > AppConstants.MAX_DESCRIPCION_XML) desc = desc.substring(0, AppConstants.MAX_DESCRIPCION_XML);
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
            LOGGER.log(Level.SEVERE, "Error al abrir CRUD de clientes", e);
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
            } catch (NumberFormatException ignored) {
                LOGGER.log(Level.WARNING, "Cantidad invalida ingresada", ignored);
            }
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
        BigDecimal iva = subtotal.multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBruto = subtotal.add(iva);
        BigDecimal descuento = totalBruto.multiply(obtenerDescuentoPct()).divide(AppConstants.CIEN).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = totalBruto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);

        lblSubtotal.setText(subtotal.setScale(2, RoundingMode.HALF_UP).toString());
        lblIva.setText(iva.toString());
        lblDescuento.setText(descuento.toString());
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
            validarCamposObligatorios();

            String codigo = lblNumFactura.getText();
            if (secuenciaDAO.existeCodigoFactura(codigo)) {
                new Alert(Alert.AlertType.ERROR, "El número " + codigo + " ya existe en la base de datos, no se puede repetir.").showAndWait();
                obtenerNumFactura();
                return;
            }

            String ambienteSri = cmbAmbiente.getValue() != null ? cmbAmbiente.getValue() : AppConstants.AMBIENTE_PRUEBAS;
            File directorioEscritorio = obtenerDirectorioEscritorio();

            FacturaService.ResultadoFactura resultado = facturaService.guardarFactura(
                    cmbCliente.getValue(), empresaActual, codigo, itemsDetalle,
                    cmbFormaPago.getValue(), cmbMesesPlazo.getValue(), cmbInteres.getValue(),
                    ambienteSri, rutaP12Config, claveP12Config, directorioEscritorio,
                    obtenerDescuentoPct()
            );

            String claveFinal = resultado.claveAcceso;
            String xmlFinal = resultado.xmlFirmado;
            String numCompFinal = resultado.numComprobante;
            String ambienteFinal = resultado.ambienteSri;
            boolean firmaOkFinal = resultado.firmaOk;

            Task<SRIWebService.SRIResponse> tareaSRI = new Task<>() {
                @Override
                protected SRIWebService.SRIResponse call() {
                    return facturaService.enviarYSolicitarAutorizacion(ambienteFinal, xmlFinal, claveFinal);
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
                finalizarGuardado(tareaSRI.getValue(), resultado);
            });
            tareaSRI.setOnFailed(e -> {
                progreso.close();
                Throwable exSRI = tareaSRI.getException();
                logDAO.guardar("FacturaController", "validarSRI", String.valueOf(exSRI));
                daoComprobante.actualizarEstado(claveFinal, AppConstants.ESTADO_PENDIENTE, "Error de conexión", xmlFinal, null, null);
                daoComprobante.guardarEnvio(claveFinal, numCompFinal, ambienteFinal, xmlFinal,
                        null, null, AppConstants.ESTADO_PENDIENTE, "Error de conexión", null, null);
                new Alert(Alert.AlertType.ERROR, "Error al consultar el SRI: "
                        + (exSRI != null ? exSRI.getMessage() : "desconocido")).showAndWait();
            });

            new Thread(tareaSRI, "Hilo-SRI").start();
            progreso.show();
        } catch (Exception e) {
            logDAO.guardar("FacturaController", "guardar", e.getMessage(), e);
            String mensaje = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + mensaje).showAndWait();
        }
    }

    private void validarCamposObligatorios() {
        if (cmbCliente.getValue() == null) {
            throw new IllegalArgumentException("Debe seleccionar un cliente.");
        }
        if (itemsDetalle.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto.");
        }
        if (empresaActual == null) {
            throw new IllegalArgumentException("No se encontraron datos de la empresa.");
        }
    }

    private void finalizarGuardado(SRIWebService.SRIResponse sriResp, FacturaService.ResultadoFactura resultado) {
        String estadoSri = sriResp.getEstado();
        String numeroAutorizacion = sriResp.getNumeroAutorizacion();
        String fechaAutorizacion = sriResp.getFechaAutorizacion();

        if (AppConstants.ESTADO_RECHAZADA.equals(estadoSri) || AppConstants.ESTADO_DEVUELTA.equals(estadoSri)) {
            new Alert(Alert.AlertType.ERROR, "El SRI " + (AppConstants.ESTADO_RECHAZADA.equals(estadoSri) ? "rechazó" : "devolvió")
                    + " el comprobante:\n" + sriResp.getMensaje()).showAndWait();
        }

        if (AppConstants.ESTADO_AUTORIZADO.equals(estadoSri) || AppConstants.ESTADO_RECHAZADA.equals(estadoSri) || AppConstants.ESTADO_DEVUELTA.equals(estadoSri)) {
            facturaService.finalizarEnvioSRI(sriResp, resultado, obtenerDirectorioEscritorio());
        } else if (!resultado.firmaOk) {
            daoComprobante.actualizarEstado(resultado.claveAcceso, AppConstants.ESTADO_PENDIENTE, "Sin envío: firma no configurada", resultado.xmlFirmado, null, null);
        } else {
            facturaService.finalizarEnvioSRI(sriResp, resultado, obtenerDirectorioEscritorio());
        }

        Alert alertExito = new Alert(Alert.AlertType.INFORMATION);
        alertExito.setTitle("Factura Electrónica registrada");
        alertExito.setHeaderText(null);
        String txtAutorizacion = (numeroAutorizacion != null && !numeroAutorizacion.isEmpty())
                ? "\nNúmero de autorización: " + numeroAutorizacion : "";
        alertExito.setContentText("Factura " + resultado.numComprobante + " registrada exitosamente.\nClave de acceso: " + resultado.claveAcceso
                + txtAutorizacion + "\nPDF guardado en: " + resultado.rutaPDF);
        alertExito.showAndWait();

        try {
            if (Desktop.isDesktopSupported()) {
                final File pdfFinal = new File(resultado.rutaPDF);
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(pdfFinal);
                    } catch (Exception ignored) {
                        LOGGER.log(Level.WARNING, "No se pudo abrir el PDF", ignored);
                    }
                }, "Abrir-PDF").start();
            }
        } catch (Exception ignored) {
            LOGGER.log(Level.WARNING, "Desktop no soportado para abrir PDF", ignored);
        }

        String correoCliente = txtCorreo.getText();
        if (correoCliente != null && !correoCliente.trim().isEmpty()) {
            final String destCorreo = correoCliente.trim();
            if (AppConstants.ESTADO_AUTORIZADO.equals(estadoSri) && numeroAutorizacion != null && !numeroAutorizacion.isEmpty()
                    && fechaAutorizacion != null && !fechaAutorizacion.isEmpty()) {
                final String nombreClienteCorreo = cmbCliente.getValue().getNombre();
                final String codigoCorreo = resultado.numComprobante;
                logDAO.guardar("FacturaController", "enviarCorreo", "Iniciando envío a " + destCorreo);
                Task<String> tareaCorreo = new Task<>() {
                    @Override
                    protected String call() {
                        return facturaService.enviarCorreoAutorizacion(destCorreo, nombreClienteCorreo, codigoCorreo, resultado.rutaPDF, resultado.rutaXML)
                                ? null : "Error enviando correo";
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
            } else {
                new Alert(Alert.AlertType.INFORMATION, "La factura está pendiente de autorización. "
                        + "El correo se enviará cuando se autorice.").showAndWait();
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
        obtenerNumFactura();
    }
}

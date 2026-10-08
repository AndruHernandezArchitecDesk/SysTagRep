package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.dao.ClienteDAO;
import com.vendex.dao.ComprobanteDAO;
import com.vendex.dao.FacturaRegistroDAO;
import com.vendex.dao.LogDAO;
import com.vendex.dao.NotaCreditoRegistroDAO;
import com.vendex.dao.GuiaRemisionRegistroDAO;
import com.vendex.dao.NotaDebitoRegistroDAO;
import com.vendex.dao.RetencionRegistroDAO;
import com.vendex.model.Cliente;
import com.vendex.model.FacturaRegistro;
import com.vendex.model.GuiaRemisionRegistro;
import com.vendex.model.NotaCreditoRegistro;
import com.vendex.model.NotaDebitoRegistro;
import com.vendex.model.RetencionRegistro;
import com.vendex.service.FacturaService;
import com.vendex.service.GuiaRemisionService;
import com.vendex.service.NotaCreditoService;
import com.vendex.service.NotaDebitoService;
import com.vendex.service.RetencionService;
import com.vendex.util.EmailService;
import com.vendex.util.ElectronicoUtil;
import com.vendex.util.SortTable;
import com.vendex.util.ComboFilter;
import com.vendex.util.SRIWebService;
import com.vendex.util.AppConstants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.vendex.dao.FacturaRegistroDAOPostgres;
import com.vendex.dao.ComprobanteDAOPostgres;
import com.vendex.dao.NotaCreditoRegistroDAOPostgres;
import com.vendex.dao.NotaDebitoRegistroDAOPostgres;
import com.vendex.dao.GuiaRemisionRegistroDAOPostgres;
import com.vendex.dao.RetencionRegistroDAOPostgres;
import com.vendex.dao.ClienteDAOPostgres;
import com.vendex.dao.LogDAOPostgres;

public class SeguimientoSriController implements Initializable {

    public SeguimientoSriController() {
        this(com.vendex.config.AppContext.getInstance());
    }

    public SeguimientoSriController(com.vendex.config.AppContext ctx) {
        this.dao = ctx.facturaRegistroDAO;
        this.logDAO = ctx.logDAO;
        this.clienteDAO = ctx.clienteDAO;
        this.facturaService = ctx.facturaService;
    }

    private static final Logger LOGGER = Logger.getLogger(SeguimientoSriController.class.getName());

    @FXML private TextField txtBuscar;
    @FXML private TableView<FacturaRegistro> tblFacturas;
    @FXML private TableColumn<FacturaRegistro, String> colCodigo;
    @FXML private TableColumn<FacturaRegistro, String> colNumComprobante;
    @FXML private TableColumn<FacturaRegistro, LocalDateTime> colFecha;
    @FXML private TableColumn<FacturaRegistro, String> colCliente;
    @FXML private TableColumn<FacturaRegistro, String> colFormaPago;
    @FXML private TableColumn<FacturaRegistro, BigDecimal> colSubtotal;
    @FXML private TableColumn<FacturaRegistro, BigDecimal> colIva;
    @FXML private TableColumn<FacturaRegistro, BigDecimal> colTotal;
    @FXML private TableColumn<FacturaRegistro, String> colEstado;
    @FXML private TableColumn<FacturaRegistro, String> colMensaje;

    @FXML private Label lblPaginaInfo;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnConsultarSri;
    @FXML private TableColumn<FacturaRegistro, String> colAcciones;
    @FXML private ComboBox<Integer> cmbPageSize;

    private int currentPage = 1;
    private int pageSize = 25;
    private int totalPages = 1;
    private int totalCount = 0;
    private final FacturaRegistroDAO dao;
    private final LogDAO logDAO;
    private final ClienteDAO clienteDAO;
    private final FacturaService facturaService;
    private final ObservableList<FacturaRegistro> listaFacturas = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNumComprobante.setCellValueFactory(new PropertyValueFactory<>("numComprobante"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colFormaPago.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoSri"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                FacturaRegistro fr = getTableRow().getItem();
                String texto = formatearEstado(estado, fr.getMensajeSri());
                setText(texto);
                setStyle("-fx-text-fill: " + colorEstado(estado, fr.getMensajeSri()) + "; -fx-font-weight: bold;");
                String detalle = fr.getMensajeSri();
                setTooltip(detalle != null && !detalle.trim().isEmpty()
                        ? new Tooltip(detalle) : null);
            }
        });
        colMensaje.setCellValueFactory(new PropertyValueFactory<>("mensajeSri"));
        colMensaje.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String mensaje, boolean empty) {
                super.updateItem(mensaje, empty);
                setText(empty || mensaje == null ? null : mensaje);
                setTooltip(mensaje != null && !mensaje.isEmpty() ? new Tooltip(mensaje) : null);
            }
        });

        tblFacturas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblFacturas);
        configurarColumnaAcciones();

        iniciarPageSize();
        txtBuscar.textProperty().addListener((obs, old, val) -> { currentPage = 1; cargarDatos(); });

        cargarDatos();
    }

    /**
     * Acciones por fila: Imprimir y Enviar correo, habilitadas SOLO con factura
     * AUTORIZADA (con numero de autorizacion). Antes de autorizar no hay nada
     * valido que imprimir ni enviar.
     */
    private void configurarColumnaAcciones() {
        if (colAcciones == null) return;
        colAcciones.setCellValueFactory(new PropertyValueFactory<>("estadoSri"));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnImprimir = new Button("Imprimir");
            private final Button btnCorreo = new Button("Correo");
            private final HBox box = new HBox(6, btnImprimir, btnCorreo);
            {
                btnImprimir.setOnAction(e -> {
                    FacturaRegistro fr = getTableRow() == null ? null : getTableRow().getItem();
                    if (fr != null) imprimirAutorizada(fr);
                });
                btnCorreo.setOnAction(e -> {
                    FacturaRegistro fr = getTableRow() == null ? null : getTableRow().getItem();
                    if (fr != null) enviarCorreoAutorizado(fr);
                });
            }
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                boolean autorizada = AppConstants.ESTADO_AUTORIZADO.equals(getTableRow().getItem().getEstadoSri());
                btnImprimir.setDisable(!autorizada);
                btnCorreo.setDisable(!autorizada);
                String tip = autorizada ? "Comprobante autorizado: disponible"
                        : "Disponible al autorizarse por el SRI";
                btnImprimir.setTooltip(new Tooltip(tip));
                btnCorreo.setTooltip(new Tooltip(tip));
                setGraphic(box);
            }
        });
    }

    /** Lee numero/fecha de autorizacion guardados; null si aun no hay. */
    private String[] datosAutorizacion(FacturaRegistro fr) {
        try {
            if (fr == null || fr.getClaveAcceso() == null) return null;
            return AppContext.getInstance().comprobanteDAO.obtenerDatosAutorizacion(fr.getClaveAcceso());
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "datosAutorizacion", String.valueOf(e));
            return null;
        }
    }

    private void imprimirAutorizada(FacturaRegistro fr) {
        String[] aut = datosAutorizacion(fr);
        if (aut == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Factura " + fr.getNumComprobante() + " aun sin autorizacion registrada. Consulte el SRI primero.").showAndWait();
            return;
        }
        try {
            String ruta = facturaService.regenerarRide(fr.getClaveAcceso(), aut[0], aut[1], obtenerDirectorioEscritorio());
            if (ruta == null) {
                new Alert(Alert.AlertType.ERROR, "No se pudo regenerar el RIDE autorizado.").showAndWait();
                return;
            }
            File pdf = new File(ruta);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT)) {
                Desktop.getDesktop().print(pdf);
                new Alert(Alert.AlertType.INFORMATION, "RIDE autorizado enviado a la impresora:\n" + ruta).showAndWait();
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdf);
                new Alert(Alert.AlertType.INFORMATION,
                        "Impresion directa no soportada: se abrio el RIDE autorizado para imprimirlo manualmente:\n" + ruta).showAndWait();
            } else {
                new Alert(Alert.AlertType.INFORMATION, "RIDE autorizado listo en:\n" + ruta).showAndWait();
            }
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "imprimirAutorizada", String.valueOf(e));
            new Alert(Alert.AlertType.ERROR, "Error al imprimir: " + e.getMessage()).showAndWait();
        }
        cargarDatos();
    }

    private void enviarCorreoAutorizado(FacturaRegistro fr) {
        String[] aut = datosAutorizacion(fr);
        if (aut == null || !ElectronicoUtil.debeEnviarNotificacion(fr.getEstadoSri(), aut[0], aut[1])) {
            new Alert(Alert.AlertType.WARNING,
                    "Factura " + fr.getNumComprobante() + " aun sin autorizacion. El correo se habilita al autorizarse.").showAndWait();
            return;
        }
        try {
            Cliente cliente = clienteDAO.obtenerPorId(fr.getClienteId());
            if (cliente == null || cliente.getCorreo() == null || cliente.getCorreo().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "El cliente no tiene correo registrado.").showAndWait();
                return;
            }
            String rutaPDF = facturaService.regenerarRide(fr.getClaveAcceso(), aut[0], aut[1], obtenerDirectorioEscritorio());
            if (rutaPDF == null) {
                new Alert(Alert.AlertType.ERROR, "No se pudo regenerar el RIDE autorizado.").showAndWait();
                return;
            }
            String rutaXML = System.getProperty("user.home") + File.separator + AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT
                    + File.separator + AppConstants.PREFIJO_PDF_FACTURA + fr.getNumComprobante().replace("-", "") + AppConstants.EXTENSION_XML;
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCorreoConArchivos(
                    cliente.getCorreo().trim(), cliente.getNombre(), fr.getCodigo(),
                    AppConstants.TIPO_DOCUMENTO_FACTURA, new File(rutaPDF), new File(rutaXML));
            if (enviado) {
                logDAO.guardar("SeguimientoSriController", "enviarCorreoAutorizado",
                        "Correo enviado a " + cliente.getCorreo() + " para factura " + fr.getClaveAcceso());
                new Alert(Alert.AlertType.INFORMATION, "Correo enviado a " + cliente.getCorreo()).showAndWait();
            } else {
                new Alert(Alert.AlertType.WARNING, "No se pudo enviar el correo: " + emailService.getUltimoError()).showAndWait();
            }
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "enviarCorreoAutorizado", String.valueOf(e));
            new Alert(Alert.AlertType.ERROR, "Error al enviar correo: " + e.getMessage()).showAndWait();
        }
        cargarDatos();
    }

    private void cargarDatos() {
        String filtro = txtBuscar.getText();
        totalCount = dao.contar(filtro);
        totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
        listaFacturas.setAll(dao.listarPaginado(currentPage, pageSize, filtro));
        tblFacturas.setItems(listaFacturas);
        actualizarPaginaInfo();
    }

    private void iniciarPageSize() {
        ComboFilter.habilitarEnteros(cmbPageSize, FXCollections.observableArrayList(25, 50, 100));
        cmbPageSize.setValue(25);
        cmbPageSize.setOnAction(e -> {
            Integer valor = cmbPageSize.getValue();
            if (valor == null) {
                try { valor = Integer.parseInt(cmbPageSize.getEditor().getText().trim()); }
                catch (NumberFormatException ignored) {}
            }
            if (valor != null && valor > 0) {
                pageSize = valor;
                currentPage = 1;
                cargarDatos();
            }
        });
    }

    @FXML private void irPaginaAnterior() {
        if (currentPage > 1) { currentPage--; cargarDatos(); }
    }

    @FXML private void irPaginaSiguiente() {
        if (currentPage < totalPages) { currentPage++; cargarDatos(); }
    }

    @FXML
    private void consultarSri() {
        // Contingencia: la cola persistente se encarga automáticamente cada 2min (ServicioReintentoSri).
        // Este botón ahora es solo vista/manual: fuerza reintento inmediato de la cola.
        com.vendex.dao.ComprobantePendienteSriDAO pendDao = AppContext.getInstance().comprobantePendienteSriDAO;
        int pendientesCola = pendDao.contarPendientes();
        if (pendientesCola == 0) {
            // fallback: también revisar tablas legacy por si hay pendientes no encolados (migración inicial)
            List<FacturaRegistro> pendientesLegacy = dao.listarPendientesSri();
            if (pendientesLegacy.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No hay comprobantes en cola SRI. La cola persistente reintenta cada 2min automáticamente.\nPendientes en cola: 0").showAndWait();
                cargarDatos();
                return;
            }
        }
        btnConsultarSri.setDisable(true);
        Task<String> tarea = new Task<>() {
            @Override protected String call() {
                // forzar proximo_intento=NOW para pendientes y ejecutar ciclo
                try {
                    for (com.vendex.model.ComprobantePendienteSri p : pendDao.listarParaReintentar(100)) {
                        pendDao.forzarReintentoAhora(p.getClaveAcceso());
                    }
                } catch (Exception e) { LOGGER.log(Level.WARNING, "forzar reintento", e); }
                com.vendex.service.ServicioReintentoSri.getInstance().ciclo();
                int restantes = pendDao.contarPendientes();
                int totalCola = pendDao.listarTodos(1000).size();
                return "Cola SRI procesada.\nPendientes restantes: " + restantes + "\nTotal en cola (incl. RECHAZADA/AGOTADA): " + totalCola + "\n\nEl servicio reintenta automáticamente cada 2min con backoff (2/15/60min, AGOTADA 24h). Las RECHAZADAS no se reintentan.";
            }
        };
        tarea.setOnSucceeded(e -> { btnConsultarSri.setDisable(false); cargarDatos(); new Alert(Alert.AlertType.INFORMATION, tarea.getValue()).showAndWait(); });
        tarea.setOnFailed(e -> { btnConsultarSri.setDisable(false); Throwable ex = tarea.getException(); logDAO.guardar("SeguimientoSriController", "consultarSri", String.valueOf(ex)); new Alert(Alert.AlertType.ERROR, "Error al consultar SRI: " + (ex != null ? ex.getMessage() : "desconocido")).showAndWait(); });
        new Thread(tarea, "Hilo-ConsultarSRI").start();
        return;
    }

    @SuppressWarnings("unused")
    private void consultarSriLegacy() {
        List<FacturaRegistro> pendientes = dao.listarPendientesSri();
        NotaCreditoRegistroDAO ncDao = AppContext.getInstance().notaCreditoRegistroDAO;
        List<NotaCreditoRegistro> pendientesNc = ncDao.listarPendientesSri();
        final List<NotaDebitoRegistro> pendientesNd = AppContext.getInstance().notaDebitoRegistroDAO.listarPendientesSri();
        final List<GuiaRemisionRegistro> pendientesGr = AppContext.getInstance().guiaRemisionRegistroDAO.listarPendientesSri();
        final List<RetencionRegistro> pendientesRet = AppContext.getInstance().retencionRegistroDAO.listarPendientesSri();
        if (pendientes.isEmpty() && pendientesNc.isEmpty() && pendientesNd.isEmpty() && pendientesGr.isEmpty() && pendientesRet.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay facturas/notas de crédito/débito/guía de remisión/retención pendientes por consultar con el SRI.").showAndWait();
            return;
        }
        btnConsultarSri.setDisable(true);
        Task<String> tarea = new Task<>() {
            @Override
            protected String call() {
                int autorizadas = 0, rechazadas = 0, pendientesN = 0, errores = 0;
                StringBuilder emailsEnviados = new StringBuilder();
                ComprobanteDAO ceDAO = AppContext.getInstance().comprobanteDAO;
                for (FacturaRegistro f : pendientes) {
                    String clave = f.getClaveAcceso();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    try {
                        String ambiente = f.getAmbienteSri() == null || f.getAmbienteSri().trim().isEmpty()
                                ? "PRUEBAS" : f.getAmbienteSri();
                        SRIWebService.SRIResponse r = new SRIWebService(ambiente).consultarAutorizacion(clave);
                        String estado = r.getEstado();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado) || AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado)) {
                            ceDAO.actualizarEstado(clave, estado, r.getMensaje(), null, r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            dao.actualizarEstado(clave, estado);
                            if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                                autorizadas++;
                                String numAut = r.getNumeroAutorizacion();
                                String fechaAut = r.getFechaAutorizacion();
                                if (ElectronicoUtil.debeEnviarNotificacion(estado, numAut, fechaAut)) {
                                    try {
                                        Cliente cliente = clienteDAO.obtenerPorId(f.getClienteId());
                                        if (cliente != null && cliente.getCorreo() != null && !cliente.getCorreo().trim().isEmpty()) {
                                            String numComp = f.getNumComprobante();
                                    String rutaPDF = System.getProperty("user.home") + File.separator + AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT
                                                    + File.separator + AppConstants.PREFIJO_PDF_FACTURA + numComp.replace("-", "") + AppConstants.EXTENSION_PDF;
                                            String rutaXML = System.getProperty("user.home") + File.separator + AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT
                                                    + File.separator + AppConstants.PREFIJO_PDF_FACTURA + numComp.replace("-", "") + AppConstants.EXTENSION_XML;
                                            String pdfRegenerado = facturaService.regenerarRide(clave, numAut, fechaAut, obtenerDirectorioEscritorio());
                                            if (pdfRegenerado != null) {
                                                rutaPDF = pdfRegenerado;
                                            }
                                            EmailService emailService = new EmailService();
                                            boolean enviado = emailService.enviarCorreoConArchivos(
                                                    cliente.getCorreo().trim(),
                                                    cliente.getNombre(),
                                                    f.getCodigo(),
                                                    AppConstants.TIPO_DOCUMENTO_FACTURA,
                                                    new File(rutaPDF),
                                                    new File(rutaXML));
                                            if (enviado) {
                                                emailsEnviados.append("✓ ").append(cliente.getCorreo()).append("\n");
                                                logDAO.guardar("SeguimientoSriController", "consultarSri",
                                                        "Correo enviado a " + cliente.getCorreo() + " para factura " + clave);
                                            } else {
                                                logDAO.guardar("SeguimientoSriController", "consultarSri",
                                                        "Fallo envío correo a " + cliente.getCorreo() + ": " + emailService.getUltimoError());
                                            }
                                        }
                                    } catch (Exception exEmail) {
                                        logDAO.guardar("SeguimientoSriController", "consultarSri",
                                                "Error enviando correo para " + clave + ": " + exEmail.getMessage(), exEmail);
                                    }
                                }
                            } else {
                                rechazadas++;
                            }
                        } else if (AppConstants.ESTADO_ERROR.equals(estado)) {
                            errores++;
                        } else {
                            pendientesN++;
                        }
                    } catch (Exception e) {
                        errores++;
                        logDAO.guardar("SeguimientoSriController", "consultarSri", "Error consultando " + clave + ": " + e.getMessage(), e);
                    }
                }
                // Procesar NC pendientes
                NotaCreditoService ncService = AppContext.getInstance().notaCreditoService;
                File dirEsc = obtenerDirectorioEscritorio();
                for (NotaCreditoRegistro nc : pendientesNc) {
                    String clave = nc.getClaveAcceso();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    try {
                        String ambiente = "PRUEBAS";
                        // intentar recuperar ambiente desde comprobante si existe
                        SRIWebService.SRIResponse r = new SRIWebService(ambiente).consultarAutorizacion(clave);
                        String estado = r.getEstado();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado) || AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado)) {
                            ceDAO.actualizarEstado(clave, estado, r.getMensaje(), null, r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            ncDao.actualizarEstado(clave, estado, r.getMensaje(), r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                                autorizadas++;
                                String numAut = r.getNumeroAutorizacion();
                                String fechaAut = r.getFechaAutorizacion();
                                try {
                                    NotaCreditoRegistro ncFull = ncDao.obtenerPorClave(clave);
                                    String pdfRegenerado = ncService.regenerarRide(clave, numAut, fechaAut, dirEsc);
                                    Cliente cli = clienteDAO.obtenerPorId(ncFull.getClienteId());
                                    if (cli != null && cli.getCorreo()!=null && !cli.getCorreo().trim().isEmpty() && pdfRegenerado!=null) {
                                        String rutaXML = System.getProperty("user.home")+File.separator+AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT+File.separator+AppConstants.PREFIJO_PDF_NOTA_CREDITO+ncFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
                                        EmailService es = new EmailService();
                                        boolean enviado = es.enviarCorreoConArchivos(cli.getCorreo().trim(), cli.getNombre(), ncFull.getNumComprobante(), AppConstants.TIPO_DOCUMENTO_NOTA_CREDITO, new File(pdfRegenerado), new File(rutaXML));
                                        if (enviado) emailsEnviados.append("✓ NC ").append(cli.getCorreo()).append("\n");
                                    }
                                } catch (Exception exEmail) { logDAO.guardar("SeguimientoSriController","consultarSri NC", "Error correo NC "+clave+": "+exEmail.getMessage(), exEmail); }
                            } else rechazadas++;
                        } else if (AppConstants.ESTADO_ERROR.equals(estado)) errores++; else pendientesN++;
                    } catch (Exception e) { errores++; logDAO.guardar("SeguimientoSriController","consultarSri NC","Error NC "+clave+": "+e.getMessage(), e); }
                }
                // Procesar ND pendientes
                NotaDebitoRegistroDAO ndDao = AppContext.getInstance().notaDebitoRegistroDAO;
                NotaDebitoService ndService = AppContext.getInstance().notaDebitoService;
                for (NotaDebitoRegistro nd : pendientesNd) {
                    String clave = nd.getClaveAcceso();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    try {
                        String ambiente = "PRUEBAS";
                        SRIWebService.SRIResponse r = new SRIWebService(ambiente).consultarAutorizacion(clave);
                        String estado = r.getEstado();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado) || AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado)) {
                            ceDAO.actualizarEstado(clave, estado, r.getMensaje(), null, r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            ndDao.actualizarEstado(clave, estado, r.getMensaje(), r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                                autorizadas++;
                                String numAut = r.getNumeroAutorizacion();
                                String fechaAut = r.getFechaAutorizacion();
                                try {
                                    NotaDebitoRegistro ndFull = ndDao.obtenerPorClave(clave);
                                    String pdfRegenerado = ndService.regenerarRide(clave, numAut, fechaAut, dirEsc);
                                    Cliente cli = clienteDAO.obtenerPorId(ndFull.getClienteId());
                                    if (cli != null && cli.getCorreo()!=null && !cli.getCorreo().trim().isEmpty() && pdfRegenerado!=null) {
                                        String rutaXML = System.getProperty("user.home")+File.separator+AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT+File.separator+AppConstants.PREFIJO_PDF_NOTA_DEBITO+ndFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
                                        EmailService es = new EmailService();
                                        boolean enviado = es.enviarCorreoConArchivos(cli.getCorreo().trim(), cli.getNombre(), ndFull.getNumComprobante(), AppConstants.TIPO_DOCUMENTO_NOTA_DEBITO, new File(pdfRegenerado), new File(rutaXML));
                                        if (enviado) emailsEnviados.append("✓ ND ").append(cli.getCorreo()).append("\n");
                                    }
                                } catch (Exception exEmail) { logDAO.guardar("SeguimientoSriController","consultarSri ND", "Error correo ND "+clave+": "+exEmail.getMessage(), exEmail); }
                            } else rechazadas++;
                        } else if (AppConstants.ESTADO_ERROR.equals(estado)) errores++; else pendientesN++;
                    } catch (Exception e) { errores++; logDAO.guardar("SeguimientoSriController","consultarSri ND","Error ND "+clave+": "+e.getMessage(), e); }
                }
                // Procesar GR pendientes
                GuiaRemisionRegistroDAO grDao = AppContext.getInstance().guiaRemisionRegistroDAO;
                GuiaRemisionService grService = AppContext.getInstance().guiaRemisionService;
                for (GuiaRemisionRegistro gr : pendientesGr) {
                    String clave = gr.getClaveAcceso();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    try {
                        String ambiente = "PRUEBAS";
                        SRIWebService.SRIResponse r = new SRIWebService(ambiente).consultarAutorizacion(clave);
                        String estado = r.getEstado();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado) || AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado)) {
                            ceDAO.actualizarEstado(clave, estado, r.getMensaje(), null, r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            grDao.actualizarEstado(clave, estado, r.getMensaje(), r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                                autorizadas++;
                                String numAut = r.getNumeroAutorizacion();
                                String fechaAut = r.getFechaAutorizacion();
                                try {
                                    GuiaRemisionRegistro grFull = grDao.obtenerPorClave(clave);
                                    String pdfRegenerado = grService.regenerarRide(clave, numAut, fechaAut, dirEsc);
                                    // intentar enviar correo si el destinatario tiene email como cliente
                                    if (pdfRegenerado != null) {
                                        String rutaXML = System.getProperty("user.home")+File.separator+AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT+File.separator+AppConstants.PREFIJO_PDF_GUIA_REMISION+grFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
                                        // correo opcional: buscar primer destinatario que coincida con cliente
                                        try {
                                            java.util.List<com.vendex.model.GuiaRemisionDestinatario> dests = AppContext.getInstance().guiaRemisionDestinatarioDAO.listarPorGuiaId(grFull.getId());
                                            if (!dests.isEmpty()) {
                                                String ident = dests.get(0).getIdentificacionDestinatario();
                                                String correo = null; String nombre = dests.get(0).getRazonSocialDestinatario();
                                                for (Cliente cli : clienteDAO.listar()) if (ident.equals(cli.getIdentificacion()) && cli.getCorreo()!=null) { correo = cli.getCorreo().trim(); nombre = cli.getNombre(); break; }
                                                if (correo != null && !correo.isEmpty()) {
                                                    EmailService es = new EmailService();
                                                    boolean enviado = es.enviarCorreoConArchivos(correo, nombre, grFull.getNumComprobante(), AppConstants.TIPO_DOCUMENTO_GUIA_REMISION, new File(pdfRegenerado), new File(rutaXML));
                                                    if (enviado) emailsEnviados.append("✓ GR ").append(correo).append("\n");
                                                }
                                            }
                                        } catch (Exception ignore) {}
                                    }
                                } catch (Exception exEmail) { logDAO.guardar("SeguimientoSriController","consultarSri GR", "Error correo GR "+clave+": "+exEmail.getMessage(), exEmail); }
                            } else rechazadas++;
                        } else if (AppConstants.ESTADO_ERROR.equals(estado)) errores++; else pendientesN++;
                    } catch (Exception e) { errores++; logDAO.guardar("SeguimientoSriController","consultarSri GR","Error GR "+clave+": "+e.getMessage(), e); }
                }
                // Procesar Retenciones pendientes
                RetencionRegistroDAO retDao = AppContext.getInstance().retencionRegistroDAO;
                RetencionService retService = AppContext.getInstance().retencionService;
                for (RetencionRegistro ret : pendientesRet) {
                    String clave = ret.getClaveAcceso();
                    if (clave == null || clave.trim().isEmpty()) continue;
                    try {
                        String ambiente = "PRUEBAS";
                        SRIWebService.SRIResponse r = new SRIWebService(ambiente).consultarAutorizacion(clave);
                        String estado = r.getEstado();
                        if (AppConstants.ESTADO_AUTORIZADO.equals(estado) || AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado)) {
                            ceDAO.actualizarEstado(clave, estado, r.getMensaje(), null, r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            retDao.actualizarEstado(clave, estado, r.getMensaje(), r.getNumeroAutorizacion(), r.getFechaAutorizacion());
                            if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                                autorizadas++;
                                String numAut = r.getNumeroAutorizacion();
                                String fechaAut = r.getFechaAutorizacion();
                                try {
                                    RetencionRegistro retFull = retDao.obtenerPorClave(clave);
                                    String pdfRegenerado = retService.regenerarRide(clave, numAut, fechaAut, dirEsc);
                                    if (pdfRegenerado != null) {
                                        String rutaXML = System.getProperty("user.home")+File.separator+AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT+File.separator+AppConstants.PREFIJO_PDF_RETENCION+retFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
                                        // correo a proveedor
                                        String correo = null; String nombre = retFull.getRazonSocialSujeto();
                                        if (retFull.getProveedorId()!=null) {
                                            for (com.vendex.model.Proveedor p : AppContext.getInstance().proveedorDAO.listar()) if (p.getId()==retFull.getProveedorId()) { correo = p.getCorreo(); nombre = p.getNombre(); break; }
                                        }
                                        if (correo==null) for (com.vendex.model.Proveedor p : AppContext.getInstance().proveedorDAO.listar()) if (retFull.getIdentificacionSujeto().equals(p.getIdentificacion())) { correo = p.getCorreo(); break; }
                                        if (correo != null && !correo.trim().isEmpty()) {
                                            EmailService es = new EmailService();
                                            boolean enviado = es.enviarCorreoConArchivos(correo.trim(), nombre, retFull.getNumComprobante(), AppConstants.TIPO_DOCUMENTO_RETENCION, new File(pdfRegenerado), new File(rutaXML));
                                            if (enviado) emailsEnviados.append("✓ RET ").append(correo).append("\n");
                                        }
                                    }
                                } catch (Exception exEmail) { logDAO.guardar("SeguimientoSriController","consultarSri RET", "Error correo RET "+clave+": "+exEmail.getMessage(), exEmail); }
                            } else rechazadas++;
                        } else if (AppConstants.ESTADO_ERROR.equals(estado)) errores++; else pendientesN++;
                    } catch (Exception e) { errores++; logDAO.guardar("SeguimientoSriController","consultarSri RET","Error RET "+clave+": "+e.getMessage(), e); }
                }

                String resumen = "Autorizadas: " + autorizadas + "\nRechazadas/Devueltas: " + rechazadas
                        + "\nSiguen pendientes: " + pendientesN + "\nCon error: " + errores;
                if (emailsEnviados.length() > 0) {
                    resumen += "\n\nCorreos enviados:\n" + emailsEnviados.toString();
                }
                return resumen;
            }
        };
        tarea.setOnSucceeded(e -> {
            btnConsultarSri.setDisable(false);
            cargarDatos();
            new Alert(Alert.AlertType.INFORMATION, "Consulta al SRI finalizada.\n\n" + tarea.getValue()).showAndWait();
        });
        tarea.setOnFailed(e -> {
            btnConsultarSri.setDisable(false);
            Throwable ex = tarea.getException();
            logDAO.guardar("SeguimientoSriController", "consultarSri", String.valueOf(ex));
            new Alert(Alert.AlertType.ERROR, "Error al consultar el SRI: "
                    + (ex != null ? ex.getMessage() : "desconocido")).showAndWait();
        });
        new Thread(tarea, "Hilo-ConsultarSRI").start();
    }

    private void actualizarPaginaInfo() {
        lblPaginaInfo.setText("Página " + currentPage + " de " + totalPages + " (" + totalCount + " registros)");
        btnAnterior.setDisable(currentPage <= 1);
        btnSiguiente.setDisable(currentPage >= totalPages);
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

    private String formatearEstado(String estado, String mensaje) {
        String e = estado == null ? "" : estado.trim().toUpperCase();
        String m = mensaje == null ? "" : mensaje.toLowerCase();
        if (m.contains("firma no configurada") || m.contains("no se configuró")) return "NO ENVIADO";
        if (m.contains("error de conexión")) return "ERROR DE CONEXIÓN";
        switch (e) {
            case "AUTORIZADO": return "AUTORIZADA";
            case "RECHAZADA": return "RECHAZADA";
            case "DEVUELTA": return "DEVUELTA";
            case "PENDIENTE": return m.contains("recibido") ? "RECIBIDO SRI (pendiente)" : "PENDIENTE";
            default: return e.isEmpty() ? "NO ENVIADO" : e;
        }
    }

    private String colorEstado(String estado, String mensaje) {
        String e = estado == null ? "" : estado.trim().toUpperCase();
        String m = mensaje == null ? "" : mensaje.toLowerCase();
        if (m.contains("firma no configurada") || m.contains("no se configuró") || e.isEmpty()) {
            return "#6c757d";
        }
        switch (e) {
            case "AUTORIZADO": return "#198754";
            case "RECHAZADA":
            case "DEVUELTA": return "#dc3545";
            case "PENDIENTE": return "#fd7e14";
            default: return "#0d6efd";
        }
    }
}
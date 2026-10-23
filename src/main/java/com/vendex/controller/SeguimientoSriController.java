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
import com.vendex.model.SeguimientoSriItem;
import com.vendex.dao.SeguimientoSriDAO;
import com.vendex.dao.ProveedorDAO;
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
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
        this.ncService = ctx.notaCreditoService;
        this.ndService = ctx.notaDebitoService;
        this.grService = ctx.guiaRemisionService;
        this.retService = ctx.retencionService;
        this.proveedorDAO = ctx.proveedorDAO;
        this.seguimientoDAO = new SeguimientoSriDAO();
    }

    private static final Logger LOGGER = Logger.getLogger(SeguimientoSriController.class.getName());

    @FXML private TextField txtBuscar;
    @FXML private HBox barraTipos;
    @FXML private ToggleButton btnTipoTodos;
    @FXML private ToggleButton btnTipoFactura;
    @FXML private ToggleButton btnTipoNc;
    @FXML private ToggleButton btnTipoNd;
    @FXML private ToggleButton btnTipoGuia;
    @FXML private ToggleButton btnTipoRet;
    @FXML private TableView<SeguimientoSriItem> tblComprobantes;
    @FXML private TableColumn<SeguimientoSriItem, String> colTipo;
    @FXML private TableColumn<SeguimientoSriItem, String> colCodigo;
    @FXML private TableColumn<SeguimientoSriItem, String> colNumComprobante;
    @FXML private TableColumn<SeguimientoSriItem, LocalDateTime> colFecha;
    @FXML private TableColumn<SeguimientoSriItem, String> colCliente;
    @FXML private TableColumn<SeguimientoSriItem, String> colFormaPago;
    @FXML private TableColumn<SeguimientoSriItem, BigDecimal> colSubtotal;
    @FXML private TableColumn<SeguimientoSriItem, BigDecimal> colIva;
    @FXML private TableColumn<SeguimientoSriItem, BigDecimal> colTotal;
    @FXML private TableColumn<SeguimientoSriItem, String> colEstado;
    @FXML private TableColumn<SeguimientoSriItem, String> colExplicacion;
    @FXML private TableColumn<SeguimientoSriItem, String> colMensaje;

    @FXML private Label lblPaginaInfo;
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Button btnConsultarSri;
    @FXML private TableColumn<SeguimientoSriItem, String> colAcciones;
    @FXML private ComboBox<Integer> cmbPageSize;

    private int currentPage = 1;
    private int pageSize = 25;
    private int totalPages = 1;
    private int totalCount = 0;
    private String tipoFiltro = "TODOS";
    private final FacturaRegistroDAO dao;
    private final LogDAO logDAO;
    private final ClienteDAO clienteDAO;
    private final FacturaService facturaService;
    private final NotaCreditoService ncService;
    private final NotaDebitoService ndService;
    private final GuiaRemisionService grService;
    private final RetencionService retService;
    private final ProveedorDAO proveedorDAO;
    private final SeguimientoSriDAO seguimientoDAO;
    private final ObservableList<SeguimientoSriItem> listaItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoLabel"));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String label, boolean empty) {
                super.updateItem(label, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                SeguimientoSriItem it = getTableRow().getItem();
                setText(it.getTipoLabel());
                setStyle("-fx-text-fill: " + colorTipo(it.getTipo()) + "; -fx-font-weight: bold;");
            }
        });
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
                SeguimientoSriItem it = getTableRow().getItem();
                String texto = formatearEstado(estado, it.getMensajeSri());
                setText(texto);
                setStyle("-fx-text-fill: " + colorEstado(estado, it.getMensajeSri()) + "; -fx-font-weight: bold;");
                String detalle = it.getMensajeSri();
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
        colExplicacion.setCellValueFactory(new PropertyValueFactory<>("explicacionSri"));
        colExplicacion.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String explicacion, boolean empty) {
                super.updateItem(explicacion, empty);
                if (empty || explicacion == null) {
                    setText(null); setTooltip(null); return;
                }
                setText(explicacion);
                setWrapText(true);
                SeguimientoSriItem item = getTableRow() == null ? null : getTableRow().getItem();
                String detalle = item == null ? null : item.getMensajeSri();
                setTooltip(detalle == null || detalle.isBlank() ? null : new Tooltip("Detalle original del SRI:\n" + detalle));
            }
        });

        tblComprobantes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SortTable.agregarBotones(tblComprobantes);
        configurarColumnaAcciones();

        iniciarPageSize();
        txtBuscar.textProperty().addListener((obs, old, val) -> { currentPage = 1; cargarDatos(); });
        if (barraTipos != null) {
            configurarBarraTipos();
        }

        cargarDatos();
    }

    private ToggleGroup grupoTipos;

    /**
     * Barra segmentada de tipos con estilo en línea (no depende de hojas de tema).
     * No seleccionado: fondo claro + letra negra en negrita.
     * Seleccionado: fondo azul + letra blanca.
     */
    private void configurarBarraTipos() {
        grupoTipos = new ToggleGroup();
        for (javafx.scene.Node n : barraTipos.getChildren()) {
            if (n instanceof ToggleButton b) b.setToggleGroup(grupoTipos);
        }
        grupoTipos.selectToggle(btnTipoTodos != null ? btnTipoTodos : null);
        grupoTipos.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                grupoTipos.selectToggle(btnTipoTodos);
                return;
            }
            Object ud = sel.getUserData();
            tipoFiltro = ud != null ? String.valueOf(ud) : "TODOS";
            aplicarEstilosTipos();
            currentPage = 1;
            cargarDatos();
        });
        tipoFiltro = "TODOS";
        aplicarEstilosTipos();
    }

    private void aplicarEstilosTipos() {
        if (grupoTipos == null) return;
        for (Toggle t : grupoTipos.getToggles()) {
            if (!(t instanceof ToggleButton b)) continue;
            boolean esSel = t == grupoTipos.getSelectedToggle();
            b.setStyle(esSel
                    ? "-fx-min-height: 36; -fx-min-width: 130; -fx-font-size: 14px; -fx-font-weight: bold;"
                      + "-fx-text-fill: white; -fx-background-color: #0d6efd; -fx-background-radius: 8;"
                    : "-fx-min-height: 36; -fx-min-width: 130; -fx-font-size: 14px; -fx-font-weight: bold;"
                      + "-fx-text-fill: black; -fx-background-color: #e9ecef; -fx-background-radius: 8;"
                      + "-fx-border-color: #adb5bd; -fx-border-radius: 8;");
        }
    }

    private static String colorTipo(String tipo) {
        if (tipo == null) return "#6c757d";
        return switch (tipo) {
            case "FACTURA" -> "#0d6efd";
            case "NC" -> "#6f42c1";
            case "ND" -> "#d63384";
            case "GUIA" -> "#0dcaf0";
            case "RETENCION" -> "#198754";
            default -> "#6c757d";
        };
    }

    /**
     * Acciones por fila: Imprimir y Enviar correo, habilitadas SOLO con comprobante
     * AUTORIZADO (con numero de autorizacion). Antes de autorizar no hay nada
     * valido que imprimir ni enviar.
     */
    private void configurarColumnaAcciones() {
        if (colAcciones == null) return;
        colAcciones.setCellValueFactory(new PropertyValueFactory<>("estadoSri"));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnImprimir = new Button("Imprimir");
            private final Button btnCorreo = new Button("Correo");
            private final Button btnAnular = new Button("Anular local");
            private final HBox box = new HBox(6, btnImprimir, btnCorreo, btnAnular);
            {
                // Texto negro sobre fondo gris: legible en modo claro y oscuro
                // (el CSS global hereda texto claro en dark y no se leia).
                btnImprimir.setStyle("-fx-text-fill: black;");
                btnCorreo.setStyle("-fx-text-fill: black;");
                btnImprimir.setOnAction(e -> {
                    SeguimientoSriItem it = getTableRow() == null ? null : getTableRow().getItem();
                    if (it != null) imprimirAutorizada(it);
                });
                btnCorreo.setOnAction(e -> {
                    SeguimientoSriItem it = getTableRow() == null ? null : getTableRow().getItem();
                    if (it != null) enviarCorreoAutorizado(it);
                });
                btnAnular.setStyle("-fx-text-fill: black;");
                btnAnular.setOnAction(e -> {
                    SeguimientoSriItem it = getTableRow() == null ? null : getTableRow().getItem();
                    if (it != null) anularLocal(it);
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
                SeguimientoSriItem item = getTableRow().getItem();
                boolean anulable = esAnulableLocal(item.getEstadoSri());
                btnImprimir.setDisable(!autorizada);
                btnCorreo.setDisable(!autorizada);
                btnAnular.setDisable(!anulable);
                String tip = autorizada ? "Comprobante autorizado: disponible"
                        : "Disponible al autorizarse por el SRI";
                btnImprimir.setTooltip(new Tooltip(tip));
                btnCorreo.setTooltip(new Tooltip(tip));
                btnAnular.setTooltip(new Tooltip(anulable
                        ? "Detiene los reintentos en Vendex; no anula legalmente ante el SRI"
                        : "Disponible solo para pendientes o reintentos agotados"));
                setGraphic(box);
            }
        });
    }

    private boolean esAnulableLocal(String estado) {
        return "PENDIENTE".equalsIgnoreCase(estado) || "AGOTADA".equalsIgnoreCase(estado);
    }

    private void anularLocal(SeguimientoSriItem item) {
        if (item == null || !esAnulableLocal(item.getEstadoSri())) return;
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "Se detendrán los reintentos de este comprobante en Vendex. Esta acción no lo anula legalmente ante el SRI.\n\n"
                        + item.getTipoLabel() + " " + item.getNumComprobante(),
                javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);
        confirmacion.setTitle("Anulación local");
        confirmacion.setHeaderText("¿Detener los reintentos SRI?");
        ((Button) confirmacion.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK)).setText("Anular localmente");
        if (confirmacion.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL)
                != javafx.scene.control.ButtonType.OK) return;
        try {
            boolean anulado = AppContext.getInstance().servicioReintentoSri
                    .anularLocal(item.getClaveAcceso(), item.getTipo());
            if (!anulado) {
                new Alert(Alert.AlertType.INFORMATION,
                        "No se anuló: el comprobante ya no está PENDIENTE/AGOTADO en la cola SRI. Se actualizará la lista.").showAndWait();
            } else {
                logDAO.guardar("SeguimientoSriController", "anularLocal",
                        "Anulación local de " + item.getTipoLabel() + " " + item.getNumComprobante()
                                + " clave=" + item.getClaveAcceso() + "; no anulada ante el SRI");
                new Alert(Alert.AlertType.INFORMATION,
                        "Reintentos detenidos localmente. El estado no implica anulación legal ante el SRI.").showAndWait();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo anular localmente " + item.getClaveAcceso(), e);
            new Alert(Alert.AlertType.ERROR, "No se pudo detener la tarea SRI: " + e.getMessage()).showAndWait();
        }
        cargarDatos();
    }

    /** Lee numero/fecha de autorizacion guardados; deduce de la clave si es AUTORIZADO sin número. */
    private String[] datosAutorizacion(SeguimientoSriItem it) {
        try {
            if (it == null || it.getClaveAcceso() == null) return null;
            String[] aut = AppContext.getInstance().comprobanteDAO.obtenerDatosAutorizacion(it.getClaveAcceso());
            if (aut != null) return aut;
            // Fallback: AUTORIZADO sin número en comprobantes -> deducir de clave (SRI offline: numero==clave).
            // Imprimir se habilita; el backfill real ocurre en regenerarRideItem().
            if (AppConstants.ESTADO_AUTORIZADO.equals(it.getEstadoSri())) {
                String num = ElectronicoUtil.resolverNumero(it.getClaveAcceso(), null);
                if (num != null && !num.trim().isEmpty()) return new String[]{num, ""};
            }
            return null;
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "datosAutorizacion", String.valueOf(e));
            return null;
        }
    }

    private void imprimirAutorizada(SeguimientoSriItem it) {
        String[] aut = datosAutorizacion(it);
        if (aut == null) {
            new Alert(Alert.AlertType.WARNING,
                    it.getTipoLabel() + " " + it.getNumComprobante() + " aun sin autorizacion registrada. Consulte el SRI primero.").showAndWait();
            return;
        }
        try {
            String ruta = regenerarRideItem(it, aut[0], aut[1]);
            if (ruta == null) {
                new Alert(Alert.AlertType.ERROR, "No se pudo regenerar el RIDE autorizado.").showAndWait();
                return;
            }
            File pdf = new File(ruta);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT)) {
                try {
                    Desktop.getDesktop().print(pdf);
                    new Alert(Alert.AlertType.INFORMATION, "RIDE autorizado enviado a la impresora:\n" + ruta).showAndWait();
                } catch (Exception exPrint) {
                    // Sin app asociada al verbo imprimir (típico sin lector PDF): respaldo abrir,
                    // que usa la app predeterminada (normalmente Edge) para imprimir con Ctrl+P.
                    logDAO.guardar("SeguimientoSriController", "imprimirDirecto",
                            "PRINT fallo, usando OPEN: " + exPrint.getMessage());
                    abrirRideParaImpresionManual(pdf, ruta);
                }
            } else if (Desktop.isDesktopSupported()) {
                abrirRideParaImpresionManual(pdf, ruta);
            } else {
                new Alert(Alert.AlertType.INFORMATION, "RIDE autorizado listo en:\n" + ruta).showAndWait();
            }
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "imprimirAutorizada", String.valueOf(e));
            new Alert(Alert.AlertType.ERROR, "Error al imprimir: " + e.getMessage()).showAndWait();
        }
        cargarDatos();
    }

    /** Regenera el RIDE del item según su tipo, en su carpeta por tipo. */
    private String regenerarRideItem(SeguimientoSriItem it, String numAut, String fechaAut) {
        String tipo = it.getTipo() == null ? "" : it.getTipo();
        return switch (tipo) {
            case "NC" -> ncService.regenerarRide(it.getClaveAcceso(), numAut, fechaAut,
                    obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_CREDITO));
            case "ND" -> ndService.regenerarRide(it.getClaveAcceso(), numAut, fechaAut,
                    obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_DEBITO));
            case "GUIA" -> grService.regenerarRide(it.getClaveAcceso(), numAut, fechaAut,
                    obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_GUIA_REMISION));
            case "RETENCION" -> retService.regenerarRide(it.getClaveAcceso(), numAut, fechaAut,
                    obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_RETENCION));
            default -> facturaService.regenerarRide(it.getClaveAcceso(), numAut, fechaAut, obtenerDirectorioEscritorio());
        };
    }

    private String prefijoPdfItem(SeguimientoSriItem it) {
        String tipo = it.getTipo() == null ? "" : it.getTipo();
        return switch (tipo) {
            case "NC" -> AppConstants.PREFIJO_PDF_NOTA_CREDITO;
            case "ND" -> AppConstants.PREFIJO_PDF_NOTA_DEBITO;
            case "GUIA" -> AppConstants.PREFIJO_PDF_GUIA_REMISION;
            case "RETENCION" -> AppConstants.PREFIJO_PDF_RETENCION;
            default -> AppConstants.PREFIJO_PDF_FACTURA;
        };
    }

    private String tipoDocItem(SeguimientoSriItem it) {
        String tipo = it.getTipo() == null ? "" : it.getTipo();
        return switch (tipo) {
            case "NC" -> AppConstants.TIPO_DOCUMENTO_NOTA_CREDITO;
            case "ND" -> AppConstants.TIPO_DOCUMENTO_NOTA_DEBITO;
            case "GUIA" -> AppConstants.TIPO_DOCUMENTO_GUIA_REMISION;
            case "RETENCION" -> AppConstants.TIPO_DOCUMENTO_RETENCION;
            default -> AppConstants.TIPO_DOCUMENTO_FACTURA;
        };
    }

    private File dirItem(SeguimientoSriItem it) {
        String tipo = it.getTipo() == null ? "" : it.getTipo();
        return switch (tipo) {
            case "NC" -> obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_CREDITO);
            case "ND" -> obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_DEBITO);
            case "GUIA" -> obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_GUIA_REMISION);
            case "RETENCION" -> obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_RETENCION);
            default -> obtenerDirectorioEscritorio();
        };
    }

    /** Respaldo: abre el RIDE (Edge/navegador) para imprimir con Ctrl+P; si ni eso hay, muestra la ruta. */
    private void abrirRideParaImpresionManual(File pdf, String ruta) {
        try {
            Desktop.getDesktop().open(pdf);
            new Alert(Alert.AlertType.INFORMATION,
                    "No hay impresion directa en esta PC: se abrio el RIDE autorizado (navegador) para imprimirlo con Ctrl+P:\n" + ruta).showAndWait();
        } catch (Exception exOpen) {
            logDAO.guardar("SeguimientoSriController", "abrirRide",
                    "OPEN tambien fallo: " + exOpen.getMessage());
            new Alert(Alert.AlertType.WARNING,
                    "Esta PC no tiene aplicacion para PDF. El RIDE autorizado esta guardado en:\n" + ruta
                    + "\n\nInstala un lector PDF (Edge, Adobe o SumatraPDF) o copia el archivo a otra PC para imprimirlo.").showAndWait();
        }
    }

    private void enviarCorreoAutorizado(SeguimientoSriItem it) {
        String[] aut = datosAutorizacion(it);
        if (aut == null || !ElectronicoUtil.debeEnviarNotificacion(it.getEstadoSri(), aut[0], aut[1])) {
            new Alert(Alert.AlertType.WARNING,
                    it.getTipoLabel() + " " + it.getNumComprobante() + " aun sin autorizacion. El correo se habilita al autorizarse.").showAndWait();
            return;
        }
        try {
            String destinatario = destinatarioItem(it);
            String nombreDest = it.getNombreCliente() != null ? it.getNombreCliente() : "";
            if (destinatario == null || destinatario.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "El destinatario no tiene correo registrado.").showAndWait();
                return;
            }
            String rutaPDF = regenerarRideItem(it, aut[0], aut[1]);
            if (rutaPDF == null) {
                new Alert(Alert.AlertType.ERROR, "No se pudo regenerar el RIDE autorizado.").showAndWait();
                return;
            }
            String rutaXML = dirItem(it).getAbsolutePath() + File.separator + prefijoPdfItem(it)
                    + (it.getNumComprobante() != null ? it.getNumComprobante().replace("-", "") : "") + AppConstants.EXTENSION_XML;
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCorreoConArchivos(
                    destinatario.trim(), nombreDest, it.getCodigo(),
                    tipoDocItem(it), new File(rutaPDF), new File(rutaXML));
            if (enviado) {
                logDAO.guardar("SeguimientoSriController", "enviarCorreoAutorizado",
                        "Correo enviado a " + destinatario + " para " + it.getClaveAcceso());
                new Alert(Alert.AlertType.INFORMATION, "Correo enviado a " + destinatario).showAndWait();
            } else {
                new Alert(Alert.AlertType.WARNING, "No se pudo enviar el correo: " + emailService.getUltimoError()).showAndWait();
            }
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "enviarCorreoAutorizado", String.valueOf(e));
            new Alert(Alert.AlertType.ERROR, "Error al enviar correo: " + e.getMessage()).showAndWait();
        }
        cargarDatos();
    }

    /** Resuelve el correo destinatario según el tipo (cliente o proveedor). */
    private String destinatarioItem(SeguimientoSriItem it) {
        try {
            String tipo = it.getTipo() == null ? "" : it.getTipo();
            if ("RETENCION".equals(tipo)) {
                for (com.vendex.model.Proveedor p : AppContext.getInstance().proveedorDAO.listar()) {
                    if (it.getRefId() != 0 && p.getId() == it.getRefId() && p.getCorreo() != null) return p.getCorreo().trim();
                }
                return null;
            }
            if ("GUIA".equals(tipo)) {
                try {
                    var dests = AppContext.getInstance().guiaRemisionDestinatarioDAO.listarPorGuiaId(it.getRegistroId());
                    if (dests != null && !dests.isEmpty()) {
                        String ident = dests.get(0).getIdentificacionDestinatario();
                        for (Cliente cli : clienteDAO.listar()) {
                            if (ident != null && ident.equals(cli.getIdentificacion()) && cli.getCorreo() != null) return cli.getCorreo().trim();
                        }
                    }
                } catch (Exception ignore) {}
                return null;
            }
            Cliente cliente = clienteDAO.obtenerPorId(it.getRefId());
            return cliente != null ? cliente.getCorreo() : null;
        } catch (Exception e) {
            logDAO.guardar("SeguimientoSriController", "destinatarioItem", String.valueOf(e));
            return null;
        }
    }

    private void cargarDatos() {
        String filtro = txtBuscar != null ? txtBuscar.getText() : null;
        try {
            totalCount = seguimientoDAO.contar(tipoFiltro, filtro);
            totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;
            listaItems.setAll(seguimientoDAO.listar(tipoFiltro, currentPage, pageSize, filtro));
        } catch (Exception e) {
            // Fallback modo remoto/sin BD local: solo facturas via DAO inyectado
            logDAO.guardar("SeguimientoSriController", "cargarDatos", "Fallback facturas: " + e.getMessage());
            try {
                totalCount = dao.contar(filtro);
                totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
                if (currentPage > totalPages) currentPage = totalPages;
                if (currentPage < 1) currentPage = 1;
                listaItems.clear();
                for (FacturaRegistro f : dao.listarPaginado(currentPage, pageSize, filtro)) {
                    SeguimientoSriItem it = new SeguimientoSriItem();
                    it.setTipo("FACTURA");
                    it.setRegistroId(f.getId());
                    it.setCodigo(f.getCodigo());
                    it.setNumComprobante(f.getNumComprobante());
                    it.setFecha(f.getFecha());
                    it.setNombreCliente(f.getNombreCliente());
                    it.setFormaPago(f.getFormaPago());
                    it.setSubtotal(f.getSubtotal());
                    it.setIva(f.getIva());
                    it.setTotal(f.getTotal());
                    it.setEstadoSri(f.getEstadoSri());
                    it.setMensajeSri(f.getMensajeSri());
                    it.setClaveAcceso(f.getClaveAcceso());
                    it.setRefId(f.getClienteId());
                    listaItems.add(it);
                }
            } catch (Exception ex) {
                logDAO.guardar("SeguimientoSriController", "cargarDatosFallback", String.valueOf(ex));
            }
        }
        tblComprobantes.setItems(listaItems);
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
                                    String rutaPDF = obtenerDirectorioEscritorio().getAbsolutePath()+File.separator + AppConstants.PREFIJO_PDF_FACTURA + numComp.replace("-", "") + AppConstants.EXTENSION_PDF;
                                            String rutaXML = obtenerDirectorioEscritorio().getAbsolutePath()+File.separator + AppConstants.PREFIJO_PDF_FACTURA + numComp.replace("-", "") + AppConstants.EXTENSION_XML;
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
                File dirNc = obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_CREDITO);
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
                                    String pdfRegenerado = ncService.regenerarRide(clave, numAut, fechaAut, dirNc);
                                    Cliente cli = clienteDAO.obtenerPorId(ncFull.getClienteId());
                                    if (cli != null && cli.getCorreo()!=null && !cli.getCorreo().trim().isEmpty() && pdfRegenerado!=null) {
                                        String rutaXML = obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_CREDITO).getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_NOTA_CREDITO+ncFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
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
                                    String pdfRegenerado = ndService.regenerarRide(clave, numAut, fechaAut, obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_DEBITO));
                                    Cliente cli = clienteDAO.obtenerPorId(ndFull.getClienteId());
                                    if (cli != null && cli.getCorreo()!=null && !cli.getCorreo().trim().isEmpty() && pdfRegenerado!=null) {
                                        String rutaXML = obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_NOTA_DEBITO).getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_NOTA_DEBITO+ndFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
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
                                    String pdfRegenerado = grService.regenerarRide(clave, numAut, fechaAut, obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_GUIA_REMISION));
                                    // intentar enviar correo si el destinatario tiene email como cliente
                                    if (pdfRegenerado != null) {
                                        String rutaXML = obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_GUIA_REMISION).getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_GUIA_REMISION+grFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
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
                                    String pdfRegenerado = retService.regenerarRide(clave, numAut, fechaAut, obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_RETENCION));
                                    if (pdfRegenerado != null) {
                                        String rutaXML = obtenerDirectorioPorTipo(com.vendex.util.DirectorioComprobantes.CARPETA_RETENCION).getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_RETENCION+retFull.getNumComprobante().replace("-","")+AppConstants.EXTENSION_XML;
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
        // Carpetas por tipo: Documentos/<Empresa>/Factura/yyyy-MM-dd/.
        return com.vendex.util.DirectorioComprobantes.dirTipoFile(com.vendex.util.DirectorioComprobantes.CARPETA_FACTURA);
    }

    private File obtenerDirectorioPorTipo(String tipo) {
        // Carpetas por tipo: Documentos/<Empresa>/<Tipo>/yyyy-MM-dd/.
        return com.vendex.util.DirectorioComprobantes.dirTipoFile(tipo);
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
            case "ANULADO": return "ANULADO LOCALMENTE";
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
            case "ANULADO": return "#6c757d";
            default: return "#0d6efd";
        }
    }
}

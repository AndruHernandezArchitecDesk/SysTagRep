package com.vendex.controller;

import com.vendex.chatbot.GeminiChatbotService;
import com.vendex.config.GeminiConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import com.vendex.model.Inventario;
import com.vendex.service.VendexiSugerenciaService;
import com.vendex.service.VetxiHistorialService;
import com.vendex.model.VetxiHistorial.*;
import java.util.ArrayList;
import java.util.List;

public class ChatWidgetController {

    public ChatWidgetController() {
        this(com.vendex.config.AppContext.getInstance());
    }

    public ChatWidgetController(com.vendex.config.AppContext ctx) {
        this(ctx, new VendexiSugerenciaService(ctx.vendexiConsumoService));
    }

    ChatWidgetController(com.vendex.config.AppContext ctx, VendexiSugerenciaService sugerencias) {
        this(ctx, sugerencias, ctx.vetxiHistorialService);
    }

    ChatWidgetController(com.vendex.config.AppContext ctx, VendexiSugerenciaService sugerencias, VetxiHistorialService historial) {
        this(ctx, sugerencias, historial, new GeminiChatbotService(ctx.repuestoChatbotDAO, ctx.herramientasAgente));
    }

    ChatWidgetController(com.vendex.config.AppContext ctx, VendexiSugerenciaService sugerencias,
                         VetxiHistorialService historial, GeminiChatbotService chatbot) {
        this(ctx, sugerencias, historial, chatbot, ctx.vendexiConsumoService);
    }

    ChatWidgetController(com.vendex.config.AppContext ctx, VendexiSugerenciaService sugerencias,
                         VetxiHistorialService historial, GeminiChatbotService chatbot,
                         com.vendex.service.VendexiConsumoService medicion) {
        this.chatbotService = chatbot;
        this.vehiculoDAO = ctx.vehiculoDAO;
        this.repuestoChatbotDAO = ctx.repuestoChatbotDAO;
        this.investigacionService = new com.vendex.service.InvestigacionWebService(ctx.vendexiConsumoService);
        this.servicioSugerencias = sugerencias;
        this.historial = historial;
        this.medicionChat = medicion;
    }

    @FXML private StackPane rootStack;
    @FXML private VBox chatPanel;
    @FXML private VBox mensajesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button bubbleButton;
    @FXML private Label badgeIA;
    @FXML private StackPane mascota;
    @FXML private javafx.scene.Group capibara;
    @FXML private VBox sugerenciaBubble;
    @FXML private Label contadorSugerencias, lblSugerenciaTitulo, lblSugerenciaResumen, lblSugerenciaDetalle, lblSugerenciaNumero;
    @FXML private Label lblContexto;
    @FXML private Button btnAgregarSugerencia, btnVerificar, btnPendientes, btnDetalles;
    @FXML private javafx.scene.control.ComboBox<Conversacion> cmbConversaciones;
    @FXML private Button btnNuevaConversacion;
    @FXML private Label lblHistorial;
    @FXML private javafx.scene.control.ComboBox<String> cmbMarca;
    @FXML private javafx.scene.control.ComboBox<String> cmbModelo;
    @FXML private javafx.scene.control.ComboBox<Integer> cmbAnio;

    private final GeminiChatbotService chatbotService;
    private final com.vendex.dao.VehiculoDAO vehiculoDAO;
    private final com.vendex.dao.RepuestoChatbotDAO repuestoChatbotDAO;
    private final com.vendex.service.InvestigacionWebService investigacionService;
    private final VendexiSugerenciaService servicioSugerencias;
    private final VetxiHistorialService historial;
    private final com.vendex.service.VendexiConsumoService medicionChat;
    private java.util.concurrent.ExecutorService hiloHistorial;
    private Ambito ambitoChat;
    private String conversacionChat = java.util.UUID.randomUUID().toString();
    private String tituloChat = "";
    private long versionChat;
    private boolean historialCargado, cargandoHistorial, consultaEnCurso, actualizandoCombo, cerrado, falloPersistencia;
    record TurnoChat(Ambito ambito, String conversacion, String titulo, long version) {}
    private DocumentoVendexi documentoActivo;
    private long revisionDocumento;
    private final List<VendexiSugerenciaService.Sugerencia> pendientes = new ArrayList<>();
    private int indiceSugerencia;
    private Thread consultaComplementarios;
    private final javafx.animation.PauseTransition esperaComplementarios = new javafx.animation.PauseTransition(Duration.millis(650));
    private final javafx.animation.PauseTransition ocultarBurbuja = new javafx.animation.PauseTransition(Duration.seconds(12));
    private MascotaAnimaciones animaciones;
    private GuiaFormulario guia;
    private boolean arrastrando;
    private double inicioMouseX, inicioMouseY, inicioMascotaX, inicioMascotaY;
    private double posicionX, posicionY;
    private final java.util.prefs.Preferences preferencias = java.util.prefs.Preferences.userRoot().node("/com/tag/vendex/asistente");
    private final javafx.beans.value.ChangeListener<Boolean> visibilidadVentana = (o, antes, visible) -> {
        if (visible) actualizarReposo(); else liberar();
    };
    private final javafx.beans.value.ChangeListener<javafx.stage.Window> ventanaListener =
             (o, antes, actual) -> vincularVentana(antes, actual);
    private final javafx.beans.value.ChangeListener<Boolean> focoVentana = (o, antes, actual) -> actualizarReposo();
    private final javafx.event.EventHandler<javafx.scene.input.InputEvent> actividadUsuario = e -> {
        if (animaciones != null) animaciones.registrarActividad();
    };
    private static final String TODAS = "Todas";
    private static final String TODOS = "Todos";
    private long sesionVendexi = -1;

    @FXML
    public void initialize() {
        actualizarBadge();
        configurarMascota();
        iniciarFiltrosVehiculo();
        ocultarBurbuja.setOnFinished(e -> ocultarSugerencias());
        sugerenciaBubble.setOnMouseEntered(e -> ocultarBurbuja.stop());
        sugerenciaBubble.setOnMouseExited(e -> {
            if (!lblSugerenciaDetalle.isVisible()) ocultarBurbuja.playFromStart();
        });
        actualizarContexto();
        cmbConversaciones.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Conversacion c) {
                if (c == null) return "";
                String fecha = c.actualizada() == null ? "" : c.actualizada().replace('T', ' ');
                if (fecha.length() > 16) fecha = fecha.substring(0, 16);
                return c.titulo() + (fecha.isBlank() ? "" : " · " + fecha);
            }
            @Override public Conversacion fromString(String texto) { return null; }
        });
        cmbConversaciones.valueProperty().addListener((o, anterior, actual) -> seleccionarConversacion());
    }

    private void iniciarFiltrosVehiculo() {
        if (cmbMarca == null) return;
        try {
            java.util.List<String> marcas = new java.util.ArrayList<>();
            marcas.add(TODAS);
            marcas.addAll(vehiculoDAO.listarMarcas());
            cmbMarca.setItems(javafx.collections.FXCollections.observableArrayList(marcas));
            cmbMarca.setValue(TODAS);
        } catch (Exception e) {
            cmbMarca.setDisable(true);
            if (cmbModelo != null) cmbModelo.setDisable(true);
            if (cmbAnio != null) cmbAnio.setDisable(true);
            return;
        }
        cmbMarca.setOnAction(e -> onMarcaSeleccionada());
        cmbModelo.setOnAction(e -> onModeloSeleccionado());
    }

    private void onMarcaSeleccionada() {
        String marca = valorSeleccionado(cmbMarca.getValue());
        java.util.List<String> modelos = new java.util.ArrayList<>();
        modelos.add(TODOS);
        if (marca != null) {
            try { modelos.addAll(vehiculoDAO.listarModelosPorMarca(marca)); } catch (Exception ignore) {}
        }
        cmbModelo.setItems(javafx.collections.FXCollections.observableArrayList(modelos));
        cmbModelo.setValue(TODOS);
        cmbAnio.getItems().clear();
    }

    private void onModeloSeleccionado() {
        String marca = valorSeleccionado(cmbMarca != null ? cmbMarca.getValue() : null);
        String modelo = valorSeleccionado(cmbModelo != null ? cmbModelo.getValue() : null);
        java.util.List<Integer> anios = new java.util.ArrayList<>();
        if (marca != null && modelo != null) {
            try { anios.addAll(vehiculoDAO.listarAniosPorModelo(marca, modelo)); } catch (Exception ignore) {}
        }
        anios.add(0, null);
        cmbAnio.setItems(javafx.collections.FXCollections.observableArrayList(anios));
        cmbAnio.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override public String toString(Integer v) { return v == null ? "Año" : String.valueOf(v); }
            @Override public Integer fromString(String s) { return null; }
        });
        cmbAnio.setValue(null);
    }

    private String valorSeleccionado(String v) {
        if (v == null || v.isBlank()) return null;
        String t = v.trim();
        if (TODAS.equalsIgnoreCase(t) || TODOS.equalsIgnoreCase(t) || "Año".equalsIgnoreCase(t)) return null;
        return t;
    }

    private String filtroMarca() { return cmbMarca != null ? valorSeleccionado(cmbMarca.getValue()) : null; }
    private String filtroModelo() { return cmbModelo != null ? valorSeleccionado(cmbModelo.getValue()) : null; }
    private Integer filtroAnio() { return cmbAnio != null ? cmbAnio.getValue() : null; }

    void cambiarMascota(String tipo) {
        terminarGuia();
        boolean robot = !"capibara".equals(tipo);
        final javafx.scene.Group dibujo;
        try {
            dibujo = javafx.fxml.FXMLLoader.load(getClass().getResource(robot ? "/view/RobotVendexi.fxml" : "/view/Capibara.fxml"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo cargar la mascota", e);
        }
        java.util.function.BooleanSupplier libre = () -> !arrastrando && !bubbleButton.isHover()
                && !chatPanel.isVisible() && !sugerenciaBubble.isVisible()
                && pendientes.isEmpty() && !inputField.isDisabled();
        MascotaAnimaciones nuevo = robot ? new VendexiAnimaciones(dibujo, libre, mascota, rootStack, this::moverMascota)
                : new VetxiAnimaciones(dibujo, mascota, rootStack, libre, this::moverMascota);
        if (animaciones != null) animaciones.setActivo(false);
        capibara = dibujo;
        bubbleButton.setGraphic(dibujo);
        bubbleButton.setAccessibleText("Vetxi, asistente. Arrastra para mover; pulsa para abrir. Clic derecho para Guía o cambiar mascota.");
        animaciones = nuevo;
        String modulo = documentoActivo == null ? "" : documentoActivo.contextoVendexi().modulo;
        animaciones.setTrabajando("factura".equals(modulo) || "proforma".equals(modulo));
        preferencias.put("mascota", robot ? "vendexi" : "capibara");
        actualizarReposo();
    }

    private void configurarMascota() {
        posicionX = limitar(preferencias.getDouble("x", 0.97), 0, 1);
        posicionY = limitar(preferencias.getDouble("y", 0.90), 0, 1);
        cambiarMascota(preferencias.get("mascota", "vendexi"));
        var grupo = new javafx.scene.control.ToggleGroup();
        var menu = new javafx.scene.control.ContextMenu();
        var guiaItem = new javafx.scene.control.MenuItem("Guía");
        guiaItem.setOnAction(e -> iniciarGuia());
        menu.getItems().addAll(guiaItem, new javafx.scene.control.SeparatorMenuItem());
        for (String tipo : new String[]{"vendexi", "capibara"}) {
            var item = new javafx.scene.control.RadioMenuItem(tipo.equals("vendexi") ? "Vendexi (robot)" : "Vetxi (capibara)");
            item.setToggleGroup(grupo);
            item.setSelected(tipo.equals(preferencias.get("mascota", "vendexi")));
            item.setOnAction(e -> cambiarMascota(tipo));
            menu.getItems().add(item);
        }
        bubbleButton.setContextMenu(menu);
        bubbleButton.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (!e.isPrimaryButtonDown()) return;
            arrastrando = false;
            inicioMouseX = e.getSceneX(); inicioMouseY = e.getSceneY();
            inicioMascotaX = mascota.getLayoutX(); inicioMascotaY = mascota.getLayoutY();
        });
        bubbleButton.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isPrimaryButtonDown()) return;
            double dx = e.getSceneX() - inicioMouseX, dy = e.getSceneY() - inicioMouseY;
            if (!arrastrando && Math.hypot(dx, dy) < 5) return;
            arrastrando = true;
            animaciones.registrarActividad();
            moverMascota(inicioMascotaX + dx, inicioMascotaY + dy);
            e.consume();
        });
        bubbleButton.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            if (arrastrando) {
                preferencias.putDouble("x", posicionX); preferencias.putDouble("y", posicionY);
                // Consume la liberación para que el skin del botón no genere ActionEvent.
                bubbleButton.disarm();
                e.consume();
                actualizarReposo();
                Platform.runLater(() -> arrastrando = false);
            }
        });
        rootStack.widthProperty().addListener((o, a, b) -> { animaciones.registrarActividad(); posicionarAsistente(); });
        rootStack.heightProperty().addListener((o, a, b) -> { animaciones.registrarActividad(); posicionarAsistente(); });
        rootStack.sceneProperty().addListener((o, antes, actual) -> {
            if (antes != null) {
                vincularActividad(antes, false);
                antes.windowProperty().removeListener(ventanaListener);
                vincularVentana(antes.getWindow(), null);
            }
            if (actual != null) {
                vincularActividad(actual, true);
                actual.windowProperty().addListener(ventanaListener);
                vincularVentana(null, actual.getWindow());
            } else if (antes != null) liberar();
        });
    }

    private void vincularVentana(javafx.stage.Window anterior, javafx.stage.Window actual) {
        if (anterior != null) {
            anterior.showingProperty().removeListener(visibilidadVentana);
            anterior.focusedProperty().removeListener(focoVentana);
            if (anterior instanceof javafx.stage.Stage stage) stage.iconifiedProperty().removeListener(focoVentana);
        }
        if (actual != null) {
            actual.showingProperty().addListener(visibilidadVentana);
            actual.focusedProperty().addListener(focoVentana);
            if (actual instanceof javafx.stage.Stage stage) stage.iconifiedProperty().addListener(focoVentana);
        }
        if (anterior != null && actual == null) liberar();
        actualizarReposo();
    }

    private void actualizarReposo() {
        boolean visible = rootStack.getScene() != null && rootStack.getScene().getWindow() != null
                && rootStack.getScene().getWindow().isShowing() && rootStack.getScene().getWindow().isFocused()
                && !(rootStack.getScene().getWindow() instanceof javafx.stage.Stage stage && stage.isIconified());
        animaciones.setActivo(visible && (guia == null || !guia.activa()));
    }

    private void liberar() {
        terminarGuia();
        cerrado = true;
        versionChat++;
        historialCargado = false;
        cargandoHistorial = false;
        consultaEnCurso = false;
        chatPanel.setVisible(false);
        mensajesBox.getChildren().clear(); inputField.clear();
        if (hiloHistorial != null) hiloHistorial.shutdown();
        animaciones.setActivo(false);
        setDocumentoActivo(null);
        finalizarSesionVendexi();
    }

    private void vincularActividad(javafx.scene.Scene escena, boolean conectar) {
        for (javafx.event.EventType<? extends javafx.scene.input.InputEvent> tipo : List.of(
                javafx.scene.input.MouseEvent.MOUSE_MOVED, javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED, javafx.scene.input.ScrollEvent.SCROLL,
                javafx.scene.input.KeyEvent.KEY_PRESSED, javafx.scene.input.TouchEvent.TOUCH_PRESSED)) {
            if (conectar) escena.addEventFilter(tipo, actividadUsuario);
            else escena.removeEventFilter(tipo, actividadUsuario);
        }
    }

    static double limitar(double valor, double minimo, double maximo) {
        return Double.isFinite(valor) ? Math.max(minimo, Math.min(maximo, valor)) : minimo;
    }

    void moverMascota(double x, double y) {
        double ancho = Math.max(0, rootStack.getWidth() - 104);
        double alto = Math.max(0, rootStack.getHeight() - 108);
        posicionX = ancho > 0 ? limitar(x, 0, ancho) / ancho : 0;
        posicionY = alto > 0 ? limitar(y, 0, alto) / alto : 0;
        posicionarAsistente();
    }

    void posicionarAsistente() {
        double w = rootStack.getWidth(), h = rootStack.getHeight();
        if (w <= 0 || h <= 0) return;
        mascota.resize(104, 108);
        mascota.relocate(posicionX * Math.max(0, w - 104), posicionY * Math.max(0, h - 108));
        posicionarCuadro(chatPanel, Math.min(380, w - 16), Math.min(490, h - 16));
        sugerenciaBubble.setPrefWidth(Math.max(0, Math.min(340, w - 16)));
        if (sugerenciaBubble.isVisible() && rootStack.getScene() != null
                && !rootStack.getScene().getStylesheets().isEmpty()) sugerenciaBubble.applyCss();
        double altoBurbuja = Math.min(sugerenciaBubble.prefHeight(sugerenciaBubble.getPrefWidth()), h - 16);
        posicionarCuadro(sugerenciaBubble, sugerenciaBubble.getPrefWidth(), altoBurbuja);
    }

    private void posicionarCuadro(javafx.scene.layout.Region cuadro, double ancho, double alto) {
        ancho = Math.max(0, ancho); alto = Math.max(0, alto);
        cuadro.resize(ancho, alto);
        double x = mascota.getLayoutX() + 104 - ancho;
        double y = mascota.getLayoutY() - alto - 10;
        if (y < 8) y = mascota.getLayoutY() + 108 + 10;
        cuadro.relocate(limitar(x, 8, Math.max(8, rootStack.getWidth() - ancho - 8)),
                limitar(y, 8, Math.max(8, rootStack.getHeight() - alto - 8)));
    }

    @FXML private void onMascota() {
        if (arrastrando) { arrastrando = false; return; }
        terminarGuia();
        if (!pendientes.isEmpty()) mostrarSugerencias(); else onToggleChat();
    }

    public void setDocumentoActivo(DocumentoVendexi documento) {
        terminarGuia();
        if (documentoActivo != null) documentoActivo.conectarVendexi(null);
        documentoActivo = documento;
        documentoModificado(documento);
        if (documento != null) documento.conectarVendexi(this);
        actualizarContexto();
    }

    void iniciarGuia() {
        if (guia != null && guia.activa()) { terminarGuia(); return; }
        javafx.scene.Parent formulario = documentoActivo == null ? null : documentoActivo.formularioGuia();
        if (formulario == null || !"factura".equals(documentoActivo.contextoVendexi().modulo)) {
            var aviso = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "La guía está disponible en Factura de venta. Abre ese formulario y elige Guía con clic derecho sobre Vetxi.");
            aviso.setHeaderText("Guía de Vetxi");
            if (rootStack.getScene() != null) aviso.initOwner(rootStack.getScene().getWindow());
            aviso.show();
            return;
        }
        chatPanel.setVisible(false); ocultarSugerencias();
        animaciones.setActivo(false);
        guia = new GuiaFormulario(rootStack, formulario, capibara, this::actualizarReposo);
        guia.iniciar();
        actualizarReposo();
    }

    private void terminarGuia() {
        if (guia != null) { guia.cerrar(); guia = null; }
    }

    public void documentoModificado(DocumentoVendexi origen) {
        if (origen != documentoActivo) return;
        revisionDocumento++;
        esperaComplementarios.stop();
        esperaComplementarios.setOnFinished(null);
        if (consultaComplementarios != null) { consultaComplementarios.interrupt(); consultaComplementarios = null; }
        pendientes.clear();
        indiceSugerencia = 0;
        ocultarSugerencias();
        actualizarContador();
    }

    private void actualizarContexto() {
        btnVerificar.setDisable(documentoActivo == null);
        String modulo = documentoActivo == null ? "" : documentoActivo.contextoVendexi().modulo;
        animaciones.setTrabajando("factura".equals(modulo) || "proforma".equals(modulo));
        lblContexto.setText(documentoActivo == null ? "Repuestos y negocio, a tu lado" :
                "Documento: " + documentoActivo.contextoVendexi().modulo.replace('_', ' '));
    }

    public void consultarComplementarios(DocumentoVendexi origen, Inventario agregado,
                                         List<Inventario> inventario, String vehiculo) {
        if (origen != documentoActivo || origen.accionAgregarVendexi() == null || !servicioSugerencias.esIaDisponible()) return;
        long revision = revisionDocumento;
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (var linea : origen.contextoVendexi().lineas) ids.add(linea.inventarioId);
        List<Inventario> snapshot = List.copyOf(inventario);
        esperaComplementarios.setOnFinished(e -> {
            consultaComplementarios = new Thread(() -> {
                List<VendexiSugerenciaService.Sugerencia> sugerencias = servicioSugerencias
                        .sugerenciasComplementarias(agregado, snapshot, ids, vehiculo);
                Platform.runLater(() -> recibirSugerencias(origen, revision, sugerencias));
            }, "vetxi-complementos");
            consultaComplementarios.setDaemon(true);
            consultaComplementarios.start();
        });
        esperaComplementarios.playFromStart();
    }

    /** Solo publica resultados del formulario y versión que originaron la consulta. */
    void recibirSugerencias(DocumentoVendexi origen, long revision, List<VendexiSugerenciaService.Sugerencia> sugerencias) {
        if (origen != documentoActivo || revision != revisionDocumento || sugerencias == null) return;
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (var linea : origen.contextoVendexi().lineas) ids.add(linea.inventarioId);
        pendientes.clear();
        for (var s : sugerencias) {
            if (s == null) continue;
            if (s.producto != null && (origen.accionAgregarVendexi() == null
                    || s.producto.getCantidad() <= 0 || !ids.add(s.producto.getId()))) continue;
            pendientes.add(s);
        }
        indiceSugerencia = 0;
        actualizarContador();
        if (!pendientes.isEmpty()) {
            animaciones.avisar();
            // No desplazar un chat que el usuario está leyendo o escribiendo.
            if (!chatPanel.isVisible()) mostrarSugerencias();
        }
    }

    @FXML void verificarDocumento() {
        if (documentoActivo == null) return;
        documentoModificado(documentoActivo);
        var checks = servicioSugerencias.verificarDocumento(documentoActivo.contextoVendexi());
        if (checks.isEmpty()) checks = List.of(new VendexiSugerenciaService.Sugerencia(
                VendexiSugerenciaService.Tipo.VERIFICACION, "Todo listo por aquí",
                "El documento no tiene observaciones locales. No se utilizó IA para esta verificación.", null));
        recibirSugerencias(documentoActivo, revisionDocumento, checks);
        mostrarSugerencias();
    }

    private void actualizarContador() {
        contadorSugerencias.setText(Integer.toString(pendientes.size()));
        contadorSugerencias.setVisible(!pendientes.isEmpty());
        btnPendientes.setText("Sugerencias (" + pendientes.size() + ")");
        btnPendientes.setDisable(pendientes.isEmpty());
    }

    @FXML void mostrarSugerencias() {
        if (guia != null && guia.activa()) return;
        if (pendientes.isEmpty()) return;
        if (chatPanel.isVisible()) onToggleChat();
        var sugerencia = pendientes.get(indiceSugerencia);
        lblSugerenciaTitulo.setText(sugerencia.titulo);
        lblSugerenciaNumero.setText((indiceSugerencia + 1) + " / " + pendientes.size());
        Inventario producto = sugerencia.producto;
        lblSugerenciaResumen.setText(producto == null ? "Verificación local · " + documentoActivo.contextoVendexi().modulo.replace('_', ' ')
                : producto.getCodigo() + " · Stock: " + producto.getCantidad() + " · "
                + (producto.getPrecioVenta() == null ? "precio n/d" : String.format(java.util.Locale.US, "$%.2f", producto.getPrecioVenta())));
        String evidencia = switch (sugerencia.evidencia) {
            case VendexiSugerenciaService.EVIDENCIA_CONFIRMADA -> "Compatibilidad confirmada";
            case VendexiSugerenciaService.EVIDENCIA_PROBABLE -> "Coincidencia probable: verificar compatibilidad";
            default -> "Asociación de ventas: compatibilidad sin confirmar";
        };
        lblSugerenciaDetalle.setText(sugerencia.motivo + (producto != null ? "\n\n" + evidencia : ""));
        lblSugerenciaDetalle.setVisible(false); lblSugerenciaDetalle.setManaged(false);
        btnDetalles.setText("Ver detalles");
        btnAgregarSugerencia.setVisible(producto != null); btnAgregarSugerencia.setManaged(producto != null);
        sugerenciaBubble.setVisible(true);
        posicionarAsistente();
        ocultarBurbuja.playFromStart();
    }

    @FXML private void detallesSugerencia() {
        boolean mostrar = !lblSugerenciaDetalle.isVisible();
        lblSugerenciaDetalle.setVisible(mostrar); lblSugerenciaDetalle.setManaged(mostrar);
        btnDetalles.setText(mostrar ? "Menos detalles" : "Ver detalles");
        ocultarBurbuja.stop();
        if (!mostrar) ocultarBurbuja.playFromStart();
        posicionarAsistente();
    }

    @FXML void ocultarSugerencias() {
        ocultarBurbuja.stop();
        sugerenciaBubble.setVisible(false);
    }

    @FXML private void siguienteSugerencia() {
        if (!pendientes.isEmpty()) { indiceSugerencia = (indiceSugerencia + 1) % pendientes.size(); mostrarSugerencias(); }
    }

    @FXML void descartarSugerencia() {
        if (pendientes.isEmpty()) return;
        pendientes.remove(indiceSugerencia);
        indiceSugerencia = 0;
        actualizarContador();
        if (pendientes.isEmpty()) ocultarSugerencias(); else mostrarSugerencias();
    }

    @FXML private void copiarSugerencia() {
        if (pendientes.isEmpty()) return;
        var sugerencia = pendientes.get(indiceSugerencia);
        var contenido = new javafx.scene.input.ClipboardContent();
        contenido.putString(sugerencia.producto != null ? sugerencia.producto.getCodigo()
                : sugerencia.titulo + "\n" + sugerencia.motivo);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(contenido);
    }

    @FXML void agregarSugerencia() {
        if (documentoActivo == null || pendientes.isEmpty()) return;
        var producto = pendientes.get(indiceSugerencia).producto;
        var agregar = documentoActivo.accionAgregarVendexi();
        if (producto == null || agregar == null || producto.getCantidad() <= 0) return;
        if (documentoActivo.contextoVendexi().lineas.stream().anyMatch(l -> l.inventarioId == producto.getId())) {
            descartarSugerencia(); return;
        }
        documentoModificado(documentoActivo);
        agregar.accept(producto);
    }

    @FXML
    public void onToggleChat() {
        animaciones.registrarActividad();
        boolean mostrar = !chatPanel.isVisible();
        if (mostrar) ocultarSugerencias();
        chatPanel.setVisible(mostrar);
        posicionarAsistente();
        actualizarBadge();
        // Fase 3: registro de tiempo de interacción (estadística, no es límite — spec §12)
        if (mostrar) {
            sesionVendexi = medicionChat.iniciarSesion("chat", "CONSULTA");
        } else {
            finalizarSesionVendexi();
        }
        if (mostrar) {
            cerrado = false;
            sincronizarIdentidad();
            if (!historialCargado && !cargandoHistorial) cargarHistorial();
            else if (mensajesBox.getChildren().isEmpty()) saludar();
            inputField.requestFocus();
        }
    }

    private void saludar() {
        agregarMensaje("¡Hola! Soy Vetxi, el agente de Vendex. Estoy aquí para ayudarte con tu negocio.\n"
                    + "• Repuestos: \"tapa radiador CHEV AVEO\" o síntoma \"ruido al frenar\"\n"
                    + "• Stock/precio: \"¿cuánto stock tiene el filtro aceite?\"\n"
                    + "• Clientes: \"¿cuánto me compró Juan?\" / \"¿cuánto me debe Juan?\"\n"
                    + "• Negocio: \"¿cuáles son mis productos más rentables?\"\n"
                    + "Puedes recuperar tus conversaciones o iniciar una nueva desde el historial.", false, false);
    }

    private void sincronizarIdentidad() {
        Ambito actual = historial.ambitoActual();
        if (java.util.Objects.equals(actual, ambitoChat)) return;
        ambitoChat = actual;
        versionChat++;
        conversacionChat = java.util.UUID.randomUUID().toString();
        tituloChat = "";
        historialCargado = false; cargandoHistorial = false; consultaEnCurso = false;
        falloPersistencia = false;
        mensajesBox.getChildren().clear(); inputField.clear();
        actualizarListaConversaciones(List.of(), null);
        actualizarControlesChat();
    }

    private TurnoChat turnoActual() { return new TurnoChat(ambitoChat, conversacionChat, tituloChat, versionChat); }
    private boolean vigente(TurnoChat turno) {
        return !cerrado && turno.version() == versionChat && turno.conversacion().equals(conversacionChat)
                && java.util.Objects.equals(turno.ambito(), ambitoChat)
                && java.util.Objects.equals(turno.ambito(), historial.ambitoActual());
    }

    private void ejecutarHistorial(Runnable accion) {
        if (hiloHistorial == null || hiloHistorial.isShutdown()) {
            hiloHistorial = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread hilo = new Thread(r, "vetxi-historial"); hilo.setDaemon(true); return hilo;
            });
        }
        hiloHistorial.execute(accion);
    }

    private void actualizarControlesChat() {
        boolean ocupado = cargandoHistorial || consultaEnCurso;
        inputField.setDisable(ocupado);
        btnNuevaConversacion.setDisable(ocupado);
        cmbConversaciones.setDisable(ocupado || ambitoChat == null);
    }

    private void actualizarListaConversaciones(List<Conversacion> lista, String seleccionar) {
        actualizandoCombo = true;
        try {
            cmbConversaciones.getItems().setAll(lista);
            cmbConversaciones.setValue(lista.stream().filter(c -> c.id().equals(seleccionar)).findFirst().orElse(null));
        } finally { actualizandoCombo = false; }
    }

    private void cargarHistorial() {
        if (ambitoChat == null) {
            historialCargado = true;
            lblHistorial.setText("Sin sesión: conversación no persistente");
            if (mensajesBox.getChildren().isEmpty()) saludar();
            actualizarControlesChat(); return;
        }
        TurnoChat turno = turnoActual();
        cargandoHistorial = true; actualizarControlesChat();
        lblHistorial.setText("Recuperando tus conversaciones…");
        ejecutarHistorial(() -> {
            try {
                var lista = historial.conversaciones(turno.ambito());
                Conversacion reciente = lista.isEmpty() ? null : lista.get(0);
                var mensajes = reciente == null ? List.<Mensaje>of() : historial.mensajes(turno.ambito(), reciente.id());
                Platform.runLater(() -> {
                    if (!vigente(turno)) return;
                    if (reciente != null) { conversacionChat = reciente.id(); tituloChat = reciente.titulo(); }
                    actualizarListaConversaciones(lista, reciente == null ? null : reciente.id());
                    restaurarMensajes(mensajes);
                });
            } catch (Exception e) { falloHistorial(turno, "No se pudo recuperar el historial. Puedes seguir conversando.", e); }
        });
    }

    private void restaurarMensajes(List<Mensaje> mensajes) {
        mensajesBox.getChildren().clear();
        for (Mensaje m : mensajes) {
            if (m.usuario()) agregarMensaje(m.texto(), true, false);
            else if (!m.fuentes().isEmpty()) agregarMensajeConFuentes(m.texto(), m.fuentes().stream()
                    .map(f -> new com.vendex.service.InvestigacionWebService.Fuente(f.titulo(), f.uri())).toList(), m.sinIA(),
                    etiquetaRol(m.rolIA(), m.rolesBloqueados()), !m.rolesBloqueados().isEmpty());
            else agregarRespuesta(new GeminiChatbotService.RespuestaChat(m.texto(), GeminiChatbotService.EstadoIA.IA_ACTIVA,
                    m.sinIA(), m.rolIA(), m.rolesBloqueados()));
        }
        if (mensajes.isEmpty()) saludar();
        historialCargado = true; cargandoHistorial = false;
        falloPersistencia = false;
        lblHistorial.setText(mensajes.isEmpty() ? "Nueva conversación" :
                "Conversación recuperada · " + mensajes.size() + " mensajes" + (mensajes.size() == 100 ? " recientes" : ""));
        actualizarControlesChat();
    }

    @FXML void nuevaConversacion() {
        if (consultaEnCurso || cargandoHistorial) return;
        sincronizarIdentidad();
        versionChat++; conversacionChat = java.util.UUID.randomUUID().toString(); tituloChat = "";
        falloPersistencia = false;
        historialCargado = true;
        actualizarListaConversaciones(new ArrayList<>(cmbConversaciones.getItems()), null);
        mensajesBox.getChildren().clear(); saludar();
        lblHistorial.setText(ambitoChat == null ? "Sin sesión: conversación no persistente" : "Nueva conversación");
        inputField.requestFocus();
    }

    @FXML void seleccionarConversacion() {
        if (actualizandoCombo || cargandoHistorial || consultaEnCurso || cmbConversaciones.getValue() == null) return;
        if (!java.util.Objects.equals(ambitoChat, historial.ambitoActual())) {
            sincronizarIdentidad(); cargarHistorial(); return;
        }
        Conversacion seleccionada = cmbConversaciones.getValue();
        versionChat++; conversacionChat = seleccionada.id(); tituloChat = seleccionada.titulo();
        TurnoChat turno = turnoActual();
        cargandoHistorial = true; actualizarControlesChat();
        mensajesBox.getChildren().clear(); lblHistorial.setText("Recuperando conversación…");
        ejecutarHistorial(() -> {
            try {
                var mensajes = historial.mensajes(turno.ambito(), turno.conversacion());
                Platform.runLater(() -> { if (vigente(turno)) restaurarMensajes(mensajes); });
            } catch (Exception e) { falloHistorial(turno, "No se pudo recuperar la conversación. Puedes seguir conversando.", e); }
        });
    }

    private void falloHistorial(TurnoChat turno, String mensaje, Exception error) {
        falloHistorial(turno, mensaje, error, false);
    }

    private void falloHistorial(TurnoChat turno, String mensaje, Exception error, boolean guardando) {
        if (error instanceof InterruptedException) Thread.currentThread().interrupt();
        java.util.logging.Logger.getLogger(ChatWidgetController.class.getName()).log(error instanceof SecurityException
                        ? java.util.logging.Level.FINE : java.util.logging.Level.WARNING,
                "Vetxi: historial no disponible (" + error.getClass().getSimpleName() + ")", error);
        Platform.runLater(() -> {
            if (!vigente(turno)) return;
            cargandoHistorial = false; historialCargado = true;
            if (guardando) falloPersistencia = true;
            lblHistorial.setText(mensaje); actualizarControlesChat();
            if (mensajesBox.getChildren().isEmpty()) saludar();
        });
    }

    private void guardarMensaje(TurnoChat turno, boolean usuario, String texto, boolean sinIA, String rol,
                                List<String> bloqueados, List<Fuente> fuentes) {
        if (turno.ambito() == null) return;
        Mensaje mensaje;
        try {
            mensaje = new Mensaje(java.util.UUID.randomUUID().toString(), turno.conversacion(), turno.titulo(),
                    usuario, texto, sinIA, rol, bloqueados, fuentes, null);
        } catch (IllegalArgumentException e) { falloHistorial(turno, "El mensaje no pudo guardarse en el historial.", e, true); return; }
        ejecutarHistorial(() -> {
            try {
                historial.guardar(turno.ambito(), mensaje);
                Platform.runLater(() -> {
                    if (!vigente(turno)) return;
                    lblHistorial.setText(falloPersistencia ? "Hay mensajes sin guardar; siguen visibles en este chat."
                            : "Historial guardado · usuario y sucursal actuales");
                    var lista = new ArrayList<>(cmbConversaciones.getItems());
                    lista.removeIf(c -> c.id().equals(turno.conversacion()));
                    lista.add(0, new Conversacion(turno.conversacion(), turno.titulo(), java.time.LocalDateTime.now().toString()));
                    if (lista.size() > 20) lista.subList(20, lista.size()).clear();
                    actualizarListaConversaciones(lista, turno.conversacion());
                });
            } catch (Exception e) { falloHistorial(turno, "No se pudo guardar el historial. La conversación sigue disponible aquí.", e, true); }
        });
    }

    private void finalizarSesionVendexi() {
        if (sesionVendexi > 0) {
            medicionChat.finalizarSesion(sesionVendexi);
            sesionVendexi = -1;
        }
    }

    public void abrir() {
        if (!chatPanel.isVisible()) onToggleChat();
    }

    @FXML
    public void onEnviar() {
        if (cargandoHistorial || consultaEnCurso) return;
        if (!java.util.Objects.equals(ambitoChat, historial.ambitoActual())) {
            sincronizarIdentidad(); cargarHistorial(); return;
        }
        String texto = inputField.getText();
        if (texto == null || texto.isBlank()) return;
        if (texto.length() > 30000) { lblHistorial.setText("La consulta es demasiado larga."); return; }

        String vehMarca = filtroMarca();
        String vehModelo = filtroModelo();
        Integer vehAnio = filtroAnio();
        String textoMostrar = texto;
        if (vehMarca != null || vehModelo != null || vehAnio != null) {
            StringBuilder sb = new StringBuilder(texto);
            sb.append(" [").append(vehMarca != null ? vehMarca : "").append(vehModelo != null ? " " + vehModelo : "").append(vehAnio != null ? " " + vehAnio : "").append("]");
            textoMostrar = sb.toString();
        }
        agregarMensaje(textoMostrar, true, false);
        if (tituloChat.isBlank()) {
            String titulo = texto.trim().replaceAll("\\s+", " ");
            tituloChat = titulo.substring(0, Math.min(100, titulo.length()));
        }
        TurnoChat turno = turnoActual();
        guardarMensaje(turno, true, textoMostrar, false, null, List.of(), List.of());
        inputField.clear();
        consultaEnCurso = true; actualizarControlesChat();
        badgeIA.setText("⏳ consultando...");

        // Investigación web solo por petición explícita; los códigos usan inventario local.
        if (com.vendex.service.InvestigacionWebService.esConsultaInvestigacion(texto)
                && com.vendex.util.LicenseManager.licenciaIncluyeIA()
                && GeminiConfig.tieneApiKey()) {
            investigarEnWeb(texto, vehMarca, vehModelo, vehAnio, turno);
            return;
        }

        Thread hilo = new Thread(() -> {
            GeminiChatbotService.RespuestaChat rc;
            try {
                rc = chatbotService.preguntarConEstado(texto, vehMarca, vehModelo, vehAnio);
            } catch (Exception e) {
                rc = new GeminiChatbotService.RespuestaChat("Ocurrió un error. Intenta de nuevo.", GeminiChatbotService.EstadoIA.SIN_IA_CONEXION, true);
            }
            GeminiChatbotService.RespuestaChat finalRc = rc;
            Platform.runLater(() -> {
                if (!vigente(turno)) return;
                agregarRespuesta(finalRc);
                guardarMensaje(turno, false, finalRc.texto, finalRc.esSinIA, finalRc.rolIA, finalRc.rolesBloqueados, List.of());
                actualizarBadge();
                consultaEnCurso = false; actualizarControlesChat();
                if (chatPanel.isVisible()) inputField.requestFocus();
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Fase 4: investigación web de repuestos con fuentes verificables (spec §8). */
    private void investigarEnWeb(String texto, String vehMarca, String vehModelo, Integer vehAnio, TurnoChat turno) {
        badgeIA.setText("⏳ investigando web...");
        Thread hilo = new Thread(() -> {
            String resumenLocal = "";
            try {
                GeminiChatbotService.ResultadoBusqueda r =
                        repuestoChatbotDAO.buscar(texto.trim(), vehMarca, vehModelo, vehAnio);
                if (r != null && r.isEncontrado()) {
                    resumenLocal = r.getCodigo() + " | " + r.getNombre() + " | stock " + r.getStock();
                }
            } catch (Exception ignored) {}
            com.vendex.service.InvestigacionWebService.Resultado resultado;
            try {
                resultado = investigacionService.investigar(texto, resumenLocal, vehMarca, vehModelo, vehAnio);
            } catch (Exception e) {
                resultado = new com.vendex.service.InvestigacionWebService.Resultado(
                        com.vendex.service.InvestigacionWebService.Estado.ERROR,
                        "No se pudo completar la investigación web. Intenta nuevamente.", List.of(), true);
            }
            final var res = resultado;
            Platform.runLater(() -> {
                if (!vigente(turno)) return;
                boolean sinIa = res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_LICENCIA
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_CAPACIDAD
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_CUOTA
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_API_KEY
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_CONEXION
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_BUSQUEDA
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.ERROR;
                String encabezado = res.estado == com.vendex.service.InvestigacionWebService.Estado.OK
                        ? "🔍 Investigación web de \"" + texto + "\"\n\n"
                        : "";
                agregarMensajeConFuentes(encabezado + res.texto, res.fuentes, sinIa);
                guardarMensaje(turno, false, encabezado + res.texto, sinIa, null, List.of(), res.fuentes.stream()
                        .map(f -> new Fuente(f.titulo, f.uri)).toList());
                actualizarBadge();
                consultaEnCurso = false; actualizarControlesChat();
                if (chatPanel.isVisible()) inputField.requestFocus();
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private void agregarMensajeConFuentes(String texto,
                                          java.util.List<com.vendex.service.InvestigacionWebService.Fuente> fuentes,
                                          boolean esSinIA) {
        agregarMensajeConFuentes(texto, fuentes, esSinIA, null, false);
    }

    private void agregarMensajeConFuentes(String texto,
                                          java.util.List<com.vendex.service.InvestigacionWebService.Fuente> fuentes,
                                          boolean esSinIA, String etiqueta, boolean bloqueada) {
        javafx.scene.layout.VBox cuerpo = new javafx.scene.layout.VBox(3);
        agregarEtiquetaRol(cuerpo, etiqueta, bloqueada);
        Label label = new Label(texto);
        label.setWrapText(true);
        label.getStyleClass().add(esSinIA ? "mensaje-bot-sin-ia" : "mensaje-bot");
        cuerpo.getChildren().add(label);
        if (fuentes != null && !fuentes.isEmpty()) {
            Label lblFuentes = new Label("Fuentes verificables:");
            lblFuentes.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            cuerpo.getChildren().add(lblFuentes);
            for (com.vendex.service.InvestigacionWebService.Fuente f : fuentes) {
                javafx.scene.control.Hyperlink link = new javafx.scene.control.Hyperlink(
                        (f.titulo != null && !f.titulo.isBlank()) ? f.titulo : f.uri);
                link.setWrapText(true);
                link.setStyle("-fx-font-size: 11px; -fx-padding: 0 4 0 8;");
                if (enlaceWebValido(f.uri)) {
                    final String uri = f.uri;
                    link.setOnAction(e -> abrirEnlace(uri));
                    link.setTooltip(new javafx.scene.control.Tooltip(uri));
                } else {
                    link.setDisable(true);
                }
                cuerpo.getChildren().add(link);
            }
        }
        HBox contenedor = new HBox(cuerpo);
        contenedor.setFillHeight(false);
        HBox.setHgrow(cuerpo, Priority.NEVER);
        contenedor.setStyle("-fx-alignment: CENTER_LEFT;");
        mensajesBox.getChildren().add(contenedor);
        Platform.runLater(() -> {
            scrollPane.layout();
            scrollPane.setVvalue(1.0);
        });
    }

    private void abrirEnlace(String uri) {
        // Fuera del hilo JavaFX: Desktop.browse puede bloquearse y congelar el chat.
        com.vendex.util.Navegador.abrir(uri, mensaje -> {
            boolean fallo = mensaje != null && (mensaje.startsWith("No se pudo")
                    || mensaje.startsWith("El enlace no es válido"));
            if (fallo) {
                Platform.runLater(() -> {
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING,
                            mensaje + "\n" + uri);
                    a.showAndWait();
                });
            }
        });
    }

    private boolean enlaceWebValido(String uri) {
        if (uri == null) return false;
        try {
            var enlace = java.net.URI.create(uri);
            return ("http".equalsIgnoreCase(enlace.getScheme()) || "https".equalsIgnoreCase(enlace.getScheme()))
                    && enlace.getHost() != null;
        } catch (IllegalArgumentException e) { return false; }
    }

    private void actualizarBadge() {
        if (badgeIA == null) return;
        // Fase 1: licencia sin Vendexi AI — solo modo local (función ERP permitida)
        if (!com.vendex.util.LicenseManager.licenciaIncluyeIA()) {
            badgeIA.setText("○ Local (licencia)");
            badgeIA.getStyleClass().setAll("badge-local");
            badgeIA.setTooltip(new javafx.scene.control.Tooltip(
                    "La licencia de esta instalación no incluye Vetxi AI. La búsqueda local en inventario sigue disponible."));
            return;
        }
        GeminiChatbotService.EstadoIA e = chatbotService.getEstado();
        // Si no hay archivo ni env, mostrar Local
        boolean tieneKey = GeminiConfig.tieneApiKey();
        if (!tieneKey) {
            badgeIA.setText("○ Local");
            badgeIA.getStyleClass().setAll("badge-local");
            badgeIA.setTooltip(new javafx.scene.control.Tooltip("Sin IA: configura API key en Configurar IA (menú lateral)"));
            return;
        }
        switch (e) {
            case IA_ACTIVA -> {
                badgeIA.setText("● IA Gemini");
                badgeIA.getStyleClass().setAll("badge-ia");
                badgeIA.setTooltip(new javafx.scene.control.Tooltip("IA activa"));
            }
            case SIN_IA_TOKENS -> {
                badgeIA.setText("⚠️ sin IA — tokens");
                badgeIA.getStyleClass().setAll("badge-sin-ia");
                badgeIA.setTooltip(new javafx.scene.control.Tooltip(chatbotService.getUltimoError()));
            }
            case SIN_IA_SERVIDOR -> {
                badgeIA.setText("⚠️ sin IA — servidor");
                badgeIA.getStyleClass().setAll("badge-sin-ia");
                badgeIA.setTooltip(new javafx.scene.control.Tooltip(chatbotService.getUltimoError()));
            }
            case SIN_IA_CONEXION -> {
                badgeIA.setText("⚠️ sin IA — conexión");
                badgeIA.getStyleClass().setAll("badge-sin-ia");
                badgeIA.setTooltip(new javafx.scene.control.Tooltip(chatbotService.getUltimoError()));
            }
            case SIN_IA_KEY_INVALIDA -> {
                badgeIA.setText("⚠️ sin IA — key inválida");
                badgeIA.getStyleClass().setAll("badge-sin-ia");
                badgeIA.setTooltip(new javafx.scene.control.Tooltip(chatbotService.getUltimoError()));
            }
            default -> {
                badgeIA.setText("○ Local");
                badgeIA.getStyleClass().setAll("badge-local");
            }
        }
    }

    private void agregarMensaje(String texto, boolean esUsuario, boolean esSinIA) {
        agregarMensaje(texto, esUsuario, esSinIA, null, false);
    }

    /** La etiqueta procede de la autorización local, nunca del texto generado por la IA. */
    void agregarRespuesta(GeminiChatbotService.RespuestaChat respuesta) {
        boolean bloqueada = !respuesta.rolesBloqueados.isEmpty();
        agregarMensaje(respuesta.texto, false, respuesta.esSinIA, etiquetaRol(respuesta.rolIA, respuesta.rolesBloqueados), bloqueada);
    }

    private String etiquetaRol(String rol, List<String> bloqueados) {
        return !bloqueados.isEmpty() ? "🔒 Licencia: requiere " + String.join(" o ", bloqueados)
                : (rol != null ? "Rol IA: " + rol : null);
    }

    private void agregarEtiquetaRol(VBox cuerpo, String etiqueta, boolean bloqueada) {
        if (etiqueta == null) return;
        Label rol = new Label(etiqueta);
        rol.setWrapText(true);
        rol.getStyleClass().add(bloqueada ? "mensaje-rol-bloqueado" : "mensaje-rol-ia");
        cuerpo.getChildren().add(rol);
    }

    private void agregarMensaje(String texto, boolean esUsuario, boolean esSinIA,
                                String etiqueta, boolean bloqueada) {
        Label label = new Label(texto);
        label.setWrapText(true);
        if (esUsuario) label.getStyleClass().add("mensaje-usuario");
        else if (esSinIA) label.getStyleClass().add("mensaje-bot-sin-ia");
        else label.getStyleClass().add("mensaje-bot");

        VBox cuerpo = new VBox(3);
        agregarEtiquetaRol(cuerpo, etiqueta, bloqueada);
        cuerpo.getChildren().add(label);
        HBox contenedor = new HBox(cuerpo);
        contenedor.setFillHeight(false);
        HBox.setHgrow(cuerpo, Priority.NEVER);
        contenedor.setStyle(esUsuario ? "-fx-alignment: CENTER_RIGHT;" : "-fx-alignment: CENTER_LEFT;");

        mensajesBox.getChildren().add(contenedor);
        Platform.runLater(() -> {
            scrollPane.layout();
            scrollPane.setVvalue(1.0);
        });
    }
}

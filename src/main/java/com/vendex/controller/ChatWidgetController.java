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
        this.chatbotService = new com.vendex.chatbot.GeminiChatbotService(ctx.repuestoChatbotDAO, ctx.herramientasAgente);
        this.vehiculoDAO = ctx.vehiculoDAO;
        this.repuestoChatbotDAO = ctx.repuestoChatbotDAO;
        this.investigacionService = new com.vendex.service.InvestigacionWebService(ctx.vendexiConsumoService);
        this.servicioSugerencias = sugerencias;
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
    @FXML private javafx.scene.control.ComboBox<String> cmbMarca;
    @FXML private javafx.scene.control.ComboBox<String> cmbModelo;
    @FXML private javafx.scene.control.ComboBox<Integer> cmbAnio;

    private final GeminiChatbotService chatbotService;
    private final com.vendex.dao.VehiculoDAO vehiculoDAO;
    private final com.vendex.dao.RepuestoChatbotDAO repuestoChatbotDAO;
    private final com.vendex.service.InvestigacionWebService investigacionService;
    private final VendexiSugerenciaService servicioSugerencias;
    private DocumentoVendexi documentoActivo;
    private long revisionDocumento;
    private final List<VendexiSugerenciaService.Sugerencia> pendientes = new ArrayList<>();
    private int indiceSugerencia;
    private Thread consultaComplementarios;
    private final javafx.animation.PauseTransition esperaComplementarios = new javafx.animation.PauseTransition(Duration.millis(650));
    private final javafx.animation.PauseTransition ocultarBurbuja = new javafx.animation.PauseTransition(Duration.seconds(12));
    private VetxiAnimaciones animaciones;
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

    private void configurarMascota() {
        posicionX = limitar(preferencias.getDouble("x", 0.97), 0, 1);
        posicionY = limitar(preferencias.getDouble("y", 0.90), 0, 1);
        animaciones = new VetxiAnimaciones(capibara, mascota, rootStack,
                () -> !arrastrando && !bubbleButton.isHover() && !chatPanel.isVisible()
                        && !sugerenciaBubble.isVisible() && pendientes.isEmpty() && !inputField.isDisabled(),
                this::moverMascota);
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
        animaciones.setActivo(visible);
    }

    private void liberar() {
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
        if (!pendientes.isEmpty()) mostrarSugerencias(); else onToggleChat();
    }

    public void setDocumentoActivo(DocumentoVendexi documento) {
        if (documentoActivo != null) documentoActivo.conectarVendexi(null);
        documentoActivo = documento;
        documentoModificado(documento);
        if (documento != null) documento.conectarVendexi(this);
        actualizarContexto();
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
            sesionVendexi = com.vendex.config.AppContext.getInstance().vendexiConsumoService
                    .iniciarSesion("chat", "CONSULTA");
        } else {
            finalizarSesionVendexi();
        }
        if (mostrar && mensajesBox.getChildren().isEmpty()) {
            agregarMensaje("¡Hola! Soy Vetxi, tu capibara de repuestos y negocio.\n"
                    + "• Repuestos: \"tapa radiador CHEV AVEO\" o síntoma \"ruido al frenar\"\n"
                    + "• Stock/precio: \"¿cuánto stock tiene el filtro aceite?\"\n"
                    + "• Clientes: \"¿cuánto me compró Juan?\" / \"¿cuánto me debe Juan?\"\n"
                    + "• Negocio: \"¿cuáles son mis productos más rentables?\"\n"
                    + "Filtros Marca/Modelo/Año afinan la búsqueda por vehículo.", false, false);
            inputField.requestFocus();
        } else if (mostrar) {
            inputField.requestFocus();
        }
    }

    private void finalizarSesionVendexi() {
        if (sesionVendexi > 0) {
            com.vendex.config.AppContext.getInstance().vendexiConsumoService.finalizarSesion(sesionVendexi);
            sesionVendexi = -1;
        }
    }

    public void abrir() {
        if (!chatPanel.isVisible()) onToggleChat();
    }

    @FXML
    public void onEnviar() {
        String texto = inputField.getText();
        if (texto == null || texto.isBlank()) return;

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
        inputField.clear();
        inputField.setDisable(true);
        badgeIA.setText("⏳ consultando...");

        // Fase 4: investigación web (§8) — códigos/números de parte o petición explícita
        if (com.vendex.service.InvestigacionWebService.esConsultaInvestigacion(texto)
                && com.vendex.util.LicenseManager.licenciaIncluyeIA()
                && GeminiConfig.tieneApiKey()) {
            investigarEnWeb(texto, vehMarca, vehModelo, vehAnio);
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
                agregarRespuesta(finalRc);
                actualizarBadge();
                inputField.setDisable(false);
                inputField.requestFocus();
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Fase 4: investigación web de repuestos con fuentes verificables (spec §8). */
    private void investigarEnWeb(String texto, String vehMarca, String vehModelo, Integer vehAnio) {
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
            com.vendex.service.InvestigacionWebService.Resultado res =
                    investigacionService.investigar(texto, resumenLocal, vehMarca, vehModelo, vehAnio);
            Platform.runLater(() -> {
                boolean sinIa = res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_LICENCIA
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_CAPACIDAD
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_API_KEY
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_CONEXION
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.SIN_BUSQUEDA
                        || res.estado == com.vendex.service.InvestigacionWebService.Estado.ERROR;
                String encabezado = res.estado == com.vendex.service.InvestigacionWebService.Estado.OK
                        ? "🔍 Investigación web de \"" + texto + "\"\n\n"
                        : "";
                agregarMensajeConFuentes(encabezado + res.texto, res.fuentes, sinIa);
                actualizarBadge();
                inputField.setDisable(false);
                inputField.requestFocus();
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private void agregarMensajeConFuentes(String texto,
                                          java.util.List<com.vendex.service.InvestigacionWebService.Fuente> fuentes,
                                          boolean esSinIA) {
        javafx.scene.layout.VBox cuerpo = new javafx.scene.layout.VBox(3);
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
                if (f.tieneUri()) {
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
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(uri));
            }
        } catch (Exception e) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "No se pudo abrir el enlace:\n" + uri);
            a.showAndWait();
        }
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
        String etiqueta = bloqueada
                ? "🔒 Licencia: requiere " + String.join(" o ", respuesta.rolesBloqueados)
                : (respuesta.rolIA != null ? "Rol IA: " + respuesta.rolIA : null);
        agregarMensaje(respuesta.texto, false, respuesta.esSinIA, etiqueta, bloqueada);
    }

    private void agregarMensaje(String texto, boolean esUsuario, boolean esSinIA,
                                String etiqueta, boolean bloqueada) {
        Label label = new Label(texto);
        label.setWrapText(true);
        if (esUsuario) label.getStyleClass().add("mensaje-usuario");
        else if (esSinIA) label.getStyleClass().add("mensaje-bot-sin-ia");
        else label.getStyleClass().add("mensaje-bot");

        VBox cuerpo = new VBox(3);
        if (etiqueta != null) {
            Label rol = new Label(etiqueta);
            rol.setWrapText(true);
            rol.getStyleClass().add(bloqueada ? "mensaje-rol-bloqueado" : "mensaje-rol-ia");
            cuerpo.getChildren().add(rol);
        }
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

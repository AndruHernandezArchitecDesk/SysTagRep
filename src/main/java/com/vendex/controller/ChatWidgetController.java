package com.vendex.controller;

import com.vendex.chatbot.GeminiChatbotService;
import com.vendex.config.GeminiConfig;
import com.vendex.dao.RepuestoChatbotDAO;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
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
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import com.vendex.dao.RepuestoChatbotDAOPostgres;

public class ChatWidgetController {

    public ChatWidgetController() {
        this(com.vendex.config.AppContext.getInstance());
    }

    public ChatWidgetController(com.vendex.config.AppContext ctx) {
        this.chatbotService = new com.vendex.chatbot.GeminiChatbotService(ctx.repuestoChatbotDAO, ctx.herramientasAgente);
        this.vehiculoDAO = ctx.vehiculoDAO;
        this.repuestoChatbotDAO = ctx.repuestoChatbotDAO;
        this.investigacionService = new com.vendex.service.InvestigacionWebService(ctx.vendexiConsumoService);
    }

    @FXML private StackPane rootStack;
    @FXML private VBox chatPanel;
    @FXML private VBox mensajesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button bubbleButton;
    @FXML private Label badgeIA;
    @FXML private Circle pulseCircle;
    @FXML private javafx.scene.control.ComboBox<String> cmbMarca;
    @FXML private javafx.scene.control.ComboBox<String> cmbModelo;
    @FXML private javafx.scene.control.ComboBox<Integer> cmbAnio;

    private final GeminiChatbotService chatbotService;
    private final com.vendex.dao.VehiculoDAO vehiculoDAO;
    private final com.vendex.dao.RepuestoChatbotDAO repuestoChatbotDAO;
    private final com.vendex.service.InvestigacionWebService investigacionService;
    private static final String TODAS = "Todas";
    private static final String TODOS = "Todos";
    private long sesionVendexi = -1;

    @FXML
    public void initialize() {
        actualizarBadge();
        iniciarPulso();
        iniciarFiltrosVehiculo();
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

    private void iniciarPulso() {
        if (pulseCircle == null) return;
        ScaleTransition scale = new ScaleTransition(Duration.seconds(1.4), pulseCircle);
        scale.setFromX(0.9); scale.setToX(1.25);
        scale.setFromY(0.9); scale.setToY(1.25);
        scale.setAutoReverse(true); scale.setCycleCount(Animation.INDEFINITE);
        FadeTransition fade = new FadeTransition(Duration.seconds(1.4), pulseCircle);
        fade.setFromValue(0.45); fade.setToValue(0.0);
        fade.setAutoReverse(true); fade.setCycleCount(Animation.INDEFINITE);
        scale.play(); fade.play();
    }

    @FXML
    public void onToggleChat() {
        boolean mostrar = !chatPanel.isVisible();
        chatPanel.setVisible(mostrar);
        chatPanel.setManaged(mostrar);
        actualizarBadge();
        // Fase 3: registro de tiempo de interacción (estadística, no es límite — spec §12)
        if (mostrar) {
            sesionVendexi = com.vendex.config.AppContext.getInstance().vendexiConsumoService
                    .iniciarSesion("chat", "CONSULTA");
        } else {
            finalizarSesionVendexi();
        }
        if (mostrar && mensajesBox.getChildren().isEmpty()) {
            agregarMensaje("¡Hola! Soy Vendexi, tu asistente de repuestos y negocio.\n"
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
                agregarMensaje(finalRc.texto, false, finalRc.esSinIA);
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
                    "La licencia de esta instalación no incluye Vendexi AI. La búsqueda local en inventario sigue disponible."));
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
        Label label = new Label(texto);
        label.setWrapText(true);
        if (esUsuario) label.getStyleClass().add("mensaje-usuario");
        else if (esSinIA) label.getStyleClass().add("mensaje-bot-sin-ia");
        else label.getStyleClass().add("mensaje-bot");

        HBox contenedor = new HBox(label);
        contenedor.setFillHeight(false);
        HBox.setHgrow(label, Priority.NEVER);
        contenedor.setStyle(esUsuario ? "-fx-alignment: CENTER_RIGHT;" : "-fx-alignment: CENTER_LEFT;");

        mensajesBox.getChildren().add(contenedor);
        Platform.runLater(() -> {
            scrollPane.layout();
            scrollPane.setVvalue(1.0);
        });
    }
}
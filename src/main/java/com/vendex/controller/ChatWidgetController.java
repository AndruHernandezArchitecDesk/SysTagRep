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

    @FXML private StackPane rootStack;
    @FXML private VBox chatPanel;
    @FXML private VBox mensajesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private Button bubbleButton;
    @FXML private Label badgeIA;
    @FXML private Circle pulseCircle;

    private final GeminiChatbotService chatbotService = new GeminiChatbotService(new RepuestoChatbotDAOPostgres());

    @FXML
    public void initialize() {
        actualizarBadge();
        iniciarPulso();
    }

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
        if (mostrar && mensajesBox.getChildren().isEmpty()) {
            agregarMensaje("¡Hola! Pregúntame por cualquier repuesto y verifico stock.\nEj: \"tapa radiador CHEV AVEO\"\nSíntoma: \"ruido al frenar\" → te sugiero repuesto", false, false);
            inputField.requestFocus();
        } else if (mostrar) {
            inputField.requestFocus();
        }
    }

    public void abrir() {
        if (!chatPanel.isVisible()) onToggleChat();
    }

    @FXML
    public void onEnviar() {
        String texto = inputField.getText();
        if (texto == null || texto.isBlank()) return;

        agregarMensaje(texto, true, false);
        inputField.clear();
        inputField.setDisable(true);
        badgeIA.setText("⏳ consultando...");

        Thread hilo = new Thread(() -> {
            GeminiChatbotService.RespuestaChat rc;
            try {
                rc = chatbotService.preguntarConEstado(texto);
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

    private void actualizarBadge() {
        if (badgeIA == null) return;
        GeminiChatbotService.EstadoIA e = chatbotService.getEstado();
        // Si no hay archivo ni env, mostrar Local
        boolean tieneKey = GeminiConfig.tieneApiKey();
        if (!tieneKey) {
            badgeIA.setText("○ Local");
            badgeIA.getStyleClass().setAll("badge-local");
            badgeIA.setTooltip(new javafx.scene.control.Tooltip("Sin IA: configura API key en Ayuda > Configurar IA"));
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
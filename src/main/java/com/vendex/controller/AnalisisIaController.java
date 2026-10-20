package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.service.AnalisisEmpresarialService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

/**
 * Fase 5 Vendexi — panel de análisis empresarial (spec §7).
 * Rotación, agotamiento, márgenes y tendencias con periodo/datos/supuestos/limitaciones.
 */
public class AnalisisIaController {

    private final AnalisisEmpresarialService servicio;

    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<Integer> cmbDias;
    @FXML private Button btnAnalizar;
    @FXML private Label lblEstado;
    @FXML private TextArea txtResultado;
    @FXML private TextArea txtDatos;

    public AnalisisIaController(AppContext ctx) {
        this.servicio = ctx.analisisEmpresarialService;
    }

    @FXML
    private void initialize() {
        cmbTipo.getItems().setAll(
                "General (tendencias + riesgo + márgenes)",
                "Rotación / capital inmovilizado",
                "Riesgo de agotamiento",
                "Márgenes reducidos",
                "Tendencias de venta");
        cmbTipo.getSelectionModel().selectFirst();
        cmbDias.getItems().setAll(7, 15, 30, 60, 90, 180, 365);
        cmbDias.getSelectionModel().select(2); // 30
        if (lblEstado != null) lblEstado.setText("Los datos se agregan localmente; la IA solo recibe agregados (§7).");
    }

    @FXML
    private void onAnalizar() {
        AnalisisEmpresarialService.Tipo tipo = switch (cmbTipo.getSelectionModel().getSelectedIndex()) {
            case 1 -> AnalisisEmpresarialService.Tipo.ROTACION;
            case 2 -> AnalisisEmpresarialService.Tipo.AGOTAMIENTO;
            case 3 -> AnalisisEmpresarialService.Tipo.MARGENES;
            case 4 -> AnalisisEmpresarialService.Tipo.TENDENCIAS;
            default -> AnalisisEmpresarialService.Tipo.GENERAL;
        };
        int dias = cmbDias.getValue() != null ? cmbDias.getValue() : 30;
        btnAnalizar.setDisable(true);
        lblEstado.setText("⏳ analizando " + tipo + " (" + dias + " días)...");
        txtResultado.setText("");

        Thread hilo = new Thread(() -> {
            // Los datos se construyen SIEMPRE localmente (visibles aunque falle la IA)
            String datos = servicio.construirDatosAutorizados(tipo, dias);
            AnalisisEmpresarialService.Resultado res = servicio.analizar(tipo, dias);
            Platform.runLater(() -> {
                txtDatos.setText(res.datosUtilizados != null && !res.datosUtilizados.isBlank()
                        ? res.datosUtilizados : datos);
                txtResultado.setText(res.texto);
                lblEstado.setText(switch (res.estado) {
                    case OK -> "✅ Análisis generado.";
                    case SIN_CAPACIDAD -> "⚠️ Capacidad de IA agotada.";
                    case SIN_LICENCIA -> "⚠️ La licencia no incluye Vendexi AI.";
                    case SIN_API_KEY -> "⚠️ Sin API key de Gemini.";
                    default -> "⚠️ " + res.texto;
                });
                btnAnalizar.setDisable(false);
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }
}

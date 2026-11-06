package com.vendex.controller;

import com.vendex.util.AmbientEffectManager;
import com.vendex.util.ThemeManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

public class AparienciaController implements Initializable {
    @FXML private VBox vistaRoot;
    @FXML private ComboBox<ThemeManager.Theme> cmbTema;
    @FXML private CheckBox chkAnimaciones;
    @FXML private ComboBox<ThemeManager.AmbientIntensity> cmbIntensidad;
    @FXML private ComboBox<ThemeManager.AmbientSpeed> cmbVelocidad;
    @FXML private Label lblEstado;

    private Scene escena;
    private ThemeManager.Theme temaGuardado;
    private boolean animacionesGuardadas;
    private ThemeManager.AmbientIntensity intensidadGuardada;
    private ThemeManager.AmbientSpeed velocidadGuardada;
    private boolean cargando;
    private boolean opcionAmbientalEditada;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTemas();
        configurarOpcionesAmbientales();
        cargarAparienciaGuardada();
        vistaRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                escena = newScene;
                vistaRoot.applyCss();
                previsualizar();
            }
        });
        cmbTema.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!cargando && newValue != null) {
                if (esTemaAnimado(newValue) && !ThemeManager.tienePreferenciaAmbiental() && !opcionAmbientalEditada)
                    chkAnimaciones.setSelected(true);
                actualizarControlesAmbientales(newValue);
                previsualizar();
            }
        });
        chkAnimaciones.selectedProperty().addListener((obs, oldValue, newValue) -> {
            if (!cargando) {
                opcionAmbientalEditada = true;
                actualizarControlesAmbientales(cmbTema.getValue());
                previsualizar();
            }
        });
        cmbIntensidad.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!cargando) previsualizar();
        });
        cmbVelocidad.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!cargando) previsualizar();
        });
    }

    private void configurarTemas() {
        cmbTema.setItems(FXCollections.observableArrayList(ThemeManager.Theme.values()));
        cmbTema.setConverter(new StringConverter<>() {
            @Override public String toString(ThemeManager.Theme theme) { return theme == null ? "" : theme.label(); }
            @Override public ThemeManager.Theme fromString(String value) {
                for (ThemeManager.Theme theme : ThemeManager.Theme.values())
                    if (theme.label().equalsIgnoreCase(value)) return theme;
                return ThemeManager.Theme.ORGANIC;
            }
        });
    }

    private void configurarOpcionesAmbientales() {
        cmbIntensidad.setItems(FXCollections.observableArrayList(ThemeManager.AmbientIntensity.values()));
        cmbIntensidad.setConverter(new StringConverter<>() {
            @Override public String toString(ThemeManager.AmbientIntensity value) {
                if (value == null) return "";
                return switch (value) { case LOW -> "Baja"; case MEDIUM -> "Media"; case HIGH -> "Alta"; };
            }
            @Override public ThemeManager.AmbientIntensity fromString(String value) {
                return switch (value == null ? "" : value) {
                    case "Media" -> ThemeManager.AmbientIntensity.MEDIUM;
                    case "Alta" -> ThemeManager.AmbientIntensity.HIGH;
                    default -> ThemeManager.AmbientIntensity.LOW;
                };
            }
        });
        cmbVelocidad.setItems(FXCollections.observableArrayList(ThemeManager.AmbientSpeed.values()));
        cmbVelocidad.setConverter(new StringConverter<>() {
            @Override public String toString(ThemeManager.AmbientSpeed value) {
                if (value == null) return "";
                return value == ThemeManager.AmbientSpeed.SLOW ? "Lenta" : "Normal";
            }
            @Override public ThemeManager.AmbientSpeed fromString(String value) {
                return "Normal".equalsIgnoreCase(value) ? ThemeManager.AmbientSpeed.NORMAL : ThemeManager.AmbientSpeed.SLOW;
            }
        });
    }

    private void cargarAparienciaGuardada() {
        cargando = true;
        temaGuardado = ThemeManager.getCurrentTheme();
        animacionesGuardadas = ThemeManager.animacionesAmbientalesActivas();
        intensidadGuardada = ThemeManager.getAmbientIntensity();
        velocidadGuardada = ThemeManager.getAmbientSpeed();
        cmbTema.setValue(temaGuardado);
        chkAnimaciones.setSelected(animacionesGuardadas);
        cmbIntensidad.setValue(intensidadGuardada);
        cmbVelocidad.setValue(velocidadGuardada);
        actualizarControlesAmbientales(temaGuardado);
        cargando = false;
    }

    private void actualizarControlesAmbientales(ThemeManager.Theme theme) {
        boolean disponible = esTemaAnimado(theme);
        chkAnimaciones.setDisable(!disponible);
        cmbIntensidad.setDisable(!disponible || !chkAnimaciones.isSelected());
        cmbVelocidad.setDisable(!disponible || !chkAnimaciones.isSelected());
    }

    private static boolean esTemaAnimado(ThemeManager.Theme theme) {
        return theme == ThemeManager.Theme.PINK_BLOSSOM || theme == ThemeManager.Theme.NATURE_BREEZE;
    }

    private void previsualizar() {
        if (escena == null || cmbTema.getValue() == null) return;
        ThemeManager.Theme theme = cmbTema.getValue();
        ThemeManager.preview(escena, theme);
        AmbientEffectManager.preview(escena, theme, esTemaAnimado(theme) && chkAnimaciones.isSelected(),
                cmbIntensidad.getValue(), cmbVelocidad.getValue());
        if (lblEstado != null) lblEstado.setText("Vista previa · pulsa Aplicar para guardar en este equipo.");
    }

    @FXML
    private void aplicar() {
        ThemeManager.Theme theme = cmbTema.getValue() == null ? ThemeManager.Theme.ORGANIC : cmbTema.getValue();
        boolean ambientValue = esTemaAnimado(theme) ? chkAnimaciones.isSelected() : animacionesGuardadas;
        ThemeManager.AmbientIntensity intensity = cmbIntensidad.getValue() == null
                ? ThemeManager.AmbientIntensity.LOW : cmbIntensidad.getValue();
        ThemeManager.AmbientSpeed ambientSpeed = cmbVelocidad.getValue() == null
                ? ThemeManager.AmbientSpeed.SLOW : cmbVelocidad.getValue();
        ThemeManager.guardarApariencia(theme, ambientValue, intensity, ambientSpeed);
        temaGuardado = theme;
        animacionesGuardadas = ambientValue;
        intensidadGuardada = intensity;
        velocidadGuardada = ambientSpeed;
        opcionAmbientalEditada = true;
        if (escena != null) {
            ThemeManager.preview(escena, theme);
            AmbientEffectManager.refresh(escena);
        }
        lblEstado.setText("Apariencia guardada localmente en este equipo.");
    }

    @FXML
    private void restablecer() {
        cargando = true;
        cmbTema.setValue(ThemeManager.Theme.ORGANIC);
        chkAnimaciones.setSelected(false);
        cmbIntensidad.setValue(ThemeManager.AmbientIntensity.LOW);
        cmbVelocidad.setValue(ThemeManager.AmbientSpeed.SLOW);
        actualizarControlesAmbientales(ThemeManager.Theme.ORGANIC);
        opcionAmbientalEditada = false;
        cargando = false;
        previsualizar();
    }

    /** Called by the navigator when leaving an uncommitted preview. */
    public void descartarVistaPrevia() {
        if (escena == null) return;
        ThemeManager.preview(escena, temaGuardado);
        AmbientEffectManager.preview(escena, temaGuardado, animacionesGuardadas,
                intensidadGuardada, velocidadGuardada);
    }
}

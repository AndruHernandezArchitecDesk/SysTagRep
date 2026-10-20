package com.vendex.controller;

import com.vendex.service.VendexiSugerenciaService;
import com.vendex.service.VendexiSugerenciaService.Sugerencia;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fase 2 Vendexi AI — panel contextual de sugerencias (componente compuesto)
 * para facturación y proformas. Muestra verificaciones del documento y
 * complementarios propuestos por la IA. El usuario acepta, descarta o copia;
 * nunca se modifica el documento en silencio.
 */
public class VendexiSugerenciasPane extends BorderPane {

    @FXML private VBox sugerenciasBox;
    @FXML private Label lblEstado;

    private final VendexiSugerenciaService servicio = new VendexiSugerenciaService();
    private String modulo = "documento";
    private Supplier<VendexiSugerenciaService.Contexto> contextoProvider = () -> null;
    private Consumer<com.vendex.model.Inventario> onAgregarProducto = inv -> {};
    private Consumer<String> onCopiarTexto = t -> {};

    public VendexiSugerenciasPane() {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/VendexiSugerencias.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar VendexiSugerencias.fxml", e);
        }
    }

    @FXML
    private void initialize() {
        if (lblEstado != null) lblEstado.setText("");
    }

    public void setModulo(String modulo) {
        this.modulo = modulo != null ? modulo : "documento";
    }

    public void setContextoProvider(Supplier<VendexiSugerenciaService.Contexto> provider) {
        if (provider != null) this.contextoProvider = provider;
    }

    public void setOnAgregarProducto(Consumer<com.vendex.model.Inventario> cb) {
        if (cb != null) this.onAgregarProducto = cb;
    }

    public void setOnCopiarTexto(Consumer<String> cb) {
        if (cb != null) this.onCopiarTexto = cb;
    }

    public boolean isIaDisponible() {
        return servicio.esIaDisponible();
    }

    /** Inserta el panel al inicio del contenedor dado (por ejemplo, el VBox central de la vista). */
    public void insertarEn(VBox contenedor, int indice) {
        if (contenedor == null) return;
        contenedor.getChildren().add(indice, this);
    }

    /**
     * Localiza el VBox central que contiene la sección del cliente subiendo por la
     * jerarquía desde el ComboBox de clientes: cmbCliente → HBox → VBox → TitledPane → VBox.
     */
    public static VBox localizarContenidoDesde(Node nodoCliente) {
        Node n = nodoCliente;
        for (int i = 0; i < 6 && n != null; i++) {
            n = n.getParent();
            if (n instanceof VBox v && v.getParent() instanceof javafx.scene.control.TitledPane) {
                return v;
            }
        }
        return null;
    }

    @FXML
    private void verificar() {
        VendexiSugerenciaService.Contexto ctx = contextoProvider.get();
        List<Sugerencia> checks = servicio.verificarDocumento(ctx);
        limpiar();
        if (checks.isEmpty()) {
            estado("Verificado: sin observaciones locales en el " + modulo + ".");
        } else {
            for (Sugerencia s : checks) agregarSugerencia(s);
            estado(checks.size() + " observación(es) — fuente: verificación local.");
        }
    }

    @FXML
    private void limpiar() {
        if (sugerenciasBox != null) sugerenciasBox.getChildren().clear();
        if (lblEstado != null) lblEstado.setText("");
    }

    /** Muestra un lote de sugerencias (reemplaza las actuales). Hilo: UI. */
    public void mostrarSugerencias(List<Sugerencia> sugerencias, String mensajeEstado) {
        runFx(() -> {
            limpiar();
            if (sugerencias != null) {
                for (Sugerencia s : sugerencias) agregarSugerenciaInternal(s);
            }
            if (mensajeEstado != null) estado(mensajeEstado);
        });
    }

    /** Agrega una tarjeta de sugerencia (acumula). Hilo: UI. */
    public void agregarSugerencia(Sugerencia s) {
        runFx(() -> agregarSugerenciaInternal(s));
    }

    public void estado(String texto) {
        runFx(() -> {
            if (lblEstado != null) lblEstado.setText(texto != null ? texto : "");
        });
    }

    private void agregarSugerenciaInternal(Sugerencia s) {
        if (s == null || sugerenciasBox == null) return;
        sugerenciasBox.getChildren().add(construirTarjeta(s));
    }

    private void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    private VBox construirTarjeta(Sugerencia s) {
        boolean esVerificacion = s.tipo == VendexiSugerenciaService.Tipo.VERIFICACION;
        Label titulo = new Label((esVerificacion ? "⚠ " : "💡 ") + s.titulo);
        titulo.setWrapText(true);
        titulo.setStyle("-fx-font-weight: bold;" + (esVerificacion ? " -fx-text-fill: #b00020;" : ""));

        Label motivo = new Label(s.motivo);
        motivo.setWrapText(true);
        motivo.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        VBox cuerpo = new VBox(2, titulo, motivo);
        if (!esVerificacion) {
            Label chip = new Label(chipEvidencia(s.evidencia));
            chip.setStyle(chipEvidenciaEstilo(s.evidencia));
            cuerpo.getChildren().add(chip);
        }
        HBox.setHgrow(cuerpo, Priority.ALWAYS);

        HBox acciones = new HBox(6);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        VBox tarjeta = new VBox();
        if (s.producto != null) {
            Button btnAceptar = new Button("Aceptar");
            btnAceptar.getStyleClass().add("vendexi-aceptar");
            btnAceptar.setOnAction(e -> confirmarYAgregar(s.producto));
            acciones.getChildren().add(btnAceptar);
        }
        Button btnCopiar = new Button("Copiar");
        btnCopiar.setOnAction(e -> onCopiarTexto.accept(
                s.producto != null
                        ? (s.producto.getCodigo() != null ? s.producto.getCodigo() : s.producto.getDescripcion())
                        : s.titulo));
        Button btnDescartar = new Button("Descartar");
        btnDescartar.setOnAction(e -> sugerenciasBox.getChildren().remove(tarjeta));
        acciones.getChildren().addAll(btnCopiar, btnDescartar);

        HBox fila = new HBox(10, cuerpo, acciones);
        fila.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getChildren().add(fila);
        tarjeta.setStyle("-fx-background-color: rgba(128,128,128,0.08); -fx-background-radius: 8; -fx-padding: 6;");
        return tarjeta;
    }

    /** Confirmación humana explícita antes de agregar un producto sugerido al documento. */
    private void confirmarYAgregar(com.vendex.model.Inventario inv) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vendexi AI");
        confirm.setHeaderText("Agregar producto sugerido");
        confirm.setContentText((inv.getCodigo() != null ? inv.getCodigo() + " — " : "")
                + inv.getDescripcion()
                + "\n\nLa sugerencia de la IA no garantiza compatibilidad. ¿Agregar 1 unidad al " + modulo + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onAgregarProducto.accept(inv);
        }
    }

    /** Spec §6: el usuario debe distinguir compatibilidad confirmada de coincidencia/asociación. */
    private static String chipEvidencia(String evidencia) {
        return switch (evidencia != null ? evidencia : VendexiSugerenciaService.EVIDENCIA_ASOCIACION) {
            case VendexiSugerenciaService.EVIDENCIA_CONFIRMADA -> "● compatibilidad confirmada";
            case VendexiSugerenciaService.EVIDENCIA_PROBABLE -> "● coincidencia probable — verificar";
            default -> "● asociación de ventas (sin verificación técnica)";
        };
    }

    private static String chipEvidenciaEstilo(String evidencia) {
        String base = "-fx-font-size: 10px; -fx-padding: 1 6 1 6; -fx-background-radius: 8;";
        return switch (evidencia != null ? evidencia : VendexiSugerenciaService.EVIDENCIA_ASOCIACION) {
            case VendexiSugerenciaService.EVIDENCIA_CONFIRMADA ->
                    base + " -fx-background-color: #e6f4ea; -fx-text-fill: #137333;";
            case VendexiSugerenciaService.EVIDENCIA_PROBABLE ->
                    base + " -fx-background-color: #fef7e0; -fx-text-fill: #b06000;";
            default -> base + " -fx-background-color: #f1f3f4; -fx-text-fill: #5f6368;";
        };
    }
}

package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.dao.MarcaAguaPDFDAO;
import com.vendex.dao.MarcaAguaPDFDAOPostgres;
import com.vendex.model.MarcaAguaPDFConfig;
import com.vendex.util.EmpresaLogo;
import com.vendex.util.MarcaAguaPDF;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public final class MarcaAguaController implements Initializable {
    private static final int MAX_LOGO_BYTES = 5 * 1024 * 1024;

    public MarcaAguaController() { this(AppContext.getInstance()); }

    public MarcaAguaController(AppContext context) {
        this.dao = new MarcaAguaPDFDAOPostgres();
    }

    private final MarcaAguaPDFDAO dao;
    private byte[] logoBytes;

    @FXML private CheckBox chkFactura;
    @FXML private CheckBox chkProforma;
    @FXML private ComboBox<String> cmbDistribucion;
    @FXML private ComboBox<String> cmbVistaPrevia;
    @FXML private Slider sldOpacidad;
    @FXML private Label lblOpacidad;
    @FXML private Label lblLogoEstado;
    @FXML private Label lblEstado;
    @FXML private ImageView imgLogo;
    @FXML private Button btnActualizarLogo;
    @FXML private Button btnVistaPrevia;
    @FXML private Button btnGuardar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbDistribucion.setItems(FXCollections.observableArrayList("Logo único centrado", "Varios logos repetidos"));
        cmbVistaPrevia.setItems(FXCollections.observableArrayList("Proforma", "Factura de venta"));
        cmbVistaPrevia.setValue("Proforma");
        sldOpacidad.valueProperty().addListener((obs, old, value) -> actualizarEtiquetaOpacidad(value.doubleValue()));
        btnVistaPrevia.setOnAction(e -> vistaPrevia());
        btnGuardar.setOnAction(e -> guardar());
        cargarConfiguracion();
    }

    private void cargarConfiguracion() {
        try {
            MarcaAguaPDFConfig cfg = dao.obtener();
            chkFactura.setSelected(cfg.isFacturaActiva());
            chkProforma.setSelected(cfg.isProformaActiva());
            cmbDistribucion.setValue(cfg.getDistribucion() == MarcaAguaPDFConfig.Distribucion.REPETIDA
                    ? "Varios logos repetidos" : "Logo único centrado");
            sldOpacidad.setValue(cfg.getOpacidad());
            byte[] logoGuardado = cfg.getLogoBytes();
            if (logoGuardado != null && logoGuardado.length > 0) {
                logoBytes = MarcaAguaPDF.normalizarLogo(logoGuardado);
                lblLogoEstado.setText("Logo compartido cargado desde dbTag.");
                mostrarLogo(logoBytes);
            } else {
                cargarLogoEmpresaLocal();
                lblLogoEstado.setText("No hay logo compartido guardado. El logo de Mi Empresa está listo para sincronizar al guardar.");
            }
            estado("Configuración compartida cargada desde dbTag.", false);
        } catch (Exception e) {
            logoBytes = null;
            cmbDistribucion.setValue("Logo único centrado");
            sldOpacidad.setValue(0.12);
            cargarLogoEmpresaLocal();
            estado("No se pudo leer la configuración compartida: " + e.getMessage(), true);
        }
        actualizarEtiquetaOpacidad(sldOpacidad.getValue());
    }

    @FXML
    private void seleccionarLogoEmpresa() {
        try {
            cargarLogoEmpresaLocal();
            if (logoBytes == null || logoBytes.length == 0) {
                estado("No hay un logo disponible en Mi Empresa para sincronizar.", true);
                return;
            }
            lblLogoEstado.setText("Logo de Mi Empresa listo para compartir en dbTag al guardar.");
            estado("Confirma el guardado para actualizar el logo compartido.", false);
        } catch (Exception e) {
            estado("No se pudo cargar el logo: " + e.getMessage(), true);
        }
    }

    /** Copia al registro compartido el logo que está configurado actualmente en Mi Empresa. */
    private void cargarLogoEmpresaLocal() {
        try (InputStream in = EmpresaLogo.abrirLogoStream(null)) {
            if (in == null) {
                logoBytes = null;
                lblLogoEstado.setText("No se encontró un logo configurado ni el logo incluido en Vendex.");
                imgLogo.setImage(null);
                return;
            }
            byte[] bytes = in.readNBytes(MAX_LOGO_BYTES + 1);
            validarLogo(bytes);
            logoBytes = MarcaAguaPDF.normalizarLogo(bytes);
            mostrarLogo(logoBytes);
        } catch (Exception e) {
            logoBytes = null;
            lblLogoEstado.setText("No se pudo cargar el logo de Mi Empresa: " + e.getMessage());
            imgLogo.setImage(null);
        }
    }

    private void mostrarLogo(byte[] bytes) {
        imgLogo.setImage(new Image(new ByteArrayInputStream(bytes), 220, 120, true, true));
    }

    private static void validarLogo(byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("El archivo está vacío.");
        if (bytes.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("El logo no puede superar 5 MB.");
    }

    private MarcaAguaPDFConfig configuracionActual(boolean habilitarVistaPrevia) {
        boolean factura = chkFactura.isSelected();
        boolean proforma = chkProforma.isSelected();
        if (habilitarVistaPrevia) {
            if (tipoVistaPrevia() == MarcaAguaPDF.TipoDocumento.FACTURA) factura = true;
            else proforma = true;
        }
        MarcaAguaPDFConfig.Distribucion distribucion = "Varios logos repetidos".equals(cmbDistribucion.getValue())
                ? MarcaAguaPDFConfig.Distribucion.REPETIDA : MarcaAguaPDFConfig.Distribucion.CENTRO;
        return new MarcaAguaPDFConfig(factura, proforma,
                distribucion, (float) sldOpacidad.getValue(), logoBytes);
    }

    private void guardar() {
        try {
            if ((chkFactura.isSelected() || chkProforma.isSelected()) && (logoBytes == null || logoBytes.length == 0)) {
                estado("Selecciona o carga un logo antes de activar la marca de agua.", true);
                return;
            }
            dao.guardar(configuracionActual(false));
            estado("Configuración y logo guardados en dbTag. Se aplicarán a todos los equipos conectados.", false);
            lblLogoEstado.setText("Logo compartido guardado en dbTag.");
            new Alert(Alert.AlertType.INFORMATION,
                    "La marca de agua compartida quedó guardada. Facturas y proformas nuevas usarán esta configuración.").showAndWait();
        } catch (Exception e) {
            estado("No se pudo guardar en la base compartida: " + e.getMessage(), true);
        }
    }

    private void vistaPrevia() {
        if (logoBytes == null || logoBytes.length == 0) cargarLogoEmpresaLocal();
        if (logoBytes == null || logoBytes.length == 0) {
            estado("No hay logo disponible para la vista previa.", true);
            return;
        }
        MarcaAguaPDF.TipoDocumento tipo = tipoVistaPrevia();
        MarcaAguaPDFConfig previewConfig = configuracionActual(true);
        btnVistaPrevia.setDisable(true);
        estado("Generando la vista previa…", false);

        Task<File> tarea = new Task<>() {
            @Override protected File call() throws Exception {
                File pdf = Files.createTempFile("vendex-marca-agua-", ".pdf").toFile();
                MarcaAguaPDF.generarVistaPrevia(pdf, previewConfig, tipo);
                return pdf;
            }
        };
        tarea.setOnSucceeded(event -> {
            btnVistaPrevia.setDisable(false);
            File pdf = tarea.getValue();
            try {
                abrirConVisorExterno(pdf);
                estado("Vista previa generada: " + pdf.getAbsolutePath(), false);
            } catch (Exception e) {
                estado("Vista previa creada, pero no se pudo abrir automáticamente: " + pdf.getAbsolutePath(), true);
            }
        });
        tarea.setOnFailed(event -> {
            btnVistaPrevia.setDisable(false);
            Throwable error = tarea.getException();
            estado("No se pudo generar la vista previa: "
                    + (error == null ? "error desconocido" : error.getMessage()), true);
        });
        Thread hilo = new Thread(tarea, "vista-previa-marca-agua");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** No mezcla AWT/GTK con JavaFX: solo inicia el visor del sistema sin esperar a que termine. */
    private static void abrirConVisorExterno(File pdf) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String uri = pdf.toURI().toString();
        List<String> command = os.contains("win")
                ? List.of("rundll32", "url.dll,FileProtocolHandler", uri)
                : os.contains("mac") ? List.of("open", uri) : List.of("xdg-open", uri);
        new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private MarcaAguaPDF.TipoDocumento tipoVistaPrevia() {
        return "Factura de venta".equals(cmbVistaPrevia.getValue())
                ? MarcaAguaPDF.TipoDocumento.FACTURA : MarcaAguaPDF.TipoDocumento.PROFORMA;
    }

    private void actualizarEtiquetaOpacidad(double value) {
        if (lblOpacidad != null) lblOpacidad.setText(String.format(java.util.Locale.ROOT, "%.0f%%", value * 100));
    }

    private void estado(String texto, boolean error) {
        lblEstado.setText(texto);
        lblEstado.setStyle(error ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
    }
}

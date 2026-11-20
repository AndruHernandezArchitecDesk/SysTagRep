package com.vendex.controller;

import com.vendex.dao.EmpresaDAO;
import com.vendex.model.Empresa;
import com.vendex.util.DatabaseInputLimits;
import com.vendex.util.EmpresaLogo;
import com.vendex.util.LogoManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EmpresaController implements Initializable {

    public EmpresaController() {
        this(com.vendex.config.AppContext.getInstance());
    }

    public EmpresaController(com.vendex.config.AppContext ctx) {
        this.empresaDAO = ctx.empresaDAO;
    }

    private final EmpresaDAO empresaDAO;
    private Empresa empresaActual;
    private File logoPendiente;
    private static final int MAX_LOGO_BYTES = 5 * 1024 * 1024;

    @FXML private TextField txtRuc;
    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtSucursal;
    @FXML private ComboBox<String> cmbObligado;
    @FXML private TextField txtAgenteRetencion;
    @FXML private TextField txtResolucion;
    @FXML private ImageView imgLogo;
    @FXML private Label lblLogoRuta;
    @FXML private Button btnExaminarLogo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblStatus;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        DatabaseInputLimits.limit(txtRuc, 13);
        DatabaseInputLimits.limit(txtRazonSocial, 300);
        DatabaseInputLimits.limit(txtTitulo, 300);
        DatabaseInputLimits.limit(txtTelefono, 20);
        DatabaseInputLimits.limit(txtCorreo, 100);
        DatabaseInputLimits.limit(txtDireccion, 200);
        DatabaseInputLimits.limit(txtSucursal, 10);
        DatabaseInputLimits.limit(txtAgenteRetencion, 10);
        DatabaseInputLimits.limit(txtResolucion, 20);
        cmbObligado.setItems(FXCollections.observableArrayList("SI", "NO"));
        cargarDatosEmpresa();
        btnGuardar.setOnAction(event -> guardar());
        btnCancelar.setOnAction(event -> cancelar());
    }

    private void cargarDatosEmpresa() {
        logoPendiente = null;
        List<Empresa> empresas = empresaDAO.listar();
        if (empresas.isEmpty()) {
            empresaActual = new Empresa();
            empresaActual.setRazonSocial("TAG Repuestos Automotrices");
            empresaActual.setObligadoContabilidad("NO");
            empresaActual.setEstado(true);
        } else {
            empresaActual = empresas.get(0);
        }
        txtRuc.setText(nulo(empresaActual.getRuc()));
        txtRazonSocial.setText(nulo(empresaActual.getRazonSocial()));
        txtTitulo.setText(nulo(empresaActual.getTitulo()));
        txtTelefono.setText(nulo(empresaActual.getTelefono()));
        txtCorreo.setText(nulo(empresaActual.getCorreo()));
        txtDireccion.setText(juntarDireccion(empresaActual));
        txtSucursal.setText(nulo(empresaActual.getSucursal()));
        cmbObligado.setValue(normalizarObligado(empresaActual.getObligadoContabilidad()));
        txtAgenteRetencion.setText(nulo(empresaActual.getAgenteRetencion()));
        txtResolucion.setText(nulo(empresaActual.getResolucion()));
        mostrarLogo();
        estado("", false);
    }

    private void mostrarLogo() {
        try {
            byte[] logoBytes = empresaActual.getLogoBytes();
            if (logoBytes != null && logoBytes.length > 0) {
                imgLogo.setImage(new Image(new ByteArrayInputStream(logoBytes), 180, 90, true, true));
                if (lblLogoRuta != null) lblLogoRuta.setText("Logo compartido en la base de datos");
                return;
            }
            String ruta = EmpresaLogo.rutaLogoConfigurado();
            Image img = null;
            if (ruta != null && !ruta.startsWith("/img/")) {
                File f = new File(ruta);
                if (f.isFile()) img = new Image(f.toURI().toString());
            }
            if (img == null) {
                String recurso = (ruta != null) ? ruta : EmpresaLogo.LOGO_RECURSO;
                try (InputStream is = EmpresaLogo.abrirLogoStream(recurso)) {
                    if (is != null) img = new Image(is);
                }
            }
            imgLogo.setImage(img);
            if (lblLogoRuta != null) {
                lblLogoRuta.setText(ruta != null ? ruta : "Logo por defecto de la aplicación");
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void cambiarLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(btnExaminarLogo != null ? btnExaminarLogo.getScene().getWindow() : null);
        if (file != null) {
            logoPendiente = file;
            imgLogo.setImage(new Image(file.toURI().toString()));
            if (lblLogoRuta != null) lblLogoRuta.setText(file.getAbsolutePath() + " (pendiente de guardar)");
        }
    }

    private void guardar() {
        try {
            String logoAnterior = empresaActual.getLogoUrl();
            String ruc = txtRuc.getText() != null ? txtRuc.getText().trim() : "";
            String nombre = txtRazonSocial.getText() != null ? txtRazonSocial.getText().trim() : "";
            if (nombre.isEmpty()) {
                estado("El nombre de la empresa es obligatorio.", true);
                return;
            }
            if (!ruc.matches("\\d{13}")) {
                estado("El RUC debe tener 13 dígitos numéricos.", true);
                return;
            }
            boolean actualizarLogoCompartido = logoPendiente != null;
            if (logoPendiente != null) {
                try {
                    if (logoPendiente.length() > MAX_LOGO_BYTES)
                        throw new IllegalArgumentException("El logo no puede superar 5 MB.");
                    byte[] original = Files.readAllBytes(logoPendiente.toPath());
                    byte[] compartido = com.vendex.util.MarcaAguaPDF.normalizarLogo(original);
                    empresaActual.setLogoUrl(EmpresaLogo.guardarLogo(logoPendiente));
                    empresaActual.setLogoBytes(compartido);
                } catch (Exception e) {
                    estado("No se pudo preparar el logo compartido: " + e.getMessage(), true);
                    return;
                }
            } else empresaActual.setLogoBytes(null); // null significa conservar los bytes actuales.

            empresaActual.setRuc(ruc);
            empresaActual.setRazonSocial(nombre);
            empresaActual.setTitulo(valor(txtTitulo));
            empresaActual.setTelefono(valor(txtTelefono));
            empresaActual.setCorreo(valor(txtCorreo));
            empresaActual.setDireccionCallePrincipal(valor(txtDireccion));
            empresaActual.setSucursal(valor(txtSucursal));
            empresaActual.setObligadoContabilidad(cmbObligado.getValue() != null ? cmbObligado.getValue() : "NO");
            empresaActual.setAgenteRetencion(valorONulo(txtAgenteRetencion));
            empresaActual.setResolucion(valorONulo(txtResolucion));
            empresaActual.setEstado(true);

            boolean ok;
            if (empresaActual.getId() == 0) {
                Empresa creada = empresaDAO.crear(empresaActual);
                ok = creada != null;
                if (ok) empresaActual = creada;
            } else {
                ok = empresaDAO.actualizar(empresaActual);
            }
            if (ok) {
                if (actualizarLogoCompartido) {
                    EmpresaLogo.eliminarLogoAnterior(logoAnterior, empresaActual.getLogoUrl());
                    EmpresaLogo.actualizarCache(empresaActual);
                } else EmpresaLogo.invalidarCache();
                cargarDatosEmpresa();
                LogoManager.recargar();
                new Alert(Alert.AlertType.INFORMATION, "Empresa guardada correctamente."
                        + (actualizarLogoCompartido ? " El logo compartido quedó guardado en dbTag para todos los equipos." : ""))
                        .showAndWait();
            } else {
                estado("Error al guardar la empresa.", true);
            }
        } catch (Exception e) {
            estado("Error: " + e.getMessage(), true);
        }
    }

    private void cancelar() {
        cargarDatosEmpresa();
    }

    private void estado(String msg, boolean error) {
        if (lblStatus == null) return;
        lblStatus.setText(msg);
        lblStatus.setStyle(error ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
    }

    private static String nulo(String s) { return s != null ? s : ""; }

    private static String valor(TextField t) { return t != null && t.getText() != null ? t.getText().trim() : ""; }

    private static String valorONulo(TextField t) {
        String v = valor(t);
        return v.isEmpty() ? null : v;
    }

    private static String juntarDireccion(Empresa e) {
        String p = nulo(e.getDireccionCallePrincipal());
        String s = nulo(e.getDireccionCalleSecundaria());
        if (!p.isEmpty() && !s.isEmpty() && !s.equals(p)) return p + " y " + s;
        return p.isEmpty() ? s : p;
    }

    private static String normalizarObligado(String v) {
        return "SI".equalsIgnoreCase(v) ? "SI" : "NO";
    }
}

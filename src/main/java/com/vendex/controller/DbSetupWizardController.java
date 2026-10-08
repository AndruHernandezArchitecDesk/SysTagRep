package com.vendex.controller;

import com.vendex.config.DatabaseConnection;
import com.vendex.config.DbConfig;
import com.vendex.util.PasswordDebilValidator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Wizard producción SIN base de datos: solo conecta a la BD existente.
 * Campos: Host/IP + Puerto + Base + Usuario + Clave, con URL auto-generada
 * y prueba de conexión obligatoria antes de Guardar.
 */
public class DbSetupWizardController {

    // Campos nuevos (producción)
    @FXML private TextField txtHost;
    @FXML private TextField txtPuerto;
    @FXML private TextField txtBase;
    @FXML private TextField txtUrlPreview;
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPasswordExistente;
    @FXML private TextField txtPasswordExistenteVisible;
    @FXML private CheckBox chkMostrarPassword;
    @FXML private Button btnProbar;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblStatus;
    @FXML private Label lblInfo;
    @FXML private TextField txtMasterKey;
    @FXML private Button btnCopiarMaster;
    @FXML private Label lblMasterInfo;

    // Legacy (FXML antiguo / tests): opcionales, pueden ser null
    @FXML private RadioButton rbNueva;
    @FXML private RadioButton rbExistente;
    @FXML private ToggleGroup modoGroup;
    @FXML private TextField txtUrl;
    @FXML private TextField txtPasswordGenerada;
    @FXML private Button btnGenerar;
    @FXML private Button btnCopiar;
    @FXML private Button btnGenerarMaster;
    @FXML private CheckBox chkMasterRespaldo;
    @FXML private Label lblCopiadoAviso;

    private boolean completed = false;
    private volatile boolean pruebaOk = false;
    private volatile String urlProbadaOk = null;
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_!@#$%&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @FXML
    public void initialize() {
        if (btnProbar != null) btnProbar.setOnAction(e -> probarConexion());
        if (btnGuardar != null) {
            btnGuardar.setOnAction(e -> guardar());
            btnGuardar.setDisable(true);
        }
        if (btnCancelar != null) btnCancelar.setOnAction(e -> cancelar());
        if (chkMostrarPassword != null) chkMostrarPassword.setOnAction(e -> alternarVisibilidad());
        if (btnCopiarMaster != null) btnCopiarMaster.setOnAction(e -> copiarMasterKey());
        // legacy handlers (si el FXML aún los trae)
        if (btnGenerar != null) btnGenerar.setOnAction(e -> generarPasswordLegacy());
        if (btnCopiar != null) btnCopiar.setOnAction(e -> copiarPasswordLegacy());
        if (btnGenerarMaster != null) btnGenerarMaster.setOnAction(e -> generarMasterKeyLegacy());

        // Precargar: archivo existente o defaults producción (IP ejemplo + app_vendex)
        String host = DbConfig.DEFAULT_HOST;
        String puerto = String.valueOf(DbConfig.DEFAULT_PORT);
        String base = DbConfig.DEFAULT_DB;
        String user = DbConfig.DEFAULT_PROD_USER;
        try {
            String[] cfg = DbConfig.cargar();
            if (DbConfig.getArchivo().exists() && cfg != null && cfg[0] != null) {
                String[] partes = DbConfig.parsear(cfg[0]);
                host = partes[0];
                puerto = partes[1];
                base = partes[2];
                if (cfg[1] != null && !cfg[1].isBlank()) user = cfg[1].trim();
            }
        } catch (Exception ignored) {}

        if (txtHost != null) txtHost.setText(host);
        if (txtPuerto != null) txtPuerto.setText(puerto);
        if (txtBase != null) txtBase.setText(base);
        if (txtUser != null) txtUser.setText(user);
        if (txtUrl != null) txtUrl.setText(DbConfig.construirUrl(host, parsePuerto(puerto), base));
        actualizarPreview();

        // live preview al editar
        if (txtHost != null) txtHost.textProperty().addListener((o, a, b) -> { pruebaOk = false; actualizarPreview(); });
        if (txtPuerto != null) txtPuerto.textProperty().addListener((o, a, b) -> { pruebaOk = false; actualizarPreview(); });
        if (txtBase != null) txtBase.textProperty().addListener((o, a, b) -> { pruebaOk = false; actualizarPreview(); });
        if (txtUser != null) txtUser.textProperty().addListener((o, a, b) -> pruebaOk = false);
        if (txtPasswordExistente != null) txtPasswordExistente.textProperty().addListener((o, a, b) -> { pruebaOk = false; actualizarBotonGuardar(); });
        if (txtPasswordExistenteVisible != null) txtPasswordExistenteVisible.textProperty().addListener((o, a, b) -> { pruebaOk = false; actualizarBotonGuardar(); });

        if (txtPasswordExistenteVisible != null) {
            txtPasswordExistenteVisible.setVisible(false);
            txtPasswordExistenteVisible.setManaged(false);
        }
        // master existente: mostrar sin generar nada nuevo
        try {
            if (txtMasterKey != null && com.vendex.util.SecureConfigStore.existeMasterKey()
                    && (txtMasterKey.getText() == null || txtMasterKey.getText().isBlank())) {
                txtMasterKey.setText(com.vendex.util.SecureConfigStore.exportarMasterKeyParaRespaldo());
                if (lblMasterInfo != null) lblMasterInfo.setText("Clave maestra ya existe en esta PC.");
            }
        } catch (Exception ignored) {}
        actualizarBotonGuardar();
    }

    private void actualizarPreview() {
        String url = construirUrlActual();
        if (txtUrlPreview != null) txtUrlPreview.setText(url);
        if (txtUrl != null) txtUrl.setText(url);
        actualizarBotonGuardar();
    }

    private void actualizarBotonGuardar() {
        if (btnGuardar == null) return;
        String pass = obtenerPassword();
        boolean camposOk = !construirUrlActual().isBlank()
                && txtUser != null && !txtUser.getText().trim().isEmpty()
                && !pass.isEmpty();
        btnGuardar.setDisable(!(camposOk && pruebaOk && urlProbadaOk != null
                && urlProbadaOk.equals(construirUrlActual())));
    }

    private int parsePuerto(String s) {
        try {
            int p = Integer.parseInt(s.trim());
            if (p > 0 && p <= 65535) return p;
        } catch (Exception ignored) {}
        return DbConfig.DEFAULT_PORT;
    }

    private String construirUrlActual() {
        String host = txtHost != null && txtHost.getText() != null ? txtHost.getText().trim() : "";
        // compatibilidad: si el FXML es el antiguo (txtUrl directo), usarlo
        if ((host.isEmpty()) && txtUrl != null && txtUrl.getText() != null && !txtUrl.getText().isBlank()) {
            return txtUrl.getText().trim();
        }
        if (host.isEmpty()) host = DbConfig.DEFAULT_HOST;
        String puertoS = txtPuerto != null && txtPuerto.getText() != null ? txtPuerto.getText().trim() : "";
        String base = txtBase != null && txtBase.getText() != null ? txtBase.getText().trim() : "";
        if (base.isEmpty()) base = DbConfig.DEFAULT_DB;
        int puerto;
        try { puerto = Integer.parseInt(puertoS); } catch (Exception e) { puerto = DbConfig.DEFAULT_PORT; }
        return DbConfig.construirUrl(host, puerto, base);
    }

    private void alternarVisibilidad() {
        if (txtPasswordExistente == null || txtPasswordExistenteVisible == null || chkMostrarPassword == null) return;
        boolean mostrar = chkMostrarPassword.isSelected();
        if (mostrar) {
            txtPasswordExistenteVisible.setText(txtPasswordExistente.getText());
            txtPasswordExistenteVisible.setVisible(true);
            txtPasswordExistenteVisible.setManaged(true);
            txtPasswordExistente.setVisible(false);
            txtPasswordExistente.setManaged(false);
        } else {
            txtPasswordExistente.setText(txtPasswordExistenteVisible.getText());
            txtPasswordExistente.setVisible(true);
            txtPasswordExistente.setManaged(true);
            txtPasswordExistenteVisible.setVisible(false);
            txtPasswordExistenteVisible.setManaged(false);
        }
    }

    private String obtenerPassword() {
        // modo producción: siempre contraseña existente
        if (chkMostrarPassword != null && chkMostrarPassword.isSelected() && txtPasswordExistenteVisible != null) {
            return txtPasswordExistenteVisible.getText() == null ? "" : txtPasswordExistenteVisible.getText();
        }
        if (txtPasswordExistente != null && txtPasswordExistente.getText() != null) {
            return txtPasswordExistente.getText();
        }
        // legacy: modo nueva (solo si el FXML antiguo lo trae)
        if (rbNueva != null && rbNueva.isSelected() && txtPasswordGenerada != null && txtPasswordGenerada.getText() != null) {
            return txtPasswordGenerada.getText();
        }
        return "";
    }

    private void probarConexion() {
        String url = construirUrlActual();
        String user = txtUser != null && txtUser.getText() != null ? txtUser.getText().trim() : "";
        String pass = obtenerPassword();
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            setStatus("Completa servidor, puerto, base, usuario y contraseña antes de probar.", true);
            return;
        }
        // validación rápida de formato antes de red
        String host = txtHost != null ? txtHost.getText().trim() : "";
        if (host.isEmpty()) {
            setStatus("Servidor vacío. Usa la IP del servidor (ej. 192.168.1.7), vendex-db o localhost.", true);
            return;
        }
        btnProbar.setDisable(true);
        setStatus("Probando conexión a " + url + " ...", false);
        new Thread(() -> {
            String diagnostico;
            try (Connection con = DriverManager.getConnection(url, user, pass)) {
                boolean ok = con.isValid(5);
                if (ok) {
                    String version = "";
                    try {
                        version = con.getMetaData().getDatabaseProductVersion();
                    } catch (Exception ignored) {}
                    final String v = version;
                    javafx.application.Platform.runLater(() -> {
                        pruebaOk = true;
                        urlProbadaOk = url;
                        setStatus("✓ Conexión exitosa a " + url
                                + (v.isEmpty() ? "" : "  (PostgreSQL " + v + ")")
                                + ". Ya puedes Guardar.", false);
                        lblStatus.setStyle("-fx-text-fill: #2e7d32;");
                        btnProbar.setDisable(false);
                        actualizarBotonGuardar();
                    });
                    return;
                }
                diagnostico = "Conexión no válida (isValid=false).";
            } catch (Exception ex) {
                diagnostico = diagnosticar(ex, host, url, pass);
            }
            final String msg = diagnostico;
            javafx.application.Platform.runLater(() -> {
                pruebaOk = false;
                urlProbadaOk = null;
                setStatus("✗ " + msg, true);
                btnProbar.setDisable(false);
                actualizarBotonGuardar();
            });
        }).start();
    }

    /** Mensajes accionables sin exponer la contraseña. */
    private String diagnosticar(Exception ex, String host, String url, String pass) {
        String raw = ex.getMessage() != null ? ex.getMessage() : ex.toString();
        if (pass != null && !pass.isEmpty() && raw.contains(pass)) raw = raw.replace(pass, "***");
        String low = raw.toLowerCase();
        String base = "Error de conexión a " + url + ": " + raw;
        if (low.contains("unknownhost") || low.contains("could not resolve") || low.contains("no such host")) {
            return base + "\n→ El nombre/IP no resuelve. Verifica la IP del servidor (ej. 192.168.1.7) o usa vendex-db con entrada en hosts.";
        }
        if (low.contains("connection refused") || low.contains("connect timed out") || low.contains("timeout") || low.contains("no route to host") || low.contains("network is unreachable")) {
            return base + "\n→ Servidor inalcanzable o puerto cerrado. Verifica: misma red/WiFi, ping al servidor, firewall TCP 5432 abierto y postgresql.conf listen_addresses='*'.";
        }
        if (low.contains("password authentication failed") || low.contains("authentication failed")) {
            return base + "\n→ Usuario o contraseña incorrectos para ese servidor. Pide la clave actual (rol app_vendex).";
        }
        if (low.contains("database") && low.contains("does not exist")) {
            return base + "\n→ La base no existe en ese servidor. Verifica el nombre (ej. dbTag). Este instalador no crea bases.";
        }
        if (low.contains("pg_hba") || low.contains("no pg_hba.conf entry")) {
            return base + "\n→ El servidor rechaza tu IP (pg_hba.conf). En el servidor autoriza: host <bd> <usuario> <tu-red>/24 scram-sha-256.";
        }
        return base + "\n→ Verifica host/puerto/BD/usuario/clave y que el servidor acepte conexiones remotas.";
    }

    private void setStatus(String msg, boolean error) {
        if (lblStatus == null) return;
        lblStatus.setText(msg);
        lblStatus.setStyle(error ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
    }

    private void guardar() {
        String url = construirUrlActual();
        String user = txtUser != null ? txtUser.getText().trim() : "";
        String pass = obtenerPassword();
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            setStatus("Servidor, usuario y contraseña son obligatorios.", true);
            return;
        }
        if (!pruebaOk || urlProbadaOk == null || !urlProbadaOk.equals(url)) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Prueba requerida");
            a.setHeaderText("Debes probar la conexión antes de guardar");
            a.setContentText("Pulsa «Probar conexión» y espera el ✓ verde. Si cambiaste algún campo, repite la prueba.");
            a.showAndWait();
            return;
        }
        if (PasswordDebilValidator.esDebil(pass)) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Contraseña débil");
            warn.setHeaderText("La contraseña parece débil o es un valor por defecto");
            warn.setContentText(PasswordDebilValidator.mensajeAdvertencia() + "\n\n¿Guardar de todos modos?");
            warn.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            var res = warn.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.YES) return;
        }
        // master opcional: solo importar si se pegó (no se genera nada en producción)
        try {
            String masterInput = txtMasterKey != null && txtMasterKey.getText() != null ? txtMasterKey.getText().trim() : "";
            if (!masterInput.isEmpty() && !com.vendex.util.SecureConfigStore.existeMasterKey()) {
                try {
                    com.vendex.util.SecureConfigStore.importarMasterKey(masterInput);
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Master key inválida: " + e.getMessage()).showAndWait();
                    return;
                }
            }
        } catch (Exception ignored) {}
        try {
            DbConfig.guardar(url, user, pass);
            DatabaseConnection.setConnectionParams(url, user, pass);
            completed = true;
            setStatus("Configuración guardada cifrada correctamente.", false);
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
        } catch (Exception ex) {
            setStatus("Error guardando: " + ex.getMessage(), true);
        }
    }

    private void copiarMasterKey() {
        if (txtMasterKey == null || txtMasterKey.getText() == null || txtMasterKey.getText().isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(txtMasterKey.getText().trim());
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Master key copiada.", false);
    }

    // ---- legacy (compatibilidad FXML antiguo) ----
    private void generarPasswordLegacy() {
        if (txtPasswordGenerada == null) return;
        txtPasswordGenerada.setText(generarPasswordFuerte());
        setStatus("Contraseña generada. Cópiala antes de guardar.", false);
    }

    private void copiarPasswordLegacy() {
        if (txtPasswordGenerada == null || txtPasswordGenerada.getText().isEmpty()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(txtPasswordGenerada.getText());
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Contraseña copiada.", false);
    }

    private void generarMasterKeyLegacy() {
        try {
            if (!com.vendex.util.SecureConfigStore.existeMasterKey()) {
                com.vendex.util.SecureConfigStore.generarNuevaMasterKey();
            }
            if (txtMasterKey != null) txtMasterKey.setText(com.vendex.util.SecureConfigStore.exportarMasterKeyParaRespaldo());
            setStatus("Master key lista. Cópiala y guárdala fuera de las PCs.", false);
        } catch (Exception e) {
            setStatus("Error generando master key: " + e.getMessage(), true);
        }
    }

    private void cancelar() {
        if (!DbConfig.getArchivo().exists()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Salir");
            confirm.setHeaderText("Sin configuración de BD la aplicación no puede iniciar");
            confirm.setContentText("¿Salir de Vendex?");
            var res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                System.exit(0);
            } else {
                return;
            }
        }
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    public boolean isCompleted() { return completed; }

    // para tests
    public static String generarPasswordFuerte() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) sb.append(ALFABETO.charAt(SECURE_RANDOM.nextInt(ALFABETO.length())));
        return sb.toString();
    }
}

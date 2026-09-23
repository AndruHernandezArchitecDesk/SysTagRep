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

public class DbSetupWizardController {

    @FXML private RadioButton rbNueva;
    @FXML private RadioButton rbExistente;
    @FXML private ToggleGroup modoGroup;
    @FXML private TextField txtUrl;
    @FXML private TextField txtUser;
    @FXML private TextField txtPasswordGenerada;
    @FXML private PasswordField txtPasswordExistente;
    @FXML private TextField txtPasswordExistenteVisible;
    @FXML private CheckBox chkMostrarPassword;
    @FXML private Button btnGenerar;
    @FXML private Button btnCopiar;
    @FXML private Button btnProbar;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblStatus;
    @FXML private Label lblInfo;
    @FXML private Label lblCopiadoAviso;
    @FXML private TextField txtMasterKey;
    @FXML private Button btnGenerarMaster;
    @FXML private Button btnCopiarMaster;
    @FXML private CheckBox chkMasterRespaldo;
    @FXML private Label lblMasterInfo;

    private boolean completed = false;
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_!@#$%&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @FXML
    public void initialize() {
        modoGroup = new ToggleGroup();
        rbNueva.setToggleGroup(modoGroup);
        rbExistente.setToggleGroup(modoGroup);
        rbNueva.setSelected(true);
        actualizarModo();

        rbNueva.setOnAction(e -> actualizarModo());
        rbExistente.setOnAction(e -> actualizarModo());

        btnGenerar.setOnAction(e -> generarPassword());
        btnCopiar.setOnAction(e -> copiarPassword());
        btnProbar.setOnAction(e -> probarConexion());
        btnGuardar.setOnAction(e -> guardar());
        btnCancelar.setOnAction(e -> cancelar());

        chkMostrarPassword.setOnAction(e -> alternarVisibilidad());

        // master key handlers (misma ventana)
        if (btnGenerarMaster != null) btnGenerarMaster.setOnAction(e -> generarMasterKey());
        if (btnCopiarMaster != null) btnCopiarMaster.setOnAction(e -> copiarMasterKey());

        // defaults
        txtUrl.setText(DbConfig.DEFAULT_URL);
        txtUser.setText(DbConfig.DEFAULT_USER);
        // si archivo ya existe, precargar url/user (no password)
        try {
            String[] cfg = DbConfig.cargar();
            if (DbConfig.getArchivo().exists()) {
                txtUrl.setText(cfg[0]);
                txtUser.setText(cfg[1]);
            }
        } catch (Exception ignored) {}

        generarPassword();
        // master key: si ya existe, mostrarla; si no, generar al vuelo para nueva instalación
        try {
            if (com.vendex.util.SecureConfigStore.existeMasterKey()) {
                txtMasterKey.setText(com.vendex.util.SecureConfigStore.exportarMasterKeyParaRespaldo());
                if (lblMasterInfo != null) lblMasterInfo.setText("Clave maestra ya existe en esta PC (tier 2).");
                if (chkMasterRespaldo != null) chkMasterRespaldo.setSelected(true);
            } else if (rbNueva.isSelected()) {
                generarMasterKey();
            }
        } catch (Exception ignored) {}
        txtPasswordExistenteVisible.setVisible(false);
        txtPasswordExistenteVisible.setManaged(false);
        if (lblCopiadoAviso != null) lblCopiadoAviso.setVisible(false);
    }

    private void actualizarModo() {
        boolean esNueva = rbNueva.isSelected();
        txtPasswordGenerada.setDisable(!esNueva);
        btnGenerar.setDisable(!esNueva);
        btnCopiar.setDisable(!esNueva);
        txtPasswordExistente.setDisable(esNueva);
        txtPasswordExistenteVisible.setDisable(esNueva);
        chkMostrarPassword.setDisable(esNueva);
        String userForMsg = txtUser.getText() == null || txtUser.getText().trim().isEmpty() ? DbConfig.DEFAULT_USER : txtUser.getText().trim();
        // master key UI por instalación (Tier 2)
        if (txtMasterKey != null) {
            txtMasterKey.setDisable(false);
            if (btnGenerarMaster != null) btnGenerarMaster.setDisable(!esNueva);
            if (chkMasterRespaldo != null) chkMasterRespaldo.setDisable(!esNueva);
            if (esNueva) {
                txtMasterKey.setPromptText("Se genera automáticamente (base64 44 chars)");
                if (lblMasterInfo != null) lblMasterInfo.setText("Master key Tier 2 (SMTP/Gemini/Nvidia): se genera con 'Generar', se muestra una sola vez. Guarda fuera de PCs.");
                if (!com.vendex.util.SecureConfigStore.existeMasterKey() && (txtMasterKey.getText()==null || txtMasterKey.getText().isBlank())) {
                    generarMasterKey();
                }
            } else {
                txtMasterKey.setPromptText("Pega la clave maestra generada en la primera PC (base64)");
                if (lblMasterInfo != null) lblMasterInfo.setText("PC adicional: pega la misma master key de la primera PC para descifrar secretos compartidos (correo).");
                if (chkMasterRespaldo != null) { chkMasterRespaldo.setSelected(false); chkMasterRespaldo.setDisable(true); }
            }
        }
        if (esNueva) {
            lblInfo.setText("Instalación nueva (" + userForMsg + " mínimo privilegio): se generará contraseña fuerte 24 chars. "
                    + "Guárdala — la usarán las demás PCs. Si es primera vez con app_vendex, ejecuta ANTES como postgres:\n"
                    + "psql -f sql/migracion_minimo_privilegio_20260920.sql\n"
                    + "Luego: CREATE ROLE " + userForMsg + " WITH LOGIN PASSWORD '***'; o ALTER ROLE " + userForMsg + " WITH PASSWORD '***'; (ver docs/permisos_bd.md)");
            btnGuardar.setText("Generar y Guardar (cifrado keyring)");
        } else {
            lblInfo.setText("PC adicional: pega la contraseña ya generada para " + userForMsg + ". "
                    + "Se probará la conexión y se guardará cifrada localmente (keyring/fallback, no portable). "
                    + "Usuario por defecto: " + DbConfig.DEFAULT_USER + " (legado postgres solo si aún no migrado).");
            btnGuardar.setText("Probar y Guardar");
        }
    }

    private void alternarVisibilidad() {
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

    private void generarPassword() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(ALFABETO.charAt(SECURE_RANDOM.nextInt(ALFABETO.length())));
        }
        String pwd = sb.toString();
        txtPasswordGenerada.setText(pwd);
        lblStatus.setText("Contraseña generada. Cópiala antes de guardar.");
        lblStatus.setStyle("-fx-text-fill: #2e7d32;");
        if (lblCopiadoAviso != null) lblCopiadoAviso.setVisible(false);
    }

    private void copiarPassword() {
        String pwd = txtPasswordGenerada.getText();
        if (pwd == null || pwd.isEmpty()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(pwd);
        Clipboard.getSystemClipboard().setContent(content);
        lblStatus.setText("Contraseña copiada al portapapeles. Guárdala en tu gestor de contraseñas.");
        lblStatus.setStyle("-fx-text-fill: #1565c0;");
        if (lblCopiadoAviso != null) {
            lblCopiadoAviso.setText("✓ Copiado — recuerda guardar en lugar seguro");
            lblCopiadoAviso.setVisible(true);
        }
    }

    private void generarMasterKey() {
        try {
            if (!com.vendex.util.SecureConfigStore.existeMasterKey()) {
                com.vendex.util.SecureConfigStore.generarNuevaMasterKey();
            }
            String b64 = com.vendex.util.SecureConfigStore.exportarMasterKeyParaRespaldo();
            txtMasterKey.setText(b64);
            lblStatus.setText("Master key generada. Cópiala y guárdala fuera de las PCs.");
            lblStatus.setStyle("-fx-text-fill: #2e7d32;");
            if (lblMasterInfo != null) lblMasterInfo.setText("Master key (44 chars base64) generada. Copia y guarda en gestor externo. Sin respaldo, Tier2 irrecuperable.");
        } catch (Exception e) {
            lblStatus.setText("Error generando master key: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: #c62828;");
        }
    }

    private void copiarMasterKey() {
        String mk = txtMasterKey.getText();
        if (mk == null || mk.isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(mk.trim());
        Clipboard.getSystemClipboard().setContent(content);
        lblStatus.setText("Master key copiada. Guárdala fuera de las PCs.");
        lblStatus.setStyle("-fx-text-fill: #1565c0;");
    }

    private void probarConexion() {
        String url = txtUrl.getText() == null ? "" : txtUrl.getText().trim();
        String user = txtUser.getText() == null ? "" : txtUser.getText().trim();
        String pass = obtenerPasswordActual();
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Completa URL, usuario y contraseña antes de probar.");
            lblStatus.setStyle("-fx-text-fill: #c62828;");
            return;
        }
        if (PasswordDebilValidator.esDebil(pass)) {
            lblStatus.setText("Advertencia: contraseña débil (ej. 'admin'). " + PasswordDebilValidator.mensajeAdvertencia());
            lblStatus.setStyle("-fx-text-fill: #e65100;");
            // no bloquea, continua prueba
        }
        btnProbar.setDisable(true);
        lblStatus.setText("Probando conexión...");
        lblStatus.setStyle("");
        // Ejecutar en background para no bloquear UI
        new Thread(() -> {
            try (Connection con = DriverManager.getConnection(url, user, pass)) {
                boolean ok = con.isValid(5);
                javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        lblStatus.setText("Conexión exitosa a: " + url);
                        lblStatus.setStyle("-fx-text-fill: #2e7d32;");
                    } else {
                        lblStatus.setText("Conexión falló: no válida.");
                        lblStatus.setStyle("-fx-text-fill: #c62828;");
                    }
                    btnProbar.setDisable(false);
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    // no incluir password en mensaje
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    // sanitizar si contiene password por accidente
                    if (pass != null && !pass.isEmpty() && msg.contains(pass)) msg = msg.replace(pass, "***");
                    lblStatus.setText("Error de conexión: " + msg);
                    lblStatus.setStyle("-fx-text-fill: #c62828;");
                    btnProbar.setDisable(false);
                });
            }
        }).start();
    }

    private String obtenerPasswordActual() {
        if (rbNueva.isSelected()) {
            return txtPasswordGenerada.getText() == null ? "" : txtPasswordGenerada.getText();
        } else {
            if (chkMostrarPassword.isSelected()) {
                return txtPasswordExistenteVisible.getText() == null ? "" : txtPasswordExistenteVisible.getText();
            } else {
                return txtPasswordExistente.getText() == null ? "" : txtPasswordExistente.getText();
            }
        }
    }

    private void guardar() {
        String url = txtUrl.getText() == null ? "" : txtUrl.getText().trim();
        String user = txtUser.getText() == null ? "" : txtUser.getText().trim();
        String pass = obtenerPasswordActual();
        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("URL, usuario y contraseña son obligatorios.");
            lblStatus.setStyle("-fx-text-fill: #c62828;");
            return;
        }
        if (PasswordDebilValidator.esDebil(pass)) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Contraseña débil");
            warn.setHeaderText("La contraseña parece débil o es un valor por defecto");
            warn.setContentText(PasswordDebilValidator.mensajeAdvertencia() + "\n\n¿Guardar de todos modos?");
            warn.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            var res = warn.showAndWait();
            if (res.isEmpty() || res.get() != ButtonType.YES) {
                return;
            }
        }
        // manejo clave maestra por instalación (Tier 2)
        String masterInput = txtMasterKey != null ? txtMasterKey.getText() : null;
        boolean esNueva = rbNueva.isSelected();
        if (esNueva) {
            if (!com.vendex.util.SecureConfigStore.existeMasterKey()) {
                if (masterInput == null || masterInput.isBlank()) {
                    generarMasterKey();
                    masterInput = txtMasterKey.getText();
                } else {
                    try { com.vendex.util.SecureConfigStore.importarMasterKey(masterInput.trim()); } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Master key inválida: " + e.getMessage()).showAndWait(); return;
                    }
                }
            }
            if (chkMasterRespaldo != null && !chkMasterRespaldo.isSelected()) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Respaldo master key");
                warn.setHeaderText("Confirma respaldo externo");
                warn.setContentText("La clave maestra solo vive en el keyring local de cada PC. Si se pierden todas las PCs sin respaldo, los secretos Tier 2 (correo) quedarán irrecuperables.\n\n¿Confirmas que ya guardaste la master key fuera de las PCs?");
                warn.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                var r = warn.showAndWait();
                if (r.isEmpty() || r.get() != ButtonType.YES) return;
            }
        } else {
            // PC adicional: importar master si se pegó
            if (masterInput != null && !masterInput.isBlank()) {
                try { com.vendex.util.SecureConfigStore.importarMasterKey(masterInput.trim()); }
                catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Master key inválida: " + e.getMessage()).showAndWait(); return; }
            } else if (!com.vendex.util.SecureConfigStore.existeMasterKey()) {
                new Alert(Alert.AlertType.WARNING, "Falta master key: pega la clave generada en la primera PC para descifrar secretos compartidos.").showAndWait();
                // no bloquea guardar db, pero advierte
            }
        }
        try {
            // probar conexión antes de guardar
            try (Connection con = DriverManager.getConnection(url, user, pass)) {
                if (!con.isValid(5)) throw new RuntimeException("Conexión no válida");
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                if (pass != null && !pass.isEmpty() && msg.contains(pass)) msg = msg.replace(pass, "***");
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error de conexión");
                err.setHeaderText("No se pudo conectar con esos datos");
                err.setContentText(msg + "\n\nVerifica host/puerto/BD y que el rol exista. Si es instalación nueva, crea el rol con:\nCREATE ROLE " + user + " WITH LOGIN PASSWORD '***';");
                err.showAndWait();
                return;
            }
            DbConfig.guardar(url, user, pass);
            // actualizar DatabaseConnection en memoria
            DatabaseConnection.setConnectionParams(url, user, pass);
            if (rbNueva.isSelected()) {
                String masterForShow = "";
                try { masterForShow = com.vendex.util.SecureConfigStore.exportarMasterKeyParaRespaldo(); } catch (Exception ignored) {}
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Contraseña y master key generadas");
                info.setHeaderText("Guarda ambas en lugar seguro (fuera de PCs)");
                info.setContentText("DB para la instalación:\n" + pass
                        + "\n\nMaster key instalación (Tier2):\n" + masterForShow
                        + "\n\nDB guardada cifrada en:\n" + DbConfig.getArchivo().getAbsolutePath()
                        + "\nMaster key guardada como Tier1 'vendex-master-key' (keyring/fallback) + backup ~/.install-master.key.bak"
                        + "\n\nLas demás PCs deben pegar AMBAS en su wizard (PC adicional).\n"
                        + "Recuerda ejecutar en Postgres si el rol no existe:\nCREATE ROLE " + user + " WITH LOGIN PASSWORD '***';\n"
                        + "o ALTER ROLE " + user + " WITH PASSWORD '***';\n\n"
                        + "Sin respaldo master, secretos Tier2 (correo) irrecuperables si se pierden PCs.");
                info.showAndWait();
            }
            completed = true;
            lblStatus.setText("Configuración guardada cifrada correctamente.");
            lblStatus.setStyle("-fx-text-fill: #2e7d32;");
            Stage stage = (Stage) btnGuardar.getScene().getWindow();
            stage.close();
        } catch (Exception ex) {
            lblStatus.setText("Error guardando: " + ex.getMessage());
            lblStatus.setStyle("-fx-text-fill: #c62828;");
        }
    }

    private void cancelar() {
        // si no existe config, no permitir cancelar sin guardar — salir de app
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

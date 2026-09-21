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
        if (esNueva) {
            lblInfo.setText("Instalación nueva: se generará una contraseña fuerte (24 caracteres). "
                    + "Guárdala en lugar seguro — la necesitarán las demás PCs que se conecten a esta BD. "
                    + "Después ejecútala en Postgres: ALTER ROLE " + txtUser.getText().trim() + " WITH PASSWORD '...';");
            btnGuardar.setText("Generar y Guardar (cifrado)");
        } else {
            lblInfo.setText("PC adicional: pega la contraseña ya generada para esta instalación. "
                    + "Se probará la conexión y se guardará cifrada localmente (AES/GCM). "
                    + "Recuerda: cada PC cifra con su propia clave local, el archivo no es portable.");
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
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Contraseña generada");
                info.setHeaderText("Guarda esta contraseña en lugar seguro");
                info.setContentText("Contraseña para la instalación:\n" + pass
                        + "\n\nSe ha guardado cifrada en:\n" + DbConfig.getArchivo().getAbsolutePath()
                        + "\n\nLas demás PCs deben pegarla en su propio wizard (PC adicional).\n"
                        + "Recuerda ejecutar en Postgres si el rol no existe:\nCREATE ROLE " + user + " WITH LOGIN PASSWORD '***';\n"
                        + "o ALTER ROLE " + user + " WITH PASSWORD '***';\n\n"
                        + "Rotación: repite este wizard en cada PC cuando cambies la contraseña.");
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

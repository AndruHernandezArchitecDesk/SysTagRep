package com.vendex.controller;

import com.vendex.dao.ConfiguracionEmailDAO;
import com.vendex.dao.LogDAO;
import com.vendex.model.ConfiguracionEmail;
import com.vendex.util.Cifrado;
import com.vendex.util.EmailService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import com.vendex.dao.LogDAOPostgres;
import com.vendex.dao.ConfiguracionEmailDAOPostgres;

public class ConfiguracionEmailController implements Initializable {

    @FXML private TextField txtHost;
    @FXML private TextField txtPuerto;
    @FXML private CheckBox chkTls;
    @FXML private TextField txtEmailRemitente;
    @FXML private TextField txtNombreRemitente;
    @FXML private TextField txtUsuarioSmtp;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtReplyTo;
    @FXML private TextField txtCorreoPrueba;
    @FXML private Label lblEstado;
    @FXML private Button btnGuardar;
    @FXML private Button btnProbarConexion;
    @FXML private Button btnEnviarPrueba;

    private final ConfiguracionEmailDAO dao = new ConfiguracionEmailDAOPostgres();
    private final LogDAO logDAO = new LogDAOPostgres();
    private ConfiguracionEmail configuracionActual;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarConfiguracion();
        txtPuerto.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("\\d*")) txtPuerto.setText(val.replaceAll("[^\\d]", ""));
        });
    }

    private void cargarConfiguracion() {
        try {
            Optional<ConfiguracionEmail> opt = dao.obtenerActiva();
            if (opt.isPresent()) {
                configuracionActual = opt.get();
                txtHost.setText(configuracionActual.getHostSmtp());
                txtPuerto.setText(String.valueOf(configuracionActual.getPuertoSmtp()));
                chkTls.setSelected(configuracionActual.isUsarTls());
                txtEmailRemitente.setText(configuracionActual.getEmailRemitente());
                txtNombreRemitente.setText(configuracionActual.getNombreRemitente());
                txtUsuarioSmtp.setText(configuracionActual.getUsuarioSmtp());
                txtPassword.setPromptText("•••••••• (dejar vacío para conservar)");
                txtPassword.setText("");
                txtReplyTo.setText(configuracionActual.getReplyTo() != null ? configuracionActual.getReplyTo() : "");
                lblEstado.setText("Configuración cargada (actualizada: " + configuracionActual.getActualizadoEn() + ")");
                lblEstado.setStyle("-fx-text-fill: #198754; -fx-font-size: 12px;");
            } else {
                lblEstado.setText("Sin configuración activa — complete los datos y guarde.");
                lblEstado.setStyle("-fx-text-fill: #856404; -fx-font-size: 12px;");
                chkTls.setSelected(true);
                txtPuerto.setText("587");
            }
        } catch (Exception e) {
            logDAO.guardar("ConfiguracionEmailController", "cargarConfiguracion", e.getMessage(), e);
            lblEstado.setText("Error cargando configuración: " + e.getMessage());
            lblEstado.setStyle("-fx-text-fill: #dc3545;");
        }
    }

    @FXML
    private void guardar() {
        String host = txtHost.getText() == null ? "" : txtHost.getText().trim();
        String puertoStr = txtPuerto.getText() == null ? "" : txtPuerto.getText().trim();
        String email = txtEmailRemitente.getText() == null ? "" : txtEmailRemitente.getText().trim();
        String nombre = txtNombreRemitente.getText() == null ? "" : txtNombreRemitente.getText().trim();
        String usuario = txtUsuarioSmtp.getText() == null ? "" : txtUsuarioSmtp.getText().trim();
        String passPlano = txtPassword.getText() == null ? "" : txtPassword.getText();
        String replyTo = txtReplyTo.getText() == null ? "" : txtReplyTo.getText().trim();

        StringBuilder err = new StringBuilder();
        if (host.isEmpty()) err.append("- Host SMTP es obligatorio.\n");
        int puerto = 587;
        try {
            if (puertoStr.isEmpty()) err.append("- Puerto SMTP es obligatorio.\n");
            else {
                puerto = Integer.parseInt(puertoStr);
                if (puerto < 1 || puerto > 65535) err.append("- Puerto fuera de rango (1-65535).\n");
                else if (puerto == 25) err.append("⚠ Puerto 25 suele estar bloqueado sin cifrar; se recomienda 587 (TLS) o 465 (SSL).\n");
            }
        } catch (NumberFormatException ex) { err.append("- Puerto debe ser numérico.\n"); }
        if (email.isEmpty()) err.append("- Email remitente es obligatorio.\n");
        else if (!email.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) err.append("- Email remitente con formato inválido.\n");
        if (nombre.isEmpty()) err.append("- Nombre remitente es obligatorio.\n");
        if (usuario.isEmpty()) err.append("- Usuario SMTP es obligatorio.\n");
        boolean esNuevo = configuracionActual == null;
        if (esNuevo && passPlano.isEmpty()) err.append("- Contraseña es obligatoria (configuración nueva).\n");
        if (!replyTo.isEmpty() && !replyTo.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) err.append("- Reply-To con formato inválido.\n");

        String msgErr = err.toString();
        boolean soloAdvertencia = msgErr.contains("⚠") && !msgErr.contains("- ");
        if (!msgErr.isEmpty() && !soloAdvertencia) {
            new Alert(Alert.AlertType.WARNING, msgErr).showAndWait();
            return;
        }
        if (soloAdvertencia) {
            Alert adv = new Alert(Alert.AlertType.CONFIRMATION, msgErr + "\n¿Guardar de todos modos?", ButtonType.YES, ButtonType.NO);
            if (adv.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        try {
            ConfiguracionEmail cfg = configuracionActual != null ? configuracionActual : new ConfiguracionEmail();
            cfg.setHostSmtp(host);
            cfg.setPuertoSmtp(puerto);
            cfg.setUsarTls(chkTls.isSelected());
            cfg.setEmailRemitente(email);
            cfg.setNombreRemitente(nombre);
            cfg.setUsuarioSmtp(usuario);
            cfg.setReplyTo(replyTo.isEmpty() ? null : replyTo);
            cfg.setActivo(true);
            if (LoginController.usuarioAutenticado != null) cfg.setActualizadoPor(LoginController.usuarioAutenticado.getId());

            if (!passPlano.isEmpty()) {
                cfg.setPasswordCifrado(com.vendex.dao.ConfiguracionEmailDAOPostgres.cifrarParaGuardar(passPlano));
            } else if (configuracionActual != null && configuracionActual.getPasswordCifrado() != null) {
                cfg.setPasswordCifrado(configuracionActual.getPasswordCifrado());
            }

            dao.guardar(cfg);
            configuracionActual = cfg;
            // recargar cache para que próximo envío use nueva config sin reiniciar
            EmailService.recargarConfiguracion();
            // recargar desde BD para obtener id/timestamp
            cargarConfiguracion();
            new Alert(Alert.AlertType.INFORMATION, "Configuración de correo guardada correctamente.").showAndWait();
            lblEstado.setText("Guardado correctamente.");
            lblEstado.setStyle("-fx-text-fill: #198754;");
        } catch (Exception e) {
            logDAO.guardar("ConfiguracionEmailController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void probarConexion() {
        ConfiguracionEmail cfg = construirDesdeFormulario();
        if (cfg == null) return;
        String passPlano = txtPassword.getText() == null ? "" : txtPassword.getText();
        if (passPlano.isEmpty() && configuracionActual != null) {
            passPlano = dao.obtenerPasswordPlano(configuracionActual);
        }
        if (passPlano.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Ingrese la contraseña para probar la conexión.").showAndWait();
            return;
        }
        String passFinal = passPlano;
        btnProbarConexion.setDisable(true);
        lblEstado.setText("Probando conexión a " + cfg.getHostSmtp() + ":" + cfg.getPuertoSmtp() + "...");
        lblEstado.setStyle("-fx-text-fill: #0d6efd;");
        Task<String> task = new Task<>() {
            @Override protected String call() { return dao.probarConexion(cfg, passFinal); }
        };
        task.setOnSucceeded(e -> {
            btnProbarConexion.setDisable(false);
            String err = task.getValue();
            if (err == null) {
                lblEstado.setText("✓ Conexión exitosa a " + cfg.getHostSmtp() + ":" + cfg.getPuertoSmtp());
                lblEstado.setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                new Alert(Alert.AlertType.INFORMATION, "Conexión SMTP exitosa. Credenciales y host válidos.").showAndWait();
            } else {
                lblEstado.setText("✗ " + err);
                lblEstado.setStyle("-fx-text-fill: #dc3545;");
                new Alert(Alert.AlertType.ERROR, err).showAndWait();
            }
        });
        task.setOnFailed(e -> {
            btnProbarConexion.setDisable(false);
            lblEstado.setText("Error: " + task.getException().getMessage());
            lblEstado.setStyle("-fx-text-fill: #dc3545;");
        });
        new Thread(task, "Test-SMTP-Conexion").start();
    }

    @FXML
    private void enviarCorreoPrueba() {
        String destino = txtCorreoPrueba.getText() == null ? "" : txtCorreoPrueba.getText().trim();
        if (destino.isEmpty() || !destino.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            new Alert(Alert.AlertType.WARNING, "Ingrese un correo de destino válido para la prueba.").showAndWait();
            return;
        }
        ConfiguracionEmail cfg = construirDesdeFormulario();
        if (cfg == null) return;
        String passPlano = txtPassword.getText() == null ? "" : txtPassword.getText();
        if (passPlano.isEmpty() && configuracionActual != null) passPlano = dao.obtenerPasswordPlano(configuracionActual);
        if (passPlano.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Ingrese la contraseña para enviar la prueba.").showAndWait();
            return;
        }
        String passFinal = passPlano;
        btnEnviarPrueba.setDisable(true);
        lblEstado.setText("Enviando correo de prueba a " + destino + "...");
        lblEstado.setStyle("-fx-text-fill: #0d6efd;");
        Task<String> task = new Task<>() {
            @Override protected String call() { return dao.probarEnvio(cfg, passFinal, destino); }
        };
        task.setOnSucceeded(e -> {
            btnEnviarPrueba.setDisable(false);
            String err = task.getValue();
            if (err == null) {
                lblEstado.setText("✓ Correo de prueba enviado a " + destino);
                lblEstado.setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                new Alert(Alert.AlertType.INFORMATION, "Correo de prueba enviado a " + destino + ".\nRevise su bandeja (y spam).").showAndWait();
            } else {
                lblEstado.setText("✗ " + err);
                lblEstado.setStyle("-fx-text-fill: #dc3545;");
                new Alert(Alert.AlertType.ERROR, err).showAndWait();
            }
        });
        task.setOnFailed(e -> {
            btnEnviarPrueba.setDisable(false);
            lblEstado.setText("Error: " + task.getException().getMessage());
        });
        new Thread(task, "Test-SMTP-Envio").start();
    }

    private ConfiguracionEmail construirDesdeFormulario() {
        String host = txtHost.getText() == null ? "" : txtHost.getText().trim();
        String puertoStr = txtPuerto.getText() == null ? "" : txtPuerto.getText().trim();
        String email = txtEmailRemitente.getText() == null ? "" : txtEmailRemitente.getText().trim();
        String nombre = txtNombreRemitente.getText() == null ? "" : txtNombreRemitente.getText().trim();
        String usuario = txtUsuarioSmtp.getText() == null ? "" : txtUsuarioSmtp.getText().trim();
        String replyTo = txtReplyTo.getText() == null ? "" : txtReplyTo.getText().trim();
        if (host.isEmpty() || puertoStr.isEmpty() || email.isEmpty() || usuario.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Complete host, puerto, email remitente y usuario antes de probar.").showAndWait();
            return null;
        }
        int puerto;
        try { puerto = Integer.parseInt(puertoStr); } catch (Exception ex) {
            new Alert(Alert.AlertType.WARNING, "Puerto inválido.").showAndWait(); return null;
        }
        ConfiguracionEmail c = new ConfiguracionEmail();
        c.setHostSmtp(host);
        c.setPuertoSmtp(puerto);
        c.setUsarTls(chkTls.isSelected());
        c.setEmailRemitente(email);
        c.setNombreRemitente(nombre.isEmpty() ? "Vendex Repuestos" : nombre);
        c.setUsuarioSmtp(usuario);
        c.setReplyTo(replyTo.isEmpty() ? null : replyTo);
        return c;
    }
}
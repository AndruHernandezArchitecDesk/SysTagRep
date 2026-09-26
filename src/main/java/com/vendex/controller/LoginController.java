package com.vendex.controller;

import com.vendex.dao.LoginIntentoLogDAO;
import com.vendex.dao.UsuarioDAO;
import com.vendex.model.Usuario;
import com.vendex.util.PoliticaBloqueo;
import com.vendex.util.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import com.vendex.dao.UsuarioDAOPostgres;

public class LoginController implements Initializable {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private ImageView imgLogo;
    @FXML private Button btnIngresar;
    @FXML private ProgressIndicator spinner;
    @FXML private Label lblStatus;

    public static Usuario usuarioAutenticado;

    private final UsuarioDAO dao = new UsuarioDAOPostgres();
    private final LoginIntentoLogDAO logDao = new LoginIntentoLogDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (imgLogo != null) {
            try { imgLogo.setImage(new Image(getClass().getResourceAsStream(ThemeManager.getLogoPath()))); } catch (Exception ignored) {}
        }
        txtPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) ingresar();
        });
        txtUsuario.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) txtPassword.requestFocus();
        });
        if (txtUsuario.getScene() != null) {
            ThemeManager.aplicarTemaGuardado(txtUsuario.getScene());
        } else {
            txtUsuario.sceneProperty().addListener((obs, old, scene) -> {
                if (scene != null) ThemeManager.aplicarTemaGuardado(scene);
            });
        }
        if (spinner != null) spinner.setVisible(false);
        if (lblStatus != null) lblStatus.setText("");
    }

    @FXML
    private void ingresar() {
        String user = txtUsuario.getText();
        String pass = txtPassword.getText();

        if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            showStatus("Ingrese usuario y contraseña.", true);
            return;
        }
        String usuarioInput = user.trim();
        // UI spinner (punto 3 con spinner)
        setLoading(true);
        String equipo = obtenerEquipo();

        Task<LoginResult> task = new Task<>() {
            @Override
            protected LoginResult call() throws Exception {
                // 0. Verificar conexión BD antes de lógica anti fuerza bruta (evita spam si db.properties tiene credenciales erróneas)
                try (java.sql.Connection testCon = com.vendex.config.DatabaseConnection.getConnection()) {
                    if (!testCon.isValid(2)) throw new RuntimeException("No se pudo validar conexión BD");
                } catch (Exception e) {
                    String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    if (m.contains("password authentication failed") || m.contains("fatal") || m.contains("role") && m.contains("does not exist")) {
                        throw new RuntimeException("Error de configuración BD (db.properties): " + e.getMessage()
                                + "\nVerifica ~/.vendex/db.properties (usuario/contraseña) o ejecuta el wizard de BD (borra el archivo y reinicia). "
                                + "Si migraste a app_vendex, asegúrate de ejecutar sql/migracion_minimo_privilegio_20260920.sql como postgres.", e);
                    }
                    throw e;
                }
                // 1. Buscar usuario (FOR UPDATE para bloqueo por cuenta en BD central, no por PC)
                Usuario u;
                try {
                    u = dao.buscarPorUsername(usuarioInput);
                } catch (Exception e) {
                    // error de conexión ya manejado arriba, pero por si acaso
                    String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    if (m.contains("password authentication failed") || m.contains("fatal")) throw e;
                    throw new RuntimeException("Error consultando usuarios: " + e.getMessage(), e);
                }

                // Usuario no existe → mensaje genérico (evitar enumeración), log, throttling por input
                if (u == null) {
                    int fallosRecientes = logDao.contarFallosRecientes(usuarioInput, PoliticaBloqueo.ventanaMinutos());
                    long demora = PoliticaBloqueo.demoraMs(fallosRecientes + 1);
                    if (demora > 0) Thread.sleep(demora);
                    logDao.registrar(usuarioInput, false, equipo);
                    return LoginResult.generico();
                }

                // Cuenta inactiva → tratar como genérico
                if (!u.isEstado()) {
                    long demora = PoliticaBloqueo.demoraMs(u.getIntentosFallidos() + 1);
                    if (demora > 0) Thread.sleep(demora);
                    logDao.registrar(usuarioInput, false, equipo);
                    return LoginResult.generico();
                }

                // Ventana: si último fallo fue hace > ventana, resetear contador
                if (u.getUltimoIntentoFallido() != null && !PoliticaBloqueo.dentroVentana(u.getUltimoIntentoFallido())) {
                    dao.resetearSiVentanaExpirada(u.getId(), u.getUltimoIntentoFallido(), PoliticaBloqueo.ventanaMinutos());
                    u.setIntentosFallidos(0);
                    u.setBloqueadoHasta(null);
                }

                // ¿Bloqueada?
                if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                    logDao.registrar(usuarioInput, false, equipo);
                    long mins = PoliticaBloqueo.minutosRestantes(u.getBloqueadoHasta());
                    return LoginResult.bloqueada(mins);
                }
                // Si bloqueo expiró, limpiar
                if (u.getBloqueadoHasta() != null && !u.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                    dao.desbloquear(u.getId());
                    u.setIntentosFallidos(0);
                    u.setBloqueadoHasta(null);
                }

                // Validar password (SHA-256 actual, pendiente bcrypt aparte)
                boolean ok = dao.verificarPassword(u, pass);
                if (ok) {
                    dao.resetearIntentos(u.getId());
                    logDao.registrar(usuarioInput, true, equipo);
                    // recargar usuario actualizado
                    Usuario fresh = dao.buscarPorUsername(usuarioInput);
                    return LoginResult.exito(fresh != null ? fresh : u);
                } else {
                    // incrementar fallo
                    dao.incrementarIntentoFallido(u.getId());
                    int nuevoIntentos = u.getIntentosFallidos() + 1;
                    // si alcanzó umbral → bloquear con backoff exponencial
                    int umbral = PoliticaBloqueo.intentosPermitidos();
                    if (nuevoIntentos >= umbral) {
                        // bloqueosPrevios estimado por exceso sobre umbral
                        int bloqueosPrevios = Math.max(0, nuevoIntentos - umbral);
                        java.time.LocalDateTime hasta = PoliticaBloqueo.calcularBloqueadoHasta(nuevoIntentos, bloqueosPrevios);
                        dao.bloquear(u.getId(), hasta);
                    }
                    long demora = PoliticaBloqueo.demoraMs(nuevoIntentos);
                    if (demora > 0) Thread.sleep(demora);
                    logDao.registrar(usuarioInput, false, equipo);
                    // El mensaje sigue siendo genérico hasta que el siguiente intento detecte el bloqueo
                    // Si justo se bloqueó, el siguiente login mostrará el mensaje de bloqueada
                    return LoginResult.generico();
                }
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            LoginResult r = task.getValue();
            if (r.exito != null) {
                usuarioAutenticado = r.exito;
                // cargar permisos granulares en sesión (una sola vez, servicio enforces)
                try { com.vendex.util.SesionActual.iniciar(r.exito); } catch (Exception ignored) {}
                abrirMain();
            } else if (r.bloqueada) {
                showStatus("Cuenta bloqueada temporalmente por múltiples intentos fallidos, intenta de nuevo en " + r.minutos + " minuto(s).", true);
            } else {
                showStatus("Usuario o contraseña incorrectos.", true);
            }
        });
        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            showStatus("Error de conexión a la base de datos: " + msg, true);
            ex.printStackTrace();
        });
        new Thread(task, "login-task").start();
    }

    private void setLoading(boolean loading) {
        if (btnIngresar != null) btnIngresar.setDisable(loading);
        if (spinner != null) spinner.setVisible(loading);
        if (lblStatus != null && loading) lblStatus.setText("Verificando...");
        if (txtUsuario != null) txtUsuario.setDisable(loading);
        if (txtPassword != null) txtPassword.setDisable(loading);
    }

    private void showStatus(String msg, boolean error) {
        if (lblStatus != null) {
            lblStatus.setText(msg);
            lblStatus.setStyle(error ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
        } else {
            new Alert(error ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION, msg).showAndWait();
        }
    }

    private String obtenerEquipo() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            String ip = InetAddress.getLocalHost().getHostAddress();
            String user = System.getProperty("user.name", "");
            String equipo = host + " (" + ip + ")";
            if (!user.isBlank()) equipo += " [" + user + "]";
            if (equipo.length() > 150) equipo = equipo.substring(0, 150);
            return equipo;
        } catch (Exception e) {
            return System.getProperty("user.name", "desconocido");
        }
    }

    private static class LoginResult {
        Usuario exito;
        boolean bloqueada;
        long minutos;
        static LoginResult exito(Usuario u) { LoginResult r=new LoginResult(); r.exito=u; return r; }
        static LoginResult generico() { return new LoginResult(); }
        static LoginResult bloqueada(long m) { LoginResult r=new LoginResult(); r.bloqueada=true; r.minutos=m; return r; }
    }

    private void abrirMain() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setTitle("Tag Repuestos Automotrices");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/inventario.png")));
            Scene scene = new Scene(root);
            ThemeManager.aplicarTemaGuardado(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
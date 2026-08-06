package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.UsuarioDAO;
import com.tag.sysTagRep.model.Usuario;
import com.tag.sysTagRep.util.ScrambleText;
import com.tag.sysTagRep.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Text txtTitulo;

    public static Usuario usuarioAutenticado;

    private final UsuarioDAO dao = new UsuarioDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        new ScrambleText(txtTitulo, "TAG Repuestos Automotrices").play();
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
                if (scene != null) {
                    ThemeManager.aplicarTemaGuardado(scene);
                }
            });
        }
    }

    @FXML
    private void ingresar() {
        String user = txtUsuario.getText();
        String pass = txtPassword.getText();

        if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Ingrese usuario y contraseña.").showAndWait();
            return;
        }

        try {
            Usuario u = dao.autenticar(user.trim(), pass);
            if (u != null) {
                usuarioAutenticado = u;
                abrirMain();
            } else {
                new Alert(Alert.AlertType.ERROR, "Usuario o contraseña incorrectos.").showAndWait();
            }
        } catch (RuntimeException e) {
            String error = "Error de conexión a la base de datos:\n" + e.getMessage();
            new Alert(Alert.AlertType.ERROR, error).showAndWait();
            e.printStackTrace();
        }
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

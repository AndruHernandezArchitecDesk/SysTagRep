package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.UsuarioDAO;
import com.tag.sysTagRep.model.Usuario;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;

    public static Usuario usuarioAutenticado;

    private final UsuarioDAO dao = new UsuarioDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) ingresar();
        });
        txtUsuario.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) txtPassword.requestFocus();
        });
    }

    @FXML
    private void ingresar() {
        String user = txtUsuario.getText();
        String pass = txtPassword.getText();

        if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Ingrese usuario y contraseña.").showAndWait();
            return;
        }

        Usuario u = dao.autenticar(user.trim(), pass);
        if (u != null) {
            usuarioAutenticado = u;
            abrirMain();
        } else {
            new Alert(Alert.AlertType.ERROR, "Usuario o contraseña incorrectos.").showAndWait();
        }
    }

    private void abrirMain() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setTitle("Tag Repuestos Automotrices");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/inventario.png")));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

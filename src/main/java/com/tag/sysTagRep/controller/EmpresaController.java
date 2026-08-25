package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.model.Empresa;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EmpresaController implements Initializable {

    private final EmpresaDAO empresaDAO = new EmpresaDAO();
    private Empresa empresaActual;

    @FXML private TextField txtRazonSocial;
    @FXML private TextField txtTitulo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtSucursal;
    @FXML private ImageView imgLogo;
    @FXML private TextField txtLogoUrl;
    @FXML private Button btnNuevo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cargarDatosEmpresa();
        btnNuevo.setOnAction(event -> nuevo());
        btnGuardar.setOnAction(event -> guardar());
        btnCancelar.setOnAction(event -> cancelar());
    }

    private void cargarDatosEmpresa() {
        List<Empresa> empresas = empresaDAO.listar();
        if (!empresas.isEmpty()) {
            empresaActual = empresas.get(0);
            txtRazonSocial.setText(empresaActual.getRazonSocial());
            txtTitulo.setText(empresaActual.getTitulo());
            txtTelefono.setText(empresaActual.getTelefono());
            txtCorreo.setText(empresaActual.getCorreo());
            txtDireccion.setText(empresaActual.getDireccionCallePrincipal() + " y " + empresaActual.getDireccionCalleSecundaria());
            txtSucursal.setText(empresaActual.getSucursal());

            String logoUrl = empresaActual.getLogoUrl();
            if (logoUrl != null && !logoUrl.isEmpty()) {
                Image image = new Image(getClass().getResourceAsStream(logoUrl));
                imgLogo.setImage(image);
                txtLogoUrl.setText(logoUrl);
            }
        }
    }

    @FXML
    private void cambiarLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            imgLogo.setImage(image);
            txtLogoUrl.setText(file.toURI().toString());
        }
    }

    private void nuevo() {
        txtRazonSocial.setText("");
        txtTitulo.setText("");
        txtTelefono.setText("");
        txtCorreo.setText("");
        txtDireccion.setText("");
        txtSucursal.setText("");
        imgLogo.setImage(null);
        empresaActual = null;
    }

    private void guardar() {
        try {
            if (empresaActual == null) {
                new Alert(Alert.AlertType.WARNING, "No hay datos de empresa cargados.").showAndWait();
                return;
            }

            empresaActual.setRazonSocial(txtRazonSocial.getText().trim());
            empresaActual.setTitulo(txtTitulo.getText().trim());
            empresaActual.setTelefono(txtTelefono.getText().trim());
            empresaActual.setCorreo(txtCorreo.getText().trim());
            empresaActual.setDireccionCallePrincipal(txtDireccion.getText().trim());
            // Note: sucursal se toma de txtSucursal

            boolean guardado = empresaDAO.actualizar(empresaActual);
            if (guardado) {
                new Alert(Alert.AlertType.INFORMATION, "Empresa actualizada correctamente.").showAndWait();
                cargarDatosEmpresa();
            } else {
                new Alert(Alert.AlertType.ERROR, "Error al actualizar la empresa.").showAndWait();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    private void cancelar() {
        cargarDatosEmpresa();
    }
}
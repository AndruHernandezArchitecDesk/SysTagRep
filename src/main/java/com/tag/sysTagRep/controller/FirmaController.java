package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.dao.LogDAO;
import com.tag.sysTagRep.util.ConfigFirma;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class FirmaController implements Initializable {

    @FXML private Label lblEstado;
    @FXML private TextField txtRutaP12;
    @FXML private PasswordField txtClaveP12;
    @FXML private CheckBox chkTerminos;

    private final LogDAO logDAO = new LogDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String[] firma = ConfigFirma.cargar();
        boolean configurada = !firma[0].isEmpty() && !firma[1].isEmpty();
        if (configurada) {
            txtRutaP12.setText(firma[0]);
            txtClaveP12.setText(firma[1]);
            lblEstado.setText("Firma configurada: " + firma[0]);
            lblEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
            if (ConfigFirma.terminosAceptados()) {
                chkTerminos.setSelected(true);
            }
        }
    }

    @FXML
    private void seleccionarFirma() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar certificado digital");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Certificado digital (*.p12, *.pfx)", "*.p12", "*.pfx"));
        File archivo = fc.showOpenDialog(txtRutaP12.getScene().getWindow());
        if (archivo != null) {
            txtRutaP12.setText(archivo.getAbsolutePath());
        }
    }

    @FXML
    private void abrirTerminos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/TerminosView.fxml"));
            Parent vista = loader.load();
            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Términos y Condiciones");
            modal.setScene(new Scene(vista, 560, 480));
            modal.showAndWait();
        } catch (Exception e) {
            logDAO.guardar("FirmaController", "abrirTerminos", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al abrir términos: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void guardar() {
        try {
            String ruta = txtRutaP12.getText() == null ? "" : txtRutaP12.getText().trim();
            String clave = txtClaveP12.getText() == null ? "" : txtClaveP12.getText();

            String msg = "";
            if (ruta.isEmpty()) msg += "- Debe seleccionar el archivo .p12 del certificado.\n";
            if (clave.isEmpty()) msg += "- Debe ingresar la contraseña de la firma.\n";
            if (!chkTerminos.isSelected()) msg += "- Debe aceptar los términos y condiciones.\n";

            if (!msg.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validación de datos");
                alert.setHeaderText("Por favor complete los siguientes campos:");
                alert.setContentText(msg);
                alert.showAndWait();
                return;
            }

            ConfigFirma.guardar(ruta, clave);
            lblEstado.setText("Firma configurada: " + ruta);
            lblEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
            new Alert(Alert.AlertType.INFORMATION, "Firma electrónica guardada correctamente (clave encriptada).").showAndWait();
        } catch (Exception e) {
            logDAO.guardar("FirmaController", "guardar", e.getMessage(), e);
            new Alert(Alert.AlertType.ERROR, "Error al guardar: " + e.getMessage()).showAndWait();
        }
    }
}

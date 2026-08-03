package com.tag.sysTagRep.controller;

import com.tag.sysTagRep.util.LicenseManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LicenseActivatorController {

    @FXML private Label lblMachineCode;
    @FXML private TextField txtLicenseKey;
    @FXML private Button btnActivate;
    @FXML private Button btnExit;
    @FXML private Label lblStatus;

    private boolean activated = false;

    @FXML
    public void initialize() {
        lblMachineCode.setText(LicenseManager.getMachineCode());
        btnActivate.setOnAction(e -> activate());
        btnExit.setOnAction(e -> {
            Stage stage = (Stage) btnExit.getScene().getWindow();
            stage.close();
        });
        txtLicenseKey.textProperty().addListener((obs, old, val) -> mostrarVencimiento(val));
    }

    private void mostrarVencimiento(String key) {
        if (key == null || key.trim().length() < 5) {
            lblStatus.setText("");
            return;
        }
        String cleanKey = key.trim();
        if (LicenseManager.isPerpetua(cleanKey)) {
            lblStatus.setText("Licencia VITALICIA — no expira.");
            lblStatus.setStyle("-fx-text-fill: #00ffcc;");
            return;
        }
        var vencimiento = LicenseManager.getVencimientoDeClave(cleanKey);
        if (vencimiento != null) {
            if (vencimiento.isBefore(java.time.LocalDate.now())) {
                lblStatus.setText("Esta licencia ya venció (" + vencimiento + ").");
                lblStatus.setStyle("-fx-text-fill: red;");
            } else {
                lblStatus.setText("Licencia válida hasta: " + vencimiento);
                lblStatus.setStyle("-fx-text-fill: #00ffcc;");
            }
        } else {
            lblStatus.setText("");
        }
    }

    private void activate() {
        String code = LicenseManager.getMachineCode();
        String key = txtLicenseKey.getText().trim();

        if (key.isEmpty()) {
            lblStatus.setText("Ingrese el código de licencia.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        boolean perpetua = LicenseManager.isPerpetua(key);
        var vencimiento = LicenseManager.getVencimientoDeClave(key);
        if (vencimiento == null && !perpetua) {
            lblStatus.setText("Formato de licencia inválido.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        if (LicenseManager.validateLicenseKey(code, key)) {
            LicenseManager.saveActivation(code, key);
            activated = true;
            String msg = perpetua
                    ? "Licencia VITALICIA activada correctamente."
                    : "Licencia activada correctamente (vence: " + vencimiento + ").";
            lblStatus.setText(msg);
            lblStatus.setStyle("-fx-text-fill: green;");
            Stage stage = (Stage) btnActivate.getScene().getWindow();
            stage.close();
        } else {
            lblStatus.setText("Código de licencia inválido.");
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    public boolean isActivated() {
        return activated;
    }
}

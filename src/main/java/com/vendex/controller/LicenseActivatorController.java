package com.vendex.controller;

import com.vendex.util.LicenseManager;
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
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(
                LicenseManager.getMachineCode(), cleanKey);
        if (LicenseManager.isPerpetua(cleanKey)) {
            lblStatus.setText("Licencia VITALICIA — no expira." + detallesVendexi(info));
            lblStatus.setStyle("");
            return;
        }
        var vencimiento = LicenseManager.getVencimientoDeClave(cleanKey);
        if (vencimiento != null) {
            if (vencimiento.isBefore(java.time.LocalDate.now())) {
                lblStatus.setText("Esta licencia ya venció (" + vencimiento + ").");
                lblStatus.setStyle("");
            } else {
                lblStatus.setText("Licencia válida hasta: " + vencimiento + detallesVendexi(info));
                lblStatus.setStyle("");
            }
        } else {
            lblStatus.setText("");
        }
    }

    private String detallesVendexi(LicenseManager.LicenseInfo info) {
        if (info == null || !info.version2) return "";
        StringBuilder sb = new StringBuilder(" · ").append(info.modalidad);
        sb.append(info.incluyeIA ? " · Vetxi AI incluida" : " · sin Vetxi AI");
        if (info.incluyeIA && !info.rolesIA.isEmpty()) {
            sb.append(" · roles: ").append(String.join(",", info.rolesIA));
        }
        if (info.incluyeIA && info.capacidadIA > 0) {
            sb.append(" · capacidad: ").append(info.capacidadIA);
        }
        return sb.toString();
    }

    private void activate() {
        String code = LicenseManager.getMachineCode();
        String key = txtLicenseKey.getText().trim();

        if (key.isEmpty()) {
            lblStatus.setText("Ingrese el código de licencia.");
            lblStatus.setStyle("");
            return;
        }

        boolean perpetua = LicenseManager.isPerpetua(key);
        var vencimiento = LicenseManager.getVencimientoDeClave(key);
        if (vencimiento == null && !perpetua) {
            lblStatus.setText("Formato de licencia inválido.");
            lblStatus.setStyle("");
            return;
        }

        if (LicenseManager.validateLicenseKey(code, key)) {
            LicenseManager.saveActivation(code, key);
            activated = true;
            LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(code, key);
            String msg = perpetua
                    ? "Licencia VITALICIA activada correctamente."
                    : "Licencia activada correctamente (vence: " + vencimiento + ").";
            msg += detallesVendexi(info);
            lblStatus.setText(msg);
            lblStatus.setStyle("");
            Stage stage = (Stage) btnActivate.getScene().getWindow();
            stage.close();
        } else {
            lblStatus.setText("Código de licencia inválido.");
            lblStatus.setStyle("");
        }
    }

    public boolean isActivated() {
        return activated;
    }
}

package com.vendex.controller;

import com.vendex.config.DbConfig;
import com.vendex.dao.LicenciaCentralDAOPostgres;
import com.vendex.service.LicenciaCentralService;
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
        // El payload Base64 de las licencias v2 distingue mayúsculas de minúsculas.
        txtLicenseKey.getStyleClass().add("no-uppercase");
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
        String key = LicenseManager.normalizeLicenseKey(txtLicenseKey.getText().trim());

        if (key == null || key.isEmpty()) {
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
            if (DbConfig.esRemota() && !validarCentral(code, key)) return;
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

    private boolean validarCentral(String code, String key) {
        java.util.Map<String, Object> r = new LicenciaCentralService(new LicenciaCentralDAOPostgres())
                .validarInstalacion(code, key, true);
        if (Boolean.TRUE.equals(r.get("valida"))) return true;
        String motivo = String.valueOf(r.getOrDefault("motivo", ""));
        String mensaje = switch (motivo) {
            case "NO_REGISTRADA" -> "La licencia no está registrada en el servidor central. "
                    + "Solicita al administrador una licencia CENTRAL para este código de máquina.";
            case "REVOCADA" -> "La licencia fue revocada en el servidor central.";
            case "MODALIDAD_INCORRECTA" -> "Esta licencia no es de modalidad CENTRAL. "
                    + "Solicita una licencia emitida para cliente remoto.";
            case "VENCIDA" -> "La licencia está vencida.";
            case "ERROR_CENTRAL" -> "No se pudo consultar el servidor de licencias. "
                    + "Verifica la conexión a la base de datos central.";
            default -> "Código de licencia inválido para esta máquina.";
        };
        lblStatus.setText(mensaje);
        lblStatus.setStyle("");
        return false;
    }

    public boolean isActivated() {
        return activated;
    }
}

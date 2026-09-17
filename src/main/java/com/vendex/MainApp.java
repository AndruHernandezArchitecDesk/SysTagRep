package com.vendex;

import com.vendex.config.DatabaseConnection;
import com.vendex.config.DbConfig;
import com.vendex.controller.LicenseActivatorController;
import com.vendex.util.LicenseManager;
import com.vendex.util.ThemeManager;
import com.vendex.util.UpperCaseTextFormatter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        cargarFuentes();
        // Cargar ~/.vendex/db.properties antes de cualquier DAO (soporte multi-PC 192.168.1.7 host)
        DatabaseConnection.initFromConfig();
        DatabaseConnection.ensureNotaCreditoSchema();
        DatabaseConnection.ensureGuiaRemisionSchema();
        DatabaseConnection.ensureRetencionSchema();
        // Activacion solo en host (192.168.1.7). PC cliente 192.168.1.5 con db.url remota no requiere licencia local.
        boolean esRemota = DbConfig.esRemota();
        if (!esRemota && !LicenseManager.isActivated()) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/LicenseActivatorView.fxml"));
            Parent root = loader.load();
            LicenseActivatorController ctrl = loader.getController();
            aplicarMayusculas(root);

            Stage licenseStage = new Stage();
            licenseStage.setTitle("Vendex - Activación");
            licenseStage.setScene(new Scene(root));
            licenseStage.initModality(Modality.APPLICATION_MODAL);
            licenseStage.setResizable(false);
            licenseStage.setOnCloseRequest(e -> System.exit(0));
            licenseStage.showAndWait();

            if (!ctrl.isActivated()) {
                Platform.exit();
                return;
            }
        }

        Parent root = FXMLLoader.load(
                getClass().getResource("/view/LoginView.fxml")
        );
        aplicarMayusculas(root);

        stage.setTitle("Vendex - Inicio de Sesión");
        Scene scene = new Scene(root);
        ThemeManager.aplicarTemaGuardado(scene);
        stage.setScene(scene);
        stage.show();
    }

    private void cargarFuentes() {
        try { Font.loadFont(getClass().getResourceAsStream("/fonts/Nunito-Regular.ttf"), 12); } catch (Exception ignored) {}
        try { Font.loadFont(getClass().getResourceAsStream("/fonts/Nunito-Medium.ttf"), 12); } catch (Exception ignored) {}
        try { Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Regular.ttf"), 12); } catch (Exception ignored) {}
        try { Font.loadFont(getClass().getResourceAsStream("/fonts/JetBrainsMono-Medium.ttf"), 12); } catch (Exception ignored) {}
    }

    private void aplicarMayusculas(Node nodo) {
        if (nodo instanceof javafx.scene.Parent parent) {
            for (Node n : parent.lookupAll(".text-field")) {
                UpperCaseTextFormatter.apply((javafx.scene.control.TextField) n);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

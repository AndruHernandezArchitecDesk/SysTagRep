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
        // Si no existe archivo y no hay override por env/property (headless/docker), mostrar wizard integrado
        boolean envOverride = System.getenv("DB_PASSWORD") != null && !System.getenv("DB_PASSWORD").isBlank()
                || System.getProperty("db.password") != null && !System.getProperty("db.password").isBlank();
        if (!DbConfig.getArchivo().exists() && !envOverride) {
            mostrarWizardDb(true);
        }
        DatabaseConnection.initFromConfig();
        // warning no bloqueante si password débil (solo log + dialog)
        if (DatabaseConnection.esPasswordDebilActual()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Contraseña de BD débil");
            alert.setHeaderText("Contraseña insegura detectada");
            alert.setContentText(com.vendex.util.PasswordDebilValidator.mensajeAdvertencia()
                    + "\n\nArchivo: " + DbConfig.getArchivo().getAbsolutePath()
                    + "\nPuedes continuar, pero rotarla en todas las PCs es recomendado.\n"
                    + "Usa el wizard de BD (borra db.properties y reinicia) o contacta soporte.");
            // no bloquea arranque, solo muestra y continúa
            alert.show();
        }
        DatabaseConnection.ensureNotaCreditoSchema();
        DatabaseConnection.ensureGuiaRemisionSchema();
        DatabaseConnection.ensureRetencionSchema();
        DatabaseConnection.ensureCertificadoEstadoSchema();
        DatabaseConnection.ensureConfiguracionEmailSchema();
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

    private void mostrarWizardDb(boolean obligatorio) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DbSetupWizardView.fxml"));
        Parent root = loader.load();
        Stage wizard = new Stage();
        wizard.setTitle("Vendex - Configuración de Base de Datos");
        wizard.setScene(new Scene(root));
        wizard.initModality(Modality.APPLICATION_MODAL);
        wizard.setResizable(false);
        if (obligatorio) {
            wizard.setOnCloseRequest(e -> {
                if (!DbConfig.getArchivo().exists()) {
                    e.consume();
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                    a.setHeaderText("Configuración requerida");
                    a.setContentText("Debes configurar la conexión a la BD para continuar.");
                    a.showAndWait();
                }
            });
        }
        wizard.showAndWait();
    }

    private void aplicarMayusculas(Node nodo) {
        if (nodo instanceof javafx.scene.Parent parent) {
            for (Node n : parent.lookupAll(".text-field")) {
                if (n instanceof javafx.scene.control.TextField tf) {
                    if (tf.getStyleClass().contains("no-uppercase")) continue;
                    UpperCaseTextFormatter.apply(tf);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.tag.sysTagRep;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.config.DbConfig;
import com.tag.sysTagRep.controller.LicenseActivatorController;
import com.tag.sysTagRep.util.LicenseManager;
import com.tag.sysTagRep.util.ThemeManager;
import com.tag.sysTagRep.util.UpperCaseTextFormatter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Cargar ~/.systag/db.properties antes de cualquier DAO (soporte multi-PC 192.168.1.7 host)
        DatabaseConnection.initFromConfig();
        // Activacion solo en host (192.168.1.7). PC cliente 192.168.1.5 con db.url remota no requiere licencia local.
        boolean esRemota = DbConfig.esRemota();
        if (!esRemota && !LicenseManager.isActivated()) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/LicenseActivatorView.fxml"));
            Parent root = loader.load();
            LicenseActivatorController ctrl = loader.getController();
            aplicarMayusculas(root);

            Stage licenseStage = new Stage();
            licenseStage.setTitle("SysTag - Activación");
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

        stage.setTitle("SysTag - Inicio de Sesión");
        Scene scene = new Scene(root);
        ThemeManager.aplicarTemaGuardado(scene);
        stage.setScene(scene);
        stage.show();
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

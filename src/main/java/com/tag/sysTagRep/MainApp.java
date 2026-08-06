package com.tag.sysTagRep;

import com.tag.sysTagRep.controller.LicenseActivatorController;
import com.tag.sysTagRep.util.LicenseManager;
import com.tag.sysTagRep.util.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        if (!LicenseManager.isActivated()) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/LicenseActivatorView.fxml"));
            Parent root = loader.load();
            LicenseActivatorController ctrl = loader.getController();

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

        stage.setTitle("SysTag - Inicio de Sesión");
        Scene scene = new Scene(root);
        ThemeManager.aplicarTemaGuardado(scene);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

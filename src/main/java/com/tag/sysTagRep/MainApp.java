package com.tag.sysTagRep;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
     /*   FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/VendedorView.fxml")
        );

        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );*/
        Parent root = FXMLLoader.load(
                getClass().getResource("/view/MainView.fxml")
        );

        stage.setTitle("Tag Repuestos Automotrices");

        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/img/inventario.png"))
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        stage.setScene(scene);
       // stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


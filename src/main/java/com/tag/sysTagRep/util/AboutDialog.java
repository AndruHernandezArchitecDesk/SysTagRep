package com.tag.sysTagRep.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class AboutDialog {

    private AboutDialog() {
    }

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Acerca de SysTag Repuestos");
        stage.setResizable(false);

        Pane canvas = new Pane();
        MetallicTextAnimation anim = new MetallicTextAnimation("Andrés Hernández - The Toy Maker");
        anim.play(canvas);

        Label version = new Label("SysTag Repuestos Automotrices — Versión 1.5");
        version.setTextFill(Color.web("#9aa0a6"));
        version.setFont(Font.font("System", 12));

        Label creditos = new Label(
                "Desarrollado por Andrés Hernández — The Toy Maker (Developer Software)\n"
                        + "Soporte: andreihernandez07@gmail.com · 0998573896\n"
                        + "Para la creación de módulos personalizados o desarrollo de software, contácteme.");
        creditos.setTextFill(Color.web("#c5c9ce"));
        creditos.setFont(Font.font("System", 11));
        creditos.setWrapText(true);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> stage.close());
        cerrar.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 24; -fx-cursor: hand;");

        VBox box = new VBox(16, canvas, version, creditos, cerrar);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setStyle("-fx-background-color: #0A0A0A;");

        stage.setOnHidden(e -> anim.stop());
        stage.setScene(new Scene(box));
        stage.show();
    }
}

package com.vendex.util;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

public class AboutDialog {

    private AboutDialog() {
    }

    public static void show(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Acerca de Vendex Repuestos");
        stage.setResizable(false);

        // NO TOCAR: efecto MetallicTextAnimation en el nombre (solo texto cambiado a The Tools Maker)
        Pane canvas = new Pane();
        canvas.setMaxWidth(Region.USE_PREF_SIZE);
        MetallicTextAnimation anim = new MetallicTextAnimation("The Tools Maker");
        anim.play(canvas);
        // Centrar canvas y quitar espacio inferior extra
        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setAlignment(Pos.CENTER);
        // reducir padding inferior del Pane interno (MetallicTextAnimation usa pad 30) via margen negativo
        VBox.setMargin(canvasWrapper, new javafx.geometry.Insets(0, 0, -12, 0));

        // Avatar fas-robot
        FontIcon avatarIcon = new FontIcon("fas-robot");
        avatarIcon.setIconSize(30);
        avatarIcon.getStyleClass().add("about-avatar-icon");
        StackPane avatar = new StackPane(avatarIcon);
        avatar.getStyleClass().add("about-avatar");

        Label subtitle = new Label("THE TOY MAKER — DEVELOPER SOFTWARE");
        subtitle.getStyleClass().add("about-subtitle");

        Region divider = new Region();
        divider.getStyleClass().add("about-divider");

        Label version = new Label("Vendex Repuestos Automotrices — Versión 2.0");
        version.getStyleClass().add("about-version");

        // Bloque log/contacto - mismo para ambos temas
        Label logLabel = new Label("CONTACTO");
        logLabel.getStyleClass().add("about-log-label");
        Label keyNombre = new Label("nombre");
        keyNombre.getStyleClass().add("about-contact-key");
        Label valNombre = new Label("Carlos Andres Hernandez Molina");
        valNombre.getStyleClass().add("about-contact-line");
        HBox rowNombre = new HBox(6, keyNombre, valNombre);
        rowNombre.setAlignment(Pos.CENTER_LEFT);
        Label keyCargo = new Label("cargo");
        keyCargo.getStyleClass().add("about-contact-key");
        Label valCargo = new Label("Systems Engineer");
        valCargo.getStyleClass().add("about-contact-line");
        HBox rowCargo = new HBox(6, keyCargo, valCargo);
        rowCargo.setAlignment(Pos.CENTER_LEFT);
        Label keyMail = new Label("mail");
        keyMail.getStyleClass().add("about-contact-key");
        Label valMail = new Label("andreihernandez07@gmail.com");
        valMail.getStyleClass().add("about-contact-line");
        HBox rowMail = new HBox(6, keyMail, valMail);
        rowMail.setAlignment(Pos.CENTER_LEFT);
        Label keyTel = new Label("tel");
        keyTel.getStyleClass().add("about-contact-key");
        Label valTel = new Label("0998573896");
        valTel.getStyleClass().add("about-contact-line");
        HBox rowTel = new HBox(6, keyTel, valTel);
        rowTel.setAlignment(Pos.CENTER_LEFT);
        VBox logBlock = new VBox(6, logLabel, rowNombre, rowCargo, rowMail, rowTel);
        logBlock.getStyleClass().add("about-log-block");

        Label footer = new Label("Para la creación de módulos personalizados o desarrollo de software, contácteme.");
        footer.getStyleClass().add("about-footer-text");
        footer.setWrapText(true);
        footer.setMaxWidth(380);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> stage.close());
        cerrar.getStyleClass().add("about-close-btn");

        VBox box = new VBox(4, avatar, canvasWrapper, subtitle, divider, version, logBlock, footer, cerrar);
        box.getStyleClass().add("about-container");
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(440);
        box.setMaxWidth(440);

        // Fijar dimensiones ventana
        Scene scene = new Scene(box);
        ThemeManager.aplicarTemaGuardado(scene);
        stage.setScene(scene);
        stage.setWidth(480);
        stage.setHeight(620);
        stage.centerOnScreen();

        stage.setOnHidden(e -> anim.stop());
        stage.show();
    }
}

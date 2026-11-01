package com.vendex.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.Map;

/** Applies database column limits to inputs identified by their FXML fx:id. */
public final class DatabaseInputLimits {
    private static final Map<String, Integer> EXACT = Map.ofEntries(
            Map.entry("txtnombre", 100), Map.entry("txtapellido", 100),
            Map.entry("txtrazonsocial", 300), Map.entry("txtrazon", 300),
            Map.entry("txtdescripcion", 300), Map.entry("txtdireccion", 100),
            Map.entry("txtdirpartida", 200), Map.entry("txtdirdest", 100),
            Map.entry("txtcorreo", 100), Map.entry("txtemailremitente", 100),
            Map.entry("txtreplyto", 100), Map.entry("txtusuario", 50),
            Map.entry("txtusername", 50), Map.entry("txtidentificacion", 13),
            Map.entry("txtruc", 13), Map.entry("txtidentdest", 13),
            Map.entry("txttelefono", 20), Map.entry("txtcelular", 20),
            Map.entry("txtcodigo", 50), Map.entry("txtcodigointerno", 50),
            Map.entry("txtplaca", 20), Map.entry("txtsucursal", 10),
            Map.entry("txtagenteretencion", 10), Map.entry("txtresolucion", 20),
            Map.entry("txtrazontransportista", 150), Map.entry("txtrazondest", 150),
            Map.entry("txtmotivotraslado", 300), Map.entry("txthost", 255),
            Map.entry("txtnombreemisor", 100), Map.entry("txtnombresmtp", 100)
    );

    private DatabaseInputLimits() { }

    public static void apply(Node root) {
        if (!(root instanceof Parent parent)) return;
        for (Node node : parent.lookupAll(".text-field")) {
            if (!(node instanceof TextField field) || field.getId() == null) continue;
            Integer maximum = EXACT.get(field.getId().toLowerCase());
            if (maximum == null) maximum = inferLimit(field.getId());
            if (maximum != null) limit(field, maximum);
        }
        for (Node node : parent.lookupAll(".text-area")) {
            if (!(node instanceof TextArea area) || area.getId() == null) continue;
            Integer maximum = inferLimit(area.getId());
            if (maximum != null) {
                int maxLength = maximum;
                area.setTextFormatter(new TextFormatter<>(change ->
                        change.getControlNewText().length() <= maxLength ? change : null));
            }
        }
    }

    private static Integer inferLimit(String id) {
        String key = id.toLowerCase();
        if (key.contains("identificacion") || key.contains("ruc")) return 13;
        if (key.contains("correo") || key.contains("email")) return 100;
        if (key.contains("telefono") || key.contains("celular")) return 20;
        if (key.contains("descripcion")) return 300;
        if (key.contains("direccion")) return 100;
        if (key.contains("nombre")) return 100;
        if (key.contains("codigo")) return 50;
        return null;
    }

    private static void limit(TextField field, int maximum) {
        boolean uppercase = !field.getStyleClass().contains("no-uppercase");
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (uppercase && change.getText() != null) change.setText(change.getText().toUpperCase());
            return change.getControlNewText().length() <= maximum ? change : null;
        }));
    }
}

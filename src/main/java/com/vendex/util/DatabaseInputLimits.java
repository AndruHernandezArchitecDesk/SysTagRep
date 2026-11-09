package com.vendex.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Applies live character limits to form inputs, using their FXML fx:id. */
public final class DatabaseInputLimits {
    private static final String MAXIMUM_KEY = DatabaseInputLimits.class.getName() + ".maximum";
    private static final String INSTALLED_KEY = DatabaseInputLimits.class.getName() + ".installed";
    private static final String INSTALLED_MAXIMUM_KEY = DatabaseInputLimits.class.getName() + ".installedMaximum";

    /* Limits reflect the persisted columns; controller-specific values can override these defaults. */
    private static final Map<String, Integer> EXACT = Map.ofEntries(
            Map.entry("txtnombre", 100), Map.entry("txtapellido", 100),
            Map.entry("txtrazonsocial", 300), Map.entry("txtrazon", 300),
            Map.entry("txttitulo", 300), Map.entry("txtdescripcion", 300),
            Map.entry("txtdireccion", 100), Map.entry("txtdirpartida", 300),
            Map.entry("txtdirdest", 300), Map.entry("txtrazontransportista", 300),
            Map.entry("txtrazondest", 300), Map.entry("txtrazonsujeto", 300),
            Map.entry("txtmotivotraslado", 300), Map.entry("txtmotivo", 300),
            Map.entry("txtcorreo", 60), Map.entry("txtemailremitente", 100),
            Map.entry("txtreplyto", 100), Map.entry("txtcorreoprueba", 100),
            Map.entry("txtnombreremitente", 100),
            Map.entry("txtusuario", 50), Map.entry("txtusuariosmtp", 100),
            Map.entry("txtusernamesmtp", 100),
            Map.entry("txtusername", 50), Map.entry("txtidentificacion", 13),
            Map.entry("txtruc", 13), Map.entry("txtidentdest", 20),
            Map.entry("txtidentsujeto", 13), Map.entry("txtructransportista", 13),
            Map.entry("txttelefono", 10), Map.entry("txtcelular", 10),
            Map.entry("txtcodigo", 50), Map.entry("txtcodigointerno", 25),
            Map.entry("txtplaca", 10), Map.entry("txtsucursal", 10),
            Map.entry("txtagenteretencion", 10), Map.entry("txtresolucion", 20),
            Map.entry("txtperiodofiscal", 7), Map.entry("txtnumdocsustento", 17),
            Map.entry("txtnumerofactura", 50), Map.entry("txtbase", 63),
            Map.entry("txtuser", 63),
            Map.entry("txtpuerto", 5), Map.entry("txthost", 255),
            Map.entry("txtnombreemisor", 100), Map.entry("txtnombresmtp", 100)
    );

    private DatabaseInputLimits() { }

    /** Applies limits to every identified text input in a loaded view. */
    public static void apply(Node root) {
        if (!(root instanceof Parent parent)) return;
        for (Node node : parent.lookupAll(".text-field")) {
            if (!(node instanceof TextField field) || field.getId() == null) continue;
            if (field.getStyleClass().contains("no-db-limit")) continue;
            Integer maximum = (Integer) field.getProperties().get(MAXIMUM_KEY);
            if (maximum == null) maximum = maximumFor(field.getId());
            if (maximum != null) limit(field, maximum);
        }
        for (Node node : parent.lookupAll(".text-area")) {
            if (!(node instanceof TextArea area) || area.getId() == null) continue;
            if (area.getStyleClass().contains("no-db-limit")) continue;
            Integer maximum = (Integer) area.getProperties().get(MAXIMUM_KEY);
            if (maximum == null) maximum = maximumFor(area.getId());
            if (maximum != null) limit(area, maximum);
        }
    }

    /** Sets/overrides the live limit for a TextField, including controls built in code. */
    public static void limit(TextField field, int maximum) {
        if (field == null || maximum < 1) return;
        field.getProperties().put(MAXIMUM_KEY, maximum);
        TextFormatter<?> current = field.getTextFormatter();
        if (current != null && current == field.getProperties().get(INSTALLED_KEY)
                && Integer.valueOf(maximum).equals(field.getProperties().get(INSTALLED_MAXIMUM_KEY))) return;

        UnaryOperator<TextFormatter.Change> previous = current == null ? null : current.getFilter();
        StringConverter<?> converter = current == null ? null : current.getValueConverter();
        Object value = current == null ? null : current.getValue();
        boolean uppercase = !field.getStyleClass().contains("no-uppercase");
        TextFormatter<Object> formatter = new TextFormatter<>((StringConverter<Object>) converter, value, change -> {
            if (uppercase && change.getText() != null) {
                change.setText(change.getText().toUpperCase(Locale.ROOT));
            }
            TextFormatter.Change accepted = previous == null ? change : previous.apply(change);
            if (accepted == null) return null;
            return accepted.getControlNewText().length() <= maximum ? accepted : null;
        });
        field.setTextFormatter(null);
        field.setTextFormatter(formatter);
        field.getProperties().put(INSTALLED_KEY, formatter);
        field.getProperties().put(INSTALLED_MAXIMUM_KEY, maximum);
    }

    /** Sets/overrides the live limit for a TextArea, including controls built in code. */
    public static void limit(TextArea area, int maximum) {
        if (area == null || maximum < 1) return;
        area.getProperties().put(MAXIMUM_KEY, maximum);
        TextFormatter<?> current = area.getTextFormatter();
        if (current != null && current == area.getProperties().get(INSTALLED_KEY)
                && Integer.valueOf(maximum).equals(area.getProperties().get(INSTALLED_MAXIMUM_KEY))) return;
        UnaryOperator<TextFormatter.Change> previous = current == null ? null : current.getFilter();
        StringConverter<?> converter = current == null ? null : current.getValueConverter();
        Object value = current == null ? null : current.getValue();
        TextFormatter<Object> formatter = new TextFormatter<>((StringConverter<Object>) converter, value, change -> {
            TextFormatter.Change accepted = previous == null ? change : previous.apply(change);
            return accepted != null && accepted.getControlNewText().length() <= maximum ? accepted : null;
        });
        area.setTextFormatter(null);
        area.setTextFormatter(formatter);
        area.getProperties().put(INSTALLED_KEY, formatter);
        area.getProperties().put(INSTALLED_MAXIMUM_KEY, maximum);
    }

    private static Integer maximumFor(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        Integer exact = EXACT.get(key);
        if (exact != null) return exact;
        if (key.contains("identificacion") || key.contains("ruc")) return 13;
        if (key.contains("correo") || key.contains("email")) return 100;
        if (key.contains("telefono") || key.contains("celular")) return 20;
        if (key.contains("descripcion")) return 300;
        if (key.contains("direccion")) return 100;
        if (key.contains("codigo")) return 50;
        return null;
    }
}

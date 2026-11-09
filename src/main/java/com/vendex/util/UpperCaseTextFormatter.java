package com.vendex.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.function.UnaryOperator;

public class UpperCaseTextFormatter {

    public static void apply(TextField textField) {
        if (textField == null) return;
        TextFormatter<?> current = textField.getTextFormatter();
        UnaryOperator<TextFormatter.Change> previous = current == null ? null : current.getFilter();
        StringConverter<?> converter = current == null ? null : current.getValueConverter();
        Object value = current == null ? null : current.getValue();
        TextFormatter<Object> formatter = new TextFormatter<>((StringConverter<Object>) converter, value, change -> {
            String text = change.getText();
            if (text != null) change.setText(text.toUpperCase(Locale.ROOT));
            return previous == null ? change : previous.apply(change);
        });
        textField.setTextFormatter(null);
        textField.setTextFormatter(formatter);
    }
}

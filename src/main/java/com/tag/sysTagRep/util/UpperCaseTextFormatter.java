package com.tag.sysTagRep.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public class UpperCaseTextFormatter {

    public static void apply(TextField textField) {
        if (textField == null) return;
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String text = change.getText();
            if (text != null && !text.equals(text.toUpperCase())) {
                change.setText(text.toUpperCase());
            }
            return change;
        });
        textField.setTextFormatter(formatter);
    }
}

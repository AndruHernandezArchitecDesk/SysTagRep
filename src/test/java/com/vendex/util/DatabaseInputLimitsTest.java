package com.vendex.util;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInputLimitsTest {
    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Other JavaFX tests may have started the toolkit first.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void appliesMappedLimitToFieldAndPreservesExistingFormatter() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                TextField field = new TextField();
                field.setId("txtNombre");
                field.setTextFormatter(new TextFormatter<>(change ->
                        change.getControlNewText().matches("\\d*") ? change : null));
                VBox root = new VBox(field);

                DatabaseInputLimits.apply(root);
                field.replaceText(0, 0, "1".repeat(101));
                assertEquals("", field.getText(), "la entrada que excede el máximo se rechaza completa");
                field.replaceText(0, 0, "1".repeat(100));
                assertEquals(100, field.getText().length());
                field.replaceText(100, 100, "2");
                assertEquals(100, field.getText().length(), "también bloquea el exceso al insertar al final");
                field.replaceText(0, 100, "2");
                assertEquals("2", field.getText(), "conserva el filtro previo y permite un reemplazo válido");

                TextField uppercase = new TextField();
                uppercase.setTextFormatter(new TextFormatter<>(change ->
                        change.getControlNewText().matches("[A-Z]*") ? change : null));
                UpperCaseTextFormatter.apply(uppercase);
                uppercase.replaceText(0, 0, "prueba");
                assertEquals("PRUEBA", uppercase.getText(), "compone mayúsculas con el filtro ya instalado");

                TextField identifiedById = new TextField();
                identifiedById.setId("txtNumeroFactura");
                VBox secondRoot = new VBox(identifiedById);
                DatabaseInputLimits.apply(secondRoot);
                identifiedById.replaceText(0, 0, "F".repeat(50));
                identifiedById.replaceText(50, 50, "X");
                assertTrue(identifiedById.getText().length() <= 50);

                TextArea note = new TextArea();
                note.setId("txtMotivo");
                DatabaseInputLimits.apply(new VBox(note));
                note.replaceText(0, 0, "N".repeat(301));
                assertEquals("", note.getText(), "el límite también aplica a áreas de texto");
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(30, TimeUnit.SECONDS), "timeout aplicando límites JavaFX");
        if (failure.get() != null) throw new AssertionError("Falló el límite de entrada", failure.get());
    }
}

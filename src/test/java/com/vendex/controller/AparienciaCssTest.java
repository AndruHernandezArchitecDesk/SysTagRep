package com.vendex.controller;

import com.vendex.util.AmbientEffectManager;
import com.vendex.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AparienciaCssTest {
    @BeforeAll
    static void iniciarJavaFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) { }
        Platform.setImplicitExit(false);
    }

    @Test
    void vistaCorreoCargaAccionParaImportarClave() throws Exception {
        enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ConfiguracionEmailView.fxml"));
            // La prueba de cableado no necesita consultar una BD ni enviar correo.
            loader.setControllerFactory(tipo -> org.mockito.Mockito.mock(ConfiguracionEmailController.class));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.preview(scene, ThemeManager.Theme.ORGANIC);
            root.applyCss();
            assertTrue(root.lookupAll(".button").stream()
                    .filter(node -> node instanceof javafx.scene.control.Button)
                    .map(node -> (javafx.scene.control.Button) node)
                    .anyMatch(button -> "Importar clave de instalación".equals(button.getText())
                            && button.getOnAction() != null));
            return null;
        });
    }

    @Test
    void navegarYCambiarTemasConPopupNoPierdePaleta() throws Exception {
        List<String> errores = new CopyOnWriteArrayList<>();
        java.util.concurrent.atomic.AtomicReference<String> paso = new java.util.concurrent.atomic.AtomicReference<>("carga");
        Logger css = Logger.getLogger("");
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                String mensaje = record.getMessage();
                if (record.getLevel().intValue() >= Level.WARNING.intValue() && mensaje != null
                        && (mensaje.contains("Could not resolve") || mensaje.contains("ClassCastException")
                        || mensaje.contains("CSS Error parsing"))) {
                    errores.add(paso.get() + ": " + mensaje);
                }
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        css.addHandler(handler);
        try {
            Stage stage = enFx(() -> {
                Stage ventana = new Stage();
                StackPane contenido = new StackPane();
                contenido.setId("contenido");
                StackPane shell = new StackPane(contenido);
                Scene scene = new Scene(shell, 900, 700);
                ThemeManager.preview(scene, ThemeManager.Theme.ORGANIC);
                ventana.setScene(scene);
                ventana.show();
                return ventana;
            });
            try {
                for (int vuelta = 0; vuelta < 2; vuelta++) {
                    Parent vista = enFx(() -> {
                        Parent contenido = FXMLLoader.load(getClass().getResource("/view/AparienciaView.fxml"));
                        StackPane shell = (StackPane) stage.getScene().getRoot().lookup("#contenido");
                        shell.getChildren().setAll(contenido);
                        shell.applyCss();
                        shell.layout();
                        return contenido;
                    });
                    // En una tarea posterior la vista ya terminó de enlazarse a la escena
                    // y su preview diferida está activa, igual que durante el uso real.
                    enFx(() -> {
                        StackPane shell = (StackPane) stage.getScene().getRoot().lookup("#contenido");
                        @SuppressWarnings("unchecked")
                        ComboBox<ThemeManager.Theme> combo = (ComboBox<ThemeManager.Theme>) vista.lookup("#cmbTema");
                        for (ThemeManager.Theme theme : ThemeManager.Theme.values()) {
                            paso.set("abrir popup antes de " + theme);
                            combo.show();
                            paso.set("seleccionar " + theme);
                            combo.setValue(theme);
                            paso.set("aplicar CSS " + theme);
                            shell.applyCss();
                            shell.layout();
                            combo.hide();
                        }
                        shell.getChildren().remove(vista);
                        return null;
                    });
                }
            } finally {
                enFx(() -> {
                    AmbientEffectManager.detach(stage.getScene());
                    stage.close();
                    return null;
                });
            }
            // Drenar previews diferidas de vistas ya retiradas.
            FutureTask<Void> drain = new FutureTask<>(() -> null);
            Platform.runLater(drain);
            drain.get(10, TimeUnit.SECONDS);
            assertTrue(errores.isEmpty(), String.join("\n", errores.stream().limit(8).toList()));
        } finally {
            css.removeHandler(handler);
        }
    }

    private static <T> T enFx(Callable<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion);
        Platform.runLater(tarea);
        return tarea.get(30, TimeUnit.SECONDS);
    }
}

package com.vendex.util;

/**
 * Mitiga el bug JavaFX "IllegalArgumentException: The start must be <= the end":
 * escribir/borrar en un ComboBox editable con un modal (showAndWait) activo deja
 * la seleccion del editor corrupta y revienta el hilo FX (se pierde la tecla).
 * Llamar justo antes de cada showAndWait: oculta popups abiertos y suelta el foco.
 */
public final class ComboPopupGuard {

    private ComboPopupGuard() {}

    public static void ocultarAntesDeModal() {
        try {
            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w == null || w.getScene() == null || w.getScene().getRoot() == null) continue;
                javafx.scene.Parent root = w.getScene().getRoot();
                for (javafx.scene.Node n : root.lookupAll(".combo-box")) {
                    try {
                        if (n instanceof javafx.scene.control.ComboBox<?> cb && cb.isShowing()) cb.hide();
                    } catch (Exception ignored) {}
                }
                try { root.requestFocus(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}

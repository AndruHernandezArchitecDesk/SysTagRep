package com.tag.sysTagRep.util;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String KEY_THEME = "app.theme";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    private static final String CSS_LIGHT = "/css/app.css";
    private static final String CSS_DARK = "/css/app-dark.css";

    private static final String CLASS_DARK = "dark-mode";

    private static final Preferences PREFS = Preferences.userRoot().node("/com/tag/sysTagRep");

    public static void aplicarTemaGuardado(Scene scene) {
        String tema = cargarTema();
        aplicarTema(scene, tema);
    }

    public static void aplicarTema(Scene scene, String tema) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = THEME_DARK.equalsIgnoreCase(tema) ? CSS_DARK : CSS_LIGHT;
        scene.getStylesheets().add(ThemeManager.class.getResource(css).toExternalForm());
        Node root = scene.getRoot();
        if (THEME_DARK.equalsIgnoreCase(tema)) {
            if (root != null && !root.getStyleClass().contains(CLASS_DARK)) {
                root.getStyleClass().add(CLASS_DARK);
            }
        } else if (root != null) {
            root.getStyleClass().remove(CLASS_DARK);
        }
        guardarTema(tema);
    }

    public static void alternarTema(Scene scene) {
        String actual = cargarTema();
        String nuevo = THEME_DARK.equalsIgnoreCase(actual) ? THEME_LIGHT : THEME_DARK;
        aplicarTema(scene, nuevo);
    }

    public static boolean esDarkMode() {
        return THEME_DARK.equalsIgnoreCase(cargarTema());
    }

    private static String cargarTema() {
        String tema = PREFS.get(KEY_THEME, THEME_LIGHT);
        return (tema == null || tema.trim().isEmpty()) ? THEME_LIGHT : tema;
    }

    private static void guardarTema(String tema) {
        PREFS.put(KEY_THEME, tema);
    }
}

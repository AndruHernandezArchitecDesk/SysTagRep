package com.vendex.util;

import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String KEY_THEME = "app.theme";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    // Nueva arquitectura base + tema (guia diseno)
    private static final String BASE = "/css/base.css";
    private static final String ORGANIC = "/css/theme-organic.css";
    private static final String CYBERPUNK = "/css/theme-cyberpunk.css";

    // Legacy para compatibilidad (se mantienen pero no se usan como primarios)
    private static final String CSS_LIGHT = "/css/app.css";
    private static final String CSS_DARK = "/css/app-dark.css";

    private static final String CLASS_DARK = "dark-mode";

    private static final Preferences PREFS = Preferences.userRoot().node("/com/tag/vendex");

    public enum Theme { ORGANIC, CYBERPUNK }

    // === Nueva API guia ===

    public static void apply(Scene scene, Theme theme) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource(BASE).toExternalForm());
        String themeFile = (theme == Theme.CYBERPUNK) ? CYBERPUNK : ORGANIC;
        scene.getStylesheets().add(ThemeManager.class.getResource(themeFile).toExternalForm());
        Node root = scene.getRoot();
        if (root != null) {
            if (theme == Theme.CYBERPUNK) {
                if (!root.getStyleClass().contains(CLASS_DARK)) root.getStyleClass().add(CLASS_DARK);
            } else {
                root.getStyleClass().remove(CLASS_DARK);
            }
        }
        guardarTema(theme == Theme.CYBERPUNK ? THEME_DARK : THEME_LIGHT);
    }

    public static Theme getCurrentTheme() {
        return THEME_DARK.equalsIgnoreCase(cargarTema()) ? Theme.CYBERPUNK : Theme.ORGANIC;
    }

    // === API legacy compat (delegan a nueva) ===

    public static void aplicarTemaGuardado(Scene scene) {
        String tema = cargarTema();
        if (THEME_DARK.equalsIgnoreCase(tema)) {
            apply(scene, Theme.CYBERPUNK);
        } else {
            apply(scene, Theme.ORGANIC);
        }
    }

    public static void aplicarTema(Scene scene, String tema) {
        if (scene == null) return;
        if (THEME_DARK.equalsIgnoreCase(tema)) {
            apply(scene, Theme.CYBERPUNK);
        } else {
            apply(scene, Theme.ORGANIC);
        }
    }

    public static void alternarTema(Scene scene) {
        Theme actual = getCurrentTheme();
        Theme nuevo = (actual == Theme.CYBERPUNK) ? Theme.ORGANIC : Theme.CYBERPUNK;
        apply(scene, nuevo);
    }

    public static boolean esDarkMode() {
        return THEME_DARK.equalsIgnoreCase(cargarTema());
    }

    public static String getLogoPath() {
        return esDarkMode() ? "/img/VendexLogoDark.png" : "/img/logoVendex.png";
    }

    private static String cargarTema() {
        String tema = PREFS.get(KEY_THEME, THEME_LIGHT);
        return (tema == null || tema.trim().isEmpty()) ? THEME_LIGHT : tema;
    }

    private static void guardarTema(String tema) {
        PREFS.put(KEY_THEME, tema);
    }
}

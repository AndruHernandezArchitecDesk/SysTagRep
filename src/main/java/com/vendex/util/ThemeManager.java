package com.vendex.util;

import javafx.scene.Node;
import javafx.scene.Scene;

import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String KEY_THEME = "app.theme";
    private static final String KEY_THEME_ID = "ui.theme";
    private static final String KEY_AMBIENT_ENABLED = "ui.ambient.enabled";
    private static final String KEY_AMBIENT_INTENSITY = "ui.ambient.intensity";
    private static final String KEY_AMBIENT_SPEED = "ui.ambient.speed";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    // Nueva arquitectura base + tema (guia diseno)
    private static final String BASE = "/css/base.css";
    private static final String ORGANIC = "/css/theme-organic.css";
    private static final String CYBERPUNK = "/css/theme-cyberpunk.css";

    private static final String PINK_BLOSSOM = "/css/theme-pink-blossom.css";
    private static final String NATURE_BREEZE = "/css/theme-nature-breeze.css";

    private static final String CLASS_DARK = "dark-mode";

    private static final Preferences PREFS = Preferences.userRoot().node("/com/tag/vendex");

    public enum Theme {
        ORGANIC("organic", "Organic", ThemeManager.ORGANIC, false),
        CYBERPUNK("cyberpunk", "Cyberpunk", ThemeManager.CYBERPUNK, true),
        PINK_BLOSSOM("pink-blossom", "Pink Blossom", ThemeManager.PINK_BLOSSOM, false),
        NATURE_BREEZE("nature-breeze", "Nature Breeze", ThemeManager.NATURE_BREEZE, false);

        private final String id;
        private final String label;
        private final String stylesheet;
        private final boolean dark;

        Theme(String id, String label, String stylesheet, boolean dark) {
            this.id = id;
            this.label = label;
            this.stylesheet = stylesheet;
            this.dark = dark;
        }

        public String id() { return id; }
        public String label() { return label; }
        public boolean isDark() { return dark; }
        private String stylesheet() { return stylesheet; }

        public static Theme fromId(String id) {
            if (id != null) for (Theme theme : values())
                if (theme.id.equalsIgnoreCase(id.trim())) return theme;
            return null;
        }
    }

    public enum AmbientIntensity { LOW, MEDIUM, HIGH }
    public enum AmbientSpeed { SLOW, NORMAL }

    // === Nueva API guia ===

    public static void apply(Scene scene, Theme theme) {
        apply(scene, theme, true);
    }

    /** Applies a temporary preview without persisting it. */
    public static void preview(Scene scene, Theme theme) {
        apply(scene, theme, false);
    }

    private static void apply(Scene scene, Theme requested, boolean persist) {
        if (scene == null) return;
        Theme theme = requested == null ? Theme.ORGANIC : requested;
        String themeFile = theme.stylesheet();
        if (ThemeManager.class.getResource(themeFile) == null) {
            theme = Theme.ORGANIC;
            themeFile = ORGANIC;
        }
        java.util.List<String> stylesheets = new java.util.ArrayList<>();
        addStylesheet(stylesheets, BASE);
        // The organic sheet supplies the shared light-control rules for the two new
        // pastel palettes; each still has exactly one final palette override sheet.
        if (theme == Theme.PINK_BLOSSOM || theme == Theme.NATURE_BREEZE) addStylesheet(stylesheets, ORGANIC);
        addStylesheet(stylesheets, themeFile);
        Node root = scene.getRoot();
        if (root != null) {
            // La selección del ComboBox notifica antes de cerrar su popup. No
            // cambiar su CSS mientras JavaFX está resolviendo la paleta anterior.
            if (!scene.getStylesheets().equals(stylesheets)) {
                for (Node node : root.lookupAll(".combo-box-base")) {
                    if (node instanceof javafx.scene.control.ComboBoxBase<?> combo) combo.hide();
                }
            }
            String themeClass = "theme-" + theme.id();
            if (!root.getStyleClass().contains(themeClass)) {
                root.getStyleClass().removeAll("theme-organic", "theme-cyberpunk", "theme-pink-blossom", "theme-nature-breeze");
                root.getStyleClass().add(themeClass);
            }
            if (theme.isDark()) {
                if (!root.getStyleClass().contains(CLASS_DARK)) root.getStyleClass().add(CLASS_DARK);
            } else {
                root.getStyleClass().remove(CLASS_DARK);
            }
        }
        // Reemplazo atómico: los controles/popup no deben resolver colores entre
        // clear() y add(), cuando la paleta todavía no existe en la escena.
        if (!scene.getStylesheets().equals(stylesheets)) scene.getStylesheets().setAll(stylesheets);
        if (root != null && scene.getWindow() != null && scene.getWindow().isShowing()) root.applyCss();
        if (persist) guardarTema(theme);
    }

    public static Theme getCurrentTheme() {
        return cargarTema();
    }

    // === API legacy compat (delegan a nueva) ===

    public static void aplicarTemaGuardado(Scene scene) {
        apply(scene, cargarTema(), false);
    }

    public static void aplicarTema(Scene scene, String tema) {
        if (scene == null) return;
        Theme resolved = Theme.fromId(tema);
        if (resolved == null) resolved = THEME_DARK.equalsIgnoreCase(tema) ? Theme.CYBERPUNK : Theme.ORGANIC;
        apply(scene, resolved);
    }

    public static void alternarTema(Scene scene) {
        Theme nuevo = getCurrentTheme().isDark() ? Theme.ORGANIC : Theme.CYBERPUNK;
        apply(scene, nuevo);
    }

    public static boolean esDarkMode() {
        return getCurrentTheme().isDark();
    }

    public static boolean animacionesAmbientalesActivas() {
        String value = PREFS.get(KEY_AMBIENT_ENABLED, null);
        return value == null ? getCurrentTheme() == Theme.PINK_BLOSSOM || getCurrentTheme() == Theme.NATURE_BREEZE
                : Boolean.parseBoolean(value);
    }

    public static boolean tienePreferenciaAmbiental() {
        return PREFS.get(KEY_AMBIENT_ENABLED, null) != null;
    }

    public static AmbientIntensity getAmbientIntensity() {
        try { return AmbientIntensity.valueOf(PREFS.get(KEY_AMBIENT_INTENSITY, "LOW")); }
        catch (IllegalArgumentException ex) { return AmbientIntensity.LOW; }
    }

    public static AmbientSpeed getAmbientSpeed() {
        try { return AmbientSpeed.valueOf(PREFS.get(KEY_AMBIENT_SPEED, "SLOW")); }
        catch (IllegalArgumentException ex) { return AmbientSpeed.SLOW; }
    }

    public static void guardarApariencia(Theme theme, boolean animaciones,
                                         AmbientIntensity intensidad, AmbientSpeed velocidad) {
        Theme selected = theme == null ? Theme.ORGANIC : theme;
        guardarTema(selected);
        PREFS.putBoolean(KEY_AMBIENT_ENABLED, animaciones);
        PREFS.put(KEY_AMBIENT_INTENSITY, (intensidad == null ? AmbientIntensity.LOW : intensidad).name());
        PREFS.put(KEY_AMBIENT_SPEED, (velocidad == null ? AmbientSpeed.SLOW : velocidad).name());
    }

    public static String getLogoPath() {
        return "/img/LogoVendex.png";
    }

    private static Theme cargarTema() {
        String themeId = PREFS.get(KEY_THEME_ID, null);
        Theme theme = Theme.fromId(themeId);
        if (theme != null) return theme;
        return THEME_DARK.equalsIgnoreCase(PREFS.get(KEY_THEME, THEME_LIGHT)) ? Theme.CYBERPUNK : Theme.ORGANIC;
    }

    private static void guardarTema(Theme theme) {
        PREFS.put(KEY_THEME_ID, theme.id());
        PREFS.put(KEY_THEME, theme.isDark() ? THEME_DARK : THEME_LIGHT);
    }

    private static void addStylesheet(java.util.List<String> stylesheets, String path) {
        java.net.URL resource = ThemeManager.class.getResource(path);
        if (resource != null) stylesheets.add(resource.toExternalForm());
    }
}

package com.vendex.util;

import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Owns one reusable ambient layer per primary JavaFX scene. */
public final class AmbientEffectManager {
    private static final Map<Scene, Registration> REGISTRATIONS = new WeakHashMap<>();

    private AmbientEffectManager() { }

    public static void attach(Scene scene) {
        if (scene == null) return;
        Registration registration = REGISTRATIONS.get(scene);
        if (registration == null) {
            Pane host = ensurePaneRoot(scene);
            AmbientEffectLayer layer = new AmbientEffectLayer();
            layer.getStyleClass().add("ambient-effect-layer");
            layer.setManaged(false);
            host.getChildren().add(layer);
            layer.resize(host.getWidth(), host.getHeight());
            ChangeListener<Number> resize = (obs, oldValue, newValue) ->
                    layer.resize(host.getWidth(), host.getHeight());
            host.widthProperty().addListener(resize);
            host.heightProperty().addListener(resize);
            registration = new Registration(host, layer, resize);
            REGISTRATIONS.put(scene, registration);
            Registration registered = registration;
            WeakReference<Scene> weakScene = new WeakReference<>(scene);
            registration.windowListener = (obs, oldWindow, newWindow) -> {
                if (oldWindow != null) registered.unbind(oldWindow);
                if (newWindow == null) {
                    Scene current = weakScene.get();
                    if (current != null) detach(current);
                } else {
                    registered.bind(newWindow);
                }
            };
            scene.windowProperty().addListener(registration.windowListener);
            if (scene.getWindow() != null) registration.bind(scene.getWindow());
        }
        refresh(scene);
    }

    /** Apply a temporary appearance preview, without changing saved settings. */
    public static void preview(Scene scene, ThemeManager.Theme theme, boolean enabled,
                               ThemeManager.AmbientIntensity intensity,
                               ThemeManager.AmbientSpeed speed) {
        attach(scene);
        Registration registration = REGISTRATIONS.get(scene);
        if (registration != null) registration.layer.configure(theme, enabled, intensity, speed);
    }

    /** Refresh effects from the locally saved appearance preferences. */
    public static void refresh(Scene scene) {
        if (scene == null) return;
        Registration registration = REGISTRATIONS.get(scene);
        if (registration != null) {
            registration.layer.configure(ThemeManager.getCurrentTheme(),
                    ThemeManager.animacionesAmbientalesActivas(),
                    ThemeManager.getAmbientIntensity(), ThemeManager.getAmbientSpeed());
            registration.updateActiveState();
        }
    }

    public static void detach(Scene scene) {
        Registration registration = REGISTRATIONS.remove(scene);
        if (registration == null) return;
        scene.windowProperty().removeListener(registration.windowListener);
        registration.unbind(registration.window);
        registration.host.widthProperty().removeListener(registration.resizeListener);
        registration.host.heightProperty().removeListener(registration.resizeListener);
        registration.host.getChildren().remove(registration.layer);
        registration.layer.dispose();
    }

    private static Pane ensurePaneRoot(Scene scene) {
        Parent root = scene.getRoot();
        if (root instanceof Pane pane) return pane;
        StackPane wrapper = new StackPane(root);
        scene.setRoot(wrapper);
        return wrapper;
    }

    private static final class Registration {
        final Pane host;
        final AmbientEffectLayer layer;
        final ChangeListener<Number> resizeListener;
        ChangeListener<Window> windowListener;
        Window window;
        javafx.event.EventHandler<WindowEvent> shownHandler;
        javafx.event.EventHandler<WindowEvent> hiddenHandler;
        ChangeListener<Boolean> iconifiedListener;

        Registration(Pane host, AmbientEffectLayer layer, ChangeListener<Number> resizeListener) {
            this.host = host;
            this.layer = layer;
            this.resizeListener = resizeListener;
        }

        void bind(Window nextWindow) {
            if (window == nextWindow) { updateActiveState(); return; }
            unbind(window);
            window = nextWindow;
            if (window == null) return;
            shownHandler = e -> {
                layer.resume();
                updateActiveState();
            };
            hiddenHandler = e -> {
                layer.setWindowActive(false);
                layer.suspend();
            };
            window.addEventHandler(WindowEvent.WINDOW_SHOWN, shownHandler);
            window.addEventHandler(WindowEvent.WINDOW_HIDDEN, hiddenHandler);
            if (window instanceof Stage stage) {
                iconifiedListener = (obs, oldValue, newValue) -> updateActiveState();
                stage.iconifiedProperty().addListener(iconifiedListener);
            }
            updateActiveState();
        }

        void unbind(Window oldWindow) {
            if (oldWindow == null) return;
            if (shownHandler != null) oldWindow.removeEventHandler(WindowEvent.WINDOW_SHOWN, shownHandler);
            if (hiddenHandler != null) oldWindow.removeEventHandler(WindowEvent.WINDOW_HIDDEN, hiddenHandler);
            if (oldWindow instanceof Stage stage && iconifiedListener != null)
                stage.iconifiedProperty().removeListener(iconifiedListener);
            if (window == oldWindow) window = null;
            shownHandler = null;
            hiddenHandler = null;
            iconifiedListener = null;
        }

        void updateActiveState() {
            boolean active = window != null && window.isShowing();
            if (window instanceof Stage stage) active &= !stage.isIconified();
            layer.setWindowActive(active);
        }
    }
}

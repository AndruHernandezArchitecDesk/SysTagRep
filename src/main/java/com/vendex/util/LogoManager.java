package com.vendex.util;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.image.Image;

import java.io.InputStream;

public final class LogoManager {

    private static final ReadOnlyObjectWrapper<Image> LOGO = new ReadOnlyObjectWrapper<>();
    private static boolean inicializado;

    private LogoManager() {
    }

    public static ReadOnlyObjectProperty<Image> logoProperty() {
        if (!inicializado) {
            recargar();
        }
        return LOGO.getReadOnlyProperty();
    }

    public static Image getLogo() {
        return logoProperty().get();
    }

    public static void recargar() {
        Image imagen = cargar(EmpresaLogo.abrirLogoStream(null));
        if (imagen == null) {
            imagen = cargar(LogoManager.class.getResourceAsStream(EmpresaLogo.LOGO_RECURSO));
        }
        LOGO.set(imagen);
        inicializado = true;
    }

    private static Image cargar(InputStream is) {
        if (is == null) return null;
        try (InputStream entrada = is) {
            return new Image(entrada);
        } catch (Exception ignored) {
            return null;
        }
    }
}

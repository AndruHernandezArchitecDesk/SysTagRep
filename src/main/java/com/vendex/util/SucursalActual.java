package com.vendex.util;

import com.vendex.model.Sucursal;

import java.util.prefs.Preferences;

/**
 * Holder para sucursal seleccionada en la sesión UI.
 * Persiste en Preferences para recordar última selección por PC.
 */
public final class SucursalActual {

    private static final String PREF_KEY = "sucursal.id";
    private static Sucursal sucursal;
    private static final Preferences PREFS = Preferences.userNodeForPackage(SucursalActual.class);

    private SucursalActual() {}

    public static void set(Sucursal s) {
        sucursal = s;
        if (s != null) {
            PREFS.putInt(PREF_KEY, s.getId());
        }
    }

    public static Sucursal get() {
        return sucursal;
    }

    public static int getId() {
        return sucursal != null ? sucursal.getId() : getIdPersistido();
    }

    public static String getCodigo() {
        return sucursal != null ? sucursal.getCodigo() : "001";
    }

    public static String getNombre() {
        return sucursal != null ? sucursal.getNombre() : "Matriz";
    }

    public static boolean isSeleccionada() {
        return sucursal != null;
    }

    public static int getIdPersistido() {
        return PREFS.getInt(PREF_KEY, 1);
    }

    public static void limpiar() {
        sucursal = null;
    }

    // Para tests
    public static void setForTest(Sucursal s) { sucursal = s; }
}

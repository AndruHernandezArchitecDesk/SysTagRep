package com.vendex.util;

import java.util.List;

/**
 * Catalogo de tipos de identificacion del SRI (Tabla 6 de la ficha tecnica de
 * comprobantes electronicos).
 */
public final class TiposIdSri {

    private static final String[] ITEMS = {
        "04-RUC",
        "05-Cedula",
        "06-Pasaporte",
    };

    private TiposIdSri() {}

    public static List<String> items() {
        return List.of(ITEMS);
    }

    public static String defaultItem() {
        return ITEMS[0];
    }

    public static String itemDe(String codigo) {
        if (codigo == null) return null;
        for (String item : ITEMS) {
            if (item.startsWith(codigo + "-")) return item;
        }
        return null;
    }

    public static String codigo(String item) {
        if (item == null || item.isBlank()) return item;
        return item.split("-", 2)[0].trim();
    }

    public static boolean esValido(String codigo) {
        if (codigo == null) return false;
        for (String item : ITEMS) {
            if (item.startsWith(codigo + "-")) return true;
        }
        return false;
    }
}

package com.vendex.util;

import java.util.List;

/**
 * Catalogo de formas de pago del SRI (Tabla 24 / Anexo Transaccional Simplificado).
 * Los items del ComboBox llevan el formato "codigo-descripcion";
 * el XML y la BD reciben solo el codigo.
 */
public final class FormasPagoSri {

    private static final String[] ITEMS = {
        "01-Sin utilizacion del sistema financiero",
        "15-Compensacion de deudas",
        "16-Tarjeta de debito",
        "17-Dinero electronico",
        "18-Tarjeta prepago",
        "19-Tarjeta de credito",
        "20-Otros con utilizacion del sistema financiero",
        "21-Endoso de titulos",
    };

    private FormasPagoSri() {}

    public static List<String> items() {
        return List.of(ITEMS);
    }

    public static String defaultItem() {
        return ITEMS[0];
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

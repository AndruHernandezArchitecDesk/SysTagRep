package com.vendex.util;

import java.util.List;

/**
 * Catalogos SRI del comprobante de retencion:
 * codigos de sustento, tipos de documento de sustento e impuestos retenidos.
 */
public final class RetencionCatalogosSri {

    private static final String[] COD_SUSTENTO = {
        "01-Credito tributario IVA",
        "02-Costo o gasto renta",
        "03-Activo fijo IVA",
    };

    private static final String[] COD_DOC_SUSTENTO = {
        "01-Factura",
        "04-Nota de credito",
        "05-Nota de debito",
    };

    private static final String[] IMPUESTO = {
        "1-Renta",
        "2-IVA",
    };

    private RetencionCatalogosSri() {}

    public static List<String> codSustentoItems() {
        return List.of(COD_SUSTENTO);
    }

    public static List<String> codDocSustentoItems() {
        return List.of(COD_DOC_SUSTENTO);
    }

    public static List<String> impuestoItems() {
        return List.of(IMPUESTO);
    }

    public static String defaultCodSustento() {
        return COD_SUSTENTO[0];
    }

    public static String defaultCodDocSustento() {
        return COD_DOC_SUSTENTO[0];
    }

    public static String defaultImpuesto() {
        return IMPUESTO[0];
    }

    public static String itemDeCodSustento(String codigo) {
        return itemDe(codigo, COD_SUSTENTO);
    }

    public static String itemDeCodDocSustento(String codigo) {
        return itemDe(codigo, COD_DOC_SUSTENTO);
    }

    public static String itemDeImpuesto(String codigo) {
        return itemDe(codigo, IMPUESTO);
    }

    private static String itemDe(String codigo, String[] catalogo) {
        if (codigo == null) return null;
        for (String item : catalogo) {
            if (item.startsWith(codigo + "-")) return item;
        }
        return null;
    }

    public static String codigo(String item) {
        if (item == null || item.isBlank()) return item;
        return item.split("-", 2)[0].trim();
    }

    public static boolean esValido(String item, List<String> catalogo) {
        if (item == null) return false;
        for (String it : catalogo) {
            if (it.startsWith(item + "-")) return true;
        }
        return false;
    }
}

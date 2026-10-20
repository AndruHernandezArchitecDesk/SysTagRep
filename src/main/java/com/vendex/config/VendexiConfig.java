package com.vendex.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

/**
 * Fase 3 Vendexi — precios de referencia para estimar costo de IA (~/.vendex/vendexi.properties).
 * No son secretos. Los valores por defecto son estimaciones configurables por el operador;
 * el costo real del proveedor debe validarse antes de fijar precios comerciales (spec §15).
 */
public final class VendexiConfig {

    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "vendexi.properties");
    private static final String PROP_ENTRADA = "costo.usd.por.1m.tokens.entrada";
    private static final String PROP_SALIDA = "costo.usd.por.1m.tokens.salida";

    // Estimación inicial (revisable): flash-lite-like
    private static final BigDecimal DEF_ENTRADA = new BigDecimal("0.075");
    private static final BigDecimal DEF_SALIDA = new BigDecimal("0.30");

    private VendexiConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    public static BigDecimal costoUsdPor1mEntrada() {
        return leer(PROP_ENTRADA, DEF_ENTRADA);
    }

    public static BigDecimal costoUsdPor1mSalida() {
        return leer(PROP_SALIDA, DEF_SALIDA);
    }

    /** Costo estimado en USD para unos tokens dados. */
    public static BigDecimal estimarCosto(long tokensEntrada, long tokensSalida) {
        BigDecimal in = costoUsdPor1mEntrada();
        BigDecimal out = costoUsdPor1mSalida();
        BigDecimal cIn = in.multiply(BigDecimal.valueOf(tokensEntrada))
                .divide(new BigDecimal("1000000"), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal cOut = out.multiply(BigDecimal.valueOf(tokensSalida))
                .divide(new BigDecimal("1000000"), 6, java.math.RoundingMode.HALF_UP);
        return cIn.add(cOut).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal leer(String prop, BigDecimal def) {
        Properties p = cargar();
        String v = p.getProperty(prop, "").trim();
        if (v.isEmpty()) return def;
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static Properties cargar() {
        Properties p = new Properties();
        if (!ARCHIVO.exists()) return p;
        try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
            p.load(fis);
        } catch (IOException ignored) {}
        return p;
    }

    /** Guarda/actualiza un precio (uso administrativo). */
    public static synchronized void guardar(BigDecimal entradaUsdPor1m, BigDecimal salidaUsdPor1m) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties p = cargar();
            p.setProperty(PROP_ENTRADA, entradaUsdPor1m.toPlainString());
            p.setProperty(PROP_SALIDA, salidaUsdPor1m.toPlainString());
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Vendexi AI - precios de referencia USD por 1M tokens (estimacion, no factura del proveedor)");
            }
        } catch (IOException ignored) {}
    }
}

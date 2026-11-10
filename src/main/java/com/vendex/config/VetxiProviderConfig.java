package com.vendex.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/** Provider seleccionado para las funciones de Vetxi; NVIDIA sigue siendo independiente. */
public final class VetxiProviderConfig {
    public enum Provider { GEMINI, OPENAI }

    private static final File FILE = new File(new File(System.getProperty("user.home"), ".vendex"), "vetxi.properties");
    private static final String KEY = "vetxi.provider";

    private VetxiProviderConfig() { }

    public static synchronized Provider getProvider() {
        Properties properties = new Properties();
        if (FILE.isFile()) {
            try (FileInputStream in = new FileInputStream(FILE)) { properties.load(in); }
            catch (IOException ignored) { }
        }
        String value = properties.getProperty(KEY, "GEMINI").trim().toUpperCase(Locale.ROOT);
        try { return Provider.valueOf(value); }
        catch (IllegalArgumentException ignored) { return Provider.GEMINI; }
    }

    public static synchronized void setProvider(Provider provider) {
        if (provider == null) return;
        File parent = FILE.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) return;
        Properties properties = new Properties();
        if (FILE.isFile()) {
            try (FileInputStream in = new FileInputStream(FILE)) { properties.load(in); }
            catch (IOException ignored) { }
        }
        properties.setProperty(KEY, provider.name());
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            properties.store(out, "Vendex - proveedor de Vetxi");
        } catch (IOException ignored) { }
    }

    public static String obtenerApiKey() {
        return getProvider() == Provider.OPENAI ? OpenAIConfig.obtenerApiKey() : GeminiConfig.obtenerApiKey();
    }

    public static boolean tieneApiKey() {
        String key = obtenerApiKey();
        return key != null && !key.isBlank();
    }

    public static String nombreProveedor() {
        return getProvider() == Provider.OPENAI ? "OpenAI / ChatGPT" : "Gemini";
    }
}

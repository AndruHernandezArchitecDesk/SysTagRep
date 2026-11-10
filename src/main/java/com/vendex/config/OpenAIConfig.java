package com.vendex.config;

import com.vendex.util.SecureConfigStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Almacena de forma cifrada la API key de OpenAI. */
public final class OpenAIConfig {
    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File FILE = new File(DIR, "openai.properties");
    private static final String KEY = "openai.api_key";

    private OpenAIConfig() { }

    public static File getArchivo() { return FILE; }

    public static synchronized String obtenerApiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("OPENAI_API_KEY");
        if (prop != null && !prop.isBlank()) return prop.trim();
        if (!FILE.isFile()) return null;
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(FILE)) {
            properties.load(in);
        } catch (IOException e) {
            return null;
        }
        String encrypted = properties.getProperty(KEY, "").trim();
        if (encrypted.isEmpty()) return null;
        try {
            String value = SecureConfigStore.descifrarConMasterKey(encrypted);
            if (value != null && !value.isBlank() && !value.equals(encrypted)) return value.trim();
        } catch (Exception ignored) { }
        try {
            String value = SecureConfigStore.descifrar(encrypted);
            return value == null || value.isBlank() ? null : value.trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static synchronized void guardarApiKey(String apiKey) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties properties = new Properties();
            if (FILE.isFile()) {
                try (FileInputStream in = new FileInputStream(FILE)) { properties.load(in); }
                catch (IOException ignored) { }
            }
            if (apiKey == null || apiKey.isBlank()) {
                properties.remove(KEY);
            } else {
                String value;
                try { value = SecureConfigStore.cifrarConMasterKey(apiKey.trim()); }
                catch (Exception e) { value = SecureConfigStore.cifrar(apiKey.trim()); }
                properties.setProperty(KEY, value);
            }
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                properties.store(out, "Vendex - OpenAI API Key (cifrada). Prioridad: OPENAI_API_KEY > archivo");
            }
        } catch (IOException ignored) { }
    }

    public static boolean tieneApiKey() {
        String key = obtenerApiKey();
        return key != null && !key.isBlank();
    }

    public static void borrarApiKey() { guardarApiKey(null); }
}

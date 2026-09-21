package com.vendex.config;

import com.vendex.util.SecureConfigStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persiste GEMINI_API_KEY en ~/.vendex/gemini.properties cifrado via SecureConfigStore.
 * Prioridad: env var GEMINI_API_KEY > System property > archivo cifrado > vacío (modo sin IA).
 * Migra automáticamente valores legacy en claro a cifrado.
 */
public final class GeminiConfig {

    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "gemini.properties");
    private static final String KEY_PROP = "gemini.api_key";

    private GeminiConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    /** Resuelve key con prioridad env var > archivo cifrado. */
    public static synchronized String obtenerApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("GEMINI_API_KEY");
        if (prop != null && !prop.isBlank()) return prop.trim();
        if (!ARCHIVO.exists()) return null;
        // migrar si aún está en claro o legacy SECRETO → keyring (Fase 1 completa)
        SecureConfigStore.migrarTodoSiEsNecesario(ARCHIVO, KEY_PROP);
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
            p.load(fis);
        } catch (IOException ignored) { return null; }
        String raw = p.getProperty(KEY_PROP, "").trim();
        if (raw.isBlank()) return null;
        String v = SecureConfigStore.descifrar(raw);
        // si descifrar devolvió el mismo valor pero no era cifrado, es legacy en claro
        return v.isBlank() ? null : v.trim();
    }

    public static synchronized void guardarApiKey(String apiKey) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties p = new Properties();
            if (ARCHIVO.exists()) {
                try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) {}
            }
            if (apiKey == null || apiKey.isBlank()) {
                p.remove(KEY_PROP);
            } else {
                p.setProperty(KEY_PROP, SecureConfigStore.cifrar(apiKey.trim()));
            }
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Vendex - Gemini API Key (cifrado AES/GCM). Obtén gratis en https://aistudio.google.com/apikey\nPrioridad: env GEMINI_API_KEY > este archivo");
            }
        } catch (IOException ignored) {}
    }

    public static synchronized boolean tieneApiKey() {
        String k = obtenerApiKey();
        return k != null && !k.isBlank();
    }

    public static synchronized void borrarApiKey() {
        guardarApiKey(null);
    }
}

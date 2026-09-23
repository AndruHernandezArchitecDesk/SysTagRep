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

    /** Resuelve key con prioridad env var > archivo cifrado Tier2 (install master, compartido). */
    public static synchronized String obtenerApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("GEMINI_API_KEY");
        if (prop != null && !prop.isBlank()) return prop.trim();
        if (!ARCHIVO.exists()) return null;
        // migrar: claro→Tier2 y Tier1→Tier2 (compartido por instalación)
        migrarATier2SiEsNecesario();
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
            p.load(fis);
        } catch (IOException ignored) { return null; }
        String raw = p.getProperty(KEY_PROP, "").trim();
        if (raw.isBlank()) return null;
        // intentar Tier2 primero (install master), fallback Tier1 por máquina para compat
        try {
            String v = SecureConfigStore.descifrarConMasterKey(raw);
            if (v != null && !v.isBlank() && !v.equals(raw)) return v.trim();
            // si descifrarConMasterKey devolvió raw (no cifrado), intentar Tier1
            v = SecureConfigStore.descifrar(raw);
            return v.isBlank() ? null : v.trim();
        } catch (Exception e) {
            // si master no existe, fallback Tier1
            try { String v2 = SecureConfigStore.descifrar(raw); return v2.isBlank()?null:v2.trim(); } catch (Exception ignored2) { return null; }
        }
    }

    private static void migrarATier2SiEsNecesario() {
        if (!ARCHIVO.exists()) return;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) { return; }
        String raw = p.getProperty(KEY_PROP, "");
        if (raw == null || raw.isBlank()) return;
        raw = raw.trim();
        if (!SecureConfigStore.esCifrado(raw)) { // claro → Tier2
            try { p.setProperty(KEY_PROP, SecureConfigStore.cifrarConMasterKey(raw)); try (FileOutputStream fos=new FileOutputStream(ARCHIVO)){p.store(fos,"Migrado a Tier2 master por instalación");} } catch (Exception ignored) {}
            return;
        }
        // si es cifrado, verificar si es Tier2 o Tier1
        try {
            SecureConfigStore.descifrarConMasterKey(raw);
            return; // ya Tier2
        } catch (Exception e) {
            // intentar descifrar Tier1
            try {
                String plano = SecureConfigStore.descifrar(raw);
                if (plano != null && !plano.isBlank() && !plano.equals(raw)) {
                    p.setProperty(KEY_PROP, SecureConfigStore.cifrarConMasterKey(plano));
                    try (FileOutputStream fos=new FileOutputStream(ARCHIVO)){p.store(fos,"Migrado Tier1→Tier2");} catch (IOException ignored){}
                }
            } catch (Exception ignored) {}
        }
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
                // Tier2 compartido por instalación (portable si se copia archivo, requiere misma install master)
                String cifrado;
                try { cifrado = SecureConfigStore.cifrarConMasterKey(apiKey.trim()); }
                catch (Exception e) { // si master no existe, fallback Tier1 por máquina
                    cifrado = SecureConfigStore.cifrar(apiKey.trim());
                }
                p.setProperty(KEY_PROP, cifrado);
            }
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Vendex - Gemini API Key (cifrado Tier2 master por instalación). Obtén gratis en https://aistudio.google.com/apikey\nPrioridad: env GEMINI_API_KEY > este archivo");
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

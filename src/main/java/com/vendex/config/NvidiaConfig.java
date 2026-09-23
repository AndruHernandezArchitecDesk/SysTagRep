package com.vendex.config;

import com.vendex.util.SecureConfigStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persiste NVIDIA_API_KEY en ~/.vendex/nvidia.properties cifrado via SecureConfigStore.
 * Prioridad: env var NVIDIA_API_KEY > System property > archivo cifrado.
 * El hardcoded previo en NvidiaCodeService se elimina por completo (Fase 1).
 */
public final class NvidiaConfig {

    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "nvidia.properties");
    private static final String KEY_PROP = "nvidia.api_key";

    private NvidiaConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    public static synchronized String obtenerApiKey() {
        String env = System.getenv("NVIDIA_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("NVIDIA_API_KEY");
        if (prop != null && !prop.isBlank()) return prop.trim();
        if (!ARCHIVO.exists()) return null;
        migrarATier2SiEsNecesario();
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) { return null; }
        String raw = p.getProperty(KEY_PROP, "").trim();
        if (raw.isBlank()) return null;
        try {
            String v = SecureConfigStore.descifrarConMasterKey(raw);
            if (v != null && !v.isBlank() && !v.equals(raw)) return v.trim();
            v = SecureConfigStore.descifrar(raw);
            return v.isBlank() ? null : v.trim();
        } catch (Exception e) {
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
        if (!SecureConfigStore.esCifrado(raw)) {
            try { p.setProperty(KEY_PROP, SecureConfigStore.cifrarConMasterKey(raw)); try (FileOutputStream fos=new FileOutputStream(ARCHIVO)){p.store(fos,"Migrado a Tier2");} } catch (Exception ignored) {}
            return;
        }
        try { SecureConfigStore.descifrarConMasterKey(raw); return; } catch (Exception e) {
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
                String cifrado;
                try { cifrado = SecureConfigStore.cifrarConMasterKey(apiKey.trim()); }
                catch (Exception e) { cifrado = SecureConfigStore.cifrar(apiKey.trim()); }
                p.setProperty(KEY_PROP, cifrado);
            }
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Vendex - NVIDIA API Key (cifrado Tier2 master por instalación). Prioridad: env NVIDIA_API_KEY > este archivo");
            }
        } catch (IOException ignored) {}
    }

    public static synchronized boolean tieneApiKey() {
        String k = obtenerApiKey();
        return k != null && !k.isBlank();
    }

    public static synchronized void borrarApiKey() { guardarApiKey(null); }
}

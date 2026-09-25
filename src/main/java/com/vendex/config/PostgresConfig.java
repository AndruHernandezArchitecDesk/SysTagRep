package com.vendex.config;

import com.vendex.util.SecureConfigStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuración migrador postgres (superuser) para Flyway.
 * Solo en host central, no en clientes. Password puede venir de:
 * 1) env POSTGRES_PASSWORD / -Dpostgres.password (para baseline manual)
 * 2) archivo ~/.vendex/postgres.properties cifrado AES/GCM (persistido)
 * 3) secreto Tier1 "postgres.password" (keyring)
 */
public final class PostgresConfig {

    private static final Logger LOG = Logger.getLogger(PostgresConfig.class.getName());
    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "postgres.properties");
    private static final String SECRETO = "postgres.password";

    private PostgresConfig() {}

    public static String getPassword() {
        // 1) env var tiene prioridad (baseline manual)
        String env = System.getenv("POSTGRES_PASSWORD");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("postgres.password");
        if (prop != null && !prop.isBlank()) return prop.trim();

        // 2) secreto Tier1 keyring
        try {
            if (SecureConfigStore.existeSecretoLocal(SECRETO)) {
                String v = SecureConfigStore.obtenerSecretoLocal(SECRETO);
                if (v != null && !v.isBlank()) return v;
            }
        } catch (Exception ignored) {}

        // 3) archivo cifrado
        if (ARCHIVO.exists()) {
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
                String raw = p.getProperty("postgres.password");
                if (raw != null && !raw.isBlank()) {
                    raw = raw.trim();
                    if (SecureConfigStore.esCifrado(raw)) {
                        try { return SecureConfigStore.descifrar(raw); } catch (Exception e) { return raw; }
                    }
                    return raw;
                }
            } catch (IOException e) {
                LOG.warning("No se pudo leer postgres.properties: " + e.getMessage());
            }
        }
        return null;
    }

    public static void guardarPassword(String password) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password vacío");
        // guardar como secreto Tier1 keyring + archivo cifrado backup
        try { SecureConfigStore.guardarSecretoLocal(SECRETO, password.trim()); } catch (Exception e) { LOG.warning("No se pudo guardar en keyring: " + e.getMessage()); }
        try {
            if (!DIR.exists() && !DIR.mkdirs()) LOG.warning("No se pudo crear dir " + DIR);
            Properties p = new Properties();
            if (ARCHIVO.exists()) {
                try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) {}
            }
            p.setProperty("postgres.password", SecureConfigStore.cifrar(password.trim()));
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Flyway migrador postgres - cifrado AES/GCM (solo host central)");
            }
            LOG.info("Password postgres guardado cifrado en " + ARCHIVO.getAbsolutePath() + " y keyring");
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar postgres.properties", e);
        }
    }

    public static boolean tienePassword() {
        String p = getPassword();
        return p != null && !p.isBlank();
    }

    public static File getArchivo() { return ARCHIVO; }
}

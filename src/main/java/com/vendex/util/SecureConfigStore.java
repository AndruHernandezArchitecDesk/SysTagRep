package com.vendex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Wrapper único para leer/escribir configuraciones sensibles cifradas.
 * Fase 1 completa: usa {@link Cifrado} con master key del SO (keyring DPAPI/Keychain/libsecret
 * vía MasterKeyManager) con HKDF HmacSHA256; fallback a PBKDF2 legacy solo para descifrar
 * archivos viejos y migrarlos. Formato sin cambio: base64(sal):base64(iv):base64(cifrado) — no portable
 * entre máquinas con keyring activo (copiar archivo a otro equipo no descifra).
 *
 * <p>configuracion_email queda fuera de keyring (decisión 1) y sigue con Cifrado legacy si se usa
 * directamente; este Store ya es keyring para por-PC (db.password, firma, gemini, nvidia).</p>
 *
 * <p>Fallback headless/CI/Linux sin libsecret: ~/.vendex/.master.key (0600) + backup cifrado .bak (decisión 2,4).</p>
 */
public final class SecureConfigStore {

    private SecureConfigStore() {}

    public static String cifrar(String plano) {
        if (plano == null || plano.isEmpty()) return "";
        try {
            return Cifrado.encriptar(plano);
        } catch (Exception e) {
            throw new RuntimeException("Error cifrando valor", e);
        }
    }

    public static String descifrar(String almacenado) {
        if (almacenado == null || almacenado.isEmpty()) return "";
        // formato cifrado contiene ":" (sal:iv:cifrado). Si no, es legacy en claro.
        if (!almacenado.contains(":")) return almacenado;
        try {
            return Cifrado.desencriptar(almacenado);
        } catch (Exception e) {
            // si falla descifrado, devolver como está (posible valor en claro con ":")
            return almacenado;
        }
    }

    public static boolean esCifrado(String valor) {
        return valor != null && valor.contains(":") && valor.split(":", -1).length == 3;
    }

    /**
     * Carga un .properties cifrando/descifrando un key específico.
     * Si el archivo tiene el valor en claro, lo retorna igual (migración lazy).
     */
    public static String leer(File archivo, String key) {
        if (!archivo.exists()) return null;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) {
            p.load(fis);
        } catch (IOException ignored) { return null; }
        String raw = p.getProperty(key);
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return null;
        // si está cifrado, descifrar; si no, es legacy en claro
        if (esCifrado(raw)) return descifrar(raw);
        return raw;
    }

    public static void guardar(File archivo, String key, String valorPlano, String comentario) {
        try {
            File dir = archivo.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) return;
            Properties p = new Properties();
            if (archivo.exists()) {
                try (FileInputStream fis = new FileInputStream(archivo)) { p.load(fis); } catch (IOException ignored) {}
            }
            if (valorPlano == null || valorPlano.isBlank()) {
                p.remove(key);
            } else {
                p.setProperty(key, cifrar(valorPlano.trim()));
            }
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                p.store(fos, comentario);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error guardando config cifrada: " + archivo, e);
        }
    }

    /**
     * Migra un valor en claro a cifrado si aún no lo está. Retorna true si migró.
     */
    public static boolean migrarSiEsNecesario(File archivo, String key) {
        if (!archivo.exists()) return false;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) { p.load(fis); } catch (IOException e) { return false; }
        String raw = p.getProperty(key);
        if (raw == null || raw.trim().isEmpty() || esCifrado(raw.trim())) return false;
        String plano = raw.trim();
        p.setProperty(key, cifrar(plano));
        try (FileOutputStream fos = new FileOutputStream(archivo)) { p.store(fos, "Migrado a cifrado AES/GCM"); } catch (IOException e) { return false; }
        return true;
    }

    /**
     * Migra cifrado legacy (SECRETO embebido) a cifrado con master key del SO.
     * Retorna true si migró. No toca archivos ya cifrados con keyring ni claros.
     */
    public static boolean migrarLegacyCifradoSiEsNecesario(File archivo, String key) {
        if (!archivo.exists()) return false;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) { p.load(fis); } catch (IOException e) { return false; }
        String raw = p.getProperty(key);
        if (raw == null) return false;
        raw = raw.trim();
        if (!esCifrado(raw)) return false;
        // si descifra con master OK, ya es nuevo
        try {
            Cifrado.desencriptarConMaster(raw);
            return false;
        } catch (Exception e) {
            // master falla, probar legacy
            String plano;
            try {
                plano = Cifrado.desencriptarLegacy(raw);
            } catch (Exception e2) {
                return false; // corrupto
            }
            // re-cifrar con master key
            String nuevo = cifrar(plano);
            p.setProperty(key, nuevo);
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                p.store(fos, "Migrado a keyring AES/GCM (Fase 1 completa)");
            } catch (IOException ex) { return false; }
            return true;
        }
    }

    /** Migra tanto claro→cifrado como legacy→keyring. Retorna true si alguno migró. */
    public static boolean migrarTodoSiEsNecesario(File archivo, String key) {
        boolean a = migrarSiEsNecesario(archivo, key);
        boolean b = migrarLegacyCifradoSiEsNecesario(archivo, key);
        return a || b;
    }
}

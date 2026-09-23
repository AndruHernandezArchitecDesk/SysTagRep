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

    // ===== Tier 1 por máquina (db.password, p12.password) — master por máquina via Cifrado/MasterKeyManager =====
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

    // ===== Tier 1 directo por nombre (java-keyring) =====
    public static void guardarSecretoLocal(String nombre, String valor) {
        if (nombre == null || nombre.isBlank()) return;
        boolean ok = false;
        try {
            Class<?>kc = Class.forName("com.github.javakeyring.Keyring");
            Object kr = kc.getMethod("create").invoke(null);
            com.vendex.security.KeyringSecretProvider kp = new com.vendex.security.KeyringSecretProvider();
            if (kp.isAvailable()) {
                kc.getMethod("setPassword", String.class, String.class, String.class).invoke(kr, "Vendex", nombre, valor == null ? "" : valor);
                ok = true;
            }
        } catch (Exception ignored) {}
        // fallback file cifrado con master por máquina (no deja en claro)
        try {
            java.io.File f = new java.io.File(System.getProperty("user.home"), ".vendex/secrets/" + nombre + ".enc");
            f.getParentFile().mkdirs();
            String toStore = (valor == null || valor.isEmpty()) ? "" : Cifrado.encriptar(valor);
            java.nio.file.Files.writeString(f.toPath(), toStore, java.nio.charset.StandardCharsets.UTF_8);
            try { java.nio.file.Files.setPosixFilePermissions(f.toPath(), java.util.EnumSet.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE)); } catch (Exception ignored) { f.setReadable(false,false); f.setReadable(true,true); f.setWritable(false,false); f.setWritable(true,true); }
        } catch (Exception e) { if (!ok) throw new RuntimeException("No se pudo guardar secreto local " + nombre, e); }
    }

    public static String obtenerSecretoLocal(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new com.vendex.exception.SecretoNoEncontradoException(nombre);
        // keyring primero
        try {
            Class<?>kc = Class.forName("com.github.javakeyring.Keyring");
            Object kr = kc.getMethod("create").invoke(null);
            String v = (String) kc.getMethod("getPassword", String.class, String.class).invoke(kr, "Vendex", nombre);
            if (v != null && !v.isEmpty()) return v;
        } catch (Exception ignored) {}
        // fallback file
        try {
            java.io.File f = new java.io.File(System.getProperty("user.home"), ".vendex/secrets/" + nombre + ".enc");
            if (f.exists()) {
                String enc = java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!enc.isEmpty()) return Cifrado.desencriptar(enc);
            }
        } catch (Exception ignored) {}
        throw new com.vendex.exception.SecretoNoEncontradoException(nombre);
    }

    public static boolean existeSecretoLocal(String nombre) {
        try { obtenerSecretoLocal(nombre); return true; } catch (Exception e) { return false; }
    }

    // ===== Tier 2 compartido (clave maestra por instalación) =====
    public static String cifrarConMasterKey(String plano) {
        if (plano == null || plano.isEmpty()) return "";
        try {
            return Cifrado.encriptarConInstallMaster(plano);
        } catch (Exception e) {
            throw new RuntimeException("Error cifrando con master key", e);
        }
    }

    public static String descifrarConMasterKey(String cifrado) {
        if (cifrado == null || cifrado.isEmpty()) return "";
        if (!cifrado.contains(":")) return cifrado;
        try {
            return Cifrado.desencriptarConInstallMaster(cifrado);
        } catch (com.vendex.exception.SecretoNoEncontradoException se) { throw se; }
        catch (Exception e) {
            // si falla por clave incorrecta, lanzar mensaje claro en vez de críptico
            String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (m.contains("tag mismatch") || m.contains("bad") || m.contains("mismatch")) {
                throw new RuntimeException("No se pudo descifrar con la clave maestra de instalación. ¿Se importó la clave correcta en esta PC? (vendex-master-key)", e);
            }
            return cifrado;
        }
    }

    public static boolean existeMasterKey() { return com.vendex.security.InstallMasterKeyManager.existeMasterKey(); }
    public static void generarNuevaMasterKey() { com.vendex.security.InstallMasterKeyManager.generarNuevaMasterKey(); }
    public static void importarMasterKey(String base64) { com.vendex.security.InstallMasterKeyManager.importarMasterKey(base64); }
    public static String exportarMasterKeyParaRespaldo() { return com.vendex.security.InstallMasterKeyManager.exportarMasterKeyParaRespaldo(); }

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

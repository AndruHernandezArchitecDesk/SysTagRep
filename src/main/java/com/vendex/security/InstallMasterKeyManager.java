package com.vendex.security;

import com.vendex.exception.SecretoNoEncontradoException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Clave maestra por instalación (Tier 2). No es por máquina: se genera una vez y se copia manualmente a cada PC.
 * Se guarda como secreto Tier 1 "vendex-master-key" (keyring si disponible, fallback file ~/.vendex/.install-master.key).
 */
public final class InstallMasterKeyManager {

    private static final Logger LOG = Logger.getLogger(InstallMasterKeyManager.class.getName());
    private static final String NOMBRE_SECRETO = "vendex-master-key";
    private static final String SERVICE = "Vendex";
    private static final int KEY_BYTES = 32;
    private static final File FALLBACK_FILE = new File(System.getProperty("user.home"), ".vendex/.install-master.key");
    private static final File BACKUP_FILE = new File(System.getProperty("user.home"), ".vendex/.install-master.key.bak");

    private InstallMasterKeyManager() {}

    public static boolean existeMasterKey() {
        // probar keyring
        try {
            KeyringSecretProvider kp = new KeyringSecretProvider();
            if (kp.isAvailable()) {
                String existing = tryGetKeyring(NOMBRE_SECRETO);
                if (existing != null && !existing.isBlank()) return true;
            }
        } catch (Exception ignored) {}
        return FALLBACK_FILE.exists();
    }

    public static void generarNuevaMasterKey() {
        byte[] key = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(key);
        String b64 = Base64.getEncoder().encodeToString(key);
        guardarEnTier1(b64);
        LOG.info("install master key generada y guardada como Tier1 " + NOMBRE_SECRETO);
    }

    public static void importarMasterKey(String base64) {
        if (base64 == null || base64.isBlank()) throw new IllegalArgumentException("clave vacía");
        String b64 = base64.trim();
        // validar base64 32 bytes
        try {
            byte[] decoded = Base64.getDecoder().decode(b64);
            if (decoded.length != KEY_BYTES) throw new IllegalArgumentException("longitud inválida: esperado 32 bytes");
        } catch (Exception e) {
            throw new IllegalArgumentException("clave base64 inválida: " + e.getMessage(), e);
        }
        guardarEnTier1(b64);
        LOG.info("install master key importada");
    }

    public static String exportarMasterKeyParaRespaldo() {
        String b64 = obtenerMasterKeyBase64();
        if (b64 == null) throw new SecretoNoEncontradoException(NOMBRE_SECRETO);
        return b64;
    }

    static String obtenerMasterKeyBase64() {
        // keyring primero
        try {
            KeyringSecretProvider kp = new KeyringSecretProvider();
            if (kp.isAvailable()) {
                String existing = tryGetKeyring(NOMBRE_SECRETO);
                if (existing != null && !existing.isBlank()) return existing.trim();
            }
        } catch (Exception ignored) {}
        // fallback file
        if (FALLBACK_FILE.exists()) {
            try {
                String content = Files.readString(FALLBACK_FILE.toPath(), StandardCharsets.UTF_8).trim();
                // si contiene ENC:..., descifrar legacy
                if (content.startsWith("ENC:")) {
                    String enc = content.substring(4).trim().split("\n")[0];
                    try {
                        return com.vendex.util.Cifrado.desencriptarLegacy(enc);
                    } catch (Exception e) {
                        return content;
                    }
                }
                // primera línea base64
                return content.split("\n")[0].trim();
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static byte[] obtenerMasterKeyBytes() {
        String b64 = obtenerMasterKeyBase64();
        if (b64 == null || b64.isBlank()) throw new SecretoNoEncontradoException(NOMBRE_SECRETO);
        try {
            return Base64.getDecoder().decode(b64.trim());
        } catch (Exception e) {
            throw new RuntimeException("install master key base64 corrupta", e);
        }
    }

    private static void guardarEnTier1(String b64) {
        boolean guardadoKeyring = false;
        try {
            KeyringSecretProvider kp = new KeyringSecretProvider();
            if (kp.isAvailable()) {
                trySetKeyring(NOMBRE_SECRETO, b64);
                guardadoKeyring = true;
            }
        } catch (Exception e) {
            LOG.fine("guardar install master en keyring falló, usando fallback file: " + e.getMessage());
        }
        // siempre guardar fallback file también como respaldo (cifrado legacy para no dejar en claro sin protección extra)
        try {
            File dir = FALLBACK_FILE.getParentFile();
            if (!dir.exists()) dir.mkdirs();
            // guardar también backup cifrado
            String toStore = b64;
            // fallback file en claro base64 + backup ENC:
            Files.writeString(FALLBACK_FILE.toPath(), b64, StandardCharsets.UTF_8);
            try { Files.setPosixFilePermissions(FALLBACK_FILE.toPath(), java.util.EnumSet.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE)); } catch (Exception ignored) { FALLBACK_FILE.setReadable(false,false); FALLBACK_FILE.setReadable(true,true); FALLBACK_FILE.setWritable(false,false); FALLBACK_FILE.setWritable(true,true); }
            // backup
            if (!BACKUP_FILE.exists()) {
                String enc = null;
                try { enc = com.vendex.util.Cifrado.encriptarLegacy(b64); } catch (Exception ignored) {}
                String bakContent = (enc != null ? "ENC:" + enc : b64) + "\n# Backup install master key Vendex - guardar fuera de PCs. Si se pierden todas las PCs sin respaldo, Tier2 irrecuperable\n";
                Files.writeString(BACKUP_FILE.toPath(), bakContent, StandardCharsets.UTF_8);
                try { Files.setPosixFilePermissions(BACKUP_FILE.toPath(), java.util.EnumSet.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE)); } catch (Exception ignored) {}
            }
            if (!guardadoKeyring) LOG.info("install master key guardada en fallback file " + FALLBACK_FILE.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar install master key", e);
        }
    }

    private static String tryGetKeyring(String account) {
        try {
            Class<?>kc = Class.forName("com.github.javakeyring.Keyring");
            Object kr = kc.getMethod("create").invoke(null);
            return (String) kc.getMethod("getPassword", String.class, String.class).invoke(kr, SERVICE, account);
        } catch (Exception e) { return null; }
    }
    private static void trySetKeyring(String account, String value) throws Exception {
        Class<?>kc = Class.forName("com.github.javakeyring.Keyring");
        Object kr = kc.getMethod("create").invoke(null);
        kc.getMethod("setPassword", String.class, String.class, String.class).invoke(kr, SERVICE, account, value);
    }
}

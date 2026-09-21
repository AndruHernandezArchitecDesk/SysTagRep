package com.vendex.security;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Fallback para headless/CI/Linux sin libsecret (decisión 2).
 * Guarda master key 32 bytes base64 en ~/.vendex/.master.key (0600).
 * Además crea backup cifrado ~/.vendex/.master.key.bak (base64 mismo contenido) para recuperación tras reinstall OS (decisión 4).
 * El backup se cifra con Cifrado legacy (SECRETO embebido) para no dejarlo en claro sin protección adicional.
 */
public class FallbackFileProvider implements SecretProvider {

    private static final Logger LOG = Logger.getLogger(FallbackFileProvider.class.getName());
    private static final int KEY_BYTES = 32;

    private final File dir;
    private final File masterFile;
    private final File backupFile;

    public FallbackFileProvider() {
        this.dir = new File(System.getProperty("user.home"), ".vendex");
        this.masterFile = new File(dir, ".master.key");
        this.backupFile = new File(dir, ".master.key.bak");
    }

    // para tests con temp dir
    FallbackFileProvider(File dir) {
        this.dir = dir;
        this.masterFile = new File(dir, ".master.key");
        this.backupFile = new File(dir, ".master.key.bak");
    }

    @Override public String getName() { return "fallback-file"; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public synchronized byte[] getOrCreateMasterKey() throws Exception {
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("no se pudo crear " + dir);
        if (masterFile.exists()) {
            String b64 = Files.readString(masterFile.toPath(), StandardCharsets.UTF_8).trim();
            if (!b64.isEmpty()) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(b64);
                    if (decoded.length == KEY_BYTES) {
                        ensureBackup(decoded);
                        restrictPermissions(masterFile);
                        return decoded;
                    }
                } catch (Exception e) {
                    LOG.warning("fallback master key corrupta, regenerando");
                }
            }
        }
        byte[] key = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(key);
        String b64 = Base64.getEncoder().encodeToString(key);
        Files.writeString(masterFile.toPath(), b64, StandardCharsets.UTF_8);
        restrictPermissions(masterFile);
        ensureBackup(key);
        LOG.info("fallback master key generada en " + masterFile.getAbsolutePath());
        return key;
    }

    private void ensureBackup(byte[] key) {
        try {
            if (!backupFile.exists()) {
                // backup cifrado con Cifrado legacy para no dejar en claro duplicado sin protección
                // si Cifrado no disponible aún, guardar base64 con comentario
                String b64 = Base64.getEncoder().encodeToString(key);
                String toStore = b64;
                try {
                    // intentar cifrar backup con legacy SECRET (Fase 1 completa backup cifrado decisión 4)
                    Class<?> cifrado = Class.forName("com.vendex.util.Cifrado");
                    // usar método que cifra con master actual sería circular; usar encriptarLegacy si existe
                    try {
                        String enc = (String) cifrado.getMethod("encriptarLegacy", String.class).invoke(null, b64);
                        toStore = "ENC:" + enc;
                    } catch (NoSuchMethodException ns) {
                        // fallback simple
                        toStore = b64;
                    }
                } catch (Exception ignored) {}
                Files.writeString(backupFile.toPath(), toStore + "\n# Backup master key Vendex - guardar en lugar seguro. Si reinstalas OS, restaura .master.key\n", StandardCharsets.UTF_8);
                restrictPermissions(backupFile);
                LOG.info("backup master key creado en " + backupFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.warning("no se pudo crear backup master key: " + e.getMessage());
        }
    }

    private void restrictPermissions(File f) {
        try {
            // POSIX 0600 si soportado
            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(f.toPath(), perms);
        } catch (Exception ignored) {
            // Windows fallback
            f.setReadable(false, false);
            f.setReadable(true, true);
            f.setWritable(false, false);
            f.setWritable(true, true);
            f.setExecutable(false, false);
        }
    }

    File getMasterFile() { return masterFile; }
    File getBackupFile() { return backupFile; }
}

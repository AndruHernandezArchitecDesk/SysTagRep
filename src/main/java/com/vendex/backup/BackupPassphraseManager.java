package com.vendex.backup;

import com.vendex.util.SecureConfigStore;

import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Gestiona la passphrase de cifrado GPG para backups.
 * Almacenada como secreto Tier1 "backup.passphrase" (keyring DPAPI / fallback file ~/.vendex/secrets/backup.passphrase.enc).
 * No va en claro en scripts ni en backup.properties.
 */
public final class BackupPassphraseManager {

    private static final Logger LOG = Logger.getLogger(BackupPassphraseManager.class.getName());
    private static final String NOMBRE_SECRETO = "backup.passphrase";
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_!@#$%&*";
    private static final int LONGITUD = 32;

    private BackupPassphraseManager() {}

    public static boolean existe() {
        return SecureConfigStore.existeSecretoLocal(NOMBRE_SECRETO);
    }

    public static String obtener() {
        try {
            return SecureConfigStore.obtenerSecretoLocal(NOMBRE_SECRETO);
        } catch (Exception e) {
            throw new RuntimeException("No existe passphrase de backup. Genere una desde Administración → Respaldos.", e);
        }
    }

    public static String obtenerOCrear() {
        if (existe()) {
            try { return obtener(); } catch (Exception ignored) {}
        }
        return generarYGuardar();
    }

    public static String generarYGuardar() {
        SecureRandom sr = new SecureRandom();
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(ALFABETO.charAt(sr.nextInt(ALFABETO.length())));
        }
        String nueva = sb.toString();
        SecureConfigStore.guardarSecretoLocal(NOMBRE_SECRETO, nueva);
        LOG.info("Passphrase de backup generada y guardada como secreto local " + NOMBRE_SECRETO);
        return nueva;
    }

    public static void guardar(String passphrase) {
        if (passphrase == null || passphrase.isBlank()) throw new IllegalArgumentException("Passphrase vacía");
        SecureConfigStore.guardarSecretoLocal(NOMBRE_SECRETO, passphrase.trim());
    }
}

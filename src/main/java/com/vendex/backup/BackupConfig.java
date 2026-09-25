package com.vendex.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuracion de backup persistida en ~/.vendex/backup.properties
 * Windows: C:\\Users\\user\\.vendex\\backup.properties
 * Valores por defecto pensados para Windows + copia a otra PC en red.
 */
public final class BackupConfig {

    private static final Logger LOG = Logger.getLogger(BackupConfig.class.getName());
    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "backup.properties");

    // defaults
    public static final String DEFAULT_DIR_WINDOWS = "C:\\Vendex\\backups";
    public static final String DEFAULT_DIR_FALLBACK = new File(DIR, "backups").getAbsolutePath();
    public static final String DEFAULT_EMAIL = "andresrockfull@gmail.com";
    public static final String DEFAULT_PG_DUMP = "pg_dump";
    public static final String DEFAULT_PG_RESTORE = "pg_restore";
    public static final String DEFAULT_GPG = "gpg";

    private BackupConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    public static Properties cargar() {
        Properties p = new Properties();
        // defaults primero
        p.setProperty("backup.dir", defaultBackupDir());
        p.setProperty("backup.offsite.path", "");
        p.setProperty("backup.email.destino", DEFAULT_EMAIL);
        p.setProperty("backup.pg_dump.path", DEFAULT_PG_DUMP);
        p.setProperty("backup.pg_restore.path", DEFAULT_PG_RESTORE);
        p.setProperty("backup.gpg.path", DEFAULT_GPG);
        p.setProperty("backup.cifrado", "true");
        p.setProperty("backup.retention.daily", "7");
        p.setProperty("backup.retention.weekly", "4");
        p.setProperty("backup.retention.monthly", "12");
        // notificar solo falla por defecto, semanal opcional
        p.setProperty("backup.notificar.exito", "false");
        p.setProperty("backup.notificar.resumen_semanal", "false");

        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                Properties loaded = new Properties();
                loaded.load(fis);
                // merge: loaded overrides defaults
                for (String k : loaded.stringPropertyNames()) {
                    p.setProperty(k, loaded.getProperty(k));
                }
            } catch (IOException e) {
                LOG.warning("No se pudo leer backup.properties: " + e.getMessage());
            }
        }
        return p;
    }

    public static void guardar(Properties p) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) {
                LOG.warning("No se pudo crear dir " + DIR);
            }
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Vendex Backup - Configuracion. backup.dir=local, backup.offsite.path=\\\\OTRA-PC\\VendexBackups, backup.email.destino=destinatario alertas");
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar backup.properties", e);
        }
    }

    private static String defaultBackupDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return DEFAULT_DIR_WINDOWS;
        }
        return DEFAULT_DIR_FALLBACK;
    }

    public static String getBackupDir() { return cargar().getProperty("backup.dir", defaultBackupDir()); }
    public static String getOffsitePath() { return cargar().getProperty("backup.offsite.path", "").trim(); }
    public static String getEmailDestino() { return cargar().getProperty("backup.email.destino", DEFAULT_EMAIL).trim(); }
    public static String getPgDumpPath() { return cargar().getProperty("backup.pg_dump.path", DEFAULT_PG_DUMP).trim(); }
    public static String getPgRestorePath() { return cargar().getProperty("backup.pg_restore.path", DEFAULT_PG_RESTORE).trim(); }
    public static String getGpgPath() { return cargar().getProperty("backup.gpg.path", DEFAULT_GPG).trim(); }
    public static boolean isCifrado() { return Boolean.parseBoolean(cargar().getProperty("backup.cifrado", "true")); }
    public static int getRetentionDaily() { return parseInt(cargar().getProperty("backup.retention.daily"), 7); }
    public static int getRetentionWeekly() { return parseInt(cargar().getProperty("backup.retention.weekly"), 4); }
    public static int getRetentionMonthly() { return parseInt(cargar().getProperty("backup.retention.monthly"), 12); }
    public static boolean isNotificarExito() { return Boolean.parseBoolean(cargar().getProperty("backup.notificar.exito", "false")); }

    private static int parseInt(String v, int def) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return def; }
    }
}

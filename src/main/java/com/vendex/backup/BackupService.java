package com.vendex.backup;

import com.vendex.config.DbConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servicio principal de backup: pg_dump custom + verificacion + cifrado GPG + copia offsite + retencion.
 * Disenado para Windows (host vendex-db, IP varia por cliente) con pg_dump y gpg en PATH.
 * Hostname vendex-db resoluble via C:\Windows\System32\drivers\etc\hosts (ver docs/hosts_setup.md).
 * <p>
 * Flujo Recomendado (lineamiento §2): pg_dump --format=custom --compress=9, verify no vacio,
 * gpg --symmetric AES256 con passphrase en SecureConfigStore Tier1, copy a \\backup-pc\share,
 * notificacion andresrockfull@gmail.com.
 */
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class.getName());
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final long MIN_SIZE_BYTES = 10 * 1024; // 10KB mínimo para detectar dump vacío/sospechoso

    public BackupResult ejecutar() {
        long inicio = System.currentTimeMillis();
        File dirLocal = null;
        File archivoDump = null;
        File archivoCifrado = null;
        try {
            String backupDirStr = BackupConfig.getBackupDir();
            dirLocal = new File(backupDirStr);
            if (!dirLocal.exists() && !dirLocal.mkdirs()) {
                String msg = "No se pudo crear directorio de backup: " + backupDirStr;
                LOG.severe(msg);
                return BackupResult.falla(msg);
            }

            // Resolver conexión BD sin exponer en logs
            String[] cfg = DbConfig.cargar();
            String url = cfg[0];
            String user = cfg[1];
            String password = cfg[2];
            String host = extraerHost(url);
            String dbName = extraerDbName(url);
            String puerto = extraerPuerto(url);

            String pgDumpPath = BackupConfig.getPgDumpPath();
            if (!existeEjecutable(pgDumpPath)) {
                String msg = "pg_dump no encontrado en: " + pgDumpPath + ". Instale PostgreSQL o configure backup.pg_dump.path en ~/.vendex/backup.properties";
                LOG.severe(msg);
                return BackupResult.falla(msg);
            }

            String fecha = LocalDateTime.now().format(FECHA_FMT);
            String nombreBase = "vendex_" + fecha;
            archivoDump = new File(dirLocal, nombreBase + ".dump");

            // 1. pg_dump custom
            LOG.info("Iniciando pg_dump -> " + archivoDump.getAbsolutePath() + " host=" + host + " db=" + dbName + " user=" + user);
            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpPath,
                    "-h", host,
                    "-p", puerto,
                    "-U", user,
                    "-d", dbName,
                    "--format=custom",
                    "--compress=9",
                    "--no-password",
                    "-f", archivoDump.getAbsolutePath()
            );
            pb.environment().put("PGPASSWORD", password);
            // En Windows, ocultar ventana
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new BufferedReader(new InputStreamReader(proc.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            boolean termino = proc.waitFor(30, TimeUnit.MINUTES);
            if (!termino) {
                proc.destroyForcibly();
                String msg = "pg_dump timeout (30m): " + output;
                LOG.severe(msg);
                return BackupResult.falla(msg);
            }
            int exit = proc.exitValue();
            if (exit != 0) {
                String msg = "pg_dump falló exit=" + exit + " output=" + output;
                LOG.severe(msg);
                return BackupResult.falla(msg);
            }

            // 2. Verificación básica: existe y no vacío
            if (!archivoDump.exists() || archivoDump.length() == 0) {
                String msg = "Backup vacío o no creado: " + archivoDump.getAbsolutePath() + " output=" + output;
                LOG.severe(msg);
                return BackupResult.falla(msg);
            }
            if (archivoDump.length() < MIN_SIZE_BYTES) {
                LOG.warning("Backup sospechosamente pequeño (" + archivoDump.length() + " bytes) — posible dump incompleto: " + archivoDump.getName());
                // no fallar, pero alertar
            }

            // 3. Cifrado GPG si habilitado
            File archivoFinal = archivoDump;
            if (BackupConfig.isCifrado()) {
                String gpgPath = BackupConfig.getGpgPath();
                if (!existeEjecutable(gpgPath)) {
                    LOG.warning("gpg no encontrado en " + gpgPath + " — se guarda sin cifrar. Instale Gpg4win y configure backup.gpg.path");
                } else {
                    String passphrase;
                    try {
                        passphrase = BackupPassphraseManager.obtenerOCrear();
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "No se pudo obtener passphrase backup, guardando sin cifrar", e);
                        passphrase = null;
                    }
                    if (passphrase != null) {
                        archivoCifrado = new File(dirLocal, nombreBase + ".dump.gpg");
                        LOG.info("Cifrando backup con gpg AES256 -> " + archivoCifrado.getName());
                        // Usar --batch --yes --passphrase para no interactivo; en Windows passphrase via arg (cuidado ps) -> alternativa via env
                        // Mejor usar --passphrase y ocultar de logs; no loguear passphrase
                        ProcessBuilder gpgPb = new ProcessBuilder(
                                gpgPath,
                                "--symmetric",
                                "--cipher-algo", "AES256",
                                "--batch", "--yes",
                                "--passphrase", passphrase,
                                "--output", archivoCifrado.getAbsolutePath(),
                                archivoDump.getAbsolutePath()
                        );
                        gpgPb.redirectErrorStream(true);
                        Process gpgProc = gpgPb.start();
                        String gpgOut = new BufferedReader(new InputStreamReader(gpgProc.getInputStream()))
                                .lines().collect(Collectors.joining("\n"));
                        boolean gpgDone = gpgProc.waitFor(5, TimeUnit.MINUTES);
                        if (!gpgDone) {
                            gpgProc.destroyForcibly();
                            LOG.warning("gpg timeout, se conserva dump sin cifrar. out=" + gpgOut);
                        } else if (gpgProc.exitValue() != 0) {
                            LOG.warning("gpg falló exit=" + gpgProc.exitValue() + " out=" + gpgOut + " — se conserva sin cifrar");
                            archivoCifrado = null;
                        } else {
                            if (!archivoCifrado.exists() || archivoCifrado.length() == 0) {
                                LOG.warning("gpg no generó archivo o vacío — se conserva sin cifrar");
                                archivoCifrado = null;
                            } else {
                                // borrar original sin cifrar tras cifrar ok (opcional: conservar cifrado solamente)
                                try {
                                    Files.deleteIfExists(archivoDump.toPath());
                                    LOG.info("Dump sin cifrar eliminado tras cifrar ok");
                                } catch (Exception e) {
                                    LOG.warning("No se pudo borrar dump sin cifrar: " + e.getMessage());
                                }
                                archivoFinal = archivoCifrado;
                            }
                        }
                    }
                }
            }

            long tamanio = archivoFinal.length();
            long duracion = System.currentTimeMillis() - inicio;

            // 4. Copia offsite a otra PC en red (\\HOST\share)
            boolean offsiteOk = false;
            String offsiteDest = null;
            String offsiteErr = null;
            String offsitePath = BackupConfig.getOffsitePath();
            if (offsitePath != null && !offsitePath.isBlank()) {
                try {
                    File offsiteDir = new File(offsitePath);
                    if (!offsiteDir.exists()) {
                        // intentar crear
                        boolean creado = offsiteDir.mkdirs();
                        if (!creado && !offsiteDir.exists()) {
                            throw new RuntimeException("No se pudo crear/acceder offsite dir: " + offsitePath);
                        }
                    }
                    File destinoOffsite = new File(offsiteDir, archivoFinal.getName());
                    Files.copy(archivoFinal.toPath(), destinoOffsite.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    offsiteOk = destinoOffsite.exists() && destinoOffsite.length() == tamanio;
                    offsiteDest = destinoOffsite.getAbsolutePath();
                    if (offsiteOk) {
                        LOG.info("Copia offsite OK -> " + offsiteDest);
                    } else {
                        offsiteErr = "Copia offsite tamaño mismatch";
                    }
                } catch (Exception e) {
                    offsiteErr = e.getMessage();
                    LOG.log(Level.WARNING, "Fallo copia offsite a " + offsitePath, e);
                }
            } else {
                offsiteErr = "Offsite no configurado (backup.offsite.path vacio). Configurar \\\\backup-pc\\VendexBackups (ver docs/hosts_setup.md)";
                LOG.info(offsiteErr);
            }

            // 5. Retención GFS
            try {
                BackupRetentionService.limpiarConConfig(dirLocal);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Fallo retención", e);
            }

            String mensaje = "Backup OK: " + archivoFinal.getName() + " (" + formatSize(tamanio) + ")"
                    + (offsiteOk ? " + offsite OK" : " offsite=" + offsiteErr);
            LOG.info(mensaje);
            return new BackupResult(true, mensaje, archivoFinal, archivoDump.exists() ? archivoDump : null,
                    tamanio, duracion, offsiteOk, offsiteDest, offsiteErr);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error ejecutando backup", e);
            return new BackupResult(false, "Excepción: " + e.getMessage(), archivoCifrado != null ? archivoCifrado : archivoDump,
                    archivoDump, 0, System.currentTimeMillis()-inicio, false, null, e.getMessage());
        }
    }

    private boolean existeEjecutable(String path) {
        if (path == null || path.isBlank()) return false;
        // si es solo nombre (sin separador), asumir en PATH y probar --version rápido
        if (!path.contains(File.separator) && !path.contains("/")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(path, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                boolean done = p.waitFor(5, TimeUnit.SECONDS);
                return done && p.exitValue() == 0;
            } catch (Exception e) {
                // si no se puede ejecutar, asumir que existe y dejar que pg_dump falle con mensaje claro
                return true;
            }
        }
        File f = new File(path);
        return f.exists() && f.canExecute();
    }

    private String extraerHost(String url) {
        try {
            // jdbc:postgresql://host:port/db
            String withoutPrefix = url.replace("jdbc:postgresql://", "");
            String hostPort = withoutPrefix.split("/")[0];
            String host = hostPort.split(":")[0];
            if (host.isBlank()) return "localhost";
            return host;
        } catch (Exception e) { return "localhost"; }
    }

    private String extraerPuerto(String url) {
        try {
            String withoutPrefix = url.replace("jdbc:postgresql://", "");
            String hostPort = withoutPrefix.split("/")[0];
            if (hostPort.contains(":")) return hostPort.split(":")[1];
            return "5432";
        } catch (Exception e) { return "5432"; }
    }

    private String extraerDbName(String url) {
        try {
            String withoutPrefix = url.replace("jdbc:postgresql://", "");
            String[] parts = withoutPrefix.split("/", 2);
            if (parts.length < 2) return "dbVendex";
            String dbAndParams = parts[1];
            String db = dbAndParams.split("\\?")[0];
            if (db.isBlank()) return "dbVendex";
            return db;
        } catch (Exception e) { return "dbVendex"; }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024*1024) return String.format("%.1f KB", bytes/1024.0);
        return String.format("%.2f MB", bytes/(1024.0*1024));
    }
}

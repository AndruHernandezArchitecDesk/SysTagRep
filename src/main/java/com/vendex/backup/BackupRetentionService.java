package com.vendex.backup;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retención GFS simple para backups: daily 7, weekly 4, monthly 12.
 * Basado en nombre archivo vendex_yyyyMMdd_HHmmss.dump.gpg
 */
public final class BackupRetentionService {

    private static final Logger LOG = Logger.getLogger(BackupRetentionService.class.getName());
    private static final Pattern PATTERN = Pattern.compile("vendex_(\\d{8})_(\\d{6})\\.dump(?:\\.gpg)?");

    private BackupRetentionService() {}

    public static class LimpiezaResult {
        public final int conservados;
        public final int eliminados;
        public final List<String> eliminadosNombres;
        public LimpiezaResult(int conservados, int eliminados, List<String> nombres) {
            this.conservados = conservados;
            this.eliminados = eliminados;
            this.eliminadosNombres = nombres;
        }
    }

    public static LimpiezaResult limpiar(File directorio, int daily, int weekly, int monthly) {
        if (directorio == null || !directorio.isDirectory()) {
            return new LimpiezaResult(0,0, List.of());
        }
        File[] archivos = directorio.listFiles((d, n) -> n.startsWith("vendex_") && (n.endsWith(".dump.gpg") || n.endsWith(".dump")));
        if (archivos == null || archivos.length == 0) return new LimpiezaResult(0,0, List.of());

        // ordenar por fecha descendente (extraída del nombre; fallback lastModified)
        List<File> lista = new ArrayList<>(Arrays.asList(archivos));
        lista.sort((a,b) -> {
            LocalDate da = extraerFecha(a.getName());
            LocalDate db = extraerFecha(b.getName());
            if (da != null && db != null) {
                int cmp = db.compareTo(da); // desc
                if (cmp != 0) return cmp;
            }
            return Long.compare(b.lastModified(), a.lastModified());
        });

        Set<File> conservar = new LinkedHashSet<>();

        // daily: últimos N
        for (int i = 0; i < Math.min(daily, lista.size()); i++) {
            conservar.add(lista.get(i));
        }

        // weekly: 1 por semana (domingo como referencia) últimos weekly distintos
        // simplificación: agrupar por semana ISO y conservar el más reciente de cada semana que no esté ya conservado
        Map<String, File> porSemana = new LinkedHashMap<>();
        WeekFields wf = WeekFields.ISO;
        for (File f : lista) {
            if (conservar.contains(f)) continue;
            LocalDate d = extraerFecha(f.getName());
            if (d == null) d = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
            String semanaKey = d.get(wf.weekBasedYear()) + "-W" + String.format("%02d", d.get(wf.weekOfWeekBasedYear()));
            if (!porSemana.containsKey(semanaKey) && porSemana.size() < weekly) {
                porSemana.put(semanaKey, f);
            }
        }
        conservar.addAll(porSemana.values());

        // monthly: 1 por mes últimos monthly
        Map<String, File> porMes = new LinkedHashMap<>();
        for (File f : lista) {
            if (conservar.contains(f)) continue;
            LocalDate d = extraerFecha(f.getName());
            if (d == null) d = Instant.ofEpochMilli(f.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
            String mesKey = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            if (!porMes.containsKey(mesKey) && porMes.size() < monthly) {
                porMes.put(mesKey, f);
            }
        }
        conservar.addAll(porMes.values());

        List<String> eliminados = new ArrayList<>();
        for (File f : lista) {
            if (!conservar.contains(f)) {
                try {
                    if (f.delete()) {
                        eliminados.add(f.getName());
                    } else {
                        LOG.warning("No se pudo eliminar backup antiguo: " + f.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOG.warning("Error eliminando " + f.getName() + ": " + e.getMessage());
                }
            }
        }
        LOG.info("Retención: conservados=" + conservar.size() + " eliminados=" + eliminados.size());
        return new LimpiezaResult(conservar.size(), eliminados.size(), eliminados);
    }

    public static LimpiezaResult limpiarConConfig(File dir) {
        return limpiar(dir, BackupConfig.getRetentionDaily(), BackupConfig.getRetentionWeekly(), BackupConfig.getRetentionMonthly());
    }

    private static LocalDate extraerFecha(String nombre) {
        try {
            Matcher m = PATTERN.matcher(nombre);
            if (m.find()) {
                String ymd = m.group(1);
                int y = Integer.parseInt(ymd.substring(0,4));
                int mo = Integer.parseInt(ymd.substring(4,6));
                int d = Integer.parseInt(ymd.substring(6,8));
                return LocalDate.of(y, mo, d);
            }
        } catch (Exception ignored) {}
        return null;
    }
}

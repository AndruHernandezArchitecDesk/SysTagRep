package com.vendex.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Carpeta diaria de comprobantes: Documentos/&lt;Empresa&gt;/yyyy-MM-dd/.
 * En Windows usa "Documents", en el resto "Documentos" (igual que etiquetas).
 * El nombre de empresa sale de la ficha (Mi Empresa), sanitizado; fallback VENDEX.
 */
public final class DirectorioComprobantes {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DirectorioComprobantes() {}

    public static Path baseDocumentos() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        Path docs = os.contains("win") ? Paths.get(home, "Documents") : Paths.get(home, "Documentos");
        try {
            Files.createDirectories(docs);
        } catch (Exception ignored) {}
        return docs;
    }

    /** Visible para tests. */
    static String sanitizar(String nombre) {
        if (nombre == null) return "VENDEX";
        String limpio = nombre.trim().replaceAll("[^A-Za-z0-9ñÑáéíóúÁÉÍÓÚüÜ ._-]", "").trim();
        if (limpio.isEmpty()) return "VENDEX";
        return limpio.length() > 60 ? limpio.substring(0, 60).trim() : limpio;
    }

    public static String nombreEmpresaCarpeta() {
        try {
            var lista = com.vendex.config.AppContext.getInstance().empresaDAO.listar();
            if (lista != null && !lista.isEmpty()) {
                return sanitizar(lista.get(0).getRazonSocial());
            }
        } catch (Exception ignored) {}
        return "VENDEX";
    }

    public static Path dirDia(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        Path dir = baseDocumentos().resolve(nombreEmpresaCarpeta()).resolve(dia.format(FORMATO_DIA));
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        return dir;
    }

    public static Path dirDia() {
        return dirDia(LocalDate.now());
    }

    public static File dirDiaFile() {
        return dirDia().toFile();
    }

    public static File dirDiaFile(LocalDate fecha) {
        return dirDia(fecha).toFile();
    }
}

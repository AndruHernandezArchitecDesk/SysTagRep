package com.vendex.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Carpeta por tipo de comprobante: Documentos/&lt;Empresa&gt;/&lt;Tipo&gt;/yyyy-MM-dd/.
 * Nombres largos: Factura, NotaCredito, NotaDebito, GuiaRemision, Retencion, NotaVenta.
 * En Windows usa "Documents", en el resto "Documentos" (igual que etiquetas).
 * El nombre de empresa sale de la ficha (Mi Empresa), sanitizado; fallback VENDEX.
 * Lo antiguo (Documentos/&lt;Empresa&gt;/yyyy-MM-dd/) se mantiene solo como fallback de lectura.
 */
public final class DirectorioComprobantes {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final String CARPETA_FACTURA = "Factura";
    public static final String CARPETA_NOTA_CREDITO = "NotaCredito";
    public static final String CARPETA_NOTA_DEBITO = "NotaDebito";
    public static final String CARPETA_GUIA_REMISION = "GuiaRemision";
    public static final String CARPETA_RETENCION = "Retencion";
    public static final String CARPETA_NOTA_VENTA = "NotaVenta";

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

    /** Normaliza cualquier alias (01/04/05/06/07, FACTURA, NOTA_CREDITO, NC, etc.) a carpeta larga. */
    public static String carpetaPara(String tipo) {
        if (tipo == null) return "";
        String t = tipo.trim().toUpperCase().replace(" ", "").replace("-", "");
        switch (t) {
            case "01":
            case "FACTURA":
            case "FACTURAELECTRONICA":
                return CARPETA_FACTURA;
            case "04":
            case "NOTACREDITO":
            case "NOTA_CREDITO":
            case "NOTADECREDITO":
            case "NC":
                return CARPETA_NOTA_CREDITO;
            case "05":
            case "NOTADEBITO":
            case "NOTA_DEBITO":
            case "NOTADEDEBITO":
            case "ND":
                return CARPETA_NOTA_DEBITO;
            case "06":
            case "GUIAREMISION":
            case "GUIA_REMISION":
            case "GUIADEREIMSION":
            case "GUIA":
                return CARPETA_GUIA_REMISION;
            case "07":
            case "RETENCION":
            case "RETENSIÓN":
            case "COMPROBANTEDERETENCION":
            case "RET":
                return CARPETA_RETENCION;
            case "NOTAVENTA":
            case "NOTA_VENTA":
            case "NV":
                return CARPETA_NOTA_VENTA;
            default:
                return sanitizar(tipo);
        }
    }

    /** Documentos/&lt;Empresa&gt;/&lt;Tipo&gt;/yyyy-MM-dd/. Crea la carpeta si no existe. */
    public static Path dirTipo(String tipo, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        String carpeta = carpetaPara(tipo);
        Path dir = carpeta.isEmpty()
                ? dirDia(dia)
                : baseDocumentos().resolve(nombreEmpresaCarpeta()).resolve(carpeta).resolve(dia.format(FORMATO_DIA));
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        return dir;
    }

    public static Path dirTipo(String tipo) {
        return dirTipo(tipo, LocalDate.now());
    }

    public static File dirTipoFile(String tipo, LocalDate fecha) {
        return dirTipo(tipo, fecha).toFile();
    }

    public static File dirTipoFile(String tipo) {
        return dirTipo(tipo, LocalDate.now()).toFile();
    }

    /**
     * Busca un archivo primero en la carpeta nueva por tipo y luego en la vieja dirDia (compatibilidad).
     * Devuelve el File existente o el destino nuevo si no existe en ninguna.
     */
    public static File resolverArchivo(String tipo, LocalDate fecha, String nombreArchivo) {
        File nuevo = dirTipo(tipo, fecha).resolve(nombreArchivo).toFile();
        if (nuevo.exists()) return nuevo;
        try {
            File viejo = dirDia(fecha).resolve(nombreArchivo).toFile();
            if (viejo.exists()) return viejo;
        } catch (Exception ignored) {}
        return nuevo;
    }
}

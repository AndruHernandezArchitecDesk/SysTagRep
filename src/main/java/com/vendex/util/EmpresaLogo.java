package com.vendex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Logo de la empresa (vista "Mi Empresa").
 * El archivo elegido se copia a una carpeta estable por equipo
 * (~/.vendex/logos/logo-empresa.&lt;ext&gt;) y se guarda su ruta en empresa.logo_url.
 * Los documentos (etiquetas JPG, PDFs) resuelven el logo con fallback al recurso bundled.
 */
public final class EmpresaLogo {

    public static final String LOGO_RECURSO = "/img/logoTAG.png";
    private static final String NOMBRE_BASE = "logo-empresa";

    private EmpresaLogo() {}

    public static Path dirLogos() {
        Path dir = Paths.get(System.getProperty("user.home"), ".vendex", "logos");
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        return dir;
    }

    /** Copia el archivo origen a la carpeta de la app y devuelve su ruta absoluta. */
    public static String guardarLogo(File origen) throws Exception {
        if (origen == null || !origen.isFile()) throw new IllegalArgumentException("Archivo de logo inválido");
        String nombre = origen.getName().toLowerCase();
        String ext = ".png";
        int punto = nombre.lastIndexOf('.');
        if (punto >= 0 && punto < nombre.length() - 1) {
            String e = nombre.substring(punto);
            if (e.matches("\\.(png|jpg|jpeg|gif)")) ext = e.equals(".jpeg") ? ".jpg" : e;
        }
        Path destino = dirLogos().resolve(NOMBRE_BASE + ext);
        // limpia extensiones previas para no dejar huérfanos
        for (String vieja : new String[]{".png", ".jpg", ".gif"}) {
            try { Files.deleteIfExists(dirLogos().resolve(NOMBRE_BASE + vieja)); } catch (Exception ignored) {}
        }
        Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino.toAbsolutePath().toString();
    }

    /** Ruta del logo configurado en la empresa, o null si no hay / no existe el archivo. */
    public static String rutaLogoConfigurado() {
        try {
            var dao = com.vendex.config.AppContext.getInstance().empresaDAO;
            var lista = dao.listar();
            if (lista == null || lista.isEmpty()) return null;
            String url = lista.get(0).getLogoUrl();
            if (url == null || url.isBlank()) return null;
            String limpio = url.trim();
            // file:/// o ruta absoluta
            if (limpio.startsWith("file:")) {
                try {
                    File f = new File(new java.net.URI(limpio));
                    if (f.isFile()) return f.getAbsolutePath();
                    return null;
                } catch (Exception e) { return null; }
            }
            if (limpio.startsWith("/")) {
                // ¿recurso bundled o ruta absoluta de otro SO? prueba archivo primero
                File f = new File(limpio);
                if (f.isFile()) return f.getAbsolutePath();
                return limpio; // se intentará como recurso
            }
            File f = new File(limpio);
            if (f.isFile()) return f.getAbsolutePath();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Para EtiquetaUtil: ruta de archivo si hay logo configurado, si no el recurso bundled. */
    public static String rutaLogoEtiqueta() {
        String cfg = rutaLogoConfigurado();
        return (cfg != null && !cfg.isBlank()) ? cfg : LOGO_RECURSO;
    }

    /** Stream del logo: archivo configurado → recurso indicado → bundled. Null si nada abre. */
    public static InputStream abrirLogoStream(String preferido) {
        // 1. archivo configurado en la empresa
        try {
            String cfg = rutaLogoConfigurado();
            if (cfg != null && !cfg.startsWith("/img/")) {
                File f = new File(cfg);
                if (f.isFile()) return new FileInputStream(f);
            }
        } catch (Exception ignored) {}
        // 2. preferido: archivo o recurso
        if (preferido != null && !preferido.isBlank()) {
            try {
                File f = new File(preferido);
                if (f.isFile()) return new FileInputStream(f);
            } catch (Exception ignored) {}
            try {
                InputStream rs = EmpresaLogo.class.getResourceAsStream(preferido);
                if (rs != null) return rs;
            } catch (Exception ignored) {}
        }
        // 3. bundled
        try {
            return EmpresaLogo.class.getResourceAsStream(LOGO_RECURSO);
        } catch (Exception ignored) {}
        return null;
    }
}

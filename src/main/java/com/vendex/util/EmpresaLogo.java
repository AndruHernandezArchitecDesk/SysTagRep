package com.vendex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Logo compartido de la empresa. La fuente canónica son los bytes de empresa.logo_bytes;
 * logo_url y el archivo ~/.vendex/logos son únicamente compatibilidad/caché local.
 * Clientes antiguos sin bytes compartidos conservan el fallback por ruta y recurso bundled.
 */
public final class EmpresaLogo {

    public static final String LOGO_RECURSO = "/img/LogoVendex.png";
    private static final String NOMBRE_BASE = "logo-empresa";
    private static final int MAX_LOGO_BYTES = 5 * 1024 * 1024;
    private static final long CACHE_LOGO_MILLIS = 60_000;
    private static volatile LogoCompartido cacheLogo;
    private static volatile long cacheLogoHasta;

    private record LogoCompartido(String ruta, byte[] bytes) {
        private LogoCompartido {
            bytes = bytes == null ? null : bytes.clone();
        }
        @Override public byte[] bytes() { return bytes == null ? null : bytes.clone(); }
    }

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
        Path destino = dirLogos().resolve(NOMBRE_BASE + "-" + java.util.UUID.randomUUID() + ext);
        // No borrar otras extensiones antes de confirmar el UPDATE de empresa.logo_url:
        // si el guardado en BD falla, el logo que aún referencia la empresa debe seguir existiendo.
        Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino.toAbsolutePath().toString();
    }

    /** Elimina el archivo anterior solo después de confirmar en BD la nueva ruta. */
    public static void eliminarLogoAnterior(String anterior, String actual) {
        Path pathAnterior = resolverArchivo(anterior);
        Path pathActual = resolverArchivo(actual);
        if (pathAnterior == null || pathAnterior.equals(pathActual)) return;
        Path carpeta = dirLogos().toAbsolutePath().normalize();
        if (!carpeta.equals(pathAnterior.getParent()) || !pathAnterior.getFileName().toString().startsWith(NOMBRE_BASE)) return;
        try { Files.deleteIfExists(pathAnterior); } catch (Exception ignored) { }
    }

    private static Path resolverArchivo(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            Path path = valor.startsWith("file:")
                    ? Paths.get(new java.net.URI(valor)) : Paths.get(valor);
            return path.toAbsolutePath().normalize();
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Ruta local heredada del logo, o null si no existe archivo accesible en este equipo. */
    public static String rutaLogoConfigurado() {
        return resolverRuta(obtenerLogoCompartido().ruta());
    }

    /** Bytes de la fila compartida en PostgreSQL; null cuando aún no hay logo migrado. */
    public static byte[] logoBytesCompartidos() {
        return obtenerLogoCompartido().bytes();
    }

    /** Refresca en memoria el caché después de guardar desde esta computadora. */
    public static void actualizarCache(com.vendex.model.Empresa empresa) {
        if (empresa == null) { invalidarCache(); return; }
        cacheLogo = new LogoCompartido(empresa.getLogoUrl(), bytesValidos(empresa.getLogoBytes()));
        cacheLogoHasta = System.currentTimeMillis() + CACHE_LOGO_MILLIS;
    }

    public static void invalidarCache() {
        cacheLogoHasta = 0;
    }

    private static LogoCompartido obtenerLogoCompartido() {
        LogoCompartido cache = cacheLogo;
        if (cache != null && System.currentTimeMillis() < cacheLogoHasta) return cache;
        synchronized (EmpresaLogo.class) {
            cache = cacheLogo;
            if (cache != null && System.currentTimeMillis() < cacheLogoHasta) return cache;
            try {
                var empresas = com.vendex.config.AppContext.getInstance().empresaDAO.listar();
                if (empresas != null && !empresas.isEmpty()) {
                    var empresa = empresas.get(0);
                    cache = new LogoCompartido(empresa.getLogoUrl(), bytesValidos(empresa.getLogoBytes()));
                } else cache = new LogoCompartido(null, null);
            } catch (Exception ignored) {
                cache = new LogoCompartido(null, null);
            }
            cacheLogo = cache;
            cacheLogoHasta = System.currentTimeMillis() + CACHE_LOGO_MILLIS;
            return cache;
        }
    }

    private static byte[] bytesValidos(byte[] bytes) {
        return bytes != null && bytes.length > 0 && bytes.length <= MAX_LOGO_BYTES ? bytes : null;
    }

    private static String resolverRuta(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim();
        try {
            if (limpio.startsWith("file:")) {
                File archivo = new File(new java.net.URI(limpio));
                return archivo.isFile() ? archivo.getAbsolutePath() : null;
            }
        } catch (Exception ignored) { return null; }
        File archivo = new File(limpio);
        if (archivo.isFile()) return archivo.getAbsolutePath();
        // Los recursos empaquetados no son rutas relativas de empresa.
        return limpio.startsWith("/img/") ? limpio : null;
    }

    /** Para EtiquetaUtil: ruta de archivo si hay logo configurado, si no el recurso bundled. */
    public static String rutaLogoEtiqueta() {
        String cfg = rutaLogoConfigurado();
        return (cfg != null && !cfg.isBlank()) ? cfg : LOGO_RECURSO;
    }

    /** Stream del logo: bytes compartidos → archivo heredado local → recurso empaquetado. */
    public static InputStream abrirLogoStream(String preferido) {
        LogoCompartido logo = obtenerLogoCompartido();
        byte[] bytes = logo.bytes();
        if (bytes != null && bytes.length > 0) return new java.io.ByteArrayInputStream(bytes);
        InputStream archivoConfigurado = abrirArchivo(resolverRuta(logo.ruta()));
        if (archivoConfigurado != null) return archivoConfigurado;
        // 2. preferido: archivo o recurso
        InputStream elegido = abrirArchivo(resolverRuta(preferido));
        if (elegido != null) return elegido;
        if (preferido != null && preferido.startsWith("/img/")) {
            InputStream recurso = EmpresaLogo.class.getResourceAsStream(preferido);
            if (recurso != null) return recurso;
        }
        // 3. bundled
        return EmpresaLogo.class.getResourceAsStream(LOGO_RECURSO);
    }

    private static InputStream abrirArchivo(String ruta) {
        if (ruta == null || ruta.startsWith("/img/")) return null;
        try {
            File archivo = new File(ruta);
            return archivo.isFile() ? new FileInputStream(archivo) : null;
        } catch (Exception ignored) { return null; }
    }
}

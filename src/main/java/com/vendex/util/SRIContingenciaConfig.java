package com.vendex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuración SRI contingencia separada por cliente (~/.vendex/contingencia.properties).
 * Email parametrizado por cliente para alertas RECHAZADA / AGOTADA.
 * Usa archivo separado (no backup.properties) por decisión 3.
 */
public final class SRIContingenciaConfig {

    private static final Logger LOG = Logger.getLogger(SRIContingenciaConfig.class.getName());
    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "contingencia.properties");

    private SRIContingenciaConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    public static Properties cargar() {
        Properties p = new Properties();
        // defaults
        p.setProperty("sri.contingencia.email", "");
        p.setProperty("sri.contingencia.modo", "true"); // true = permitir NC sobre PENDIENTE en contingencia
        p.setProperty("sri.contingencia.backoff.ciclo_minutos", "2");
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                Properties loaded = new Properties();
                loaded.load(fis);
                for (String k : loaded.stringPropertyNames()) {
                    p.setProperty(k, loaded.getProperty(k));
                }
            } catch (IOException e) {
                LOG.warning("No se pudo leer contingencia.properties: " + e.getMessage());
            }
        }
        return p;
    }

    public static void guardar(Properties p) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) LOG.warning("No se pudo crear dir " + DIR);
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Vendex SRI Contingencia - email parametrizado por cliente. sri.contingencia.email=destino alertas RECHAZADA/AGOTADA");
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar contingencia.properties", e);
        }
    }

    public static String getEmailDestino() {
        String v = cargar().getProperty("sri.contingencia.email", "").trim();
        if (!v.isBlank()) return v;
        // fallback a configuracion_email si existe
        try {
            var dao = new com.vendex.dao.ConfiguracionEmailDAOPostgres();
            var opt = dao.obtenerActiva();
            if (opt.isPresent()) return opt.get().getEmailRemitente();
        } catch (Exception ignored) {}
        return "";
    }

    public static boolean isModoContingencia() {
        String v = cargar().getProperty("sri.contingencia.modo", "true").trim();
        return Boolean.parseBoolean(v);
    }

    public static void setEmailDestino(String email) {
        Properties p = cargar();
        p.setProperty("sri.contingencia.email", email == null ? "" : email.trim());
        guardar(p);
    }

    public static void setModoContingencia(boolean modo) {
        Properties p = cargar();
        p.setProperty("sri.contingencia.modo", String.valueOf(modo));
        guardar(p);
    }
}

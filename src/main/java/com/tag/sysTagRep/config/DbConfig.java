package com.tag.sysTagRep.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persiste la configuracion de conexion a PostgreSQL en ~/.systag/db.properties
 * para soportar despliegue multi-PC con BD compartida.
 *
 * PC host (192.168.1.7):  db.url=jdbc:postgresql://localhost:5432/dbTag
 * PC cliente (192.168.1.5): db.url=jdbc:postgresql://192.168.1.7:5432/dbTag
 *
 * Si el archivo no existe se crea con defaults. Si existe, DatabaseConnection
 * lo lee al inicio via {@link DatabaseConnection#initFromConfig()}.
 */
public final class DbConfig {

    private static final File DIR = new File(System.getProperty("user.home"), ".systag");
    private static final File ARCHIVO = new File(DIR, "db.properties");

    public static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/dbTag";
    public static final String DEFAULT_USER = "postgres";
    public static final String DEFAULT_PASSWORD = "admin";

    private DbConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    /**
     * Carga url/user/password desde archivo. Si no existe, lo crea con defaults
     * y retorna los defaults.
     */
    public static synchronized String[] cargar() {
        Properties p = new Properties();
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
            } catch (IOException ignored) {}
        } else {
            // crear archivo con defaults para que el usuario sepa donde editar
            guardar(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
            return new String[]{DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD};
        }
        String url = p.getProperty("db.url", DEFAULT_URL).trim();
        String user = p.getProperty("db.user", DEFAULT_USER).trim();
        String pass = p.getProperty("db.password", DEFAULT_PASSWORD);
        // pass puede contener espacios, no trim
        if (url.isEmpty()) url = DEFAULT_URL;
        if (user.isEmpty()) user = DEFAULT_USER;
        return new String[]{url, user, pass};
    }

    public static synchronized void guardar(String url, String user, String password) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties p = new Properties();
            p.setProperty("db.url", url == null || url.isBlank() ? DEFAULT_URL : url.trim());
            p.setProperty("db.user", user == null || user.isBlank() ? DEFAULT_USER : user.trim());
            p.setProperty("db.password", password == null ? "" : password);
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "SysTagRep - Conexion PostgreSQL. Editar db.url para BD remota. Ej: jdbc:postgresql://192.168.1.7:5432/dbTag");
            }
        } catch (IOException ignored) {}
    }

    /**
     * @return true si ya existe configuracion para BD remota (url no es localhost)
     */
    public static boolean esRemota() {
        String[] c = cargar();
        return !c[0].contains("localhost") && !c[0].contains("127.0.0.1");
    }
}

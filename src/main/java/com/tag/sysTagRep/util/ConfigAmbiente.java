package com.tag.sysTagRep.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persiste el ambiente SRI (PRUEBAS/PRODUCCION) en el directorio del usuario
 * para mantenerlo entre sesiones.
 */
public class ConfigAmbiente {

    private static final File DIR = new File(System.getProperty("user.home"), ".systag");
    private static final File ARCHIVO = new File(DIR, "ambiente.properties");

    private ConfigAmbiente() {}

    /**
     * @return el ambiente guardado, o "PRUEBAS" por defecto.
     */
    public static String cargar() {
        Properties p = new Properties();
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
            } catch (IOException ignored) {}
        }
        return p.getProperty("ambiente", "PRUEBAS");
    }

    public static void guardar(String ambiente) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties p = new Properties();
            p.setProperty("ambiente", ambiente == null ? "PRUEBAS" : ambiente);
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Ambiente SRI");
            }
        } catch (IOException ignored) {}
    }
}

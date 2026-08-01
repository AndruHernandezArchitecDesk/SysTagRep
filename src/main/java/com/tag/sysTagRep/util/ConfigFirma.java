package com.tag.sysTagRep.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persiste la configuración de la firma electrónica (ruta del .p12 y contraseña
 * encriptada) en el directorio del usuario para mantenerla entre sesiones.
 */
public class ConfigFirma {

    private static final File DIR = new File(System.getProperty("user.home"), ".systag");
    private static final File ARCHIVO = new File(DIR, "firma.properties");

    private ConfigFirma() {}

    /**
     * @return arreglo {rutaP12, clave} con los valores guardados (pueden estar vacíos).
     * La clave se devuelve desencriptada.
     */
    public static String[] cargar() {
        Properties p = new Properties();
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
            } catch (IOException ignored) {}
        }
        String ruta = p.getProperty("rutaP12", "");
        String clave = p.getProperty("clave", "");
        if (!clave.isEmpty()) {
            try {
                clave = Cifrado.desencriptar(clave);
            } catch (Exception e) {
                clave = "";
            }
        }
        return new String[] { ruta, clave };
    }

    public static boolean estaConfigurada() {
        String[] c = cargar();
        return !c[0].isEmpty() && !c[1].isEmpty();
    }

    public static boolean terminosAceptados() {
        Properties p = new Properties();
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
            } catch (IOException ignored) {}
        }
        return Boolean.parseBoolean(p.getProperty("terminosAceptados", "false"));
    }

    public static void guardar(String rutaP12, String clave) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties p = new Properties();
            p.setProperty("rutaP12", rutaP12 == null ? "" : rutaP12);
            p.setProperty("clave", Cifrado.encriptar(clave == null ? "" : clave));
            p.setProperty("terminosAceptados", "true");
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Firma electronica");
            }
        } catch (Exception ignored) {}
    }
}

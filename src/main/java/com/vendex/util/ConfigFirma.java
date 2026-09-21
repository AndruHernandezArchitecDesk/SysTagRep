package com.vendex.util;

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

    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "firma.properties");

    private ConfigFirma() {}

    /**
     * @return arreglo {rutaP12, clave} con los valores guardados (pueden estar vacíos).
     * La clave se devuelve desencriptada. Migra lazy legacy→keyring.
     */
    public static String[] cargar() {
        Properties p = new Properties();
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
            } catch (IOException ignored) {}
            // migrar tanto claro→cifrado como legacy→keyring (Fase 1 completa)
            boolean migro = SecureConfigStore.migrarTodoSiEsNecesario(ARCHIVO, "clave");
            if (migro) {
                p.clear();
                try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) {}
            }
        }
        String ruta = p.getProperty("rutaP12", "");
        String clave = p.getProperty("clave", "");
        if (!clave.isEmpty()) {
            // SecureConfigStore.descifrar maneja dual-read keyring→legacy; fallback a Cifrado directo
            String desc = SecureConfigStore.descifrar(clave);
            // si SecureConfigStore no descifró (retornó raw por no ser esCifrado pero es legacy claro), intentar Cifrado
            if (desc.equals(clave) && SecureConfigStore.esCifrado(clave)) {
                try { desc = Cifrado.desencriptar(clave); } catch (Exception e) { desc = ""; }
            } else if (!SecureConfigStore.esCifrado(clave)) {
                // legacy claro con ":" falló, intentar igual
                try { String tmp = Cifrado.desencriptar(clave); if (!tmp.isEmpty()) desc = tmp; } catch (Exception ignored) {}
            }
            clave = desc;
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
            if (ARCHIVO.exists()) {
                try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) {}
            }
            p.setProperty("rutaP12", rutaP12 == null ? "" : rutaP12);
            String claveCifrada = (clave == null || clave.isEmpty()) ? "" : SecureConfigStore.cifrar(clave);
            p.setProperty("clave", claveCifrada);
            p.setProperty("terminosAceptados", "true");
            try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                p.store(fos, "Firma electronica - clave cifrada keyring AES/GCM");
            }
        } catch (Exception ignored) {}
    }
}

package com.vendex.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Wrapper único para leer/escribir configuraciones sensibles cifradas.
 * Fase 1 del plan de secretos: evita repetir lógica de cifrado por módulo.
 *
 * Usa {@link Cifrado} (AES/GCM + PBKDF2) para cifrar valores en reposo.
 * Formato: base64(sal):base64(iv):base64(cifrado) — compatible con ConfigFirma/ConfiguracionEmail.
 * Si el valor ya está en claro (migración legacy), lo descifra como texto plano y
 * la próxima escritura lo re-cifra.
 *
 * Nota de seguridad (Fase 1): la clave maestra hoy deriva de {@code Cifrado.SECRETO}
 * embebido. Esto elimina el secreto del binario compartido (ya no viaja en claro)
 * y es el paso inmediato de contención. El paso siguiente (Fase 1 completa)
 * es derivar la clave de un keyring OS (DPAPI/Keychain/libsecret) via java-keyring;
 * este Store está diseñado para cambiar el proveedor sin tocar los callers.
 */
public final class SecureConfigStore {

    private SecureConfigStore() {}

    public static String cifrar(String plano) {
        if (plano == null || plano.isEmpty()) return "";
        try {
            return Cifrado.encriptar(plano);
        } catch (Exception e) {
            throw new RuntimeException("Error cifrando valor", e);
        }
    }

    public static String descifrar(String almacenado) {
        if (almacenado == null || almacenado.isEmpty()) return "";
        // formato cifrado contiene ":" (sal:iv:cifrado). Si no, es legacy en claro.
        if (!almacenado.contains(":")) return almacenado;
        try {
            return Cifrado.desencriptar(almacenado);
        } catch (Exception e) {
            // si falla descifrado, devolver como está (posible valor en claro con ":")
            return almacenado;
        }
    }

    public static boolean esCifrado(String valor) {
        return valor != null && valor.contains(":") && valor.split(":", -1).length == 3;
    }

    /**
     * Carga un .properties cifrando/descifrando un key específico.
     * Si el archivo tiene el valor en claro, lo retorna igual (migración lazy).
     */
    public static String leer(File archivo, String key) {
        if (!archivo.exists()) return null;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) {
            p.load(fis);
        } catch (IOException ignored) { return null; }
        String raw = p.getProperty(key);
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return null;
        // si está cifrado, descifrar; si no, es legacy en claro
        if (esCifrado(raw)) return descifrar(raw);
        return raw;
    }

    public static void guardar(File archivo, String key, String valorPlano, String comentario) {
        try {
            File dir = archivo.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) return;
            Properties p = new Properties();
            if (archivo.exists()) {
                try (FileInputStream fis = new FileInputStream(archivo)) { p.load(fis); } catch (IOException ignored) {}
            }
            if (valorPlano == null || valorPlano.isBlank()) {
                p.remove(key);
            } else {
                p.setProperty(key, cifrar(valorPlano.trim()));
            }
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                p.store(fos, comentario);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error guardando config cifrada: " + archivo, e);
        }
    }

    /**
     * Migra un valor en claro a cifrado si aún no lo está. Retorna true si migró.
     */
    public static boolean migrarSiEsNecesario(File archivo, String key) {
        if (!archivo.exists()) return false;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(archivo)) { p.load(fis); } catch (IOException e) { return false; }
        String raw = p.getProperty(key);
        if (raw == null || raw.trim().isEmpty() || esCifrado(raw.trim())) return false;
        String plano = raw.trim();
        p.setProperty(key, cifrar(plano));
        try (FileOutputStream fos = new FileOutputStream(archivo)) { p.store(fos, "Migrado a cifrado AES/GCM"); } catch (IOException e) { return false; }
        return true;
    }
}

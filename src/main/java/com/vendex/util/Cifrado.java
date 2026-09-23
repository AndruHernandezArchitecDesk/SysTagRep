package com.vendex.util;

import com.vendex.security.MasterKeyManager;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifrado simétrico AES/GCM para guardar la contraseña de la firma electrónica
 * de forma local. Fase 1 completa: clave deriva de master key del SO (keyring DPAPI/Keychain/libsecret
 * via MasterKeyManager) con HKDF HmacSHA256; fallback a PBKDF2 con SECRETO embebido solo para
 * descifrar archivos legacy y para backup cifrado (decisión 4). Formato almacenado sin cambio:
 * base64(sal):base64(iv):base64(cifrado) — no portable entre máquinas con keyring activo.
 *
 * <p>configuracion_email queda fuera de keyring (decisión 1) y sigue usando legacy si se desea,
 * pero este Cifrado ya migra todo lo por-PC (db.password, firma, gemini, nvidia).</p>
 */
public class Cifrado {

    private static final String SECRETO = "Vendex-Firma-2026";
    private static final int ITERACIONES = 65536;
    private static final int TAM_CLAVE_BITS = 128;
    private static final int TAM_IV = 12;
    private static final int TAM_SAL = 16;

    private Cifrado() {}

    public static String encriptar(String textoPlano) throws Exception {
        if (textoPlano == null) textoPlano = "";
        SecureRandom sr = new SecureRandom();
        byte[] sal = new byte[TAM_SAL];
        byte[] iv = new byte[TAM_IV];
        sr.nextBytes(sal);
        sr.nextBytes(iv);
        byte[] clave = derivarClaveConMaster(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(cifrado);
    }

    /** Solo para backup fallback file (decisión 4) — cifra con SECRETO legacy. */
    public static String encriptarLegacy(String textoPlano) throws Exception {
        if (textoPlano == null) textoPlano = "";
        SecureRandom sr = new SecureRandom();
        byte[] sal = new byte[TAM_SAL];
        byte[] iv = new byte[TAM_IV];
        sr.nextBytes(sal);
        sr.nextBytes(iv);
        byte[] clave = derivarClaveLegacy(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(iv) + ":"
                + Base64.getEncoder().encodeToString(cifrado);
    }

    public static String desencriptar(String textoCifrado) throws Exception {
        if (textoCifrado == null || !textoCifrado.contains(":")) {
            return "";
        }
        // intentar con master key (keyring/fallback), si falla AEAD intentar legacy
        try {
            return desencriptarConMaster(textoCifrado);
        } catch (Exception e) {
            // si master no disponible o tag invalido, probar legacy para migración transparente
            try {
                return desencriptarLegacy(textoCifrado);
            } catch (Exception e2) {
                // propagar original si ambos fallan (SecureConfigStore capturará y retornará raw)
                throw e;
            }
        }
    }

    public static String desencriptarConMaster(String textoCifrado) throws Exception {
        String[] partes = textoCifrado.split(":", 3);
        if (partes.length != 3) throw new IllegalArgumentException("formato invalido");
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] iv = Base64.getDecoder().decode(partes[1]);
        byte[] cifrado = Base64.getDecoder().decode(partes[2]);
        byte[] clave = derivarClaveConMaster(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
    }

    public static String desencriptarLegacy(String textoCifrado) throws Exception {
        if (textoCifrado == null || !textoCifrado.contains(":")) return "";
        String[] partes = textoCifrado.split(":", 3);
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] iv = Base64.getDecoder().decode(partes[1]);
        byte[] cifrado = Base64.getDecoder().decode(partes[2]);
        byte[] clave = derivarClaveLegacy(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
    }

    private static byte[] derivarClaveConMaster(byte[] sal) throws Exception {
        byte[] master = MasterKeyManager.getMasterKey(); // 32 bytes
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(master, "HmacSHA256"));
        byte[] out = mac.doFinal(sal);
        return Arrays.copyOf(out, TAM_CLAVE_BITS / 8);
    }

    private static byte[] derivarClaveLegacy(byte[] sal) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(SECRETO.toCharArray(), sal, ITERACIONES, TAM_CLAVE_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }

    private static byte[] derivarClave(byte[] sal) throws Exception {
        // compatibilidad: delegar a master (nuevo), legacy solo via desencriptar fallback
        return derivarClaveConMaster(sal);
    }

    // ===== Tier 2: install master key por instalación =====
    public static String encriptarConInstallMaster(String textoPlano) throws Exception {
        if (textoPlano == null) textoPlano = "";
        SecureRandom sr = new SecureRandom();
        byte[] sal = new byte[TAM_SAL];
        byte[] iv = new byte[TAM_IV];
        sr.nextBytes(sal);
        sr.nextBytes(iv);
        byte[] clave = derivarClaveConInstallMaster(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cifrado);
    }

    public static String desencriptarConInstallMaster(String textoCifrado) throws Exception {
        if (textoCifrado == null || !textoCifrado.contains(":")) return "";
        String[] partes = textoCifrado.split(":", 3);
        if (partes.length != 3) throw new IllegalArgumentException("formato invalido");
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] iv = Base64.getDecoder().decode(partes[1]);
        byte[] cifrado = Base64.getDecoder().decode(partes[2]);
        byte[] clave = derivarClaveConInstallMaster(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
    }

    private static byte[] derivarClaveConInstallMaster(byte[] sal) throws Exception {
        byte[] master = com.vendex.security.InstallMasterKeyManager.obtenerMasterKeyBytes();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(master, "HmacSHA256"));
        byte[] out = mac.doFinal(sal);
        return Arrays.copyOf(out, TAM_CLAVE_BITS / 8);
    }
}

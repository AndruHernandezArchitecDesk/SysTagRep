package com.tag.sysTagRep.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifrado simétrico AES/GCM para guardar la contraseña de la firma electrónica
 * de forma local. La clave se deriva de un secreto de la aplicación con PBKDF2.
 * Formato almacenado: base64(sal):base64(iv):base64(cifrado)
 */
public class Cifrado {

    private static final String SECRETO = "SysTagRep-Firma-2026";
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

        byte[] clave = derivarClave(sal);
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
        String[] partes = textoCifrado.split(":", 3);
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] iv = Base64.getDecoder().decode(partes[1]);
        byte[] cifrado = Base64.getDecoder().decode(partes[2]);

        byte[] clave = derivarClave(sal);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
    }

    private static byte[] derivarClave(byte[] sal) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(SECRETO.toCharArray(), sal, ITERACIONES, TAM_CLAVE_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }
}

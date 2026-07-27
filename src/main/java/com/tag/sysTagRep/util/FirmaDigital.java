package com.tag.sysTagRep.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Firma digital de XMLs usando certificado .p12 (BouncyCastle).
 * Stub listo para conectarse cuando se tenga el certificado.
 */
public class FirmaDigital {

    private PrivateKey privateKey;
    private X509Certificate certificate;

    /**
     * Carga el certificado digital .p12
     * @param rutaP12 ruta al archivo .p12
     * @param password contraseña del certificado
     */
    public boolean cargarCertificado(String rutaP12, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            keyStore.load(new FileInputStream(rutaP12), password.toCharArray());

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
                    certificate = (X509Certificate) keyStore.getCertificate(alias);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Firma un XML y retorna el XML firmado.
     * TODO: Implementar firma XML con BouncyCastle (XML-DSIG)
     */
    public String firmarXml(String xml) {
        if (privateKey == null || certificate == null) {
            System.out.println("ADVERTENCIA: Certificado no cargado. XML sin firmar.");
            return xml;
        }
        // TODO: Implementar firma XML DSIG cuando se tenga el certificado
        return xml;
    }

    public boolean isCertificadoCargado() {
        return privateKey != null && certificate != null;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }
}

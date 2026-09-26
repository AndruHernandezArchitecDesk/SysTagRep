package com.vendex.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;

public class CertificadoDigitalInfo {

    public enum Nivel { VIGENTE, AVISO, ADVERTENCIA, CRITICO, EXPIRADO }

    private final String titular;
    private final String emisor;
    private final LocalDate fechaEmision;
    private final LocalDate fechaExpiracion;
    private final long diasRestantes;
    private final Nivel nivel;

    private CertificadoDigitalInfo(String titular, String emisor, LocalDate emision, LocalDate expiracion) {
        this.titular = titular;
        this.emisor = emisor;
        this.fechaEmision = emision;
        this.fechaExpiracion = expiracion;
        this.diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), expiracion);
        this.nivel = calcularNivel(diasRestantes);
    }

    public static Nivel calcularNivel(long dias) {
        if (dias <= 0) return Nivel.EXPIRADO;
        if (dias <= 7) return Nivel.CRITICO;
        if (dias <= 30) return Nivel.ADVERTENCIA;
        if (dias <= 60) return Nivel.AVISO;
        return Nivel.VIGENTE;
    }

    public static CertificadoDigitalInfo leer(String rutaP12, String clave) throws Exception {
        if (rutaP12 == null || rutaP12.trim().isEmpty()) throw new IllegalArgumentException("Ruta p12 vacía");
        if (clave == null) clave = "";
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(rutaP12)) {
            try {
                ks.load(fis, clave.toCharArray());
            } catch (java.io.IOException e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                Throwable cause = e.getCause();
                String causeMsg = cause != null && cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
                if (msg.contains("keystore password was incorrect") || msg.contains("badpaddingexception") || causeMsg.contains("badpaddingexception") || msg.contains("failed to decrypt")) {
                    throw new IllegalArgumentException("Contraseña del .p12 incorrecta: reconfigure en Firma Electrónica (Archivo > Firma).", e);
                }
                throw e;
            }
        }
        Enumeration<String> aliases = ks.aliases();
        X509Certificate cert = null;
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                cert = (X509Certificate) ks.getCertificate(alias);
                break;
            }
        }
        if (cert == null) throw new IllegalStateException("No se encontró certificado en el p12");
        String titular = cert.getSubjectX500Principal().getName();
        String emisor = cert.getIssuerX500Principal().getName();
        LocalDate emision = cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate expiracion = cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return new CertificadoDigitalInfo(titular, emisor, emision, expiracion);
    }

    public String getTitular() { return titular; }
    public String getEmisor() { return emisor; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public LocalDate getFechaExpiracion() { return fechaExpiracion; }
    public long getDiasRestantes() { return diasRestantes; }
    public Nivel getNivel() { return nivel; }
    public String getNivelNombre() { return nivel.name(); }
}

package com.vendex.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class LicenseManager {

    /** Secreto legacy usado por licencias v1 ya emitidas. Solo se usa para migrarlo al store local. */
    private static final String LEGACY_HMAC_KEY = "Vendex2024!@#SecureKey_9f8a2b";

    private static String getSecretKey() {
        // 0. override explícito (operaciones/test): env var o system property
        String s = System.getProperty("VENDEX_LICENSE_HMAC_KEY");
        if (s == null || s.isBlank()) s = System.getenv("VENDEX_LICENSE_HMAC_KEY");
        if (s != null && !s.isBlank()) return s.trim();
        // 1. secreto local Tier1 (keyring / ~/.vendex/secrets)
        try {
            String local = com.vendex.util.SecureConfigStore.obtenerSecretoLocal("license.hmac.key");
            if (local != null && !local.isBlank()) return local;
        } catch (Exception ignored) {}
        // 2. migración segura: sembrar el valor legacy en el store local para no romper
        //    licencias ya emitidas; a partir de ahí el secreto vive fuera del binario.
        try {
            com.vendex.util.SecureConfigStore.guardarSecretoLocal("license.hmac.key", LEGACY_HMAC_KEY);
        } catch (Exception ignored) {}
        return LEGACY_HMAC_KEY;
    }
    private static final String LICENSE_FILE = ".vendex_license";
    private static final String ALGORITHM = "HmacSHA256";
    private static final int VALIDEZ_DIAS = 30;
    private static final int HMAC_LEN = 20;
    private static final int EXPIRY_LEN = 8;
    public static final String PERPETUA_MARKER = "00000000";

    // ===== Fase 1 Vendexi: dimensiones de la licencia (formato v2) =====
    public static final String MODALIDAD_LOCAL = "LOCAL";
    public static final String MODALIDAD_CENTRAL = "CENTRAL";
    public static final String V2_PREFIX = "V2";
    /** Roles de IA según spec Vendexi (sección 4). */
    public static final String ROL_OPERATIVO = "OPERATIVO";
    public static final String ROL_FACTURACION = "FACTURACION";
    public static final String ROL_PROFORMAS = "PROFORMAS";
    public static final String ROL_VENTAS = "VENTAS";
    public static final String ROL_INVENTARIO = "INVENTARIO";
    public static final String ROL_FINANCIERO = "FINANCIERO";
    public static final String ROL_ASESOR = "ASESOR";
    public static final String ROL_INVESTIGADOR = "INVESTIGADOR";
    public static final java.util.List<String> TODOS_ROLES_IA = java.util.List.of(
            ROL_OPERATIVO, ROL_FACTURACION, ROL_PROFORMAS, ROL_VENTAS,
            ROL_INVENTARIO, ROL_FINANCIERO, ROL_ASESOR, ROL_INVESTIGADOR);

    private static String cachedMachineCode;

    /**
     * Información de una licencia (v1 o v2) ya validada.
     * Claves v1 (sin payload) equivalen a: LOCAL + IA habilitada + todos los roles + capacidad 0 (sin límite),
     * para no degradar instalaciones existentes.
     */
    public static final class LicenseInfo {
        public final boolean valida;
        public final boolean version2;
        public final boolean perpetua;
        public final java.time.LocalDate vencimiento;
        public final String modalidad;
        public final boolean incluyeIA;
        public final java.util.Set<String> rolesIA;
        public final long capacidadIA;

        LicenseInfo(boolean valida, boolean version2, boolean perpetua, java.time.LocalDate vencimiento,
                    String modalidad, boolean incluyeIA, java.util.Set<String> rolesIA, long capacidadIA) {
            this.valida = valida;
            this.version2 = version2;
            this.perpetua = perpetua;
            this.vencimiento = vencimiento;
            this.modalidad = modalidad;
            this.incluyeIA = incluyeIA;
            this.rolesIA = rolesIA;
            this.capacidadIA = capacidadIA;
        }
    }

    public static String getMachineCode() {
        if (cachedMachineCode != null) return cachedMachineCode;

        try {
            StringBuilder sb = new StringBuilder();
            List<NetworkInterface> interfaces = Collections.list(
                    NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                if (!ni.isLoopback() && ni.isUp() && ni.getHardwareAddress() != null) {
                    byte[] mac = ni.getHardwareAddress();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    break;
                }
            }
            String raw = sb.toString();
            if (raw.isEmpty()) {
                raw = System.getProperty("user.name") + "@" +
                      java.net.InetAddress.getLocalHost().getHostName();
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            cachedMachineCode = Base64.getEncoder().withoutPadding()
                    .encodeToString(hash).substring(0, 24);
            return cachedMachineCode;
        } catch (Exception e) {
            return "UNKNOWN-" + System.getProperty("user.name");
        }
    }

    public static String generateLicenseKey(String machineCode) {
        return generateLicenseKey(machineCode, getFechaVencimiento());
    }

    public static String generateLicenseKeyPermanente(String machineCode) {
        return generateLicenseKey(machineCode, null);
    }

    public static String generateLicenseKey(String machineCode, LocalDate expiryDate) {
        try {
            String expiryStr = expiryDate == null
                    ? PERPETUA_MARKER
                    : expiryDate.toString().replace("-", "");
            byte[] hmac = calcularHmac(machineCode + "|" + expiryStr);
            String hex = bytesToHex(hmac).substring(0, HMAC_LEN);
            return formatKey(hex) + "-" + expiryStr;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Genera una licencia v2 con dimensiones de Vendexi (modalidad, IA, roles, capacidad).
     * El payload viaja firmado dentro del HMAC: cualquier alteración invalida la clave.
     */
    public static String generateLicenseKeyV2(String machineCode, LocalDate expiryDate,
                                              String modalidad, boolean incluyeIA,
                                              java.util.Collection<String> rolesIA, long capacidadIA) {
        try {
            String mod = normalizeModalidad(modalidad);
            String rolesStr = "-";
            if (incluyeIA && rolesIA != null && !rolesIA.isEmpty()) {
                java.util.Set<String> norm = normalizeRoles(rolesIA);
                rolesStr = norm.isEmpty() ? "-" : String.join(",", norm);
            }
            long cap = Math.max(0, capacidadIA);
            String payload = V2_PREFIX + "|" + mod + "|" + (incluyeIA ? "1" : "0")
                    + "|" + rolesStr + "|" + cap;
            String expiryStr = expiryDate == null
                    ? PERPETUA_MARKER
                    : expiryDate.toString().replace("-", "");
            byte[] hmac = calcularHmac(machineCode + "|" + expiryStr + "|" + payload);
            String hex = bytesToHex(hmac).substring(0, HMAC_LEN);
            String payloadB64 = Base64.getEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return formatKey(hex + expiryStr + payloadB64);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDate getFechaVencimiento() {
        return LocalDate.now().plusDays(VALIDEZ_DIAS);
    }

    public static LocalDate getVencimientoDeClave(String licenseKey) {
        if (licenseKey == null) return null;
        String clean = licenseKey.replace("-", "").replace(" ", "");
        if (clean.length() < HMAC_LEN + EXPIRY_LEN) return null;
        String expiryStr = clean.substring(HMAC_LEN, HMAC_LEN + EXPIRY_LEN);
        if (PERPETUA_MARKER.equals(expiryStr)) return null;
        try {
            return LocalDate.parse(expiryStr, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean isPerpetua(String licenseKey) {
        if (licenseKey == null) return false;
        String clean = licenseKey.replace("-", "").replace(" ", "");
        return clean.length() >= HMAC_LEN + EXPIRY_LEN
                && PERPETUA_MARKER.equals(clean.substring(HMAC_LEN, HMAC_LEN + EXPIRY_LEN));
    }

    public static boolean validateLicenseKey(String machineCode, String licenseKey) {
        return getInfoDeClave(machineCode, licenseKey).valida;
    }

    /**
     * Valida y descompone una clave v1 o v2 para el machineCode indicado.
     * Siempre devuelve un objeto (nunca null); revisar {@link LicenseInfo#valida}.
     */
    public static LicenseInfo getInfoDeClave(String machineCode, String licenseKey) {
        if (machineCode == null || licenseKey == null) {
            return new LicenseInfo(false, false, false, null, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
        }
        String clean = licenseKey.replace("-", "").replace(" ", "");
        if (clean.length() < HMAC_LEN + EXPIRY_LEN) {
            return new LicenseInfo(false, false, false, null, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
        }

        String expiryStr = clean.substring(HMAC_LEN, HMAC_LEN + EXPIRY_LEN);
        boolean perpetua = PERPETUA_MARKER.equals(expiryStr);
        LocalDate expiry = null;
        if (!perpetua) {
            try {
                expiry = LocalDate.parse(expiryStr, DateTimeFormatter.BASIC_ISO_DATE);
            } catch (DateTimeParseException e) {
                return new LicenseInfo(false, false, false, null, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
            }
        }
        boolean vigente = perpetua || !expiry.isBefore(LocalDate.now());

        String payloadB64 = clean.substring(HMAC_LEN + EXPIRY_LEN);
        boolean esV2 = !payloadB64.isEmpty();
        String payload = null;
        if (esV2) {
            try {
                payload = new String(Base64.getDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return new LicenseInfo(false, true, perpetua, expiry, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
            }
        }

        try {
            String data = esV2
                    ? machineCode + "|" + expiryStr + "|" + payload
                    : machineCode + "|" + expiryStr;
            byte[] hmac = calcularHmac(data);
            String expected = bytesToHex(hmac).substring(0, HMAC_LEN);
            if (!expected.equalsIgnoreCase(clean.substring(0, HMAC_LEN))) {
                return new LicenseInfo(false, esV2, perpetua, expiry, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
            }
        } catch (Exception e) {
            return new LicenseInfo(false, esV2, perpetua, expiry, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
        }

        if (!esV2) {
            // Compatibilidad: licencias v1 = LOCAL con IA habilitada (comportamiento pre-Fase-1).
            return new LicenseInfo(vigente, false, perpetua, expiry,
                    MODALIDAD_LOCAL, true, new java.util.LinkedHashSet<>(TODOS_ROLES_IA), 0);
        }

        String[] partes = payload.split("\\|", -1);
        if (partes.length != 5 || !V2_PREFIX.equals(partes[0])) {
            return new LicenseInfo(false, true, perpetua, expiry, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
        }
        String modalidad = partes[1];
        if (!MODALIDAD_LOCAL.equals(modalidad) && !MODALIDAD_CENTRAL.equals(modalidad)) {
            return new LicenseInfo(false, true, perpetua, expiry, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
        }
        boolean incluyeIA = "1".equals(partes[2]);
        java.util.Set<String> roles;
        if (!incluyeIA) {
            roles = java.util.Set.of();
        } else if (partes[3].isBlank() || "-".equals(partes[3])) {
            roles = new java.util.LinkedHashSet<>(TODOS_ROLES_IA);
        } else {
            roles = normalizeRoles(java.util.Arrays.asList(partes[3].split(",")));
        }
        long capacidad;
        try {
            capacidad = Long.parseLong(partes[4]);
            if (capacidad < 0) capacidad = 0;
        } catch (NumberFormatException e) {
            return new LicenseInfo(false, true, perpetua, expiry, modalidad, false, java.util.Set.of(), 0);
        }
        return new LicenseInfo(vigente, true, perpetua, expiry, modalidad, incluyeIA, roles, capacidad);
    }

    private static String normalizeModalidad(String modalidad) {
        if (MODALIDAD_CENTRAL.equalsIgnoreCase(modalidad)) return MODALIDAD_CENTRAL;
        return MODALIDAD_LOCAL;
    }

    private static java.util.Set<String> normalizeRoles(java.util.Collection<String> roles) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (roles == null) return out;
        for (String r : roles) {
            if (r == null) continue;
            String u = r.trim().toUpperCase();
            if (TODOS_ROLES_IA.contains(u)) out.add(u);
        }
        return out;
    }

    private static String formatKey(String key) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            if (i > 0 && i % 5 == 0) sb.append('-');
            sb.append(key.charAt(i));
        }
        return sb.toString();
    }

    private static byte[] calcularHmac(String data) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                getSecretKey().getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(keySpec);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02X", b));
        return hex.toString();
    }

    private static Path getLicensePath() {
        String appData;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            appData = System.getenv("APPDATA");
            if (appData == null)
                appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        } else {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, LICENSE_FILE);
    }

    public static boolean isActivated() {
        try {
            Path path = getLicensePath();
            if (!Files.exists(path)) return false;

            List<String> lines = Files.readAllLines(path);
            if (lines.size() < 2) return false;

            String storedMachineCode = lines.get(0).trim();
            String storedLicenseKey = lines.get(1).trim();
            String currentMachineCode = getMachineCode();

            return storedMachineCode.equals(currentMachineCode)
                    && validateLicenseKey(currentMachineCode, storedLicenseKey);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean saveActivation(String machineCode, String licenseKey) {
        try {
            Path path = getLicensePath();
            Files.write(path, (machineCode + "\n" + licenseKey + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Info de la licencia almacenada en esta máquina, o null si no hay archivo de activación. */
    public static LicenseInfo getLicenseInfo() {
        try {
            Path path = getLicensePath();
            if (!Files.exists(path)) return null;
            List<String> lines = Files.readAllLines(path);
            if (lines.size() < 2) return null;
            String storedMachineCode = lines.get(0).trim();
            String storedLicenseKey = lines.get(1).trim();
            if (!storedMachineCode.equals(getMachineCode())) {
                return new LicenseInfo(false, false, false, null, MODALIDAD_LOCAL, false, java.util.Set.of(), 0);
            }
            return getInfoDeClave(storedMachineCode, storedLicenseKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * true si la licencia almacenada (v2 válida) habilita Vendexi AI.
     * Sin restricción (true) para: sin licencia almacenada (modo remoto), claves v1
     * (compatibilidad con instalaciones existentes) o licencia inválida — el gate de
     * activación ya se aplica al arrancar. La restricción estricta por licencia v2
     * es el único caso que bloquea la IA.
     */
    public static boolean licenciaIncluyeIA() {
        LicenseInfo info = getLicenseInfo();
        if (info != null && info.valida && info.version2) return info.incluyeIA;
        return true;
    }

    /** true si el rol está habilitado por la licencia almacenada (v2 válida con IA). */
    public static boolean licenciaPermiteRolIA(String rol) {
        LicenseInfo info = getLicenseInfo();
        if (info == null || !info.valida || !info.version2 || !info.incluyeIA) return true;
        if (rol == null) return true;
        return info.rolesIA.contains(rol.trim().toUpperCase());
    }
}

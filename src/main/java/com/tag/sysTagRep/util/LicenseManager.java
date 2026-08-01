package com.tag.sysTagRep.util;

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

    private static final String SECRET_KEY = "SysTagRep2024!@#SecureKey_9f8a2b";
    private static final String LICENSE_FILE = ".systag_license";
    private static final String ALGORITHM = "HmacSHA256";
    private static final int VALIDEZ_DIAS = 30;
    private static final int HMAC_LEN = 20;
    private static final int EXPIRY_LEN = 8;

    private static String cachedMachineCode;

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

    public static String generateLicenseKey(String machineCode, LocalDate expiryDate) {
        try {
            String expiryStr = expiryDate.toString().replace("-", "");
            byte[] hmac = calcularHmac(machineCode + "|" + expiryStr);
            String hex = bytesToHex(hmac).substring(0, HMAC_LEN);
            return formatKey(hex) + "-" + expiryStr;
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
        if (clean.length() != HMAC_LEN + EXPIRY_LEN) return null;
        String expiryStr = clean.substring(HMAC_LEN);
        try {
            return LocalDate.parse(expiryStr, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean validateLicenseKey(String machineCode, String licenseKey) {
        if (machineCode == null || licenseKey == null) return false;
        String clean = licenseKey.replace("-", "").replace(" ", "");
        if (clean.length() != HMAC_LEN + EXPIRY_LEN) return false;

        String expiryStr = clean.substring(HMAC_LEN);
        LocalDate expiry;
        try {
            expiry = LocalDate.parse(expiryStr, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            return false;
        }

        if (expiry.isBefore(LocalDate.now())) return false;

        try {
            byte[] hmac = calcularHmac(machineCode + "|" + expiryStr);
            String expected = bytesToHex(hmac).substring(0, HMAC_LEN);
            return expected.equalsIgnoreCase(clean.substring(0, HMAC_LEN));
        } catch (Exception e) {
            return false;
        }
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
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
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
}

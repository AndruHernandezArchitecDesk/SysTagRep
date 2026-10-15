package com.vendex.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LicenseGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=== SYS-TAG License Generator ===");
            System.out.println("Machine Code: " + LicenseManager.getMachineCode());
            System.out.println();
            System.out.println("Las licencias normales vencen " + LicenseManager.getFechaVencimiento());
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code>");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> vitalicia");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> <dias>");
            System.out.println("      Ej: ... <machine-code> 12   (vence hoy + 12 dias)");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> <YYYYMMDD>");
            System.out.println("      Ej: ... <machine-code> 20261026");
            System.out.println();
            System.out.println("IMPORTANTE: ejecutar en la PC destino (usa su secreto local si existe).");
            return;
        }

        String machineCode = args[0];
        boolean perpetua = args.length > 1 && (
                args[1].equalsIgnoreCase("vitalicia")
                || args[1].equalsIgnoreCase("permanente")
                || args[1].equalsIgnoreCase("forever")
                || args[1].equalsIgnoreCase("siempre"));

        System.out.println("Machine Code: " + machineCode);
        if (perpetua) {
            String licenseKey = LicenseManager.generateLicenseKeyPermanente(machineCode);
            System.out.println("License Key:  " + licenseKey);
            System.out.println("Válida hasta: PERMANENTE (no expira)");
        } else if (args.length > 1 && args[1].matches("\\d{8}")) {
            LocalDate vencimiento = LocalDate.parse(args[1], DateTimeFormatter.BASIC_ISO_DATE);
            String licenseKey = LicenseManager.generateLicenseKey(machineCode, vencimiento);
            System.out.println("License Key:  " + licenseKey);
            System.out.println("Válida hasta: " + vencimiento);
        } else if (args.length > 1 && args[1].matches("\\d{1,3}")) {
            LocalDate vencimiento = LocalDate.now().plusDays(Integer.parseInt(args[1]));
            String licenseKey = LicenseManager.generateLicenseKey(machineCode, vencimiento);
            System.out.println("License Key:  " + licenseKey);
            System.out.println("Válida hasta: " + vencimiento);
        } else {
            String licenseKey = LicenseManager.generateLicenseKey(machineCode);
            LocalDate vencimiento = LicenseManager.getVencimientoDeClave(licenseKey);
            System.out.println("License Key:  " + licenseKey);
            System.out.println("Válida hasta: " + (vencimiento != null ? vencimiento : "???"));
        }
    }
}

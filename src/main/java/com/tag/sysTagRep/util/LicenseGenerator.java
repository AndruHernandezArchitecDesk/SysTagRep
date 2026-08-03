package com.tag.sysTagRep.util;

import java.time.LocalDate;

public class LicenseGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=== SYS-TAG License Generator ===");
            System.out.println("Machine Code: " + LicenseManager.getMachineCode());
            System.out.println();
            System.out.println("Las licencias normales vencen " + LicenseManager.getFechaVencimiento());
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java -cp SysTagRep.jar com.tag.sysTagRep.util.LicenseGenerator <machine-code>");
            System.out.println("  java -cp SysTagRep.jar com.tag.sysTagRep.util.LicenseGenerator <machine-code> vitalicia");
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
        } else {
            String licenseKey = LicenseManager.generateLicenseKey(machineCode);
            LocalDate vencimiento = LicenseManager.getVencimientoDeClave(licenseKey);
            System.out.println("License Key:  " + licenseKey);
            System.out.println("Válida hasta: " + (vencimiento != null ? vencimiento : "???"));
        }
    }
}

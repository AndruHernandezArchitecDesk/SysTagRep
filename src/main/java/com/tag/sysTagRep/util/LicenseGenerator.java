package com.tag.sysTagRep.util;

import java.time.LocalDate;

public class LicenseGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=== SYS-TAG License Generator ===");
            System.out.println("Machine Code: " + LicenseManager.getMachineCode());
            System.out.println();
            System.out.println("Las licencias vencen " + LicenseManager.getFechaVencimiento());
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java -cp SysTagRep.jar com.tag.sysTagRep.util.LicenseGenerator <machine-code>");
            return;
        }

        String machineCode = args[0];
        String licenseKey = LicenseManager.generateLicenseKey(machineCode);
        LocalDate vencimiento = LicenseManager.getVencimientoDeClave(licenseKey);
        System.out.println("Machine Code: " + machineCode);
        System.out.println("License Key:  " + licenseKey);
        System.out.println("Válida hasta: " + (vencimiento != null ? vencimiento : "???"));
    }
}

package com.vendex.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LicenseGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=== SYS-TAG License Generator (v1 + v2 Vendexi) ===");
            System.out.println("Machine Code: " + LicenseManager.getMachineCode());
            System.out.println();
            System.out.println("Las licencias normales vencen " + LicenseManager.getFechaVencimiento());
            System.out.println();
            System.out.println("Usage (v1, sin IA Vendexi):");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code>");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> vitalicia");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> <dias>");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> <YYYYMMDD>");
            System.out.println();
            System.out.println("Uso v2 (añade dimensiones Vendexi; con cualquier flag -- se genera v2):");
            System.out.println("  ... <machine-code> [vitalicia|dias|YYYYMMDD] [--central] [--ia|--no-ia]");
            System.out.println("      [--roles=OPERATIVO,FACTURACION,PROFORMAS,VENTAS,INVENTARIO,FINANCIERO,ASESOR,INVESTIGADOR]");
            System.out.println("      [--capacidad=N]   (0 = sin límite definido; la medición llega en Fase 3)");
            System.out.println("  Ej: ... CLAVE vitalicia --ia --roles=FACTURACION,PROFORMAS --capacidad=50000");
            System.out.println();
            System.out.println("IMPORTANTE: ejecutar en la PC destino (usa su secreto local si existe).");
            return;
        }

        String machineCode = args[0];
        List<String> posicionales = new ArrayList<>();
        String modalidad = LicenseManager.MODALIDAD_LOCAL;
        Boolean ia = null;
        List<String> roles = new ArrayList<>();
        long capacidad = 0;
        boolean esV2 = false;

        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a.equalsIgnoreCase("--central")) { modalidad = LicenseManager.MODALIDAD_CENTRAL; esV2 = true; }
            else if (a.equalsIgnoreCase("--local")) { modalidad = LicenseManager.MODALIDAD_LOCAL; esV2 = true; }
            else if (a.equalsIgnoreCase("--ia")) { ia = Boolean.TRUE; esV2 = true; }
            else if (a.equalsIgnoreCase("--no-ia")) { ia = Boolean.FALSE; esV2 = true; }
            else if (a.startsWith("--roles=")) {
                String v = a.substring("--roles=".length()).trim();
                if (!v.isEmpty() && !"-".equals(v)) {
                    for (String r : v.split(",")) if (!r.isBlank()) roles.add(r.trim());
                }
                esV2 = true;
            } else if (a.startsWith("--capacidad=")) {
                try { capacidad = Long.parseLong(a.substring("--capacidad=".length()).trim()); }
                catch (NumberFormatException e) { System.err.println("capacidad inválida: " + a); return; }
                esV2 = true;
            } else posicionales.add(a);
        }

        boolean perpetua = !posicionales.isEmpty() && (
                posicionales.get(0).equalsIgnoreCase("vitalicia")
                || posicionales.get(0).equalsIgnoreCase("permanente")
                || posicionales.get(0).equalsIgnoreCase("forever")
                || posicionales.get(0).equalsIgnoreCase("siempre"));

        LocalDate vencimiento = null;
        if (perpetua) vencimiento = null;
        else if (!posicionales.isEmpty() && posicionales.get(0).matches("\\d{8}")) {
            vencimiento = LocalDate.parse(posicionales.get(0), DateTimeFormatter.BASIC_ISO_DATE);
        } else if (!posicionales.isEmpty() && posicionales.get(0).matches("\\d{1,3}")) {
            vencimiento = LocalDate.now().plusDays(Integer.parseInt(posicionales.get(0)));
        } else {
            vencimiento = LicenseManager.getFechaVencimiento(); // default: hoy + 30 días (comportamiento v1 histórico)
        }

        System.out.println("Machine Code: " + machineCode);
        String licenseKey;
        if (esV2) {
            boolean incluyeIA = ia != null && ia;
            licenseKey = LicenseManager.generateLicenseKeyV2(
                    machineCode, vencimiento, modalidad, incluyeIA, roles, capacidad);
            System.out.println("License Key (v2): " + licenseKey);
            System.out.println("Modalidad:  " + modalidad);
            System.out.println("Vendexi AI: " + (incluyeIA ? "SÍ" : "NO"));
            if (incluyeIA) {
                System.out.println("Roles IA:   " + (roles.isEmpty() ? "(todos)" : String.join(",", roles)));
                System.out.println("Capacidad:  " + (capacidad == 0 ? "(sin límite definido)" : capacidad));
            }
        } else if (perpetua) {
            licenseKey = LicenseManager.generateLicenseKeyPermanente(machineCode);
            System.out.println("License Key:  " + licenseKey);
        } else {
            licenseKey = LicenseManager.generateLicenseKey(machineCode, vencimiento);
            System.out.println("License Key:  " + licenseKey);
        }
        LocalDate venc = LicenseManager.getVencimientoDeClave(licenseKey);
        System.out.println("Válida hasta: " + (LicenseManager.isPerpetua(licenseKey) ? "PERMANENTE (no expira)" : String.valueOf(venc)));
    }
}

package com.vendex.util;

import com.vendex.config.DatabaseConnection;
import com.vendex.config.DbConfig;
import com.vendex.dao.LicenciaCentralDAOPostgres;
import com.vendex.service.LicenciaCentralService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LicenseGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=== SYS-TAG License Generator (v1 + v2 Vetxi) ===");
            System.out.println("Machine Code: " + LicenseManager.getMachineCode());
            System.out.println();
            System.out.println("Las licencias normales vencen " + LicenseManager.getFechaVencimiento());
            System.out.println();
            System.out.println("Usage (v1, sin IA Vetxi):");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code>");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> vitalicia");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> <dias>");
            System.out.println("  java -cp Vendex.jar com.vendex.util.LicenseGenerator <machine-code> <YYYYMMDD>");
            System.out.println();
            System.out.println("Uso v2 (añade dimensiones Vetxi; con cualquier flag -- se genera v2):");
            System.out.println("  ... <machine-code> [vitalicia|dias|YYYYMMDD] [--central] [--ia|--no-ia]");
            System.out.println("      [--roles=OPERATIVO,FACTURACION,PROFORMAS,VENTAS,INVENTARIO,FINANCIERO,ASESOR,INVESTIGADOR]");
            System.out.println("      [--capacidad=N]   (0 = sin límite definido; la medición llega en Fase 3)");
            System.out.println("  Emitir y registrar en la BD central (cliente remoto):");
            System.out.println("  ... <machine-code> vitalicia --central --ia --registrar-central");
            System.out.println("  Registrar en otra BD (p. ej. dbTag del servidor):");
            System.out.println("  DB_PASSWORD=... java ... <machine-code> vitalicia --central --ia --registrar-central \\");
            System.out.println("      --db-url=jdbc:postgresql://192.168.1.4:5432/dbTag --db-user=postgres");
            System.out.println("  Ej: ... CLAVE vitalicia --ia --roles=FACTURACION,PROFORMAS --capacidad=50000");
            System.out.println();
            System.out.println("IMPORTANTE: la clave se firma con el secreto HMAC del host; debe ser el mismo en el cliente.");
            System.exit(0);
            return;
        }

        String machineCode = args[0];
        List<String> posicionales = new ArrayList<>();
        String modalidad = LicenseManager.MODALIDAD_LOCAL;
        Boolean ia = null;
        List<String> roles = new ArrayList<>();
        long capacidad = 0;
        boolean esV2 = false;
        boolean registrarCentral = false;
        String dbUrl = null;
        String dbUser = null;

        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a.equalsIgnoreCase("--central")) { modalidad = LicenseManager.MODALIDAD_CENTRAL; esV2 = true; }
            else if (a.equalsIgnoreCase("--local")) { modalidad = LicenseManager.MODALIDAD_LOCAL; esV2 = true; }
            else if (a.equalsIgnoreCase("--registrar-central")) { registrarCentral = true; modalidad = LicenseManager.MODALIDAD_CENTRAL; esV2 = true; }
            else if (a.equalsIgnoreCase("--ia")) { ia = Boolean.TRUE; esV2 = true; }
            else if (a.equalsIgnoreCase("--no-ia")) { ia = Boolean.FALSE; esV2 = true; }
            else if (a.startsWith("--db-url=")) { dbUrl = a.substring("--db-url=".length()).trim(); }
            else if (a.startsWith("--db-user=")) { dbUser = a.substring("--db-user=".length()).trim(); }
            else if (a.startsWith("--roles=")) {
                String v = a.substring("--roles=".length()).trim();
                if (!v.isEmpty() && !"-".equals(v)) {
                    for (String r : v.split(",")) if (!r.isBlank()) roles.add(r.trim());
                }
                esV2 = true;
            } else if (a.startsWith("--capacidad=")) {
                try { capacidad = Long.parseLong(a.substring("--capacidad=".length()).trim()); }
                catch (NumberFormatException e) { System.err.println("capacidad inválida: " + a); System.exit(1); return; }
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
        boolean incluyeIA = ia != null && ia;

        if (registrarCentral) {
            try {
                configurarConexionCentral(dbUrl, dbUser);
                DatabaseConnection.ensureLicenciasCentralSchema();
                LicenciaCentralService servicio = new LicenciaCentralService(new LicenciaCentralDAOPostgres());
                java.util.Map<String, Object> r = servicio.emitir(machineCode,
                        LicenseManager.MODALIDAD_CENTRAL, incluyeIA, roles, capacidad, vencimiento, null);
                licenseKey = (String) r.get("clave");
                System.out.println("License Key (v2 CENTRAL registrada): " + licenseKey);
                System.out.println("Modalidad:  CENTRAL");
                System.out.println("Vetxi AI: " + (incluyeIA ? "SÍ" : "NO"));
                if (incluyeIA) {
                    System.out.println("Roles IA:   " + (roles.isEmpty() ? "(todos)" : String.join(",", roles)));
                    System.out.println("Capacidad:  " + (capacidad == 0 ? "(sin límite definido)" : capacidad));
                }
                System.out.println("Registro:   licencia_emitida (hash SHA-256, sin clave en claro)");
            } catch (RuntimeException e) {
                System.err.println("No se pudo registrar la licencia central: " + e.getMessage());
                DatabaseConnection.shutdown();
                System.exit(1);
                return;
            }
        } else if (esV2) {
            licenseKey = LicenseManager.generateLicenseKeyV2(
                    machineCode, vencimiento, modalidad, incluyeIA, roles, capacidad);
            System.out.println("License Key (v2): " + licenseKey);
            System.out.println("Modalidad:  " + modalidad);
            System.out.println("Vetxi AI: " + (incluyeIA ? "SÍ" : "NO"));
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
        DatabaseConnection.shutdown();
        System.exit(0);
    }

    private static void configurarConexionCentral(String dbUrl, String dbUser) {
        if (dbUrl == null && dbUser == null) {
            DatabaseConnection.initFromConfig();
            return;
        }
        String[] cfg = DbConfig.cargar();
        String url = dbUrl != null && !dbUrl.isBlank() ? dbUrl : cfg[0];
        String user = dbUser != null && !dbUser.isBlank() ? dbUser : cfg[1];
        String pass = System.getenv("DB_PASSWORD");
        if (pass == null || pass.isBlank()) pass = System.getProperty("db.password");
        if (pass == null || pass.isBlank()) pass = cfg[2];
        DatabaseConnection.setConnectionParams(url, user, pass);
    }
}

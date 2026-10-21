package com.vendex.service;

import com.vendex.dao.LicenciaCentralDAO;
import com.vendex.model.LicenciaEmitida;
import com.vendex.util.LicenseManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fase 6b: administración central de licencias.
 * Emite claves v2 (HMAC local de la instancia central), registra la emisión
 * (hash de la clave, nunca la clave en claro) y mantiene la lista de
 * revocaciones para validación central de clientes en modalidad CENTRAL.
 */
public class LicenciaCentralService {

    public static final String MODALIDAD_LOCAL = "LOCAL";
    public static final String MODALIDAD_CENTRAL = "CENTRAL";

    private final LicenciaCentralDAO dao;

    public LicenciaCentralService(LicenciaCentralDAO dao) {
        this.dao = dao;
    }

    /** Emite una licencia v2 para el machineCode indicado y registra la emisión. */
    public Map<String, Object> emitir(String machineCode, String modalidad, boolean incluyeIA,
                                      Collection<String> rolesIA, long capacidadIA,
                                      LocalDate expiracion, Integer emitidaPor) {
        if (machineCode == null || machineCode.isBlank())
            throw new IllegalArgumentException("machineCode requerido");
        String mod = normalizeModalidad(modalidad);
        if (rolesIA != null && !rolesIA.isEmpty() && !incluyeIA)
            throw new IllegalArgumentException("rolesIA solo aplica si incluyeIA=true");
        if (capacidadIA < 0)
            throw new IllegalArgumentException("capacidad no puede ser negativa");
        if (expiracion != null && expiracion.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("expiracion ya vencida");

        Set<String> efectivos = new java.util.LinkedHashSet<>();
        if (incluyeIA) {
            if (rolesIA == null || rolesIA.isEmpty()) {
                efectivos.addAll(LicenseManager.TODOS_ROLES_IA);
            } else {
                for (String r : rolesIA) {
                    if (r == null || r.isBlank()) continue;
                    String u = r.trim().toUpperCase(java.util.Locale.ROOT);
                    if (!LicenseManager.TODOS_ROLES_IA.contains(u))
                        throw new IllegalArgumentException("roles desconocidos: " + u);
                    efectivos.add(u);
                }
            }
        }
        String rolesStr = efectivos.isEmpty() ? "-" : String.join(",", efectivos);

        String clave = LicenseManager.generateLicenseKeyV2(machineCode, expiracion, mod, incluyeIA,
                efectivos, capacidadIA);
        if (clave == null)
            throw new IllegalStateException("no se pudo generar la clave");

        LicenciaEmitida e = new LicenciaEmitida();
        e.setMachineCode(machineCode.trim());
        e.setClaveHash(sha256Hex(clave));
        e.setModalidad(mod);
        e.setIncluyeIA(incluyeIA);
        e.setRoles(rolesStr);
        e.setCapacidad(capacidadIA);
        e.setExpiracion(expiracion);
        e.setEmitidaPor(emitidaPor);
        dao.insertarEmitida(e);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("clave", clave);
        out.put("machineCode", e.getMachineCode());
        out.put("modalidad", mod);
        out.put("incluyeIA", incluyeIA);
        out.put("roles", rolesStr);
        out.put("capacidadIA", capacidadIA);
        out.put("expiracion", expiracion != null ? expiracion.toString() : "PERPETUA");
        return out;
    }

    /** Valida una clave contra la instancia central: firma + vigencia + revocación. */
    public Map<String, Object> validarCentral(String machineCode, String clave) {
        if (machineCode == null || machineCode.isBlank() || clave == null || clave.isBlank())
            throw new IllegalArgumentException("machineCode y clave requeridos");
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(machineCode.trim(), clave.trim());
        boolean revocada = dao.estaRevocada(machineCode.trim());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("valida", info != null && info.valida && !revocada);
        out.put("revocada", revocada);
        if (info != null) {
            out.put("firmaValida", info.valida);
            out.put("version2", info.version2);
            out.put("perpetua", info.perpetua);
            out.put("vencimiento", info.vencimiento != null ? info.vencimiento.toString() : null);
            out.put("modalidad", info.modalidad);
            out.put("incluyeIA", info.incluyeIA);
            out.put("roles", info.rolesIA != null ? String.join(",", info.rolesIA) : "-");
            out.put("capacidadIA", info.capacidadIA);
        }
        return out;
    }

    /** Revoca (o re-motiva) todas las licencias vigentes de un machineCode. */
    public void revocar(String machineCode, String motivo, Integer usuarioId) {
        if (machineCode == null || machineCode.isBlank())
            throw new IllegalArgumentException("machineCode requerido");
        dao.revocar(machineCode.trim(), motivo != null && !motivo.isBlank() ? motivo.trim() : null, usuarioId);
    }

    public boolean estaRevocada(String machineCode) {
        return machineCode != null && dao.estaRevocada(machineCode.trim());
    }

    public List<LicenciaEmitida> listarEmitidas(int limite) { return dao.listarEmitidas(limite); }
    public List<String> listarRevocadas(int limite) { return dao.listarRevocadas(limite); }

    static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format(java.util.Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizeModalidad(String m) {
        if (m == null || m.isBlank()) return MODALIDAD_LOCAL;
        String u = m.trim().toUpperCase(java.util.Locale.ROOT);
        if (u.equals(MODALIDAD_LOCAL) || u.equals(MODALIDAD_CENTRAL)) return u;
        throw new IllegalArgumentException("modalidad invalida (LOCAL|CENTRAL): " + m);
    }
}

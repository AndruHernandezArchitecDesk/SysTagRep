package com.vendex.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 1 Vendexi — modelo de licencia v1 (compat) y v2 (modalidad, IA, roles, capacidad).
 * Usa VENDEX_LICENSE_HMAC_KEY para determinismo y no tocar el keyring real.
 */
class LicenseManagerTest {

    private static final String MACHINE = "TESTMACHINE000000000001";
    private static final int HMAC_BODY = 20; // longitud del HMAC hex en la clave limpia

    @BeforeAll
    static void fijarSecreto() {
        System.setProperty("VENDEX_LICENSE_HMAC_KEY", "clave-de-prueba-fase1-2026");
    }

    @AfterAll
    static void limpiarSecreto() {
        System.clearProperty("VENDEX_LICENSE_HMAC_KEY");
    }

    // ===== v1 (compatibilidad) =====

    @Test
    void v1_perpetua_generaYvalida() {
        String key = LicenseManager.generateLicenseKeyPermanente(MACHINE);
        assertNotNull(key);
        assertTrue(LicenseManager.isPerpetua(key));
        assertTrue(LicenseManager.validateLicenseKey(MACHINE, key));
    }

    @Test
    void v1_conVencimiento_futura_esValida() {
        String key = LicenseManager.generateLicenseKey(MACHINE, LocalDate.now().plusDays(15));
        assertNotNull(key);
        assertEquals(LocalDate.now().plusDays(15), LicenseManager.getVencimientoDeClave(key));
        assertTrue(LicenseManager.validateLicenseKey(MACHINE, key));
    }

    @Test
    void v1_expirada_esInvalida() {
        String key = LicenseManager.generateLicenseKey(MACHINE, LocalDate.now().minusDays(1));
        assertFalse(LicenseManager.validateLicenseKey(MACHINE, key));
    }

    @Test
    void v1_otroMachineCode_esInvalida() {
        String key = LicenseManager.generateLicenseKeyPermanente(MACHINE);
        assertFalse(LicenseManager.validateLicenseKey("OTRAMACHINE", key));
    }

    @Test
    void v1_alterada_esInvalida() {
        String key = LicenseManager.generateLicenseKeyPermanente(MACHINE);
        char first = key.charAt(0);
        String alterada = (first == 'Z' ? "Y" : "Z") + key.substring(1);
        assertFalse(LicenseManager.validateLicenseKey(MACHINE, alterada));
    }

    @Test
    void v1_info_equivaleA_LOCAL_conIA_yTodosLosRoles() {
        String key = LicenseManager.generateLicenseKeyPermanente(MACHINE);
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(MACHINE, key);
        assertTrue(info.valida);
        assertFalse(info.version2);
        assertEquals(LicenseManager.MODALIDAD_LOCAL, info.modalidad);
        assertTrue(info.incluyeIA);
        assertEquals(LicenseManager.TODOS_ROLES_IA.size(), info.rolesIA.size());
        assertTrue(info.rolesIA.containsAll(LicenseManager.TODOS_ROLES_IA));
        assertEquals(0, info.capacidadIA);
    }

    // ===== v2 (Vendexi) =====

    @Test
    void v2_iaRolesCapacidad_generaYvalida() {
        String key = LicenseManager.generateLicenseKeyV2(MACHINE, LocalDate.now().plusDays(30),
                LicenseManager.MODALIDAD_LOCAL, true,
                List.of(LicenseManager.ROL_FACTURACION, LicenseManager.ROL_PROFORMAS), 50000);
        assertNotNull(key);
        assertTrue(LicenseManager.validateLicenseKey(MACHINE, key));

        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(MACHINE, key);
        assertTrue(info.valida);
        assertTrue(info.version2);
        assertFalse(info.perpetua);
        assertEquals(LocalDate.now().plusDays(30), info.vencimiento);
        assertEquals(LicenseManager.MODALIDAD_LOCAL, info.modalidad);
        assertTrue(info.incluyeIA);
        assertEquals(2, info.rolesIA.size());
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_FACTURACION));
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_PROFORMAS));
        assertEquals(50000, info.capacidadIA);
    }

    @Test
    void v2_sinIA_rolesVacios() {
        String key = LicenseManager.generateLicenseKeyV2(MACHINE, null,
                LicenseManager.MODALIDAD_LOCAL, false, List.of(), 0);
        assertTrue(LicenseManager.isPerpetua(key));
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(MACHINE, key);
        assertTrue(info.valida);
        assertTrue(info.perpetua);
        assertFalse(info.incluyeIA);
        assertTrue(info.rolesIA.isEmpty());
        assertEquals(0, info.capacidadIA);
    }

    @Test
    void v2_central_iaTodosLosRolesPorDefecto() {
        String key = LicenseManager.generateLicenseKeyV2(MACHINE, LocalDate.now().plusDays(365),
                LicenseManager.MODALIDAD_CENTRAL, true, List.of(), 0);
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(MACHINE, key);
        assertTrue(info.valida);
        assertEquals(LicenseManager.MODALIDAD_CENTRAL, info.modalidad);
        assertTrue(info.incluyeIA);
        assertEquals(LicenseManager.TODOS_ROLES_IA.size(), info.rolesIA.size());
        assertTrue(info.rolesIA.containsAll(LicenseManager.TODOS_ROLES_IA));
    }

    @Test
    void v2_payloadAlterado_esInvalida() {
        String key = LicenseManager.generateLicenseKeyV2(MACHINE, LocalDate.now().plusDays(30),
                LicenseManager.MODALIDAD_LOCAL, true,
                List.of(LicenseManager.ROL_FACTURACION), 50000);
        // alterar un carácter del payload (no del HMAC): cambiar un carácter del medio
        String clean = key.replace("-", "");
        int pos = HMAC_BODY + 8 + 2; // dentro del base64 del payload
        char c = clean.charAt(pos);
        String alterada = clean.substring(0, pos) + (c == 'A' ? 'B' : 'A') + clean.substring(pos + 1);
        assertFalse(LicenseManager.validateLicenseKey(MACHINE, alterada));
    }

    @Test
    void v2_expirada_esInvalida() {
        String key = LicenseManager.generateLicenseKeyV2(MACHINE, LocalDate.now().minusDays(2),
                LicenseManager.MODALIDAD_LOCAL, true, List.of(), 0);
        assertFalse(LicenseManager.validateLicenseKey(MACHINE, key));
    }

    @Test
    void v2_rolesDesconocidos_seIgnoran() {
        String key = LicenseManager.generateLicenseKeyV2(MACHINE, LocalDate.now().plusDays(30),
                LicenseManager.MODALIDAD_LOCAL, true,
                List.of(LicenseManager.ROL_VENTAS, "ROL_INEXISTENTE"), 100);
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(MACHINE, key);
        assertTrue(info.valida);
        assertEquals(1, info.rolesIA.size());
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_VENTAS));
    }

    @Test
    void v1_y_v2_noSeConfunden() {
        String v1 = LicenseManager.generateLicenseKeyPermanente(MACHINE);
        String v2 = LicenseManager.generateLicenseKeyV2(MACHINE, null,
                LicenseManager.MODALIDAD_LOCAL, false, List.of(), 0);
        assertNotEquals(v1.length(), v2.length());
        assertTrue(LicenseManager.getInfoDeClave(MACHINE, v1).valida);
        assertTrue(LicenseManager.getInfoDeClave(MACHINE, v2).valida);
        assertFalse(LicenseManager.getInfoDeClave(MACHINE, v1).version2);
        assertTrue(LicenseManager.getInfoDeClave(MACHINE, v2).version2);
    }

    @Test
    void clavesInvalidas_devuelvenInfoNoValida() {
        assertFalse(LicenseManager.getInfoDeClave(MACHINE, "abc").valida);
        assertFalse(LicenseManager.getInfoDeClave(null, "abc").valida);
        assertFalse(LicenseManager.getInfoDeClave(MACHINE, null).valida);
        assertFalse(LicenseManager.getInfoDeClave(MACHINE, "XXXXX-XXXXX-XXXXX-XXXXX-00000000").valida);
    }

    @Test
    void licenciaPermiteRolIA_sinObstaculo_paraV1() {
        String v1 = LicenseManager.generateLicenseKeyPermanente(MACHINE);
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave(MACHINE, v1);
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_OPERATIVO));
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_INVESTIGADOR));
    }
}

package com.vendex.service;

import com.vendex.dao.LicenciaCentralDAO;
import com.vendex.model.LicenciaEmitida;
import com.vendex.util.LicenseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 6b: emisión/revocación/validación central de licencias (DAO falso en memoria). */
class LicenciaCentralServiceTest {

    /** DAO falso: memoria pura, sin BD. */
    static class FakeDao implements LicenciaCentralDAO {
        final List<LicenciaEmitida> emitidas = new ArrayList<>();
        final Map<String, String> revocadas = new HashMap<>(); // machineCode -> motivo

        @Override public void insertarEmitida(LicenciaEmitida e) { emitidas.add(e); }
        @Override public List<LicenciaEmitida> listarEmitidas(int limite) {
            return emitidas.subList(0, Math.min(limite, emitidas.size()));
        }
        @Override public Optional<LicenciaEmitida> buscarPorClaveHash(String claveHash) {
            return emitidas.stream().filter(e -> e.getClaveHash().equals(claveHash)).findFirst();
        }
        @Override public void revocar(String machineCode, String motivo, Integer usuarioId) {
            revocadas.put(machineCode, motivo);
        }
        @Override public boolean estaRevocada(String machineCode) { return revocadas.containsKey(machineCode); }
        @Override public List<String> listarRevocadas(int limite) {
            List<String> out = new ArrayList<>(revocadas.keySet());
            return out.subList(0, Math.min(limite, out.size()));
        }
    }

    private FakeDao dao;
    private LicenciaCentralService svc;

    @BeforeEach
    void setUp() {
        dao = new FakeDao();
        svc = new LicenciaCentralService(dao);
    }

    @Test
    void emitir_generaClaveValida_yRegistraHashSinClaveEnClaro() {
        Map<String, Object> r = svc.emitir("MC001", "LOCAL", true,
                List.of(LicenseManager.ROL_OPERATIVO, LicenseManager.ROL_VENTAS), 0, null, 7);
        String clave = (String) r.get("clave");
        assertNotNull(clave);
        // La clave emitida valida contra el LicenseManager de la instancia central
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave("MC001", clave);
        assertTrue(info.valida);
        assertTrue(info.version2);
        assertTrue(info.perpetua);
        assertEquals("LOCAL", info.modalidad);
        assertTrue(info.incluyeIA);
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_OPERATIVO));
        assertTrue(info.rolesIA.contains(LicenseManager.ROL_VENTAS));
        assertEquals(0, info.capacidadIA);
        // Registro: hash SHA-256, nunca la clave en claro
        assertEquals(1, dao.emitidas.size());
        LicenciaEmitida e = dao.emitidas.get(0);
        assertEquals(LicenciaCentralService.sha256Hex(clave), e.getClaveHash());
        assertFalse(e.getClaveHash().contains(clave));
        assertEquals(7, e.getEmitidaPor());
    }

    @Test
    void emitir_rolesVaciosConIA_incluyeTodosLosRoles() {
        Map<String, Object> r = svc.emitir("MC002", "CENTRAL", true, null, 1000,
                LocalDate.now().plusDays(365), null);
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave("MC002", (String) r.get("clave"));
        assertTrue(info.valida);
        assertEquals(LicenseManager.TODOS_ROLES_IA.size(), info.rolesIA.size());
        assertEquals(1000, info.capacidadIA);
        assertFalse(info.perpetua);
        assertEquals(LocalDate.now().plusDays(365), info.vencimiento);
    }

    @Test
    void emitir_sinIA_ignoraRolesYCapacidad() {
        Map<String, Object> r = svc.emitir("MC003", "LOCAL", false, null, 500, null, null);
        LicenseManager.LicenseInfo info = LicenseManager.getInfoDeClave("MC003", (String) r.get("clave"));
        assertTrue(info.valida);
        assertFalse(info.incluyeIA);
        assertTrue(info.rolesIA.isEmpty());
    }

    @Test
    void emitir_rechazaEntradasInvalidas() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.emitir(" ", "LOCAL", true, null, 0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> svc.emitir("MC", "OTRA", true, null, 0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> svc.emitir("MC", "LOCAL", true, List.of("ROL_INEXISTENTE"), 0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> svc.emitir("MC", "LOCAL", true, null, -1, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> svc.emitir("MC", "LOCAL", true, null, 0, LocalDate.now().minusDays(1), null));
        assertThrows(IllegalArgumentException.class,
                () -> svc.emitir("MC", "LOCAL", false, List.of(LicenseManager.ROL_OPERATIVO), 0, null, null));
    }

    @Test
    void validarCentral_aceptaClaveEmitida() {
        String clave = (String) svc.emitir("MC100", "LOCAL", true, null, 0, null, null).get("clave");
        Map<String, Object> v = svc.validarCentral("MC100", clave);
        assertEquals(Boolean.TRUE, v.get("valida"));
        assertEquals(Boolean.FALSE, v.get("revocada"));
        assertEquals(Boolean.TRUE, v.get("firmaValida"));
    }

    @Test
    void validarCentral_rechazaClaveDeOtroMachineCode() {
        String clave = (String) svc.emitir("MC100", "LOCAL", true, null, 0, null, null).get("clave");
        Map<String, Object> v = svc.validarCentral("OTRO-MACHINE", clave);
        assertEquals(Boolean.FALSE, v.get("valida"));
        assertEquals(Boolean.FALSE, v.get("firmaValida"));
    }

    @Test
    void validarCentral_detectaRevocacion_aunqueFirmaSeaValida() {
        String clave = (String) svc.emitir("MC200", "LOCAL", true, null, 0, null, null).get("clave");
        svc.revocar("MC200", "robo de clave", 1);
        Map<String, Object> v = svc.validarCentral("MC200", clave);
        assertEquals(Boolean.FALSE, v.get("valida"));
        assertEquals(Boolean.TRUE, v.get("revocada"));
        assertEquals(Boolean.TRUE, v.get("firmaValida")); // la clave sigue siendo auténtica
        assertTrue(svc.estaRevocada("MC200"));
    }

    @Test
    void revocar_esIdempotente() {
        svc.revocar("MC300", "motivo 1", 1);
        svc.revocar("MC300", "motivo 2", 2);
        assertEquals(1, svc.listarRevocadas(10).size());
        assertEquals("motivo 2", dao.revocadas.get("MC300"));
    }

    @Test
    void listados_respetaLimite() {
        for (int i = 0; i < 5; i++)
            svc.emitir("MC-L" + i, "LOCAL", false, null, 0, null, null);
        assertEquals(3, svc.listarEmitidas(3).size());
        assertEquals(5, svc.listarEmitidas(50).size());
    }

    @Test
    void validarCentral_rechazaEntradasVacias() {
        assertThrows(IllegalArgumentException.class, () -> svc.validarCentral(null, null));
        assertThrows(IllegalArgumentException.class, () -> svc.validarCentral("MC", " "));
    }
}

package com.vendex.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 7: fusión de permisos granulares por usuario sobre los permisos del rol. */
class SesionActualPermisosTest {

    @Test
    void otorgar_anadePermisoQueElRolNoDa() {
        Set<String> efectivos = new HashSet<>(Set.of("FACTURA_EMITIR"));
        Map<String, Boolean> extras = Map.of("USUARIO_GESTIONAR", true);
        SesionActual.aplicarPermisosDeUsuario(efectivos, extras);
        assertTrue(efectivos.contains("USUARIO_GESTIONAR"));
        assertTrue(efectivos.contains("FACTURA_EMITIR"));
    }

    @Test
    void denegar_quitaPermisoQueElRolDa() {
        Set<String> efectivos = new HashSet<>(Set.of("FACTURA_EMITIR", "REPORTE_VER"));
        Map<String, Boolean> extras = Map.of("FACTURA_EMITIR", false);
        SesionActual.aplicarPermisosDeUsuario(efectivos, extras);
        assertFalse(efectivos.contains("FACTURA_EMITIR"));
        assertTrue(efectivos.contains("REPORTE_VER"));
    }

    @Test
    void claves_seNormalizanAMayusculas() {
        Set<String> efectivos = new HashSet<>();
        Map<String, Boolean> extras = new HashMap<>();
        extras.put("  caja_abrir  ", true);
        SesionActual.aplicarPermisosDeUsuario(efectivos, extras);
        assertTrue(efectivos.contains("CAJA_ABRIR"));
    }

    @Test
    void denegar_loQueElRolNoDa_noProduceEstadoInvalido() {
        Set<String> efectivos = new HashSet<>(Set.of("CAJA_ABRIR"));
        Map<String, Boolean> extras = Map.of("INVENTARIO_AJUSTAR", false);
        SesionActual.aplicarPermisosDeUsuario(efectivos, extras);
        assertEquals(1, efectivos.size());
        assertFalse(efectivos.contains("INVENTARIO_AJUSTAR"));
    }

    @Test
    void vacioONulo_noTocaLosPermisos() {
        Set<String> efectivos = new HashSet<>(Set.of("CAJA_ABRIR"));
        SesionActual.aplicarPermisosDeUsuario(efectivos, null);
        SesionActual.aplicarPermisosDeUsuario(efectivos, Map.of());
        assertEquals(1, efectivos.size());
    }
}

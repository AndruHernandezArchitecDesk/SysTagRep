package com.vendex.util;

import com.vendex.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 6 Vendexi — aislamiento de sesión (§13.2): la sesión API por-hilo no
 * contamina a otros hilos ni a la sesión global de la UI local.
 */
class SesionActualAislamientoTest {

    @AfterEach
    void limpiar() {
        SesionActual.cerrarApi();
        SesionActual.cerrar();
    }

    private Usuario user(int id, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("u" + id);
        u.setRol(rol);
        return u;
    }

    @Test
    void sesionApi_noSeComparteEntreHilos() throws Exception {
        SesionActual.iniciarApi(user(10, null), 1, null);
        assertEquals(10, SesionActual.getUsuario().getId());
        assertTrue(SesionActual.tieneSesionApiActiva());

        final int[] vistoEnOtroHilo = {-1};
        Thread otro = new Thread(() -> vistoEnOtroHilo[0] =
                SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : -1);
        otro.start();
        otro.join();
        assertEquals(-1, vistoEnOtroHilo[0], "la sesión API no debe verse desde otro hilo");
    }

    @Test
    void cerrarApi_limpiaSoloElHilo() {
        SesionActual.iniciarApi(user(11, null), 1, null);
        SesionActual.cerrarApi();
        assertNull(SesionActual.getUsuario());
        assertFalse(SesionActual.tieneSesionApiActiva());
    }

    @Test
    void cerrarDesdeHiloApi_noBorraSesionGlobalDeUI() throws Exception {
        // UI (hilo FX simulado por el hilo principal): login global
        SesionActual.iniciar(user(20, "ADMINISTRADOR"));
        assertEquals(20, SesionActual.getUsuario().getId());

        // petición API en OTRO hilo: login + logout (cerrar) no debe tocar la global
        Thread hiloApi = new Thread(() -> {
            SesionActual.iniciarApi(user(30, null), 1, null);
            assertEquals(30, SesionActual.getUsuario().getId());
            SesionActual.cerrar(); // logout API
            assertNull(SesionActual.getUsuario(), "en el hilo API queda limpio");
        });
        hiloApi.start();
        hiloApi.join();

        assertNotNull(SesionActual.getUsuario(), "la sesión global de la UI debe sobrevivir");
        assertEquals(20, SesionActual.getUsuario().getId());
    }

    @Test
    void loginApi_enMismoHilo_sobrescribeSinTocarGlobal() {
        SesionActual.iniciar(user(40, "ADMINISTRADOR"));
        SesionActual.iniciarApi(user(41, null), 1, null);
        assertEquals(41, SesionActual.getUsuario().getId(), "el hilo API ve su usuario");
        SesionActual.cerrarApi();
        assertEquals(40, SesionActual.getUsuario().getId(), "al cerrar API vuelve la sesión global");
    }

    @Test
    void sucursalYPuntoEmision_deLaSesionApi() {
        SesionActual.iniciarApi(user(50, null), 7, 3);
        assertEquals(7, SesionActual.getSucursalId());
        assertEquals(3, SesionActual.getPuntoEmisionId());
        SesionActual.cerrarApi();
        assertEquals(1, SesionActual.getSucursalId(), "sin sesión: default 1");
    }
}

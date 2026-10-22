package com.vendex.dao;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 7: selección de sucursal válida del usuario (lógica pura, sin BD). */
class UsuarioSucursalResolverTest {

    @Test
    void sinFilas_devuelveLaPreferida() {
        assertEquals(3, UsuarioSucursalDAOPostgres.resolver(3, List.of()));
        assertEquals(1, UsuarioSucursalDAOPostgres.resolver(null, List.of()));
        assertEquals(1, UsuarioSucursalDAOPostgres.resolver(null, null));
    }

    @Test
    void preferidaPermitida_seConserva() {
        assertEquals(2, UsuarioSucursalDAOPostgres.resolver(2, List.of(1, 2, 3)));
    }

    @Test
    void preferidaNoPermitida_caeALaPrimeraDeLaLista() {
        assertEquals(1, UsuarioSucursalDAOPostgres.resolver(9, List.of(1, 2)));
        assertEquals(7, UsuarioSucursalDAOPostgres.resolver(null, List.of(7, 8)));
    }
}

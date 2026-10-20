package com.vendex.service;

import com.vendex.dao.VendexiConsumoDAO;
import com.vendex.model.VendexiConsumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fase 3 Vendexi — medición de tokens, capacidad de licencia (§10) y sesiones (§12).
 * DAO en memoria, sin BD.
 */
class VendexiConsumoServiceTest {

    /** DAO en memoria para tests. */
    private static final class FakeDAO implements VendexiConsumoDAO {
        final List<VendexiConsumo> consumos = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 1, 1, 0, 0);
        long idSesion = 0;
        final List<String> eventosSesion = new ArrayList<>();
        boolean fallarInsert = false;

        @Override
        public void insertar(VendexiConsumo c) {
            if (fallarInsert) throw new RuntimeException("sin BD");
            c.setId(consumos.size() + 1);
            consumos.add(c);
        }

        @Override
        public long sumarTokensDesde(LocalDateTime desde) {
            return consumos.stream().filter(c -> c.getFecha() != null && !c.getFecha().isBefore(desde))
                    .mapToLong(VendexiConsumo::getTotalTokens).sum();
        }

        @Override
        public BigDecimal sumarCostoDesde(LocalDateTime desde) {
            return consumos.stream().filter(c -> c.getFecha() != null && !c.getFecha().isBefore(desde))
                    .map(c -> c.getCostoEstimado() != null ? c.getCostoEstimado() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public List<VendexiConsumo> listarReciente(int limite) {
            List<VendexiConsumo> out = new ArrayList<>(consumos);
            java.util.Collections.reverse(out);
            return out.size() > limite ? new ArrayList<>(out.subList(0, limite)) : out;
        }

        @Override
        public LocalDateTime obtenerInicioPeriodo() { return inicio; }

        @Override
        public void actualizarInicioPeriodo(LocalDateTime nuevo) { this.inicio = nuevo; }

        @Override
        public long iniciarSesion(String modulo, String tipo) {
            eventosSesion.add("iniciar:" + modulo + ":" + tipo);
            return ++idSesion;
        }

        @Override
        public void finalizarSesion(long idSesion) {
            eventosSesion.add("finalizar:" + idSesion);
        }
    }

    private static VendexiConsumoService servicio(FakeDAO dao, long capacidad) {
        return new VendexiConsumoService(dao, () -> capacidad);
    }

    // ===== medición =====

    @Test
    void registrarTokens_acumulaTokensYCosto() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 0);
        s.registrarTokens("chatbot", "chat", "gemini-x", 1000, 500);
        s.registrarTokens("contextual", "venta_cruzada", "gemini-x", 2000, 1000);
        assertEquals(4500, s.estado().consumido);
        assertTrue(s.estado().costoPeriodo.doubleValue() > 0, "costo estimado > 0");
        assertEquals(2, dao.consumos.size());
        assertEquals("chatbot", dao.consumos.get(0).getModulo());
    }

    @Test
    void registrarTokens_daoFalla_noLanza() {
        FakeDAO dao = new FakeDAO();
        dao.fallarInsert = true;
        VendexiConsumoService s = servicio(dao, 0);
        s.registrarTokens("chatbot", "chat", "m", 1, 1); // best-effort
        assertEquals(0, s.estado().consumido);
        assertTrue(s.hayCapacidadDisponible());
    }

    @Test
    void estado_sinRegistros_cero() {
        VendexiConsumoService s = servicio(new FakeDAO(), 1000);
        assertEquals(0, s.estado().consumido);
        assertEquals(1000, s.estado().restante());
        assertFalse(s.estado().agotada());
    }

    // ===== capacidad de la licencia (§10.2) =====

    @Test
    void capacidadCero_esSinLimite_nuncaAgotada() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 0);
        for (int i = 0; i < 10; i++) s.registrarTokens("m", "op", "m", 1_000_000, 1_000_000);
        assertFalse(s.estado().agotada());
        assertEquals(-1, s.estado().restante());
        assertTrue(s.hayCapacidadDisponible());
    }

    @Test
    void capacidadAgotada_bloqueaNuevasOperaciones() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 1000);
        s.registrarTokens("m", "op", "m", 600, 600); // 1200 >= 1000
        assertTrue(s.estado().agotada());
        assertEquals(0, s.estado().restante());
        assertFalse(s.hayCapacidadDisponible());
        assertTrue(VendexiConsumoService.mensajeCapacidadAgotada().contains("agotado"));
    }

    @Test
    void capacidadParcial_disponible() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 1000);
        s.registrarTokens("m", "op", "m", 500, 200);
        assertTrue(s.hayCapacidadDisponible());
        assertEquals(300, s.estado().restante());
    }

    // ===== periodo (§10.3) =====

    @Test
    void renovarPeriodo_reiniciaConteo() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 1000);
        s.registrarTokens("m", "op", "m", 900, 900);
        assertTrue(s.estado().agotada(), "1800 >= 1000 antes de renovar");
        assertEquals(1800, s.estado().consumido);
        assertFalse(s.hayCapacidadDisponible());

        s.renovarPeriodo();
        assertTrue(dao.inicio.isAfter(LocalDateTime.of(2026, 1, 1, 0, 0)));
        assertEquals(0, s.estado().consumido, "registros antiguos quedan fuera del periodo");
        assertTrue(s.hayCapacidadDisponible());
    }

    @Test
    void listarReciente_respetaLimite() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 0);
        for (int i = 0; i < 5; i++) s.registrarTokens("m", "op", "m", 1, 1);
        assertEquals(3, s.listarReciente(3).size());
        assertEquals(5, s.listarReciente(50).size());
    }

    // ===== sesiones (§12) =====

    @Test
    void sesiones_registrarInicioFin() {
        FakeDAO dao = new FakeDAO();
        VendexiConsumoService s = servicio(dao, 0);
        long id = s.iniciarSesion("chat", "CONSULTA");
        assertTrue(id > 0);
        s.finalizarSesion(id);
        s.finalizarSesion(-1); // ignorada
        assertEquals(List.of("iniciar:chat:CONSULTA", "finalizar:" + id), dao.eventosSesion);
    }
}

package com.vendex.service;

import com.vendex.dao.EntidadFinancieraDAO;
import com.vendex.dao.PrestamoDAO;
import com.vendex.dao.VentaResumenDAO;
import com.vendex.model.Prestamo;
import com.vendex.model.PrestamoCuota;
import com.vendex.model.Usuario;
import com.vendex.model.VentaResumen;
import com.vendex.util.SesionActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Lógica de negocio de PrestamoService: sugerencias de abono,
 * verificación de metas de venta y validaciones de permisos.
 */
class PrestamoServiceTest {

    private PrestamoDAO prestamoDAO;
    private EntidadFinancieraDAO entidadDAO;
    private VentaResumenDAO ventaDAO;
    private PrestamoService service;

    @BeforeEach
    void setUp() {
        prestamoDAO = Mockito.mock(PrestamoDAO.class);
        entidadDAO = Mockito.mock(EntidadFinancieraDAO.class);
        ventaDAO = Mockito.mock(VentaResumenDAO.class);
        service = new PrestamoService(prestamoDAO, entidadDAO, ventaDAO);
        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("ADMINISTRADOR");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("PRESTAMO_VER", "PRESTAMO_GESTIONAR", "PRESTAMO_ABONAR"));
    }

    // ---------- filas de cuota pendiente: [cuotaId, prestamoId, entidad, numero, venc, monto, pagado, falta, ...] ----------

    private Object[] filaCuota(int cuotaId, int prestamoId, String entidad, int numero, LocalDate vence, String falta) {
        return new Object[]{cuotaId, prestamoId, entidad, numero, vence,
                new BigDecimal("100.00"), new BigDecimal("0.00"), new BigDecimal(falta),
                "PENDIENTE", "M", new BigDecimal("100.00")};
    }

    @Test
    void sugerirAbonos_ordenaVencidasPrimeroYAcumula() {
        when(prestamoDAO.listarCuotasPendientes()).thenReturn(Arrays.asList(
                filaCuota(11, 1, "Banco Pichincha", 3, LocalDate.now().minusDays(5), "100.00"),
                filaCuota(12, 1, "Banco Pichincha", 4, LocalDate.now().plusDays(2), "100.00"),
                filaCuota(21, 2, "Particular", 1, LocalDate.now().plusDays(10), "50.00")
        ));
        // disponibilidad 220 → cubre las 3
        List<Object[]> sugeridas = service.sugerirAbonos(new BigDecimal("220"));
        assertEquals(3, sugeridas.size());

        // disponibilidad 150 → cubre 2 (100 + 100 no entra completa… 150 cubre la 1ra completa y 50 de la 2da)
        List<Object[]> parciales = service.sugerirAbonos(new BigDecimal("150"));
        assertEquals(2, parciales.size());
        assertEquals(11, parciales.get(0)[0]);
        assertEquals(12, parciales.get(1)[0]);
    }

    @Test
    void sugerirAbonos_disponibilidadMenorQuePrimeraCuota_sugiereSoloLaPrimera() {
        when(prestamoDAO.listarCuotasPendientes()).thenReturn(List.<Object[]>of(
                filaCuota(11, 1, "Banco", 1, LocalDate.now(), "100.00")
        ));
        List<Object[]> sugeridas = service.sugerirAbonos(new BigDecimal("30"));
        assertEquals(1, sugeridas.size());
    }

    @Test
    void sugerirAbonos_disponibilidadCero_oNegativa_devuelveVacio() {
        when(prestamoDAO.listarCuotasPendientes()).thenReturn(List.<Object[]>of(
                filaCuota(11, 1, "Banco", 1, LocalDate.now(), "100.00")
        ));
        assertTrue(service.sugerirAbonos(BigDecimal.ZERO).isEmpty());
        assertTrue(service.sugerirAbonos(new BigDecimal("-5")).isEmpty());
        assertTrue(service.sugerirAbonos(null).isEmpty());
    }

    // ---------- metas ----------

    @Test
    void rangoPeriodoAnterior_diaria_esAyer() {
        LocalDate[] r = PrestamoService.rangoPeriodoAnterior("DIARIA", LocalDate.of(2026, 10, 21));
        assertEquals(LocalDate.of(2026, 10, 20), r[0]);
        assertEquals(LocalDate.of(2026, 10, 20), r[1]);
    }

    @Test
    void rangoPeriodoAnterior_semanal_sieteDiasTerminandoAyer() {
        LocalDate[] r = PrestamoService.rangoPeriodoAnterior("SEMANAL", LocalDate.of(2026, 10, 21));
        assertEquals(LocalDate.of(2026, 10, 14), r[0]);
        assertEquals(LocalDate.of(2026, 10, 20), r[1]);
    }

    @Test
    void verificarMeta_ventasSuficientes_cumple() {
        Prestamo p = new Prestamo();
        p.setEntidadNombre("Banco");
        p.setMetaPeriodicidad("DIARIA");
        p.setMetaMonto(new BigDecimal("50.00"));
        VentaResumen v = new VentaResumen();
        v.setTotal(new BigDecimal("80.00"));
        when(ventaDAO.listarPorRango(any(), any())).thenReturn(List.of(v));

        Object[] res = service.verificarMeta(p, LocalDate.now().minusDays(1), LocalDate.now().minusDays(1));
        assertTrue((Boolean) res[0]);
        assertEquals(0, ((BigDecimal) res[1]).compareTo(new BigDecimal("80.00")));
        assertEquals(0, ((BigDecimal) res[3]).compareTo(BigDecimal.ZERO));
    }

    @Test
    void verificarMeta_ventasInsuficientes_calculaFaltante() {
        Prestamo p = new Prestamo();
        p.setEntidadNombre("Banco");
        p.setMetaPeriodicidad("SEMANAL");
        p.setMetaMonto(new BigDecimal("500.00"));
        VentaResumen v1 = new VentaResumen();
        v1.setTotal(new BigDecimal("200.00"));
        VentaResumen v2 = new VentaResumen();
        v2.setTotal(new BigDecimal("150.00"));
        when(ventaDAO.listarPorRango(any(), any())).thenReturn(List.of(v1, v2));

        Object[] res = service.verificarMeta(p, LocalDate.now().minusDays(7), LocalDate.now().minusDays(1));
        assertFalse((Boolean) res[0]);
        assertEquals(0, ((BigDecimal) res[1]).compareTo(new BigDecimal("350.00")));
        assertEquals(0, ((BigDecimal) res[3]).compareTo(new BigDecimal("150.00")));
    }

    @Test
    void calcularMetaAutomatica_diaria_divideEntre30() {
        // cuota mensual 300 → meta diaria 10
        BigDecimal meta = PrestamoService.calcularMetaAutomatica(new BigDecimal("300.00"), "M", "DIARIA");
        assertEquals(0, meta.compareTo(new BigDecimal("10.00")));
    }

    @Test
    void calcularMetaAutomatica_semanal_sobrePrestamoMensual() {
        // cuota mensual 300 → meta semanal ≈ 70.00 (300 / (30/7))
        BigDecimal meta = PrestamoService.calcularMetaAutomatica(new BigDecimal("300.00"), "M", "SEMANAL");
        assertEquals(0, meta.compareTo(new BigDecimal("70.00")));
    }

    @Test
    void calcularMetaAutomatica_semanal_sobrePrestamoSemanal_esLaCuota() {
        BigDecimal meta = PrestamoService.calcularMetaAutomatica(new BigDecimal("120.00"), "S", "SEMANAL");
        assertEquals(0, meta.compareTo(new BigDecimal("120.00")));
    }

    // ---------- validaciones ----------

    @Test
    void registrarAbono_montoInvalido_lanza() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarAbono(1, BigDecimal.ZERO, null, "MANUAL", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarAbono(1, null, null, "MANUAL", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarAbono(1, new BigDecimal("-1"), null, "MANUAL", null, null));
    }

    @Test
    void registrarAbono_prestamoNoExiste_lanza() {
        when(prestamoDAO.obtenerPorId(99)).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarAbono(99, new BigDecimal("10"), null, "MANUAL", null, null));
    }

    @Test
    void registrarAbono_prestamoNoActivo_lanza() {
        Prestamo p = new Prestamo();
        p.setId(5);
        p.setEstado("PAGADO");
        when(prestamoDAO.obtenerPorId(5)).thenReturn(p);
        assertThrows(IllegalStateException.class,
                () -> service.registrarAbono(5, new BigDecimal("10"), null, "MANUAL", null, null));
    }

    @Test
    void registrarAbono_sinPermiso_lanzaSinPermiso() {
        SesionActual.setPermisosForTest(Set.of("PRESTAMO_VER"));
        assertThrows(com.vendex.exception.SinPermisoException.class,
                () -> service.registrarAbono(1, new BigDecimal("10"), null, "MANUAL", null, null));
    }

    @Test
    void registrarAbono_delegaEnDaoYDevuelveAplicado() {
        Prestamo p = new Prestamo();
        p.setId(5);
        p.setEstado("ACTIVO");
        when(prestamoDAO.obtenerPorId(5)).thenReturn(p);
        when(prestamoDAO.registrarAbono(eq(5), eq(new BigDecimal("100.00")), isNull(), eq("CAJA"), eq(7), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        BigDecimal aplicado = service.registrarAbono(5, new BigDecimal("100.00"), null, "CAJA", 7, "nota test");
        assertEquals(0, aplicado.compareTo(new BigDecimal("100.00")));
    }

    // ---------- creación ----------

    @Test
    void crearPrestamo_validaEntidad() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearPrestamo(0, new BigDecimal("100"), BigDecimal.ONE, "M", 3, LocalDate.now(), null, null, null));
    }

    @Test
    void crearPrestamo_validaMonto() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearPrestamo(1, BigDecimal.ZERO, BigDecimal.ONE, "M", 3, LocalDate.now(), null, null, null));
    }

    @Test
    void crearPrestamo_validaPeriodicidad() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearPrestamo(1, new BigDecimal("100"), BigDecimal.ONE, "Q", 3, LocalDate.now(), null, null, null));
    }

    @Test
    void crearPrestamo_persistePrestamoYCuotas() {
        when(prestamoDAO.insertar(any())).thenReturn(42);
        Prestamo p = service.crearPrestamo(1, new BigDecimal("1000"), TASA_5(), "M", 6,
                LocalDate.of(2026, 1, 1), "DIARIA", new BigDecimal("10.00"), "test");
        assertEquals(42, p.getId());
        assertEquals("ACTIVO", p.getEstado());
        assertTrue(p.getTotalAPagar().compareTo(new BigDecimal("1000")) > 0);
        assertEquals(0, p.getTotalInteres().compareTo(p.getTotalAPagar().subtract(new BigDecimal("1000"))));
        Mockito.verify(prestamoDAO).insertarCuotas(argThat(cuotas -> cuotas.size() == 6));
    }

    private static BigDecimal TASA_5() { return new BigDecimal("5"); }
}

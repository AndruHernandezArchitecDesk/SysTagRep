package com.vendex.service;

import com.vendex.dao.DashboardDAO;
import com.vendex.dao.HistorialProductoDAO;
import com.vendex.dao.InventarioDAO;
import com.vendex.model.HistorialProducto;
import com.vendex.model.Inventario;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 5 Vendexi — análisis empresarial (§7): agregados autorizados y prompt
 * con periodo/datos/supuestos/limitaciones. Sin red ni BD.
 */
class AnalisisEmpresarialServiceTest {

    private Inventario inv(String cod, String desc, int stock, String costo, String venta) {
        Inventario i = new Inventario();
        i.setId(cod.hashCode());
        i.setCodigo(cod);
        i.setDescripcion(desc);
        i.setCantidad(stock);
        i.setCostoSinIVA(costo != null ? new BigDecimal(costo) : null);
        i.setPrecioVenta(venta != null ? new BigDecimal(venta) : null);
        return i;
    }

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> iface, Map<String, Function<Object[], Object>> metodos) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[]{iface}, (p, m, a) -> {
            Function<Object[], Object> f = metodos.get(m.getName());
            if (f == null) throw new UnsupportedOperationException(m.getName());
            return f.apply(a);
        });
    }

    private AnalisisEmpresarialService servicio(List<Inventario> activos, List<Inventario> bajos,
                                                List<Inventario> negativos, List<HistorialProducto> hist,
                                                Map<String, Double> ventas) {
        InventarioDAO invDao = fake(InventarioDAO.class, Map.of(
                "listar", a -> activos,
                "listarStockBajo", a -> bajos,
                "listarStockNegativo", a -> negativos));
        DashboardDAO dashDao = fake(DashboardDAO.class, Map.of(
                "ventasPorDia", a -> ventas,
                "comprasPorDia", a -> Map.of()));
        HistorialProductoDAO histDao = fake(HistorialProductoDAO.class, Map.of(
                "listar", a -> hist));
        return new AnalisisEmpresarialService(invDao, dashDao, histDao, null);
    }

    private HistorialProducto venta(String cod, String desc, int unidades, int diasAtras) {
        HistorialProducto h = new HistorialProducto();
        h.setProductoCodigo(cod);
        h.setProductoDescripcion(desc);
        h.setCantidad(unidades);
        h.setFechaVenta(LocalDateTime.now().minusDays(diasAtras));
        return h;
    }

    // ===== agregados autorizados =====

    @Test
    void agotamiento_incluyeStockBajoYNegativo() {
        var svc = servicio(
                List.of(inv("A", "PROD A", 0, "5", "10")),
                List.of(inv("B", "PROD B", 3, "5", "10")),
                List.of(inv("C", "PROD C", -2, "5", "10")),
                List.of(), Map.of());
        String datos = svc.construirDatosAutorizados(AnalisisEmpresarialService.Tipo.AGOTAMIENTO, 30);
        assertTrue(datos.contains("RIESGO DE AGOTAMIENTO"));
        assertTrue(datos.contains("B | PROD B | stock: 3"));
        assertTrue(datos.contains("STOCK NEGATIVO"));
        assertTrue(datos.contains("C | PROD C | stock: -2"));
        assertTrue(datos.contains("PERIODO: últimos 30 días"));
    }

    @Test
    void margenes_calculaPorcentaje() {
        var svc = servicio(
                List.of(inv("M1", "MARGEN BAJO", 1, "80", "100"),
                        inv("M2", "MARGEN ALTO", 1, "50", "100")),
                List.of(), List.of(), List.of(), Map.of());
        String datos = svc.construirDatosAutorizados(AnalisisEmpresarialService.Tipo.MARGENES, 30);
        assertTrue(datos.contains("M1 | MARGEN BAJO | margen: 20.0%"));
        assertTrue(datos.contains("M2 | MARGEN ALTO | margen: 50.0%"));
        // el más bajo va primero (lista ordenada ascendente)
        assertTrue(datos.indexOf("MARGEN BAJO") < datos.indexOf("MARGEN ALTO"));
    }

    @Test
    void rotacion_sumaUnidades_yCapitalInmovilizado() {
        var hist = List.of(
                venta("P1", "FILTRO", 3, 2),
                venta("P1", "FILTRO", 2, 5),
                venta("P2", "BANDA", 1, 1));
        var activos = List.of(inv("P1", "FILTRO", 10, "5", "8"),
                inv("P3", "SIN VENTA", 4, "5", "8"),
                inv("P2", "BANDA", 0, "5", "8"));
        var svc = servicio(activos, List.of(), List.of(), hist, Map.of());
        String datos = svc.construirDatosAutorizados(AnalisisEmpresarialService.Tipo.ROTACION, 30);
        assertTrue(datos.contains("P1 | FILTRO | unidades: 5"));
        assertTrue(datos.contains("P2 | BANDA | unidades: 1"));
        assertTrue(datos.contains("CAPITAL INMOVILIZADO"));
        assertTrue(datos.contains("P3 | SIN VENTA | stock: 4"));
        assertFalse(datos.contains("P1 | FILTRO | stock"), "P1 rota: no es capital inmovilizado");
    }

    @Test
    void tendencias_incluyeVentasYTotal() {
        var svc = servicio(List.of(), List.of(), List.of(), List.of(),
                Map.of("2026-10-01", 100.0, "2026-10-02", 250.5));
        String datos = svc.construirDatosAutorizados(AnalisisEmpresarialService.Tipo.TENDENCIAS, 7);
        assertTrue(datos.contains("TENDENCIAS DE VENTA"));
        assertTrue(datos.contains("2026-10-02 | 250.50"));
        assertTrue(datos.contains("TOTAL venta periodo: 350.50"));
        assertTrue(datos.contains("COMPRAS por día"));
    }

    // ===== prompt (§7) =====

    @Test
    void prompt_pidePeriodoDatosSupuestosLimitaciones() {
        var svc = servicio(List.of(), List.of(), List.of(), List.of(), Map.of());
        String p = svc.construirPrompt(AnalisisEmpresarialService.Tipo.MARGENES, 30, "DATOS...");
        assertTrue(p.contains("MARGENES"));
        assertTrue(p.contains("últimos 30 días"));
        assertTrue(p.contains("Periodo"));
        assertTrue(p.contains("Supuestos"));
        assertTrue(p.contains("Limitaciones"));
        assertTrue(p.contains("No garantices ganancias"));
        assertTrue(p.contains("no presentes predicciones como hechos"));
        assertTrue(p.contains("no los inventes"));
        assertTrue(p.contains("DATOS..."));
    }

    @Test
    void resumenCorto_limitaLineas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("linea ").append(i).append('\n');
        String r = AnalisisEmpresarialService.resumenCorto(sb.toString());
        assertTrue(r.contains("linea 0"));
        assertTrue(r.contains("completos enviados"));
        assertTrue(r.lines().count() <= 30);
    }
}

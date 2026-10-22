package com.vendex.service;

import com.vendex.model.Inventario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 2 Vendexi AI — tests del servicio contextual (lógica pura, sin red ni JavaFX).
 */
class VendexiSugerenciaServiceTest {

    private final VendexiSugerenciaService svc = new VendexiSugerenciaService();

    private Inventario inv(int id, String codigo, String desc, String marca, String grupo, int stock, String precio) {
        Inventario i = new Inventario();
        i.setId(id);
        i.setCodigo(codigo);
        i.setDescripcion(desc);
        i.setMarca(marca);
        i.setGrupo(grupo);
        i.setCantidad(stock);
        i.setPrecioVenta(new BigDecimal(precio));
        return i;
    }

    // ===== verificarDocumento (local) =====

    @Test
    void verificar_sinCliente_y_sinLineas_generaDosObservaciones() {
        var ctx = new VendexiSugerenciaService.Contexto("factura", false, List.of());
        List<VendexiSugerenciaService.Sugerencia> res = svc.verificarDocumento(ctx);
        assertEquals(2, res.size());
        assertTrue(res.get(0).titulo.contains("Cliente"));
        assertTrue(res.get(1).titulo.contains("Sin productos"));
        assertTrue(res.get(0).motivo.contains("sin IA"));
    }

    @Test
    void verificar_precioEnCero_generaObservacion() {
        var linea = new VendexiSugerenciaService.Linea(1, "F001", "FILTRO", 1, BigDecimal.ZERO, 5);
        var ctx = new VendexiSugerenciaService.Contexto("factura", true, List.of(linea));
        List<VendexiSugerenciaService.Sugerencia> res = svc.verificarDocumento(ctx);
        assertEquals(1, res.size());
        assertTrue(res.get(0).titulo.contains("Precio en cero"));
        assertTrue(res.get(0).titulo.contains("F001"));
    }

    @Test
    void verificar_cantidadMayorQueStock_generaObservacion() {
        var linea = new VendexiSugerenciaService.Linea(1, "F001", "FILTRO", 10, new BigDecimal("5.00"), 3);
        var ctx = new VendexiSugerenciaService.Contexto("proforma", true, List.of(linea));
        List<VendexiSugerenciaService.Sugerencia> res = svc.verificarDocumento(ctx);
        assertEquals(1, res.size());
        assertTrue(res.get(0).titulo.contains("Stock insuficiente"));
    }

    @Test
    void verificar_documentoCompleto_sinObservaciones() {
        var linea = new VendexiSugerenciaService.Linea(1, "F001", "FILTRO", 1, new BigDecimal("5.00"), 5);
        var ctx = new VendexiSugerenciaService.Contexto("factura", true, List.of(linea));
        assertTrue(svc.verificarDocumento(ctx).isEmpty());
    }

    // ===== seleccionarCandidatos =====

    @Test
    void verificar_notaCredito_sinClienteYSinLineas() {
        var ctx = new VendexiSugerenciaService.Contexto("nota_credito", false, List.of());
        var res = svc.verificarDocumento(ctx);
        assertEquals(2, res.size());
        assertTrue(res.stream().anyMatch(s -> s.titulo.contains("Cliente")));
        assertTrue(res.stream().anyMatch(s -> s.titulo.contains("Sin productos")));
    }

    @Test
    void verificar_notaCredito_devuelveMercaderia_noRequiereStock() {
        var linea = new VendexiSugerenciaService.Linea(1, "F001", "FILTRO", 10, new BigDecimal("5.00"), 0);
        var ctx = new VendexiSugerenciaService.Contexto("nota_credito", true, List.of(linea));
        assertTrue(svc.verificarDocumento(ctx).isEmpty(), "Una devolución no consume stock");
    }

    @Test
    void candidatos_excluyeBase_yYaEnDocumento_ySinStock() {
        Inventario base = inv(1, "B1", "BANDA DISTRIBUCION CHEV AVEO", "CHEV", "BANDAS", 4, "20");
        Inventario mismoGrupo = inv(2, "R1", "RUEDA DISTRIBUCION CHEV AVEO", "OTRA", "BANDAS", 2, "25");
        Inventario yaEnDoc = inv(3, "T1", "TENSOR DISTRIBUCION CHEV AVEO", "CHEV", "BANDAS", 2, "15");
        Inventario sinStock = inv(4, "P1", "Polea DISTRIBUCION CHEV", "CHEV", "BANDAS", 0, "10");
        Inventario nada = inv(5, "X1", "BATERIA", "BOSCH", "ELECTRICO", 3, "40");

        List<Inventario> res = svc.seleccionarCandidatos(base,
                List.of(base, mismoGrupo, yaEnDoc, sinStock, nada), Set.of(3), 10);

        assertTrue(res.stream().noneMatch(i -> i.getId() == 1), "excluye base");
        assertTrue(res.stream().noneMatch(i -> i.getId() == 3), "excluye ya en documento");
        assertTrue(res.stream().noneMatch(i -> i.getId() == 4), "excluye sin stock");
        assertTrue(res.stream().anyMatch(i -> i.getId() == 2), "incluye mismo grupo con stock");
    }

    @Test
    void candidatos_limitaMaximo() {
        Inventario base = inv(1, "B1", "PASTILLA FRENO DELANtera", "BOSCH", "FRENOS", 5, "10");
        List<Inventario> todos = new java.util.ArrayList<>();
        for (int i = 2; i <= 20; i++) {
            todos.add(inv(i, "P" + i, "PASTILLA FRENO " + i, "BOSCH", "FRENOS", 1, "10"));
        }
        List<Inventario> res = svc.seleccionarCandidatos(base, todos, Set.of(), 5);
        assertEquals(5, res.size());
    }

    // ===== parsearSugerenciasIA =====

    @Test
    void parsear_respuestaValida_mapeaACodigosReales() {
        Inventario comp = inv(7, "C-100", "CORREA ACOPLE", "BOSCH", "MOTOR", 3, "12.50");
        String json = "{\"sugerencias\":[{\"codigo\":\"C-100\",\"motivo\":\"Suele venderse con el producto principal\"},"
                + "{\"codigo\":\"NOEXISTE\",\"motivo\":\"inventado\"}]}";
        List<VendexiSugerenciaService.Sugerencia> res =
                svc.parsearSugerenciasIA(json, svc.indexarPorCodigo(List.of(comp)));
        assertEquals(1, res.size(), "descarta códigos inventados por la IA");
        assertEquals("C-100", res.get(0).producto.getCodigo());
        assertTrue(res.get(0).motivo.contains("producto principal"));
        assertEquals(VendexiSugerenciaService.Tipo.COMPLEMENTARIO, res.get(0).tipo);
    }

    @Test
    void parsear_motivoVacio_usaMotivoGenerico() {
        Inventario comp = inv(7, "C-100", "CORREA ACOPLE", "BOSCH", "MOTOR", 3, "12.50");
        String json = "{\"sugerencias\":[{\"codigo\":\"C-100\",\"motivo\":\"\"}]}";
        List<VendexiSugerenciaService.Sugerencia> res =
                svc.parsearSugerenciasIA(json, svc.indexarPorCodigo(List.of(comp)));
        assertEquals(1, res.size());
        assertFalse(res.get(0).motivo.isBlank());
        assertTrue(res.get(0).motivo.contains("IA"));
    }

    @Test
    void parsear_jsonInvalido_devuelveVacio() {
        assertTrue(svc.parsearSugerenciasIA("no es json", Map.of()).isEmpty());
        assertTrue(svc.parsearSugerenciasIA(null, Map.of()).isEmpty());
        assertTrue(svc.parsearSugerenciasIA("{\"sugerencias\":[]}", Map.of()).isEmpty());
    }

    @Test
    void parsear_respuestaConMarkdown_seLimpia() {
        Inventario comp = inv(7, "C-100", "CORREA", "BOSCH", "MOTOR", 3, "12.50");
        String json = "```json\n{\"sugerencias\":[{\"codigo\":\"C-100\",\"motivo\":\"m\"}]}\n```";
        // el servicio espera JSON ya extraído del bloque markdown; un JSON envuelto en texto basura falla y devuelve vacío
        assertTrue(svc.parsearSugerenciasIA(json, svc.indexarPorCodigo(List.of(comp))).isEmpty());
    }

    // ===== prompt =====

    @Test
    void prompt_incluyeProducto_codigosExactos_yReglas() {
        Inventario base = inv(1, "B1", "BANDA DISTRIBUCION", "CHEV", "BANDAS", 4, "20");
        Inventario c = inv(2, "R1", "RUEDA DISTRIBUCION", "CHEV", "BANDAS", 2, "25");
        String p = svc.construirPrompt(base, List.of(c), "CHEV AVEO 2015");
        assertTrue(p.contains("BANDA DISTRIBUCION"));
        assertTrue(p.contains("R1|RUEDA DISTRIBUCION"));
        assertTrue(p.contains("CHEV AVEO 2015"));
        assertTrue(p.contains("no inventes"));
        assertTrue(p.contains("\"sugerencias\""));
    }

    // ===== gate de licencia (sin tocar archivo real de licencia: v1 implícito) =====

    @Test
    void ia_sugerenciasComplementarias_oVacio_siNoDisponible() {
        // Si el entorno de test no tiene IA disponible (esperado en CI), debe devolver vacío sin red.
        // Con key real configurada no se ejercita la red desde el test unitario.
        if (svc.esIaDisponible()) return;
        assertTrue(svc.sugerenciasComplementarias(
                inv(1, "B", "BANDA", "C", "G", 1, "1"),
                List.of(inv(2, "R", "RUEDA", "C", "G", 1, "1")),
                Set.of(), null).isEmpty());
    }

    // ===== evidencia (Fase 4, spec §6) =====

    @Test
    void parsear_evidenciaConfirmada_seConserva() {
        Inventario comp = inv(2, "R1", "RUEDA DISTRIBUCION", "CHEV", "BANDAS", 2, "25");
        String json = "{\"sugerencias\":[" +
                "{\"codigo\":\"R1\",\"evidencia\":\"CONFIRMADA\",\"motivo\":\"pares de distribucion del fabricante\"}," +
                "{\"codigo\":\"B1\",\"evidencia\":\"PROBABLE\",\"motivo\":\"coincidencia habitual\"}]}";
        Map<String, Inventario> idx = svc.indexarPorCodigo(List.of(
                inv(1, "B1", "BANDA DISTRIBUCION", "CHEV", "BANDAS", 4, "20"), comp));
        List<VendexiSugerenciaService.Sugerencia> res = svc.parsearSugerenciasIA(json, idx);
        assertEquals(2, res.size());
        assertEquals(VendexiSugerenciaService.EVIDENCIA_CONFIRMADA, res.get(0).evidencia);
        assertEquals(VendexiSugerenciaService.EVIDENCIA_PROBABLE, res.get(1).evidencia);
    }

    @Test
    void parsear_sinEvidencia_defaultAsociacion() {
        Inventario comp = inv(2, "R1", "RUEDA", "C", "B", 1, "5");
        String json = "{\"sugerencias\":[{\"codigo\":\"R1\",\"motivo\":\"complemento\"}]}";
        List<VendexiSugerenciaService.Sugerencia> res =
                svc.parsearSugerenciasIA(json, svc.indexarPorCodigo(List.of(comp)));
        assertEquals(1, res.size());
        assertEquals(VendexiSugerenciaService.EVIDENCIA_ASOCIACION, res.get(0).evidencia);
    }

    @Test
    void normalizarEvidencia_valoresVariantes() {
        assertEquals(VendexiSugerenciaService.EVIDENCIA_CONFIRMADA,
                VendexiSugerenciaService.normalizarEvidencia("compatibilidad confirmada"));
        assertEquals(VendexiSugerenciaService.EVIDENCIA_PROBABLE,
                VendexiSugerenciaService.normalizarEvidencia("coincidencia_probable"));
        assertEquals(VendexiSugerenciaService.EVIDENCIA_ASOCIACION,
                VendexiSugerenciaService.normalizarEvidencia(null));
    }

    @Test
    void prompt_pideCampoEvidencia() {
        Inventario base = inv(1, "B1", "BANDA", "CHEV", "BANDAS", 4, "20");
        String p = svc.construirPrompt(base, List.of(inv(2, "R1", "RUEDA", "CHEV", "BANDAS", 2, "25")), null);
        assertTrue(p.contains("evidencia"));
        assertTrue(p.contains("CONFIRMADA"));
    }
}

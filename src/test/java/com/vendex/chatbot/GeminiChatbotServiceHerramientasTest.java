package com.vendex.chatbot;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase agente IA — herramientas de lectura del ERP (sin red: proveedor IA falso
 * y DAOs falsos). Cubre clasificación de intención, ejecución de cada herramienta,
 * degradaciones y el flujo completo clasificar → ejecutar → redactar.
 */
class GeminiChatbotServiceHerramientasTest {

    // ---------- DAOs falsos ----------

    private static final class RepuestoDaoFalso implements GeminiChatbotService.RepuestoDao {
        @Override
        public GeminiChatbotService.ResultadoBusqueda buscar(String descripcion, String marca, String modelo, Integer anio) {
            if (descripcion != null && descripcion.toLowerCase().contains("tapa")) {
                return new GeminiChatbotService.ResultadoBusqueda(true, "TAPA-01", "TAPA RADIADOR CHEV AVEO", 7, 25.90);
            }
            return GeminiChatbotService.ResultadoBusqueda.noEncontrado();
        }
    }

    private static final class HerramientasFalsas implements HerramientasAgente {
        @Override
        public List<Producto> productos(String criterio, int limite) {
            if (criterio != null && criterio.toLowerCase().contains("filtro")) {
                return List.of(new Producto("FIL-01", "FILTRO ACEITE TOYOTA HILUX", 12,
                        new BigDecimal("15.50"), new BigDecimal("9.00"), 41.9));
            }
            return List.of();
        }

        @Override
        public List<Cliente> clientes(String criterio, int limite) {
            if (criterio == null) return List.of();
            String c = criterio.toLowerCase();
            if (c.contains("maria")) return List.of(new Cliente(10, "Maria Sola", "1799988877"));
            if (c.contains("juan")) return List.of(
                    new Cliente(1, "Juan Perez", "1712345678"),
                    new Cliente(2, "Juan Lopez", "0987654321"));
            return List.of();
        }

        @Override
        public Ventas ventas(int clienteId) {
            if (clienteId == 10) return new Ventas(3, new BigDecimal("450.75"), "2026-10-01");
            return null;
        }

        @Override
        public BigDecimal saldo(int clienteId) {
            if (clienteId == 10) return new BigDecimal("120.00");
            return BigDecimal.ZERO;
        }

        @Override
        public List<Top> masVendidos(int limite) {
            return List.of(new Top("PAST-01", "PASTILLA FRENO", 40, new BigDecimal("600.00"), null));
        }

        @Override
        public List<Top> mejorMargen(int limite) {
            return List.of(new Top("TAPA-01", "TAPA RADIADOR", 0, null, 52.3));
        }
    }

    /** Proveedor Gemini falso: responde de una cola; si se vacía, lanza (la redacción es best-effort). */
    private static final class ConGeminiFake extends GeminiChatbotService {
        private final ArrayDeque<String> respuestas = new ArrayDeque<>();
        private final java.util.Set<String> rolesBloqueados = new java.util.HashSet<>();

        ConGeminiFake(HerramientasAgente herramientas) {
            super(new RepuestoDaoFalso(), herramientas);
        }

        void ofrecer(String... rs) {
            for (String r : rs) respuestas.add(r);
        }

        void bloquearRol(String rol) {
            rolesBloqueados.add(rol);
        }

        @Override
        protected boolean rolPermitido(String rol) {
            return !rolesBloqueados.contains(rol) && super.rolPermitido(rol);
        }

        @Override
        protected String llamarGemini(String prompt, String apiKey) {
            if (respuestas.isEmpty()) throw new RuntimeException("proveedor IA fake sin respuestas");
            return respuestas.poll();
        }
    }

    private static GeminiChatbotService.Clasificacion clasif(String json) {
        return GeminiChatbotService.parsearClasificacion(json);
    }

    // ---------- parseo del clasificador ----------

    @Test
    void parsear_jsonInvalido_devuelveNull() {
        assertNull(clasif(null));
        assertNull(clasif(""));
        assertNull(clasif("esto no es json"));
    }

    @Test
    void parsear_jsonLegacySinAccion_esBuscarRepuesto() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"descripcion\":\"pastilla freno\",\"marca\":\"CHEV\",\"modelo\":null,\"anio\":null,\"motivo\":\"Ruido\",\"esSintoma\":true}");
        assertNotNull(c);
        assertEquals(GeminiChatbotService.Accion.BUSCAR_REPUESTO, c.accion, "compat con prompts viejos");
        assertEquals("pastilla freno", c.descripcion);
        assertEquals("CHEV", c.marca);
        assertTrue(c.esSintoma);
    }

    @Test
    void parsear_accionYCliente() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"VENTAS_CLIENTE\",\"descripcion\":null,\"cliente\":\"Maria Sola\"}");
        assertNotNull(c);
        assertEquals(GeminiChatbotService.Accion.VENTAS_CLIENTE, c.accion);
        assertEquals("Maria Sola", c.cliente);
    }

    @Test
    void parsear_accionDesconocida_caeEnBuscarRepuesto() {
        GeminiChatbotService.Clasificacion c = clasif("{\"accion\":\"FACTURAR\",\"descripcion\":\"x\"}");
        assertNotNull(c);
        assertEquals(GeminiChatbotService.Accion.BUSCAR_REPUESTO, c.accion);
    }

    // ---------- ejecución de herramientas (datos reales locales) ----------

    private GeminiChatbotService nuevo() {
        return new GeminiChatbotService(new RepuestoDaoFalso(), new HerramientasFalsas());
    }

    @Test
    void consultarProducto_devuelveStockPrecioYMargen() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro aceite\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto stock tiene el filtro aceite?");
        assertTrue(out.contains("FIL-01"), out);
        assertTrue(out.contains("stock: 12"), out);
        assertTrue(out.contains("$15.50"), out);
        assertTrue(out.contains("margen"), out);
    }

    @Test
    void consultarProducto_sinCoincidencias_loDice() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"rueda espejo\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto stock de rueda espejo?");
        assertTrue(out.toLowerCase().contains("no encontr"), out);
    }

    @Test
    void ventasCliente_unico_devuelveTotal() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Maria\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto me compró Maria?");
        assertTrue(out.contains("Maria Sola"), out);
        assertTrue(out.contains("3 factura"), out);
        assertTrue(out.contains("450.75"), out);
        assertTrue(out.contains("2026-10-01"), out);
    }

    @Test
    void ventasCliente_ambiguo_listaCandidatos() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Juan\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto me compró Juan?");
        assertTrue(out.contains("2 clientes"), out);
        assertTrue(out.contains("Juan Perez") && out.contains("Juan Lopez"), out);
    }

    @Test
    void ventasCliente_inexistente_loDice() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Nadie Existe\"}");
        String out = nuevo().ejecutarLocal(c, "ventas de Nadie Existe");
        assertTrue(out.contains("No encontré al cliente"), out);
    }

    @Test
    void cuentasCobrar_devuelveSaldo() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CUENTAS_COBRAR\",\"cliente\":\"Maria\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto me debe Maria?");
        assertTrue(out.contains("120.00"), out);
        assertTrue(out.contains("debe"), out);
    }

    @Test
    void cuentasCobrar_clienteAlDia_loDice() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CUENTAS_COBRAR\",\"cliente\":\"Juan\"}");
        // 2 candidatos -> lista candidatos; con uno solo sería ZERO -> al día
        GeminiChatbotService.Clasificacion c2 = clasif(
                "{\"accion\":\"CUENTAS_COBRAR\",\"cliente\":\"Maria\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto me debe Juan?");
        assertTrue(out.contains("clientes"), out);
        String out2 = nuevo().ejecutarLocal(c2, "¿cuánto me debe Maria?");
        assertTrue(out2.contains("120.00"), out2);
    }

    @Test
    void rentabilidad_incluyeTopMargenYTopVendidos() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"RENTABILIDAD\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuáles son mis productos más rentables?");
        assertTrue(out.contains("TAPA-01"), out);
        assertTrue(out.contains("52.3%"), out);
        assertTrue(out.contains("PAST-01"), out);
        assertTrue(out.contains("Más vendidos"), out);
    }

    @Test
    void general_devuelveVacio_paraQueLaIARedacte() {
        GeminiChatbotService.Clasificacion c = clasif("{\"accion\":\"GENERAL\"}");
        assertEquals("", nuevo().ejecutarLocal(c, "hola buenos días"));
    }

    @Test
    void sinHerramientas_degradaABusquedaDeRepuestos() {
        GeminiChatbotService sinHerr = new GeminiChatbotService(new RepuestoDaoFalso());
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"tapa radiador\"}");
        String out = sinHerr.ejecutarLocal(c, "stock de tapa radiador");
        assertTrue(out.contains("TAPA-01"), out);
    }

    @Test
    void buscarRepuesto_sintoma_anteponeMotivo() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"BUSCAR_REPUESTO\",\"descripcion\":\"tapa radiador\",\"motivo\":\"Pérdida de líquido\",\"esSintoma\":true}");
        String out = nuevo().ejecutarLocal(c, "pierde liquido");
        assertTrue(out.contains("💡 Sugerencia IA: Pérdida de líquido"), out);
        assertTrue(out.contains("TAPA-01"), out);
    }

    // ---------- flujo completo: clasificar -> ejecutar -> redactar ----------

    @Test
    void flujoCompleto_conRedaccion_usaRespuestaRedactada() throws Exception {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ofrecer(
                "{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Maria\"}",
                "Maria te compró $450.75 en 3 facturas.");
        String out = fake.preguntarConGemini("¿cuánto me compró Maria?", "fake-key", null, null, null);
        assertEquals("Maria te compró $450.75 en 3 facturas.", out);
    }

    @Test
    void flujoCompleto_redaccionFalla_usaDatosLocales() throws Exception {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ofrecer("{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro aceite\"}");
        String out = fake.preguntarConGemini("stock filtro aceite", "fake-key", null, null, null);
        assertTrue(out.contains("FIL-01"), out);
        assertTrue(out.contains("stock: 12"), out);
    }

    @Test
    void flujoCompleto_jsonRoto_degradaABusqueda() throws Exception {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ofrecer("no-json", "Redacción genérica");
        String out = fake.preguntarConGemini("tapa radiador aveo", "fake-key", null, null, null);
        assertEquals("Redacción genérica", out);
    }

    @Test
    void filtrosDeLaUI_tienenPrioridadSobreLaClasificacion() throws Exception {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        // El clasificador no trae marca; la UI envía marca=CHEV -> búsqueda de repuestos con filtro
        fake.ofrecer("{\"accion\":\"BUSCAR_REPUESTO\",\"descripcion\":\"tapa radiador\"}",
                "ok");
        String out = fake.preguntarConGemini("tapa radiador", "fake-key", "CHEV", "AVEO", 2015);
        assertEquals("ok", out);
    }

    // ---------- Fase B: roles contextuales §4 ----------

    @Test
    void rolesDeAccion_mapeaAccionesARolesDeLicencia() {
        assertEquals(java.util.List.of("VENTAS", "INVENTARIO"),
                GeminiChatbotService.rolesDeAccion(GeminiChatbotService.Accion.BUSCAR_REPUESTO));
        assertEquals(java.util.List.of("VENTAS", "INVENTARIO"),
                GeminiChatbotService.rolesDeAccion(GeminiChatbotService.Accion.CONSULTAR_PRODUCTO));
        assertEquals(java.util.List.of("VENTAS"),
                GeminiChatbotService.rolesDeAccion(GeminiChatbotService.Accion.VENTAS_CLIENTE));
        assertEquals(java.util.List.of("FINANCIERO"),
                GeminiChatbotService.rolesDeAccion(GeminiChatbotService.Accion.CUENTAS_COBRAR));
        assertEquals(java.util.List.of("FINANCIERO"),
                GeminiChatbotService.rolesDeAccion(GeminiChatbotService.Accion.RENTABILIDAD));
        assertTrue(GeminiChatbotService.rolesDeAccion(GeminiChatbotService.Accion.GENERAL).isEmpty());
    }

    @Test
    void rolNoPermitido_bloqueaHerramientaFinanciera_sinDatos() {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        fake.bloquearRol("FINANCIERO");
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CUENTAS_COBRAR\",\"cliente\":\"Maria\"}");
        String out = fake.ejecutarLocal(c, "¿cuánto me debe Maria?");
        assertTrue(out.contains("roles IA [FINANCIERO]"), out);
        assertFalse(out.contains("120.00"), "no debe filtrar datos financieros");
    }

    @Test
    void repuestos_aceptanRolVentasAunqueFaltaInventario() {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        fake.bloquearRol("INVENTARIO"); // licencia solo-VENTAS
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro aceite\"}");
        String out = fake.ejecutarLocal(c, "stock filtro");
        assertTrue(out.contains("FIL-01"), "VENTAS habilita consultar stock: " + out);
        assertFalse(out.contains("roles IA"), "no debe bloquear: " + out);
    }

    @Test
    void rolNoPermitido_ventas_bloqueaSinRedaccion() throws Exception {
        ConGeminiFake fake = new ConGeminiFake(new HerramientasFalsas());
        fake.bloquearRol("VENTAS");
        // Solo el clasificador en la cola: si hubiese redacción, lanza y fallaría el test
        fake.ofrecer("{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Maria\"}");
        String out = fake.preguntarConGemini("¿cuánto me compró Maria?", "fake-key", null, null, null);
        assertTrue(out.contains("roles IA [VENTAS]"), out);
        assertFalse(out.contains("450.75"), "no debe ejecutar la herramienta de ventas");
    }

    @Test
    void rolPermitido_noBloquea_PorDefecto() {
        GeminiChatbotService s = nuevo();
        assertTrue(s.rolPermitido("VENTAS"));
        assertTrue(s.rolPermitido("FINANCIERO"));
        assertTrue(s.rolPermitido("INVENTARIO"));
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CUENTAS_COBRAR\",\"cliente\":\"Maria\"}");
        assertTrue(s.ejecutarLocal(c, "saldo Maria").contains("120.00"));
    }

    @Test
    void promptRedaccion_adoptaPersonalidadDelRol() {
        GeminiChatbotService s = nuevo();
        GeminiChatbotService.Clasificacion fin = clasif("{\"accion\":\"RENTABILIDAD\"}");
        assertTrue(s.promptRedaccion(fin, "rentabilidad", "x").contains("analista financiero de Vendex (rol FINANCIERO)"));
        GeminiChatbotService.Clasificacion ven = clasif("{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"m\"}");
        assertTrue(s.promptRedaccion(ven, "ventas", "x").contains("asesor comercial de Vendex (rol VENTAS)"));
        GeminiChatbotService.Clasificacion inv = clasif("{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"f\"}");
        assertTrue(s.promptRedaccion(inv, "stock", "x").contains("especialista en inventario de repuestos (rol INVENTARIO)"));
        GeminiChatbotService.Clasificacion gen = clasif("{\"accion\":\"GENERAL\"}");
        assertTrue(s.promptRedaccion(gen, "hola", "").contains("asistente general"));
    }
}

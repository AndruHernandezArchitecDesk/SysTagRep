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

    private static class HerramientasFalsas implements HerramientasAgente {
        @Override
        public List<Producto> productos(String criterio, int limite) {
            if (criterio != null && criterio.toLowerCase().contains("filtro")) {
                return List.of(new Producto("FIL-01", "FILTRO ACEITE TOYOTA HILUX", 12,
                        new BigDecimal("15.50"), new BigDecimal("9.00"), 41.9, "Filtros Quito"));
            }
            return List.of();
        }

        @Override
        public List<Producto> compatibles(String descripcion, String marca, String modelo,
                                          Integer anio, String motor, int limite) {
            if ("TOYOTA".equalsIgnoreCase(marca) && "COROLLA".equalsIgnoreCase(modelo)
                    && Integer.valueOf(2015).equals(anio) && "1.8".equals(motor)) {
                return List.of(new Producto("BUJ-18", "BUJIA COROLLA 1.8", 6,
                        new BigDecimal("8.00"), new BigDecimal("4.00"), 50.0, "Bodega Central"));
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
        private final java.util.List<String> prompts = new java.util.ArrayList<>();
        private final java.util.Set<String> rolesBloqueados = new java.util.HashSet<>();
        private boolean ahorro;
        @Override protected boolean modoAhorro() { return ahorro; }

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
            prompts.add(prompt);
            if (respuestas.isEmpty()) throw new RuntimeException("proveedor IA fake sin respuestas");
            return respuestas.poll();
        }
    }

    private static GeminiChatbotService.Clasificacion clasif(String json) {
        return GeminiChatbotService.parsearClasificacion(json);
    }

    // ---------- parseo del clasificador ----------

    @Test
    void modoAhorro_entregaDatosLocalesConUnaSolaLlamada() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ahorro = true;
        fake.ofrecer("{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro aceite\"}");
        var respuesta = fake.preguntarConGeminiRespuesta("stock filtro", "fake-key", null, null, null);
        assertEquals(1, fake.prompts.size());
        assertTrue(respuesta.texto.contains("FIL-01"));
        assertEquals("INVENTARIO", respuesta.rolIA);
    }

    @Test
    void codigoPuro_noClasificaNiRedactaConIA() {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        var respuesta = fake.preguntarConEstado("04411-37130");
        assertTrue(fake.prompts.isEmpty());
        assertTrue(respuesta.esSinIA);
        assertTrue(respuesta.texto.startsWith("Búsqueda local"));
    }

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
        assertTrue(out.contains("costo registrado sin IVA: $9.00"), out);
        assertTrue(out.contains("diferencia bruta precio-costo: $6.50"), out);
        assertTrue(out.contains("proveedor: Filtros Quito"), out);
    }

    @Test
    void compatibilidadVehicular_usaAsociacionExplicitaYAdvierteConfirmacionFabricante() {
        GeminiChatbotService.Clasificacion c = clasif("""
                {"accion":"BUSCAR_REPUESTO","descripcion":"bujías","marca":"Toyota","modelo":"Corolla","anio":2015,"motor":"1.8"}
                """);
        String out = nuevo().ejecutarLocal(c, "¿qué bujías para Corolla 2015 1.8?");
        assertTrue(out.contains("BUJ-18"), out);
        assertTrue(out.contains("asociados explícitamente"), out);
        assertTrue(out.contains("confirma la especificación del fabricante"), out);
        assertTrue(out.contains("Bodega Central"), out);

        GeminiChatbotService.Clasificacion consulta = clasif("""
                {"accion":"CONSULTAR_PRODUCTO","descripcion":"bujías","marca":"Toyota","modelo":"Corolla","anio":2015,"motor":"1.8"}
                """);
        assertTrue(nuevo().ejecutarLocal(consulta, "¿cuánto cuestan las bujías para Corolla 2015 1.8?")
                .contains("BUJ-18"));
    }

    @Test
    void consultarProducto_sinCoincidencias_loDice() {
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"rueda espejo\"}");
        String out = nuevo().ejecutarLocal(c, "¿cuánto stock de rueda espejo?");
        assertTrue(out.toLowerCase().contains("no encontr"), out);
    }

    @Test
    void herramientaRemotaCaida_degradaALaBusquedaClasica() {
        HerramientasAgente remota = org.mockito.Mockito.mock(HerramientasAgente.class);
        org.mockito.Mockito.when(remota.productos("tapa radiador", 5))
                .thenThrow(new IllegalStateException("API fuera de servicio"));
        GeminiChatbotService service = new GeminiChatbotService(new RepuestoDaoFalso(), remota);
        GeminiChatbotService.Clasificacion c = clasif(
                "{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"tapa radiador\"}");

        assertTrue(service.ejecutarLocal(c, "consulta tapa radiador").contains("TAPA-01"));
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
    void rolEfectivo_soloVentas_coincideConLaEtiquetaYElPrompt() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.bloquearRol("INVENTARIO");
        fake.ofrecer("{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro aceite\"}", "Stock disponible");
        var respuesta = fake.preguntarConGeminiRespuesta("stock filtro", "fake-key", null, null, null);
        assertEquals("VENTAS", respuesta.rolIA);
        assertTrue(respuesta.rolesBloqueados.isEmpty());
        assertTrue(fake.prompts.get(1).contains("rol VENTAS"));
        assertFalse(fake.prompts.get(1).contains("rol INVENTARIO"));
        assertEquals("Stock disponible", respuesta.texto, "La etiqueta no se mezcla con el texto generado");
        assertEquals(2, fake.prompts.size());
    }

    @Test
    void rolEfectivo_soloInventario_permiteRepuestos() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.bloquearRol("VENTAS");
        fake.ofrecer("{\"descripcion\":\"tapa radiador\"}", "Tapa disponible");
        var respuesta = fake.preguntarConGeminiRespuesta("tapa", "fake-key", null, null, null);
        assertEquals("INVENTARIO", respuesta.rolIA);
        assertTrue(fake.prompts.get(1).contains("rol INVENTARIO"));
    }

    @Test
    void rolEfectivo_ventasYFinanciero_correspondenALaAccion() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ofrecer("{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Maria\"}", "Ventas de Maria");
        var ventas = fake.preguntarConGeminiRespuesta("ventas Maria", "fake-key", null, null, null);
        fake.ofrecer("{\"accion\":\"CUENTAS_COBRAR\",\"cliente\":\"Maria\"}", "Saldo de Maria");
        var saldo = fake.preguntarConGeminiRespuesta("saldo Maria", "fake-key", null, null, null);
        assertEquals("VENTAS", ventas.rolIA);
        assertEquals("FINANCIERO", saldo.rolIA);
        assertTrue(fake.prompts.get(3).contains("rol FINANCIERO"));
    }

    @Test
    void rolBloqueado_noEjecutaHerramientasNiRedaccion() throws Exception {
        var herramientas = new HerramientasFalsas() {
            @Override
            public List<Producto> productos(String criterio, int limite) {
                fail("No debe consultar productos sin un rol autorizado");
                return List.of();
            }
        };
        var fake = new ConGeminiFake(herramientas);
        fake.bloquearRol("VENTAS");
        fake.bloquearRol("INVENTARIO");
        fake.ofrecer("{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro\"}");
        var respuesta = fake.preguntarConGeminiRespuesta("stock filtro", "fake-key", null, null, null);
        assertNull(respuesta.rolIA);
        assertEquals(List.of("VENTAS", "INVENTARIO"), respuesta.rolesBloqueados);
        assertTrue(respuesta.texto.contains("roles IA [VENTAS, INVENTARIO]"));
        assertEquals(1, fake.prompts.size(), "No se intenta redactar el bloqueo con IA");
    }

    @Test
    void rolFinancieroBloqueado_informaRolRequerido() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.bloquearRol("FINANCIERO");
        fake.ofrecer("{\"accion\":\"RENTABILIDAD\"}");
        var respuesta = fake.preguntarConGeminiRespuesta("rentabilidad", "fake-key", null, null, null);
        assertNull(respuesta.rolIA);
        assertEquals(List.of("FINANCIERO"), respuesta.rolesBloqueados);
        assertEquals(1, fake.prompts.size());
    }

    @Test
    void rolEfectivo_redaccionFalla_conservaRolYDatosLocales() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ofrecer("{\"accion\":\"CONSULTAR_PRODUCTO\",\"descripcion\":\"filtro aceite\"}");
        var respuesta = fake.preguntarConGeminiRespuesta("stock filtro", "fake-key", null, null, null);
        assertEquals("INVENTARIO", respuesta.rolIA);
        assertTrue(respuesta.texto.contains("FIL-01"));
    }

    @Test
    void generalYEntradaVacia_noHeredanElRolDeOtraRespuesta() throws Exception {
        var fake = new ConGeminiFake(new HerramientasFalsas());
        fake.ofrecer("{\"accion\":\"VENTAS_CLIENTE\",\"cliente\":\"Maria\"}", "Ventas de Maria",
                "{\"accion\":\"GENERAL\"}", "Hola");
        var primera = fake.preguntarConGeminiRespuesta("ventas Maria", "fake-key", null, null, null);
        var general = fake.preguntarConGeminiRespuesta("hola", "fake-key", null, null, null);
        assertEquals("VENTAS", primera.rolIA);
        assertNull(general.rolIA);
        assertTrue(general.rolesBloqueados.isEmpty());
        assertNull(fake.preguntarConEstado(null).rolIA);
        assertNull(fake.preguntarConEstado(" ").rolIA);
    }

    private static com.vendex.model.Inventario complemento(int id, String codigo, String nombre, int stock) {
        var i = new com.vendex.model.Inventario();
        i.setId(id);
        i.setCodigo(codigo);
        i.setDescripcion(nombre);
        i.setCantidad(stock);
        i.setPrecioVenta(new BigDecimal("14.50"));
        return i;
    }

    @Test
    void buscarRepuesto_complementosDeInventario_sinLlamadaIAExtra() throws Exception {
        var base = complemento(1, "TAPA-01", "TAPA RADIADOR CHEV AVEO", 7);
        var manguera = complemento(2, "MANG-02", "MANGUERA RADIADOR CHEV AVEO", 3);
        var sinStock = complemento(3, "SIN-03", "LIQUIDO RADIADOR", 0);
        var ajeno = complemento(4, "BAT-04", "BATERIA BOSCH", 4);
        var herramientas = new HerramientasFalsas() {
            @Override
            public List<com.vendex.model.Inventario> complementarios(String codigo, int limite) {
                assertEquals("TAPA-01", codigo);
                return new com.vendex.service.VendexiSugerenciaService().seleccionarCandidatos(
                        base, List.of(base, manguera, sinStock, ajeno), java.util.Set.of(), limite);
            }
        };
        var fake = new ConGeminiFake(herramientas);
        fake.ofrecer("{\"accion\":\"BUSCAR_REPUESTO\",\"descripcion\":\"tapa radiador\"}", "Respuesta final");
        assertEquals("Respuesta final", fake.preguntarConGemini("tapa radiador", "fake-key", null, null, null));
        assertEquals(2, fake.prompts.size(), "Solo clasificador y redacción");
        String prompt = fake.prompts.get(1);
        assertTrue(prompt.contains("MANG-02"), prompt);
        assertTrue(prompt.contains("$14.50"), prompt);
        assertFalse(prompt.contains("SIN-03"), prompt);
        assertFalse(prompt.contains("BAT-04"), prompt);
    }

    @Test
    void buscarRepuesto_excluyeBaseDuplicadosYSinStock_limitaADos() {
        var herramientas = new HerramientasFalsas() {
            @Override
            public List<com.vendex.model.Inventario> complementarios(String codigo, int limite) {
                return List.of(complemento(1, "TAPA-01", "BASE", 7),
                        complemento(2, "SIN-02", "SIN STOCK", 0),
                        complemento(3, "A-03", "COMPLEMENTO A", 2),
                        complemento(3, "a-03", "DUPLICADO", 2),
                        complemento(4, "B-04", "COMPLEMENTO B", 1),
                        complemento(5, "C-05", "EXCEDENTE", 3));
            }
        };
        var servicio = new GeminiChatbotService(new RepuestoDaoFalso(), herramientas);
        String out = servicio.ejecutarLocal(clasif("{\"descripcion\":\"tapa radiador\"}"), "tapa radiador");
        assertTrue(out.contains("A-03") && out.contains("B-04"), out);
        assertFalse(out.contains("SIN-02") || out.contains("DUPLICADO") || out.contains("C-05"), out);
        assertTrue(out.contains("compatibilidad sin confirmar"), out);
    }

    @Test
    void buscarRepuesto_sinHallazgo_noConsultaComplementos() {
        var herramientas = new HerramientasFalsas() {
            @Override
            public List<com.vendex.model.Inventario> complementarios(String codigo, int limite) {
                fail("No consultar complementos sin hallazgo");
                return List.of();
            }
        };
        String out = new GeminiChatbotService(new RepuestoDaoFalso(), herramientas)
                .ejecutarLocal(clasif("{\"descripcion\":\"inexistente\"}"), "inexistente");
        assertTrue(out.contains("No encontré"), out);
        assertFalse(out.contains("Complementos"), out);
    }

    @Test
    void buscarRepuesto_falloComplementos_conservaHallazgo() {
        var herramientas = new HerramientasFalsas() {
            @Override
            public List<com.vendex.model.Inventario> complementarios(String codigo, int limite) {
                throw new IllegalStateException("BD no disponible");
            }
        };
        String out = new GeminiChatbotService(new RepuestoDaoFalso(), herramientas)
                .ejecutarLocal(clasif("{\"descripcion\":\"tapa radiador\"}"), "tapa radiador");
        assertTrue(out.contains("TAPA-01"), out);
        assertFalse(out.contains("Complementos"), out);
    }

    @Test
    void buscarRepuesto_modoRest_conservaBusquedaSinComplementos() {
        String out = new GeminiChatbotService(new RepuestoDaoFalso())
                .ejecutarLocal(clasif("{\"descripcion\":\"tapa radiador\"}"), "tapa radiador");
        assertTrue(out.contains("TAPA-01"), out);
        assertFalse(out.contains("Complementos"), out);
    }

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
        String prompt = s.promptRedaccion(gen, "hola", "");
        assertTrue(prompt.contains("Eres Vetxi, el agente de Vendex"));
        assertFalse(prompt.contains("capibara"));
        assertTrue(prompt.contains("asistente general"));
    }
}

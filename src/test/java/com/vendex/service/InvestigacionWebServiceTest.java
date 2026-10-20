package com.vendex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 4 Vendexi — investigación web (§8): detección de intención y parseo
 * de grounding (fuentes) sin red.
 */
class InvestigacionWebServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final InvestigacionWebService svc = new InvestigacionWebService();

    // ===== esConsultaInvestigacion =====

    @Test
    void intencion_explicitaInvestigar() {
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("investiga el código 0K937-30Q0A"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("Investigar filtro de aceite"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("busca en internet 1GR-12345"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("búsqueda web bomba agua A3-456"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("google 5W-30"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("consulta web para 04411-37130"));
    }

    @Test
    void codigoPuro_disparaInvestigacion() {
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("0K937-30Q0A"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("5W-30"));
        assertTrue(InvestigacionWebService.esConsultaInvestigacion("04411-37130"));
        // código interno del ERP sin guion: NO debe robar la búsqueda local
        assertFalse(InvestigacionWebService.esConsultaInvestigacion("FILTRO1"));
        assertFalse(InvestigacionWebService.esConsultaInvestigacion("BANDA01"));
    }

    @Test
    void consultaNormal_esLocal_noInvestigacion() {
        assertFalse(InvestigacionWebService.esConsultaInvestigacion("tapa radiador aveo"));
        assertFalse(InvestigacionWebService.esConsultaInvestigacion("busca tapa radiador aveo"));
        assertFalse(InvestigacionWebService.esConsultaInvestigacion("¿cuánto stock hay de filtros?"));
        assertFalse(InvestigacionWebService.esConsultaInvestigacion(null));
        assertFalse(InvestigacionWebService.esConsultaInvestigacion("   "));
    }

    // ===== parseo de grounding =====

    @Test
    void parsearFuentes_extraeUriTitulo_yOmiteDuplicadas() throws Exception {
        String json = """
                {"candidates":[{"content":{"parts":[{"text":"ok"}]},
                  "groundingMetadata":{"groundingChunks":[
                    {"web":{"uri":"https://www.rockauto.com/catalog/x","title":"RockAuto"}},
                    {"web":{"uri":"https://www.rockauto.com/catalog/x","title":"RockAuto dup"}},
                    {"web":{"uri":"https://ac.delco.com/parts","title":"ACDelco"}},
                    {"web":{"uri":"ftp://no-http","title":"invalida"}},
                    {"web":{"title":"sin uri"}}
                  ]}
                }]}""";
        List<InvestigacionWebService.Fuente> fuentes =
                InvestigacionWebService.parsearFuentes(MAPPER.readTree(json));
        assertEquals(2, fuentes.size());
        assertEquals("RockAuto", fuentes.get(0).titulo);
        assertEquals("https://www.rockauto.com/catalog/x", fuentes.get(0).uri);
        assertTrue(fuentes.get(0).tieneUri());
        assertEquals("ACDelco", fuentes.get(1).titulo);
    }

    @Test
    void parsearFuentes_sinGrounding_vacio() throws Exception {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"sin fuentes\"}]}}]}";
        assertTrue(InvestigacionWebService.parsearFuentes(MAPPER.readTree(json)).isEmpty());
        assertTrue(InvestigacionWebService.parsearFuentes(null).isEmpty());
    }

    @Test
    void extraerTexto_concatenaParts() throws Exception {
        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hola \"},{\"text\":\"mundo\"}]}}]}";
        assertEquals("Hola mundo", InvestigacionWebService.extraerTexto(MAPPER.readTree(json)));
        assertEquals("", InvestigacionWebService.extraerTexto(MAPPER.readTree("{}")));
    }

    // ===== prompt =====

    @Test
    void prompt_incluyeReglasS8() {
        String p = svc.construirPrompt("0K937-30Q0A", "0K937-30Q0A | FILTRO | stock 3", "CHEV", "AVEO", 2015);
        assertTrue(p.contains("0K937-30Q0A"));
        assertTrue(p.contains("stock 3"));
        assertTrue(p.contains("CHEV"));
        assertTrue(p.contains("COMPAT_CONFIRMADA"));
        assertTrue(p.contains("No se encontro evidencia suficiente") || p.contains("evidencia suficiente"));
        assertTrue(p.contains("no inventes"));
    }

    @Test
    void investigar_consultaVacia_errorSinRed() {
        var res = svc.investigar("  ", null, null, null, null);
        assertFalse(res.esOk());
        assertEquals(InvestigacionWebService.Estado.ERROR, res.estado);
    }
}

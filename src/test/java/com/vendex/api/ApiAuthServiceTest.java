package com.vendex.api;

import com.vendex.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 6 Vendexi — autenticación de la API: token HMAC, expiración, revocación,
 * extracción Bearer y matriz de permisos (§13.2). Sin red ni BD.
 */
class ApiAuthServiceTest {

    private Usuario usuario(int id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("user" + id);
        u.setSucursalId(2);
        return u;
    }

    @Test
    void token_roundtrip_claimsCorrectos() {
        String tok = ApiAuthService.crearToken(usuario(7), 3, 5);
        ApiAuthService.Claims c = ApiAuthService.verificar(tok);
        assertNotNull(c);
        assertEquals(7, c.userId());
        assertEquals(3, c.sucursalId());
        assertEquals(5, c.puntoEmisionId());
        assertFalse(c.expirado());
        assertFalse(c.jti().isBlank());
    }

    @Test
    void token_nulo_vacio_oBasura_invalido() {
        assertNull(ApiAuthService.verificar(null));
        assertNull(ApiAuthService.verificar(""));
        assertNull(ApiAuthService.verificar("abc"));
        assertNull(ApiAuthService.verificar("abc.def"));
        assertNull(ApiAuthService.verificar("noexiste.hhhh"));
    }

    @Test
    void token_alterado_invalido() {
        String tok = ApiAuthService.crearToken(usuario(7), 3, 5);
        String body = tok.substring(0, tok.lastIndexOf('.'));
        String cuerpoModificado = body.substring(0, body.length() - 2) + "AA";
        assertNull(ApiAuthService.verificar(cuerpoModificado + "." + tok.substring(tok.lastIndexOf('.') + 1)));
        assertNull(ApiAuthService.verificar(tok + "x"));
    }

    @Test
    void token_expirado_invalido() {
        String tok = ApiAuthService.crearToken(usuario(1), 1, 1, System.currentTimeMillis() - 1000);
        assertNull(ApiAuthService.verificar(tok));
    }

    @Test
    void logout_revocaToken() {
        String tok = ApiAuthService.crearToken(usuario(4), 1, 1);
        assertNotNull(ApiAuthService.verificar(tok));
        ApiAuthService.revocar(tok);
        assertNull(ApiAuthService.verificar(tok));
        // revocación es por jti: otro token del mismo usuario sigue válido
        String otro = ApiAuthService.crearToken(usuario(4), 1, 1);
        assertNotNull(ApiAuthService.verificar(otro));
    }

    @Test
    void extraerBearer_variaciones() {
        assertEquals("XYZ", ApiAuthService.extraerBearer("Bearer XYZ"));
        assertEquals("XYZ", ApiAuthService.extraerBearer("bearer XYZ"));
        assertEquals("XYZ", ApiAuthService.extraerBearer("  Bearer   XYZ  "));
        assertNull(ApiAuthService.extraerBearer("Basic XYZ"));
        assertNull(ApiAuthService.extraerBearer(null));
    }

    // ===== paths públicos y matriz de permisos =====

    @Test
    void pathsPublicos_loginYHealth() {
        assertTrue(ApiAuthService.esPublico("GET", "/api/health"));
        assertTrue(ApiAuthService.esPublico("POST", "/api/auth/login"));
        assertTrue(ApiAuthService.esPublico("OPTIONS", "/api/inventario"));
        assertFalse(ApiAuthService.esPublico("GET", "/api/inventario"));
        assertFalse(ApiAuthService.esPublico("POST", "/api/facturas"));
        assertFalse(ApiAuthService.esPublico("GET", "/api/auth/me"));
    }

    @Test
    void permisos_lecturaAutenticada_escrituraPorModulo() {
        // lecturas: solo autenticación
        assertNull(ApiAuthService.permisoRequerido("GET", "/api/facturas"));
        assertNull(ApiAuthService.permisoRequerido("GET", "/api/inventario"));
        // escrituras por módulo
        assertEquals("FACTURA_EMITIR", ApiAuthService.permisoRequerido("POST", "/api/facturas"));
        assertEquals("FACTURA_EMITIR", ApiAuthService.permisoRequerido("PUT", "/api/facturas/estado"));
        assertEquals("NOTA_CREDITO_EMITIR", ApiAuthService.permisoRequerido("POST", "/api/nota-credito"));
        assertEquals("GUIA_REMISION_EMITIR", ApiAuthService.permisoRequerido("POST", "/api/guia-remision"));
        assertEquals("RETENCION_EMITIR", ApiAuthService.permisoRequerido("POST", "/api/retencion"));
        assertEquals("INVENTARIO_AJUSTAR", ApiAuthService.permisoRequerido("POST", "/api/inventario/1/stock/descontar"));
        assertEquals("INVENTARIO_AJUSTAR", ApiAuthService.permisoRequerido("POST", "/api/transferencias"));
        assertEquals("CAJA_ABRIR", ApiAuthService.permisoRequerido("POST", "/api/caja/abrir"));
        assertEquals("USUARIO_GESTIONAR", ApiAuthService.permisoRequerido("POST", "/api/empresa"));
        assertEquals("USUARIO_GESTIONAR", ApiAuthService.permisoRequerido("POST", "/api/sucursales"));
        // clientes/comprobantes: requieren sesión, permiso dentro del servicio
        assertNull(ApiAuthService.permisoRequerido("POST", "/api/clientes"));
        assertNull(ApiAuthService.permisoRequerido("POST", "/api/comprobantes"));
    }

    @Test
    void claims_resumenNoExponeFirma() {
        String tok = ApiAuthService.crearToken(usuario(9), 1, 1);
        ApiAuthService.Claims c = ApiAuthService.verificar(tok);
        Map<String, Object> resumen = ApiAuthService.resumenClaims(c);
        assertEquals(9, resumen.get("userId"));
        assertFalse(resumen.containsKey("jti"));
        assertFalse(resumen.values().stream().anyMatch(v -> String.valueOf(v).contains(".")));
    }
}

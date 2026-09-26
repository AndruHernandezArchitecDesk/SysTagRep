package com.vendex.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppContextTest {

    @Test
    void appContext_resuelveDependenciasSinExcepcion() {
        AppContext.reset();
        AppContext ctx = AppContext.getInstance();
        assertNotNull(ctx);
        assertNotNull(ctx.facturaService);
        assertNotNull(ctx.notaCreditoService);
        assertNotNull(ctx.notaDebitoService);
        assertNotNull(ctx.guiaRemisionService);
        assertNotNull(ctx.retencionService);
        assertNotNull(ctx.facturaRegistroDAO);
        assertNotNull(ctx.inventarioDAO);
        assertNotNull(ctx.clienteDAO);
        assertNotNull(ctx.empresaDAO);
        // Verificar que segunda llamada devuelve misma instancia (singleton)
        AppContext ctx2 = AppContext.getInstance();
        assertSame(ctx, ctx2);
        AppContext.reset();
    }

    @Test
    void vendexControllerFactory_fallbackNoLanzaParaControllersNoMigrados() {
        AppContext ctx = AppContext.getInstance();
        VendexControllerFactory factory = new VendexControllerFactory(ctx);
        // Controller no migrado debe resolverse via reflexión fallback
        Object ctrl = factory.call(com.vendex.controller.LoginController.class);
        assertNotNull(ctrl);
        assertTrue(ctrl instanceof com.vendex.controller.LoginController);
        AppContext.reset();
    }
}

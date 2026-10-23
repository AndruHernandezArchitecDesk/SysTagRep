package com.vendex.controller.api;

import com.vendex.chatbot.HerramientasAgente;
import com.vendex.api.ApiAuthException;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HerramientasAgenteApiControllerTest {
    @AfterEach void limpiarSesion() { SesionActual.cerrar(); }

    @Test void cadaPeticionConstruyeHerramientasConLaSucursalAutenticada() {
        Usuario usuario = new Usuario(); usuario.setId(3); usuario.setSucursalId(2);
        SesionActual.iniciar(usuario, 8, null);
        AtomicInteger sucursal = new AtomicInteger();
        HerramientasAgente tools = mock(HerramientasAgente.class);
        when(tools.productos("aceite", 5)).thenReturn(List.of());
        var controller = new HerramientasAgenteApiController(sid -> { sucursal.set(sid); return tools; });
        Context ctx = mock(Context.class);
        when(ctx.queryParam("criterio")).thenReturn("aceite");
        when(ctx.queryParam("limite")).thenReturn(null);

        controller.productos(ctx);

        assertEquals(8, sucursal.get());
        verify(tools).productos("aceite", 5);
        verify(ctx).json(List.of());
    }

    @Test void endpointsExigenSesionAutenticada() {
        var controller = new HerramientasAgenteApiController(sid -> mock(HerramientasAgente.class));
        Context ctx = mock(Context.class);
        assertThrows(ApiAuthException.class, () -> controller.productos(ctx));
    }
}

package com.vendex.controller.api;

import com.vendex.api.ApiAuthException;
import com.vendex.model.VetxiHistorial.*;
import com.vendex.service.VetxiHistorialService;
import io.javalin.http.Context;
import java.util.Map;

/** El ámbito viene de la sesión autenticada; el enviado por el cliente solo detecta cambios de sesión. */
public class VetxiHistorialApiController {
    public record Solicitud(Ambito ambito, Mensaje mensaje) {}
    private final VetxiHistorialService servicio;
    public VetxiHistorialApiController(VetxiHistorialService servicio) { this.servicio = servicio; }

    private Ambito autenticar(Ambito esperado) {
        Ambito actual = servicio.ambitoActual();
        if (actual == null) throw new ApiAuthException(401, "Sesión requerida para el historial de Vetxi");
        if (esperado != null && !actual.equals(esperado)) throw new ApiAuthException(403, "La sesión o sucursal del historial cambió");
        return actual;
    }

    private Ambito lectura(Context ctx) {
        Ambito actual = autenticar(null);
        int usuario = ctx.queryParam("usuarioId") == null ? actual.usuarioId() : Integer.parseInt(ctx.queryParam("usuarioId"));
        int sucursal = ctx.queryParam("sucursalId") == null ? actual.sucursalId() : Integer.parseInt(ctx.queryParam("sucursalId"));
        return autenticar(new Ambito(usuario, sucursal));
    }

    private int limite(Context ctx, int defecto) {
        return ctx.queryParam("limite") == null ? defecto : Integer.parseInt(ctx.queryParam("limite"));
    }
    public void conversaciones(Context ctx) throws Exception { ctx.json(servicio.conversaciones(lectura(ctx), limite(ctx, 20))); }
    public void mensajes(Context ctx) throws Exception { ctx.json(servicio.mensajes(lectura(ctx), ctx.pathParam("id"), limite(ctx, 100))); }
    public void guardar(Context ctx) throws Exception {
        Solicitud solicitud = ctx.bodyAsClass(Solicitud.class);
        if (solicitud.ambito() == null || solicitud.mensaje() == null) throw new IllegalArgumentException("Ámbito y mensaje requeridos");
        servicio.guardar(autenticar(solicitud.ambito()), solicitud.mensaje());
        ctx.json(Map.of("guardado", true));
    }
}

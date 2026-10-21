package com.vendex.controller.api;

import com.vendex.service.LicenciaCentralService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Fase 6b: administración central de licencias (emisión/revocación/validación). */
public class LicenciaCentralApiController {

    private final LicenciaCentralService servicio;

    public LicenciaCentralApiController(LicenciaCentralService servicio) {
        this.servicio = servicio;
    }

    public void emitir(Context ctx) {
        Map<String, Object> b = ctx.bodyAsClass(Map.class);
        String machineCode = str(b.get("machineCode"));
        String modalidad = str(b.get("modalidad"));
        boolean incluyeIA = bool(b.get("incluyeIA"), true);
        @SuppressWarnings("unchecked")
        List<String> roles = b.get("roles") instanceof List ? (List<String>) b.get("roles") : null;
        long capacidad = b.get("capacidad") instanceof Number ? ((Number) b.get("capacidad")).longValue() : 0L;
        LocalDate expiracion = null;
        Object exp = b.get("expiracion");
        if (exp != null && !String.valueOf(exp).isBlank())
            expiracion = LocalDate.parse(String.valueOf(exp));
        Integer emitidaPor = SesionActualHolder.usuarioId();
        Map<String, Object> r = servicio.emitir(machineCode, modalidad, incluyeIA, roles, capacidad, expiracion, emitidaPor);
        ctx.status(201).json(r);
    }

    public void validar(Context ctx) {
        String machineCode = ctx.queryParam("machineCode");
        String clave = ctx.queryParam("clave");
        if ((machineCode == null || machineCode.isBlank()) && ctx.body() != null && !ctx.body().isBlank()) {
            Map<String, Object> b = ctx.bodyAsClass(Map.class);
            if (machineCode == null || machineCode.isBlank()) machineCode = str(b.get("machineCode"));
            if (clave == null || clave.isBlank()) clave = str(b.get("clave"));
        }
        ctx.json(servicio.validarCentral(machineCode, clave));
    }

    public void revocar(Context ctx) {
        Map<String, Object> b = ctx.bodyAsClass(Map.class);
        servicio.revocar(str(b.get("machineCode")), str(b.get("motivo")), SesionActualHolder.usuarioId());
        ctx.json(Map.of("revocado", true, "machineCode", str(b.get("machineCode"))));
    }

    public void listarEmitidas(Context ctx) {
        int limite = 50;
        try { if (ctx.queryParam("limite") != null) limite = Integer.parseInt(ctx.queryParam("limite")); } catch (NumberFormatException ignore) {}
        ctx.json(servicio.listarEmitidas(limite));
    }

    public void listarRevocadas(Context ctx) {
        int limite = 50;
        try { if (ctx.queryParam("limite") != null) limite = Integer.parseInt(ctx.queryParam("limite")); } catch (NumberFormatException ignore) {}
        ctx.json(servicio.listarRevocadas(limite));
    }

    /** Evita dependencia circular con SesionActual en tests (solo getters). */
    static final class SesionActualHolder {
        static Integer usuarioId() {
            com.vendex.model.Usuario u = com.vendex.util.SesionActual.getUsuario();
            return u != null ? u.getId() : null;
        }
    }

    private static String str(Object o) { return o != null ? String.valueOf(o) : null; }

    private static boolean bool(Object o, boolean def) {
        if (o == null) return def;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(String.valueOf(o));
    }
}

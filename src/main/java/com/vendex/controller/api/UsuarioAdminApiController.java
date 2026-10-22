package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.model.Usuario;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fase 7: administración por API de permisos granulares y sucursales de un usuario. */
public class UsuarioAdminApiController {

    public void obtenerPermisos(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Usuario u = existe(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("usuarioId", u.getId());
        out.put("username", u.getUsername());
        out.put("rolId", u.getRolId());
        out.put("rol", u.getRol());
        out.put("sucursalPrincipalId", u.getSucursalId());
        out.put("granulares", AppContext.getInstance().usuarioPermisoDAO.listarPorUsuario(id));
        ctx.json(out);
    }

    public void guardarPermisos(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        existe(id);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        Object raw = body.get("permisos");
        Map<String, Boolean> permisos = new LinkedHashMap<>();
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                if (e.getKey() == null) continue;
                boolean otorgar = e.getValue() instanceof Boolean ? (Boolean) e.getValue()
                        : Boolean.parseBoolean(String.valueOf(e.getValue()));
                permisos.put(String.valueOf(e.getKey()), otorgar);
            }
        }
        // Validar que los códigos existan en el catálogo
        List<String> conocidos = AppContext.getInstance().permisoDAO.listarTodos().stream()
                .map(p -> p.getCodigo()).toList();
        for (String cod : permisos.keySet()) {
            if (!conocidos.contains(cod.trim().toUpperCase(java.util.Locale.ROOT)))
                throw new com.vendex.api.ApiAuthException(400, "Permiso desconocido: " + cod);
        }
        AppContext.getInstance().usuarioPermisoDAO.reemplazar(id, permisos);
        auditar(id, "permisos granulares: " + permisos);
        ctx.json(Map.of("usuarioId", id, "granulares",
                AppContext.getInstance().usuarioPermisoDAO.listarPorUsuario(id)));
    }

    public void obtenerSucursales(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        existe(id);
        ctx.json(Map.of(
                "usuarioId", id,
                "sucursales", AppContext.getInstance().usuarioSucursalDAO.listarSucursales(id)
        ));
    }

    public void guardarSucursales(Context ctx) {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        existe(id);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        List<Integer> sucursales = body.get("sucursales") instanceof List
                ? ((List<?>) body.get("sucursales")).stream()
                    .map(v -> Integer.parseInt(String.valueOf(v))).toList()
                : List.of();
        // Validar sucursales existentes y activas
        for (Integer s : sucursales) {
            var suc = AppContext.getInstance().sucursalDAO.obtenerPorId(s);
            if (suc.isEmpty() || !suc.get().isActivo())
                throw new com.vendex.api.ApiAuthException(400, "Sucursal inexistente o inactiva: " + s);
        }
        AppContext.getInstance().usuarioSucursalDAO.reemplazar(id, sucursales);
        auditar(id, "sucursales permitidas: " + sucursales);
        ctx.json(Map.of("usuarioId", id, "sucursales",
                AppContext.getInstance().usuarioSucursalDAO.listarSucursales(id)));
    }

    private Usuario existe(int id) {
        Usuario u = AppContext.getInstance().usuarioDAO.obtenerPorId(id);
        if (u == null) throw new com.vendex.api.ApiAuthException(404, "Usuario inexistente: " + id);
        return u;
    }

    private void auditar(int usuarioId, String detalle) {
        try {
            Integer yo = com.vendex.util.SesionActual.getUsuario() != null
                    ? com.vendex.util.SesionActual.getUsuario().getId() : null;
            new com.vendex.dao.AuditoriaAccionDAOPostgres().registrar(
                    yo != null ? yo : usuarioId, "USUARIO_GESTIONAR", "PERMITIDO",
                    "API actualizó " + detalle + " del usuario " + usuarioId);
        } catch (Exception ignored) {}
    }
}

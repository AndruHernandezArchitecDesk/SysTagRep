package com.vendex.controller.api;

import com.vendex.dao.InventarioDAO;
import com.vendex.service.TransferenciaInventarioService;
import io.javalin.http.Context;

import java.util.Map;

public class TransferenciaApiController {

    private final TransferenciaInventarioService service;
    private final com.vendex.dao.TransferenciaInventarioDAO transferenciaDAO;

    public TransferenciaApiController(TransferenciaInventarioService service, com.vendex.dao.TransferenciaInventarioDAO transferenciaDAO) {
        this.service = service;
        this.transferenciaDAO = transferenciaDAO;
    }

    public static class TransferenciaRequest {
        public int inventarioIdOrigen;
        public int destinoSucursalId;
        public int cantidad;
        public String motivo;
    }

    public void transferir(Context ctx) {
        TransferenciaRequest req = ctx.bodyAsClass(TransferenciaRequest.class);
        if (req.inventarioIdOrigen <= 0 || req.destinoSucursalId <= 0 || req.cantidad <= 0) {
            ctx.status(400).json(Map.of("error", "inventarioIdOrigen, destinoSucursalId y cantidad >0 son obligatorios"));
            return;
        }
        // API usa usuario sistema con permiso (sin login UI)
        var prevUser = com.vendex.util.SesionActual.getUsuario();
        var prevPerms = com.vendex.util.SesionActual.getPermisos();
        try {
            var sysUser = new com.vendex.model.Usuario();
            sysUser.setId(1);
            sysUser.setRol("ADMINISTRADOR");
            com.vendex.util.SesionActual.setUsuarioForTest(sysUser);
            com.vendex.util.SesionActual.setPermisosForTest(java.util.Set.of("INVENTARIO_AJUSTAR"));
            var trans = service.transferir(req.inventarioIdOrigen, req.destinoSucursalId, req.cantidad, req.motivo);
            ctx.status(201).json(trans);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("permiso")) {
                ctx.status(403).json(Map.of("error", e.getMessage()));
            } else {
                ctx.status(500).json(Map.of("error", e.getMessage()));
            }
        } finally {
            if (prevUser != null) com.vendex.util.SesionActual.setUsuarioForTest(prevUser);
            else com.vendex.util.SesionActual.cerrar();
            try { com.vendex.util.SesionActual.setPermisosForTest(prevPerms); } catch (Exception ignore) {}
        }
    }

    public void listar(Context ctx) {
        String inventarioIdStr = ctx.queryParam("inventarioId");
        String sucursalIdStr = ctx.queryParam("sucursalId");
        if (inventarioIdStr != null) {
            int invId = Integer.parseInt(inventarioIdStr);
            ctx.json(transferenciaDAO.listarPorInventario(invId));
            return;
        }
        if (sucursalIdStr != null) {
            int sid = Integer.parseInt(sucursalIdStr);
            ctx.json(transferenciaDAO.listarPorSucursal(sid, 50));
            return;
        }
        ctx.json(transferenciaDAO.listarTodas(50));
    }
}

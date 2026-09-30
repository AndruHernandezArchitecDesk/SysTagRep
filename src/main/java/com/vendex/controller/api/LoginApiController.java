package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.UsuarioDAO;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;

import java.util.Map;

public class LoginApiController {

    private final UsuarioDAO usuarioDAO;

    public LoginApiController() {
        this(AppContext.getInstance());
    }

    public LoginApiController(AppContext ctx) {
        this.usuarioDAO = ctx.usuarioDAO;
    }

    public void login(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String username = body.get("username");
            String password = body.get("password");
            if (username == null || password == null) {
                ctx.status(400).json(Map.of("error", "username y password son obligatorios"));
                return;
            }
            Usuario u = usuarioDAO.autenticar(username, password);
            if (u == null) {
                ctx.status(401).json(Map.of("error", "Credenciales inválidas"));
                return;
            }
            SesionActual.iniciar(u);
            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("id", u.getId());
            resp.put("username", u.getUsername());
            resp.put("nombre", u.getNombre() + " " + (u.getApellido() != null ? u.getApellido() : ""));
            resp.put("rol", u.getRol());
            resp.put("rolId", u.getRolId());
            resp.put("sucursalId", SesionActual.getSucursalId());
            resp.put("puntoEmisionId", SesionActual.getPuntoEmisionId());
            resp.put("permisos", SesionActual.getPermisos());
            ctx.json(resp);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void logout(Context ctx) {
        SesionActual.cerrar();
        ctx.json(Map.of("ok", true));
    }

    public void me(Context ctx) {
        Usuario u = SesionActual.getUsuario();
        if (u == null) ctx.status(401).json(Map.of("error", "No autenticado"));
        else ctx.json(Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "nombre", u.getNombre() + " " + (u.getApellido() != null ? u.getApellido() : ""),
                "rol", u.getRol(),
                "sucursalId", SesionActual.getSucursalId(),
                "puntoEmisionId", SesionActual.getPuntoEmisionId()
        ));
    }
}

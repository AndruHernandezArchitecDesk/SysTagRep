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
            // Anti fuerza bruta (Fase 6): bloqueo por intentos
            Usuario previo = usuarioDAO.buscarPorUsername(username);
            if (previo != null && previo.getBloqueadoHasta() != null
                    && previo.getBloqueadoHasta().isAfter(java.time.LocalDateTime.now())) {
                ctx.status(423).json(Map.of("error",
                        "Cuenta bloqueada hasta " + previo.getBloqueadoHasta().withNano(0)));
                return;
            }
            Usuario u = usuarioDAO.autenticar(username, password);
            if (u == null) {
                if (previo != null) {
                    usuarioDAO.incrementarIntentoFallido(previo.getId());
                    int intentos = previo.getIntentosFallidos() + 1;
                    if (intentos >= 5) {
                        usuarioDAO.bloquear(previo.getId(), java.time.LocalDateTime.now().plusMinutes(15));
                    }
                }
                ctx.status(401).json(Map.of("error", "Credenciales inválidas"));
                return;
            }
            usuarioDAO.resetearIntentos(u.getId());
            // Sesión por-hilo (aislamiento API) — no altera la sesión global de la UI local
            SesionActual.iniciarApi(u, u.getSucursalId(), null);
            String token = com.vendex.api.ApiAuthService.crearToken(u, u.getSucursalId(), null);
            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("token", token);
            resp.put("tokenType", "Bearer");
            resp.put("expiraEnMinutos", Long.getLong("api.token.ttl.minutos", 720L));
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
        String token = com.vendex.api.ApiAuthService.extraerBearer(ctx.header("Authorization"));
        if (token != null) com.vendex.api.ApiAuthService.revocar(token);
        SesionActual.cerrarApi();
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

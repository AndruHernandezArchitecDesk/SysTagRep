package com.vendex.util;

import com.vendex.dao.AuditoriaAccionDAO;
import com.vendex.dao.PermisoDAO;
import com.vendex.exception.SinPermisoException;
import com.vendex.model.Usuario;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import com.vendex.dao.PermisoDAOPostgres;
import com.vendex.dao.AuditoriaAccionDAOPostgres;

/**
 * Holder de sesión. Carga permisos una sola vez al login desde rol_permiso.
 * Valida en servicio/DAO, no solo en UI.
 */
public final class SesionActual {

    private static Usuario usuario;
    private static Set<String> permisos = Collections.emptySet();
    private static BigDecimal limiteDescuento;

    private SesionActual() {}

    public static void iniciar(Usuario u) {
        usuario = u;
        if (u == null) {
            permisos = Collections.emptySet();
            limiteDescuento = null;
            return;
        }
        // resolver rol_id: si tiene rol_id usarlo, si no fallback a rol string
        int rolId = u.getRolId();
        if (rolId == 0 && u.getRol() != null) {
            // fallback compat: buscar rol por nombre
            try {
                var rol = new com.vendex.dao.RolDAOPostgres().obtenerPorNombre(u.getRol());
                if (rol != null) {
                    rolId = rol.getId();
                    // cache limite del rol
                    limiteDescuento = rol.getLimiteDescuentoPct();
                }
            } catch (Exception ignored) {}
        } else if (rolId != 0) {
            try {
                var rol = new com.vendex.dao.RolDAOPostgres().obtenerPorId(rolId);
                if (rol != null) limiteDescuento = rol.getLimiteDescuentoPct();
            } catch (Exception ignored) {}
        }
        // override por usuario
        if (u.getLimiteDescuentoPct() != null) limiteDescuento = u.getLimiteDescuentoPct();

        if (rolId != 0) {
            permisos = new HashSet<>(new PermisoDAOPostgres().listarPorRol(rolId));
        } else {
            permisos = Collections.emptySet();
        }
        // compat CSV legacy: si tiene permisos CSV antiguos, agregarlos como permisos también
        if (u.getPermisos() != null && !u.getPermisos().isBlank()) {
            for (String p : u.getPermisos().split(",")) {
                if (!p.isBlank()) permisos.add(p.trim().toUpperCase());
            }
        }
    }

    public static void cerrar() {
        usuario = null;
        permisos = Collections.emptySet();
        limiteDescuento = null;
    }

    public static Usuario getUsuario() { return usuario; }
    public static Set<String> getPermisos() { return Collections.unmodifiableSet(permisos); }
    public static BigDecimal getLimiteDescuento() { return limiteDescuento; }

    public static boolean tienePermiso(String codigo) {
        if (usuario == null) return false;
        // ADMINISTRADOR siempre tiene todo si tiene USUARIO_GESTIONAR o si rol es ADMINISTRADOR
        if (permisos.contains(codigo)) return true;
        // fallback legacy: si es ADMINISTRADOR permitir todo (compat migración)
        if (usuario.getRol() != null && "ADMINISTRADOR".equals(usuario.getRol()) && !permisos.isEmpty()) {
            // si es admin y permisos están vacíos (pre-migración), permitir
            // pero si ya hay permisos granulares, exigir explícito
            return false;
        }
        if ("ADMINISTRADOR".equals(usuario.getRol()) && permisos.isEmpty()) return true;
        return false;
    }

    public static void exigirPermiso(String codigo) {
        if (!tienePermiso(codigo)) {
            // auditoría DENEGADO
            try {
                if (usuario != null) new AuditoriaAccionDAOPostgres().registrar(usuario.getId(), codigo, "DENEGADO", "Intento sin permiso");
            } catch (Exception ignored) {}
            throw new SinPermisoException(codigo);
        }
        // auditoría PERMITIDO solo para sensibles
        if (esSensible(codigo)) {
            try {
                if (usuario != null) new AuditoriaAccionDAOPostgres().registrar(usuario.getId(), codigo, "PERMITIDO", null);
            } catch (Exception ignored) {}
        }
    }

    public static void exigirPermisoConDetalle(String codigo, String detalle) {
        if (!tienePermiso(codigo)) {
            try { if (usuario != null) new AuditoriaAccionDAOPostgres().registrar(usuario.getId(), codigo, "DENEGADO", detalle); } catch (Exception ignored) {}
            throw new SinPermisoException(codigo, detalle);
        }
        if (esSensible(codigo)) {
            try { if (usuario != null) new AuditoriaAccionDAOPostgres().registrar(usuario.getId(), codigo, "PERMITIDO", detalle); } catch (Exception ignored) {}
        }
    }

    public static void exigirDescuento(BigDecimal pctSolicitado) {
        if (pctSolicitado == null || pctSolicitado.compareTo(BigDecimal.ZERO) <= 0) return;
        exigirPermiso("DESCUENTO_APLICAR");
        BigDecimal limite = getLimiteDescuento();
        if (limite == null) return; // sin tope (admin)
        if (pctSolicitado.compareTo(limite) > 0) {
            try { if (usuario != null) new AuditoriaAccionDAOPostgres().registrar(usuario.getId(), "DESCUENTO_APLICAR", "DENEGADO", "Tope " + limite + "% solicitado " + pctSolicitado + "%"); } catch (Exception ignored) {}
            throw new SinPermisoException("DESCUENTO_APLICAR", "Tope excedido: máximo " + limite + "% solicitado " + pctSolicitado + "%");
        }
    }

    private static boolean esSensible(String codigo) {
        return "FACTURA_ANULAR".equals(codigo) || "USUARIO_GESTIONAR".equals(codigo) || codigo.startsWith("CONFIGURACION");
    }

    // para tests
    public static void setPermisosForTest(Set<String> perms) { permisos = new HashSet<>(perms); }
    public static void setUsuarioForTest(Usuario u) { usuario = u; }
}
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
 *
 * Fase 6: soporta DOS niveles para aislamiento (spec §13.2/§13.3):
 * - Sesión global: login de la UI JavaFX (hilo FX), comportamiento legacy.
 * - Sesión por-hilo ({@link #iniciarApi}): cada petición HTTP de la API REST
 *   ejecuta en su propio hilo y su sesión no contamina a otros usuarios
 *   concurrentes ni a la UI local.
 */
public final class SesionActual {

    private static final class Ses {
        Usuario usuario;
        Set<String> permisos = Collections.emptySet();
        BigDecimal limiteDescuento;
        Integer sucursalId;
        Integer puntoEmisionId;
    }

    private static final Ses GLOBAL = new Ses();
    private static final ThreadLocal<Ses> TLS = new ThreadLocal<>();

    private SesionActual() {}

    /** Sesión activa de ESTE hilo: la de la API si existe, si no la global (UI). */
    private static Ses s() {
        Ses v = TLS.get();
        return v != null ? v : GLOBAL;
    }

    public static void iniciar(Usuario u) {
        iniciar(u, u != null ? u.getSucursalId() : null, null);
    }

    public static void iniciar(Usuario u, Integer sucursalId, Integer puntoEmisionId) {
        TLS.remove(); // la UI trabaja sobre la sesión global
        cargar(GLOBAL, u, sucursalId, puntoEmisionId);
    }

    /**
     * Fase 6: sesión de petición API (aislamiento por hilo).
     * Cada hilo de Javalin llama esto tras validar el token Bearer.
     */
    public static void iniciarApi(Usuario u, Integer sucursalId, Integer puntoEmisionId) {
        Ses ses = TLS.get();
        if (ses == null) {
            ses = new Ses();
            TLS.set(ses);
        }
        cargar(ses, u, sucursalId, puntoEmisionId);
    }

    /** Limpia la sesión del hilo API (after de cada petición). */
    public static void cerrarApi() {
        TLS.remove();
    }

    private static void cargar(Ses ses, Usuario u, Integer sucursalId, Integer puntoEmisionId) {
        ses.usuario = u;
        ses.sucursalId = sucursalId;
        ses.puntoEmisionId = puntoEmisionId;
        ses.permisos = Collections.emptySet();
        ses.limiteDescuento = null;
        if (u == null) return;
        int rolId = u.getRolId();
        if (rolId == 0 && u.getRol() != null) {
            try {
                var rol = new com.vendex.dao.RolDAOPostgres().obtenerPorNombre(u.getRol());
                if (rol != null) {
                    rolId = rol.getId();
                    ses.limiteDescuento = rol.getLimiteDescuentoPct();
                }
            } catch (Exception ignored) {}
        } else if (rolId != 0) {
            try {
                var rol = new com.vendex.dao.RolDAOPostgres().obtenerPorId(rolId);
                if (rol != null) ses.limiteDescuento = rol.getLimiteDescuentoPct();
            } catch (Exception ignored) {}
        }
        if (u.getLimiteDescuentoPct() != null) ses.limiteDescuento = u.getLimiteDescuentoPct();
        if (rolId != 0) {
            ses.permisos = new HashSet<>(new PermisoDAOPostgres().listarPorRol(rolId));
        } else {
            ses.permisos = new HashSet<>();
        }
        if (u.getPermisos() != null && !u.getPermisos().isBlank()) {
            for (String p : u.getPermisos().split(",")) {
                if (!p.isBlank()) ses.permisos.add(p.trim().toUpperCase());
            }
        }
        // Fase 7: permisos granulares por usuario — amplían (otorgar) o restringen (denegar) los del rol
        try {
            java.util.Map<String, Boolean> extras =
                    new com.vendex.dao.UsuarioPermisoDAOPostgres().listarPorUsuario(u.getId());
            aplicarPermisosDeUsuario(ses.permisos, extras);
        } catch (Exception ignored) {}
    }

    /** Fase 7: aplica los permisos granulares por usuario sobre los permisos efectivos. */
    static void aplicarPermisosDeUsuario(Set<String> permisos, java.util.Map<String, Boolean> extras) {
        if (extras == null || extras.isEmpty()) return;
        for (java.util.Map.Entry<String, Boolean> e : extras.entrySet()) {
            String cod = e.getKey() != null ? e.getKey().trim().toUpperCase(java.util.Locale.ROOT) : "";
            if (cod.isEmpty()) continue;
            if (Boolean.TRUE.equals(e.getValue())) permisos.add(cod);
            else permisos.remove(cod);
        }
    }

    public static void cerrar() {
        if (TLS.get() != null) {
            TLS.remove(); // logout desde un hilo API no debe tocar la sesión global
        } else {
            GLOBAL.usuario = null;
            GLOBAL.permisos = Collections.emptySet();
            GLOBAL.limiteDescuento = null;
            GLOBAL.sucursalId = null;
            GLOBAL.puntoEmisionId = null;
        }
    }

    public static Usuario getUsuario() { return s().usuario; }
    public static Set<String> getPermisos() { return Collections.unmodifiableSet(s().permisos); }
    public static BigDecimal getLimiteDescuento() { return s().limiteDescuento; }
    public static Integer getSucursalId() { Integer v = s().sucursalId; return v != null ? v : 1; }
    public static Integer getPuntoEmisionId() { Integer v = s().puntoEmisionId; return v != null ? v : 1; }

    public static boolean tienePermiso(String codigo) {
        Ses ses = s();
        if (ses.usuario == null) return false;
        // ADMINISTRADOR siempre tiene todo si tiene USUARIO_GESTIONAR o si rol es ADMINISTRADOR
        if (ses.permisos.contains(codigo)) return true;
        // fallback legacy: si es ADMINISTRADOR permitir todo (compat migración)
        if (ses.usuario.getRol() != null && "ADMINISTRADOR".equals(ses.usuario.getRol()) && !ses.permisos.isEmpty()) {
            // si es admin y permisos están vacíos (pre-migración), permitir
            // pero si ya hay permisos granulares, exigir explícito
            return false;
        }
        if ("ADMINISTRADOR".equals(ses.usuario.getRol()) && ses.permisos.isEmpty()) return true;
        return false;
    }

    public static void exigirPermiso(String codigo) {
        if (!tienePermiso(codigo)) {
            // auditoría DENEGADO
            try {
                if (s().usuario != null) new AuditoriaAccionDAOPostgres().registrar(s().usuario.getId(), codigo, "DENEGADO", "Intento sin permiso");
            } catch (Exception ignored) {}
            throw new SinPermisoException(codigo);
        }
        // auditoría PERMITIDO solo para sensibles
        if (esSensible(codigo)) {
            try {
                if (s().usuario != null) new AuditoriaAccionDAOPostgres().registrar(s().usuario.getId(), codigo, "PERMITIDO", null);
            } catch (Exception ignored) {}
        }
    }

    public static void exigirPermisoConDetalle(String codigo, String detalle) {
        if (!tienePermiso(codigo)) {
            try { if (s().usuario != null) new AuditoriaAccionDAOPostgres().registrar(s().usuario.getId(), codigo, "DENEGADO", detalle); } catch (Exception ignored) {}
            throw new SinPermisoException(codigo, detalle);
        }
        if (esSensible(codigo)) {
            try { if (s().usuario != null) new AuditoriaAccionDAOPostgres().registrar(s().usuario.getId(), codigo, "PERMITIDO", detalle); } catch (Exception ignored) {}
        }
    }

    /**
     * Descuento en VALOR FIJO ($). El tope del rol sigue expresado en %:
     * se convierte el monto a % equivalente sobre el total bruto.
     */
    public static void exigirDescuento(BigDecimal valorFijo, BigDecimal totalBruto) {
        if (valorFijo == null || valorFijo.compareTo(BigDecimal.ZERO) <= 0) return;
        exigirPermiso("DESCUENTO_APLICAR");
        BigDecimal limite = getLimiteDescuento();
        if (limite == null) return; // sin tope (admin)
        BigDecimal pctSolicitado = BigDecimal.ZERO;
        if (totalBruto != null && totalBruto.compareTo(BigDecimal.ZERO) > 0) {
            pctSolicitado = valorFijo.multiply(new BigDecimal("100"))
                    .divide(totalBruto, 2, java.math.RoundingMode.HALF_UP);
        }
        if (pctSolicitado.compareTo(limite) > 0) {
            try { if (s().usuario != null) new AuditoriaAccionDAOPostgres().registrar(s().usuario.getId(), "DESCUENTO_APLICAR", "DENEGADO", "Tope " + limite + "% solicitado " + pctSolicitado + "% ($" + valorFijo + ")"); } catch (Exception ignored) {}
            throw new SinPermisoException("DESCUENTO_APLICAR", "Tope excedido: máximo " + limite + "% solicitado " + pctSolicitado + "%");
        }
    }

    private static boolean esSensible(String codigo) {
        return "FACTURA_ANULAR".equals(codigo) || "USUARIO_GESTIONAR".equals(codigo) || codigo.startsWith("CONFIGURACION");
    }

    // para tests
    public static void setPermisosForTest(Set<String> perms) { s().permisos = new HashSet<>(perms); }
    public static void setUsuarioForTest(Usuario u) { s().usuario = u; }
    public static void setSucursalIdForTest(Integer s) { s().sucursalId = s; }
    public static void setPuntoEmisionIdForTest(Integer p) { s().puntoEmisionId = p; }

    /** para tests de aislamiento API: ¿este hilo tiene sesión propia? */
    public static boolean tieneSesionApiActiva() { return TLS.get() != null; }
}

package com.vendex.api;

import com.vendex.model.Usuario;
import com.vendex.util.SecureConfigStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fase 6 Vendexi — autenticación de la API REST (spec §13.2).
 *
 * - Token firmado (HMAC-SHA256) con expiración y `jti` revocable en logout.
 * - Secreto: env/sysprop {@code VENDEX_API_HMAC_KEY} > store local
 *   {@code api.hmac.key} > semilla aleatoria persistida (nunca en el binario).
 * - Stateless + revocación en memoria: no guarda sesiones en la BD.
 * - NUNCA loguea tokens ni secretos.
 */
public final class ApiAuthService {

    private static final Logger LOG = Logger.getLogger(ApiAuthService.class.getName());
    private static final String PROP = "VENDEX_API_HMAC_KEY";
    private static final long TTL_MS = Long.getLong("api.token.ttl.minutos", 720L) * 60_000L; // 12 h
    private static final Set<String> REVOCADOS = ConcurrentHashMap.newKeySet();
    private static final int MAX_REVOCADOS = 10_000;

    private ApiAuthService() {}

    public record Claims(int userId, Integer sucursalId, Integer puntoEmisionId, long expMs, String jti) {
        public boolean expirado() { return System.currentTimeMillis() > expMs; }
    }

    private static volatile String secretCache;

    private static String secreto() {
        String s = secretCache;
        if (s != null) return s;
        s = System.getProperty(PROP, System.getenv(PROP) != null ? System.getenv(PROP) : "");
        if (s != null && !s.isBlank()) { secretCache = s.trim(); return secretCache; }
        try {
            String local = SecureConfigStore.obtenerSecretoLocal("api.hmac.key");
            if (local != null && !local.isBlank()) { secretCache = local; return local; }
        } catch (Exception ignored) {}
        try {
            byte[] rnd = java.security.SecureRandom.getInstanceStrong().generateSeed(32);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : rnd) sb.append(String.format("%02x", b));
            SecureConfigStore.guardarSecretoLocal("api.hmac.key", sb.toString());
            secretCache = sb.toString();
            LOG.info("Secreto HMAC de API generado y guardado en store local");
            return secretCache;
        } catch (Exception e) {
            // último recurso determinista (solo si el store falla): debe alertar en logs
            LOG.log(Level.WARNING, "No se pudo persistir secreto de API; usando semilla temporal", e);
            secretCache = "vendex-api-temp-" + java.util.UUID.nameUUIDFromBytes("vendex".getBytes());
            return secretCache;
        }
    }

    private static String hmacHex(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Emite un token para el usuario autenticado. */
    public static String crearToken(Usuario u, Integer sucursalId, Integer puntoEmisionId) {
        return crearToken(u, sucursalId, puntoEmisionId, System.currentTimeMillis() + TTL_MS);
    }

    /** Emite un token con expiración absoluta (para tests y casos especiales). */
    static String crearToken(Usuario u, Integer sucursalId, Integer puntoEmisionId, long expMs) {
        try {
            String jti = UUID.randomUUID().toString();
            String payload = "{\"u\":" + u.getId()
                    + ",\"suc\":" + (sucursalId != null ? sucursalId : u.getSucursalId())
                    + ",\"pem\":" + (puntoEmisionId != null ? puntoEmisionId : 1)
                    + ",\"exp\":" + expMs
                    + ",\"jti\":\"" + jti + "\"}";
            String body = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            return body + "." + hmacHex(body);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo emitir token de API", e);
        }
    }

    /** Valida token; devuelve claims o null si es inválido/expirado/revocado. */
    public static Claims verificar(String token) {
        try {
            if (token == null || token.isBlank()) return null;
            String t = token.trim();
            int dot = t.lastIndexOf('.');
            if (dot <= 0) return null;
            String body = t.substring(0, dot);
            String firma = t.substring(dot + 1);
            String esperada = hmacHex(body);
            if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
                    firma.toLowerCase().getBytes(StandardCharsets.UTF_8))) return null;
            String json = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
            Claims c = parsear(json);
            if (c == null || c.expirado()) return null;
            if (REVOCADOS.contains(c.jti())) return null;
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    static Claims parsear(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode n = m.readTree(json);
            return new Claims(
                    n.path("u").asInt(0),
                    n.path("suc").asInt(1),
                    n.path("pem").asInt(1),
                    n.path("exp").asLong(0),
                    n.path("jti").asText(""));
        } catch (Exception e) {
            return null;
        }
    }

    /** Revoca el jti del token (logout). Idempotente. */
    public static void revocar(String token) {
        Claims c = verificarSinRevocacion(token);
        if (c != null) {
            if (REVOCADOS.size() >= MAX_REVOCADOS) REVOCADOS.clear();
            REVOCADOS.add(c.jti());
        }
    }

    private static Claims verificarSinRevocacion(String token) {
        try {
            if (token == null || token.isBlank()) return null;
            String t = token.trim();
            int dot = t.lastIndexOf('.');
            if (dot <= 0) return null;
            String body = t.substring(0, dot);
            if (!hmacHex(body).equalsIgnoreCase(t.substring(dot + 1))) return null;
            return parsear(new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    /** Extrae el token del header Authorization: Bearer xxx. */
    public static String extraerBearer(String header) {
        if (header == null) return null;
        String h = header.trim();
        if (h.regionMatches(true, 0, "Bearer ", 0, 7)) return h.substring(7).trim();
        return null;
    }

    /** true si el path es público (sin token). */
    public static boolean esPublico(String method, String path) {
        if (path == null) return false;
        if ("GET".equalsIgnoreCase(method) && path.equals("/api/health")) return true;
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/auth/login")) return true;
        // Opciones de CORS (preflight) sin token; la respuesta filtrará CORS si está deshabilitado
        return "OPTIONS".equalsIgnoreCase(method);
    }

    /**
     * Permiso requerido para una petición de escritura (spec §13.2 autorización).
     * GET → solo autenticar (null). Escrituras → permiso del módulo ERP.
     */
    public static String permisoRequerido(String method, String path) {
        if (path == null) return null;
        String p = path.toLowerCase();
        // Fase 6b: reglas de administración que aplican también a GET
        if (p.startsWith("/api/licencias/validar")) return null; // clientes CENTRAL autenticados validan su clave
        if (p.startsWith("/api/licencias") || p.startsWith("/api/respaldos")
                || p.startsWith("/api/usuarios")) return "USUARIO_GESTIONAR"; // también GET (lecturas sensibles)
        if (p.equals("/api/ia/cuotas/panel") || p.equals("/api/ia/cuotas/config")) return "USUARIO_GESTIONAR";
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) return null;
        if (p.startsWith("/api/facturas")) return "FACTURA_EMITIR";
        if (p.startsWith("/api/nota-venta")) return "FACTURA_EMITIR";
        if (p.startsWith("/api/nota-credito")) return "NOTA_CREDITO_EMITIR";
        if (p.startsWith("/api/nota-debito")) return "NOTA_DEBITO_EMITIR";
        if (p.startsWith("/api/guia-remision")) return "GUIA_REMISION_EMITIR";
        if (p.startsWith("/api/retencion")) return "RETENCION_EMITIR";
        if (p.startsWith("/api/inventario") || p.startsWith("/api/transferencias")) return "INVENTARIO_AJUSTAR";
        if (p.startsWith("/api/caja")) return "CAJA_ABRIR";
        if (p.equals("/api/ia/consumo/renovar") || p.equals("/api/ia/consumo/reciente")) return "USUARIO_GESTIONAR";
        if (p.startsWith("/api/empresa") || p.startsWith("/api/sucursales")) return "USUARIO_GESTIONAR";
        // clientes, vehículos, comprobantes, crédito, historial, logs, secuencias:
        // requieren sesión válida (permisos granulares se validan dentro del servicio)
        return null;
    }

    /** Mapa de claims a debug sin exponer firma (para /api/auth/me). */
    public static Map<String, Object> resumenClaims(Claims c) {
        return Map.of("userId", c.userId(), "sucursalId", c.sucursalId(),
                "puntoEmisionId", c.puntoEmisionId(), "expiraEn", c.expMs());
    }
}

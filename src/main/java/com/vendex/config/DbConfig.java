package com.vendex.config;

import com.vendex.util.SecureConfigStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persiste la configuracion de conexion a PostgreSQL en ~/.vendex/db.properties
 * para soportar despliegue multi-PC con BD compartida.
 *
 * <p><b>Instalador producción (sin BD):</b> el MSI no instala ni crea PostgreSQL.
 * La BD ya existe en el servidor. Cada cliente solo configura Host/IP + puerto + BD
 * + usuario + clave existentes y prueba la conexión antes de guardar.</p>
 *
 * PC host:  db.url=jdbc:postgresql://localhost:5432/dbVendex
 * PC cliente: db.url=jdbc:postgresql://192.168.1.7:5432/dbVendex (IP directa del servidor
 *   en la misma LAN) o jdbc:postgresql://vendex-db:5432/dbVendex (hostname via hosts/DNS).
 *
 * <p>Mínimo privilegio (2026-09-20): rol recomendado {@code app_vendex} con grants explícitos
 * por tabla/sequence (ver {@code sql/migracion_minimo_privilegio_20260920.sql} y {@code docs/permisos_bd.md}).
 * Ejecutar migración como {@code postgres} y luego configurar wizard con {@code app_vendex}.
 * DEFAULT_USER sigue siendo {@code postgres} por compatibilidad con instalaciones legacy.</p>
 *
 * <p>Seguridad: db.password cifrado AES/GCM keyring (HKDF master key SO) via {@link SecureConfigStore}.
 * Formato: sal:iv:cifrado. Migración lazy claro/legacy→keyring. Prioridad env DB_PASSWORD &gt; -Ddb.password &gt; archivo &gt; default.</p>
 *
 * <p>Si el archivo no existe NO se autocrea con password por defecto. El wizard de primer arranque
 * ({@link com.vendex.MainApp}) se encarga de generarlo/solicitarlo (por defecto {@code postgres}, cambiar a {@code app_vendex} tras migración).</p>
 *
 * <p>SPOF mitigado (§0 LINEAMIENTO_SPOF_SERVIDOR): en vez de IP fija por cliente, usar hostname vendex-db
 * resoluble via C:\Windows\System32\drivers\etc\hosts (distribuido). Cambiar IP del host = editar 1 línea hosts por PC, no N db.properties.</p>
 */
public final class DbConfig {

    private static final File DIR = new File(System.getProperty("user.home"), ".vendex");
    private static final File ARCHIVO = new File(DIR, "db.properties");

    public static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/dbVendex";
    public static final String DEFAULT_HOST = "192.168.1.7";
    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_DB = "dbVendex";
    /** Usuario de producción con mínimo privilegio (clientes nuevos). */
    public static final String DEFAULT_PROD_USER = "app_vendex";
    /** Usuario por defecto para instalaciones legacy (compatibilidad). Para mínimo privilegio ejecutar sql/migracion_minimo_privilegio_20260920.sql y luego configurar wizard con app_vendex. */
    public static final String DEFAULT_USER = "postgres";
    /** Rol con mínimo privilegio (GRANT explícito por tabla). Requiere migración previa como postgres. */
    public static final String APP_VENDEX_USER = "app_vendex";
    /** Fallback superuser solo para instalaciones legacy sin migrar; rotar a app_vendex cuanto antes. */
    public static final String LEGACY_DEFAULT_USER = "postgres";
    public static final String DEFAULT_PASSWORD = "admin";

    private DbConfig() {}

    public static File getArchivo() { return ARCHIVO; }

    public static File getDir() { return DIR; }

    /**
     * Carga url/user/password. Password se descifra si está en formato cifrado.
     * No autocrea archivo con defaults; si no existe retorna defaults solo en memoria.
     */
    public static synchronized String[] cargar() {
        Properties p = new Properties();
        if (ARCHIVO.exists()) {
            try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                p.load(fis);
            } catch (IOException ignored) {}
            // migración lazy: claro→cifrado y legacy→keyring (Fase 1 completa), recargar si migró
            boolean migro = SecureConfigStore.migrarTodoSiEsNecesario(ARCHIVO, "db.password");
            if (migro) {
                p.clear();
                try (FileInputStream fis = new FileInputStream(ARCHIVO)) {
                    p.load(fis);
                } catch (IOException ignored) {}
            }
        } else {
            // no autocrear con defaults; retornar defaults solo en memoria para que el wizard lo genere
            String envPass = resolverPasswordEnv();
            if (envPass != null) return new String[]{DEFAULT_URL, DEFAULT_USER, envPass};
            return new String[]{DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD};
        }
        String url = p.getProperty("db.url", DEFAULT_URL);
        String user = p.getProperty("db.user", DEFAULT_USER);
        String rawPass = p.getProperty("db.password", null);
        url = url == null ? DEFAULT_URL : url.trim();
        user = user == null ? DEFAULT_USER : user.trim();
        if (url.isEmpty()) url = DEFAULT_URL;
        if (user.isEmpty()) user = DEFAULT_USER;

        String pass;
        if (rawPass == null) {
            String envPass = resolverPasswordEnv();
            pass = envPass != null ? envPass : DEFAULT_PASSWORD;
        } else {
            rawPass = rawPass.trim();
            if (rawPass.isEmpty()) {
                String envPass = resolverPasswordEnv();
                pass = envPass != null ? envPass : "";
            } else if (SecureConfigStore.esCifrado(rawPass)) {
                String desc = SecureConfigStore.descifrar(rawPass);
                // si descifrado falla, SecureConfigStore retorna raw; detectar y fallback
                pass = desc.isEmpty() && !rawPass.isEmpty() ? rawPass : desc;
                // env override tiene prioridad sobre archivo
                String envPass = resolverPasswordEnv();
                if (envPass != null) pass = envPass;
            } else {
                // legacy en claro
                pass = rawPass;
                String envPass = resolverPasswordEnv();
                if (envPass != null) pass = envPass;
            }
        }
        return new String[]{url, user, pass};
    }

    private static String resolverPasswordEnv() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty("db.password");
        if (prop != null && !prop.isBlank()) return prop;
        return null;
    }

    public static synchronized void guardar(String url, String user, String password) {
        try {
            if (!DIR.exists() && !DIR.mkdirs()) return;
            Properties p = new Properties();
            if (ARCHIVO.exists()) {
                try (FileInputStream fis = new FileInputStream(ARCHIVO)) { p.load(fis); } catch (IOException ignored) {}
            }
            p.setProperty("db.url", url == null || url.isBlank() ? DEFAULT_URL : url.trim());
            p.setProperty("db.user", user == null || user.isBlank() ? DEFAULT_USER : user.trim());
            // password cifrado via SecureConfigStore; si vacío, eliminar key
            if (password == null || password.isBlank()) {
                p.remove("db.password");
                try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                    p.store(fos, "Vendex - Conexion PostgreSQL. Editar db.url para BD remota. Ej: jdbc:postgresql://vendex-db:5432/dbVendex (vendex-db via hosts, ver docs/hosts_setup.md)");
                }
            } else {
                // guardar url/user directos y password cifrado (SecureConfigStore maneja cifrado)
                // para no duplicar store, escribir manualmente con cifrado
                String cifrado = SecureConfigStore.cifrar(password);
                p.setProperty("db.password", cifrado);
                try (FileOutputStream fos = new FileOutputStream(ARCHIVO)) {
                    p.store(fos, "Vendex - Conexion PostgreSQL. Editar db.url para BD remota. Ej: jdbc:postgresql://vendex-db:5432/dbVendex (vendex-db via hosts) - db.password cifrado AES/GCM");
                }
            }
        } catch (IOException ignored) {}
    }

    /**
     * @return true si ya existe configuracion para BD remota (url no es localhost)
     */
    public static boolean esRemota() {
        String[] c = cargar();
        return !c[0].contains("localhost") && !c[0].contains("127.0.0.1");
    }

    /** Arma jdbc:postgresql://host:puerto/bd a partir de campos separados (wizard producción). */
    public static String construirUrl(String host, int puerto, String baseDatos) {
        String h = host == null || host.isBlank() ? DEFAULT_HOST : host.trim();
        int p = (puerto <= 0 || puerto > 65535) ? DEFAULT_PORT : puerto;
        String b = baseDatos == null || baseDatos.isBlank() ? DEFAULT_DB : baseDatos.trim();
        // quitar prefijo jdbc si el usuario lo pegó por error en el campo host
        if (h.startsWith("jdbc:")) {
            try {
                String[] partes = parsear(h);
                return construirUrl(partes[0], Integer.parseInt(partes[1]), partes[2]);
            } catch (Exception ignored) { /* cae al armado simple */ }
        }
        // quitar "/" inicial de la bd si viene con slash
        while (b.startsWith("/")) b = b.substring(1);
        // cortar query params accidentales
        int q = b.indexOf('?');
        if (q >= 0) b = b.substring(0, q);
        if (b.isEmpty()) b = DEFAULT_DB;
        return "jdbc:postgresql://" + h + ":" + p + "/" + b;
    }

    /**
     * Parsea una JDBC URL existente a [host, puerto, baseDatos].
     * Acepta jdbc:postgresql://host:port/bd[?params]. Si no parsea, retorna defaults.
     */
    public static String[] parsear(String jdbcUrl) {
        String host = DEFAULT_HOST;
        String puerto = String.valueOf(DEFAULT_PORT);
        String base = DEFAULT_DB;
        if (jdbcUrl != null) {
            try {
                String s = jdbcUrl.trim();
                String pref = "jdbc:postgresql://";
                if (s.startsWith(pref)) {
                    String resto = s.substring(pref.length());
                    int slash = resto.indexOf('/');
                    String hostPort = slash >= 0 ? resto.substring(0, slash) : resto;
                    String bd = slash >= 0 ? resto.substring(slash + 1) : "";
                    int q = bd.indexOf('?');
                    if (q >= 0) bd = bd.substring(0, q);
                    if (!bd.isBlank()) base = bd.trim();
                    int dos = hostPort.lastIndexOf(':');
                    if (dos >= 0) {
                        String h = hostPort.substring(0, dos).trim();
                        String po = hostPort.substring(dos + 1).trim();
                        if (!h.isEmpty()) host = h;
                        try {
                            int pi = Integer.parseInt(po);
                            if (pi > 0 && pi <= 65535) puerto = String.valueOf(pi);
                        } catch (NumberFormatException ignored) {}
                    } else if (!hostPort.isBlank()) {
                        host = hostPort.trim();
                    }
                }
            } catch (Exception ignored) {}
        }
        return new String[]{host, puerto, base};
    }
}

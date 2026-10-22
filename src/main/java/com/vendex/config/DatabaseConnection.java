package com.vendex.config;

import com.vendex.util.PasswordDebilValidator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final String DEFAULT_URL = DbConfig.DEFAULT_URL;
    private static final String DEFAULT_USER = DbConfig.DEFAULT_USER;
    private static final String DEFAULT_PASSWORD = DbConfig.DEFAULT_PASSWORD;

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    private static final ThreadLocal<String> URL = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();

    // Config global por JVM: los hilos de fondo (login-task, sri-reintento, sync)
    // nacen con ThreadLocal vacio; sin esto caian al DEFAULT localhost aunque el
    // wizard hubiera guardado una URL remota. El ThreadLocal queda solo como
    // override por-hilo (tests aislados); si es null se usa lo global.
    private static volatile String globalUrl = DEFAULT_URL;
    private static volatile String globalUser = DEFAULT_USER;
    private static volatile String globalPass = DEFAULT_PASSWORD;

    private static volatile boolean configLoaded = false;

    // HikariCP pool — singleton por JVM (una instancia de app por PC)
    private static volatile HikariDataSource dataSource;
    private static final Object POOL_LOCK = new Object();
    private static volatile String poolUrl;
    private static volatile String poolUser;

    static {
        aplicarParams(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    /** Fija config global + hilo actual. */
    private static void aplicarParams(String url, String user, String password) {
        globalUrl = url;
        globalUser = user;
        globalPass = password;
        URL.set(url);
        USER.set(user);
        PASSWORD.set(password);
    }

    /** Resuelve param efectivo: override del hilo si existe, si no lo global. */
    private static String efectivo(ThreadLocal<String> hilo, String global, String defecto) {
        String v = hilo.get();
        if (v != null) return v;
        if (global != null) return global;
        return defecto;
    }

    /**
     * Carga ~/.vendex/db.properties una vez al inicio. Llamado desde MainApp.start().
     * Descifra db.password en memoria via DbConfig/SecureConfigStore. Valida password débil
     * con warning+log (no bloqueante).
     */
    public static synchronized void initFromConfig() {
        if (configLoaded) return;
        try {
            String[] cfg = DbConfig.cargar();
            aplicarParams(cfg[0], cfg[1], cfg[2]);
            configLoaded = true;
            validarPasswordDebil(cfg[2]);
            // Inicializar pool si es PostgreSQL
            ensurePool(cfg[0], cfg[1], cfg[2]);
        } catch (Exception e) {
            // fallback a defaults si el archivo esta corrupto — no loguear password ni connection string
            LOG.log(Level.WARNING, "No se pudo cargar db.properties, usando defaults en memoria", e);
            aplicarParams(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
            configLoaded = true;
            validarPasswordDebil(DEFAULT_PASSWORD);
            try { ensurePool(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD); } catch (Exception ex) {
                LOG.log(Level.WARNING, "No se pudo inicializar pool con defaults", ex);
            }
        }
    }

    private static void validarPasswordDebil(String password) {
        if (PasswordDebilValidator.esDebil(password)) {
            LOG.warning("Contraseña de BD débil detectada (valor por defecto o predecible). "
                    + "Se recomienda rotarla cuanto antes en todas las PCs de la instalación. "
                    + "No se bloquea el arranque por compatibilidad con instalaciones existentes.");
        }
    }

    /** Solo warning+log, no bloquea. Para uso desde UI si se quiere mostrar dialog. */
    public static boolean esPasswordDebilActual() {
        String p = efectivo(PASSWORD, globalPass, DEFAULT_PASSWORD);
        return PasswordDebilValidator.esDebil(p);
    }

    private static boolean isHsqldbUrl(String url) {
        return url != null && url.toLowerCase().contains("hsqldb");
    }

    private static boolean isTestContainerUrl(String url) {
        return url != null && url.contains("tc:");
    }

    private static void ensurePool(String url, String user, String password) {
        if (url == null || isHsqldbUrl(url) || isTestContainerUrl(url)) {
            // HSQLDB y tc: no usar pool, se maneja via DriverManager directo
            return;
        }
        synchronized (POOL_LOCK) {
            if (dataSource != null && !dataSource.isClosed()
                    && url.equals(poolUrl) && user.equals(poolUser)) {
                return;
            }
            // URL o usuario cambió — cerrar pool anterior
            closePoolLocked();
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(6);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(300_000);
            // leakDetection 60s en prod, 30s en test/staging via -Dvendex.leakThreshold
            long leakThreshold = 60_000;
            String prop = System.getProperty("vendex.leakThreshold");
            if (prop != null) {
                try { leakThreshold = Long.parseLong(prop); } catch (NumberFormatException ignore) {}
            } else if (isTestProfile()) {
                leakThreshold = 30_000;
            }
            config.setLeakDetectionThreshold(leakThreshold);
            config.setPoolName("vendex-pool");
            config.setAutoCommit(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            // Validación de conexión
            config.setConnectionTestQuery("SELECT 1");
            dataSource = new HikariDataSource(config);
            poolUrl = url;
            poolUser = user;
            LOG.info("HikariCP pool inicializado: url=" + url + " maxPool=6 minIdle=2 leakThreshold=" + leakThreshold + "ms");
            // shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { closePool(); } catch (Exception ignore) {}
            }));
        }
    }

    private static boolean isTestProfile() {
        String v = System.getProperty("vendex.env");
        return "test".equalsIgnoreCase(v);
    }

    private static void closePoolLocked() {
        if (dataSource != null) {
            try {
                dataSource.close();
                LOG.info("HikariCP pool cerrado");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error cerrando HikariCP pool", e);
            } finally {
                dataSource = null;
                poolUrl = null;
                poolUser = null;
            }
        }
    }

    public static void closePool() {
        synchronized (POOL_LOCK) {
            closePoolLocked();
        }
    }

    /** Para tests y monitoreo */
    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        // lazy init por si alguien llama antes de MainApp (ej. TestConexion)
        if (!configLoaded) {
            synchronized (DatabaseConnection.class) {
                if (!configLoaded) initFromConfig();
            }
        }
        String url = efectivo(URL, globalUrl, DEFAULT_URL);
        String user = efectivo(USER, globalUser, DEFAULT_USER);
        String pass = efectivo(PASSWORD, globalPass, DEFAULT_PASSWORD);

        // HSQLDB / tc: bypass pool, DriverManager directo (tests aislados)
        if (isHsqldbUrl(url)) {
            return DriverManager.getConnection(url, user, pass);
        }
        if (isTestContainerUrl(url)) {
            return DriverManager.getConnection(url, user, pass);
        }

        // PostgreSQL: usar pool
        ensurePool(url, user, pass);
        HikariDataSource ds = dataSource;
        if (ds != null && !ds.isClosed()) {
            return ds.getConnection();
        }
        // fallback
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * DDL de migración: requiere rol con CREATE (postgres/vendex_migrator), no app_vendex.
     * Con mínimo privilegio, app_vendex tiene REVOKE CREATE — este método loguea WARNING si
     * falla por permission denied e ignora IF NOT EXISTS en ejecuciones normales.
     * Ver sql/migracion_minimo_privilegio_20260920.sql y docs/permisos_bd.md
     */
    private static void logIfPermissionDenied(SQLException e, String contexto) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("permission denied") || msg.contains("must be owner") || msg.contains("not allowed")) {
            LOG.log(Level.WARNING, contexto + " requiere rol migrador (postgres) — app_vendex sin CREATE: " + e.getMessage());
        }
    }

    public static void ensureNotaCreditoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS nota_credito_registro ("+
            "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, factura_registro_id INTEGER NOT NULL REFERENCES factura_registro(id),"+
            "establecimiento VARCHAR(3) NOT NULL DEFAULT '001', punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL,"+
            "fecha_emision TIMESTAMP NOT NULL DEFAULT now(), cliente_id INTEGER NOT NULL REFERENCES cliente(id), motivo VARCHAR(300) NOT NULL,"+
            "tipo_motivo VARCHAR(20) NOT NULL CHECK (tipo_motivo IN ('DEVOLUCION','DESCUENTO','ANULACION')),"+
            "total_sin_impuestos NUMERIC(12,2) NOT NULL, valor_iva NUMERIC(12,2) NOT NULL, valor_modificacion NUMERIC(12,2) NOT NULL,"+
            "reingresa_stock BOOLEAN NOT NULL DEFAULT false, estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', mensaje_sri TEXT,"+
            "numero_autorizacion VARCHAR(49), fecha_autorizacion TIMESTAMP, xml_firmado TEXT, usuario_id INTEGER NOT NULL REFERENCES usuarios(id), creado_en TIMESTAMP NOT NULL DEFAULT now())",
            "CREATE TABLE IF NOT EXISTS nota_credito_detalle ("+
            "id SERIAL PRIMARY KEY, nota_credito_id INTEGER NOT NULL REFERENCES nota_credito_registro(id) ON DELETE CASCADE,"+
            "factura_detalle_id INTEGER REFERENCES factura_detalle(id), inventario_id INTEGER REFERENCES inventario(id),"+
            "descripcion VARCHAR(300) NOT NULL, cantidad NUMERIC(10,2) NOT NULL, precio_unitario NUMERIC(12,4) NOT NULL,"+
            "descuento NUMERIC(12,2) NOT NULL DEFAULT 0, codigo_porcentaje_iva VARCHAR(2) NOT NULL DEFAULT '4', precio_total_sin_impuesto NUMERIC(12,2) NOT NULL)",
            "INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('NOTA_CREDITO','001','001','001',1) ON CONFLICT (tipo) DO NOTHING",
            "ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS estado_nc VARCHAR(20) NOT NULL DEFAULT 'NINGUNA'",
            "CREATE INDEX IF NOT EXISTS idx_nota_credito_factura ON nota_credito_registro(factura_registro_id)",
            "CREATE INDEX IF NOT EXISTS idx_nota_credito_estado ON nota_credito_registro(estado_sri)",
             "CREATE INDEX IF NOT EXISTS idx_nota_credito_detalle_nc ON nota_credito_detalle(nota_credito_id)",
             "ALTER TABLE comprobantes_electronicos ALTER COLUMN numero_comprobante TYPE VARCHAR(30)",
             "ALTER TABLE comprobantes_electronicos DROP CONSTRAINT IF EXISTS comprobantes_electronicos_nota_venta_id_fkey",
             "ALTER TABLE comprobantes_electronicos RENAME COLUMN nota_venta_id TO documento_relacionado_id",
             "CREATE TABLE IF NOT EXISTS nota_debito_registro ("+
             "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, factura_registro_id INTEGER NOT NULL REFERENCES factura_registro(id),"+
             "establecimiento VARCHAR(3) NOT NULL DEFAULT '001', punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL,"+
             "fecha_emision TIMESTAMP NOT NULL DEFAULT now(), cliente_id INTEGER NOT NULL REFERENCES cliente(id), forma_pago VARCHAR(2) NOT NULL,"+
             "total_sin_impuestos NUMERIC(12,2) NOT NULL, valor_iva NUMERIC(12,2) NOT NULL DEFAULT 0, valor_total NUMERIC(12,2) NOT NULL,"+
             "estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', mensaje_sri TEXT, numero_autorizacion VARCHAR(49),"+
             "fecha_autorizacion TIMESTAMP, xml_firmado TEXT, usuario_id INTEGER NOT NULL REFERENCES usuarios(id), creado_en TIMESTAMP NOT NULL DEFAULT now())",
             "CREATE TABLE IF NOT EXISTS nota_debito_motivo ("+
             "id SERIAL PRIMARY KEY, nota_debito_id INTEGER NOT NULL REFERENCES nota_debito_registro(id) ON DELETE CASCADE,"+
             "razon VARCHAR(300) NOT NULL, valor NUMERIC(12,2) NOT NULL, grava_iva BOOLEAN NOT NULL DEFAULT true,"+
             "codigo_porcentaje_iva VARCHAR(2) NOT NULL DEFAULT '4')",
             "INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('NOTA_DEBITO','001','001','001',1) ON CONFLICT (tipo) DO NOTHING",
             "ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS estado_nd VARCHAR(20) NOT NULL DEFAULT 'NINGUNA'",
              "CREATE INDEX IF NOT EXISTS idx_nota_debito_factura ON nota_debito_registro(factura_registro_id)",
              "CREATE INDEX IF NOT EXISTS idx_nota_debito_estado ON nota_debito_registro(estado_sri)",
              "CREATE INDEX IF NOT EXISTS idx_nota_debito_motivo ON nota_debito_motivo(nota_debito_id)"
          };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureNotaCreditoSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureNotaCreditoSchema conexión"); }
    }

    public static void ensureGuiaRemisionSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS guia_remision_registro ("+
            "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, establecimiento VARCHAR(3) NOT NULL DEFAULT '001',"+
            "punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL, fecha_emision TIMESTAMP NOT NULL DEFAULT now(),"+
            "dir_partida VARCHAR(300) NOT NULL, razon_social_transportista VARCHAR(300) NOT NULL, tipo_identificacion_transportista VARCHAR(2) NOT NULL,"+
            "ruc_transportista VARCHAR(13) NOT NULL, placa VARCHAR(10) NOT NULL, fecha_ini_transporte DATE NOT NULL, fecha_fin_transporte DATE NOT NULL,"+
            "estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', mensaje_sri TEXT, numero_autorizacion VARCHAR(49),"+
            "fecha_autorizacion TIMESTAMP, xml_firmado TEXT, usuario_id INTEGER NOT NULL REFERENCES usuarios(id), creado_en TIMESTAMP NOT NULL DEFAULT now())",
            "CREATE TABLE IF NOT EXISTS guia_remision_destinatario ("+
            "id SERIAL PRIMARY KEY, guia_remision_id INTEGER NOT NULL REFERENCES guia_remision_registro(id) ON DELETE CASCADE,"+
            "identificacion_destinatario VARCHAR(20) NOT NULL, razon_social_destinatario VARCHAR(300) NOT NULL,"+
            "direccion_destinatario VARCHAR(300) NOT NULL, motivo_traslado VARCHAR(300) NOT NULL,"+
            "factura_registro_id INTEGER REFERENCES factura_registro(id), cod_doc_sustento VARCHAR(2),"+
            "num_doc_sustento VARCHAR(17), num_aut_doc_sustento VARCHAR(49), fecha_emision_doc_sustento DATE)",
            "CREATE TABLE IF NOT EXISTS guia_remision_detalle ("+
            "id SERIAL PRIMARY KEY, guia_remision_destinatario_id INTEGER NOT NULL REFERENCES guia_remision_destinatario(id) ON DELETE CASCADE,"+
            "inventario_id INTEGER REFERENCES inventario(id), codigo_interno VARCHAR(25) NOT NULL, descripcion VARCHAR(300) NOT NULL, cantidad NUMERIC(10,2) NOT NULL)",
            "INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('GUIA_REMISION','001','001','001',1) ON CONFLICT (tipo) DO NOTHING",
            "ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS estado_gr VARCHAR(20) NOT NULL DEFAULT 'NINGUNA'",
            "CREATE INDEX IF NOT EXISTS idx_guia_remision_clave ON guia_remision_registro(clave_acceso)",
            "CREATE INDEX IF NOT EXISTS idx_guia_remision_estado ON guia_remision_registro(estado_sri)",
            "CREATE INDEX IF NOT EXISTS idx_guia_destinatario_guia ON guia_remision_destinatario(guia_remision_id)",
            "CREATE INDEX IF NOT EXISTS idx_guia_detalle_dest ON guia_remision_detalle(guia_remision_destinatario_id)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureGuiaRemisionSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureGuiaRemisionSchema conexión"); }
    }

    public static void ensureRetencionSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS retencion_registro ("+
            "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, establecimiento VARCHAR(3) NOT NULL DEFAULT '001',"+
            "punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL, fecha_emision TIMESTAMP NOT NULL DEFAULT now(),"+
            "periodo_fiscal VARCHAR(7) NOT NULL, proveedor_id INTEGER REFERENCES proveedor(id), tipo_identificacion_sujeto VARCHAR(2) NOT NULL,"+
            "razon_social_sujeto VARCHAR(300) NOT NULL, identificacion_sujeto VARCHAR(13) NOT NULL, estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',"+
            "mensaje_sri TEXT, numero_autorizacion VARCHAR(49), fecha_autorizacion TIMESTAMP, xml_firmado TEXT,"+
            "usuario_id INTEGER NOT NULL REFERENCES usuarios(id), creado_en TIMESTAMP NOT NULL DEFAULT now())",
            "CREATE TABLE IF NOT EXISTS retencion_documento_sustento ("+
            "id SERIAL PRIMARY KEY, retencion_id INTEGER NOT NULL REFERENCES retencion_registro(id) ON DELETE CASCADE,"+
            "cod_sustento VARCHAR(2) NOT NULL DEFAULT '01', cod_doc_sustento VARCHAR(2) NOT NULL DEFAULT '01',"+
            "num_doc_sustento VARCHAR(17) NOT NULL, fecha_emision_doc_sustento DATE NOT NULL, total_sin_impuestos NUMERIC(12,2) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS retencion_detalle ("+
            "id SERIAL PRIMARY KEY, doc_sustento_id INTEGER NOT NULL REFERENCES retencion_documento_sustento(id) ON DELETE CASCADE,"+
            "codigo VARCHAR(1) NOT NULL, codigo_retencion VARCHAR(4) NOT NULL, base_imponible NUMERIC(12,2) NOT NULL,"+
            "porcentaje_retener NUMERIC(5,2) NOT NULL, valor_retenido NUMERIC(12,2) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS tabla_retencion ("+
            "id SERIAL PRIMARY KEY, codigo_retencion VARCHAR(4) NOT NULL, descripcion VARCHAR(200) NOT NULL, tipo VARCHAR(1) NOT NULL,"+
            "porcentaje NUMERIC(5,2) NOT NULL, vigente_desde DATE NOT NULL, vigente_hasta DATE)",
            "INSERT INTO secuencia_documento (tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('RETENCION','001','001','001',1) ON CONFLICT (tipo) DO NOTHING",
            "CREATE INDEX IF NOT EXISTS idx_retencion_clave ON retencion_registro(clave_acceso)",
            "CREATE INDEX IF NOT EXISTS idx_retencion_estado ON retencion_registro(estado_sri)",
            "CREATE INDEX IF NOT EXISTS idx_retencion_doc_retencion ON retencion_documento_sustento(retencion_id)",
            "CREATE INDEX IF NOT EXISTS idx_retencion_det_doc ON retencion_detalle(doc_sustento_id)",
            "CREATE INDEX IF NOT EXISTS idx_tabla_retencion_codigo ON tabla_retencion(codigo_retencion)",
            "INSERT INTO tabla_retencion (codigo_retencion, descripcion, tipo, porcentaje, vigente_desde) SELECT '303','Honorarios profesionales','1',10.00,'2026-03-01' WHERE NOT EXISTS (SELECT 1 FROM tabla_retencion WHERE codigo_retencion='303' AND vigente_desde='2026-03-01')",
            "INSERT INTO tabla_retencion (codigo_retencion, descripcion, tipo, porcentaje, vigente_desde) SELECT '312','Transporte privado pasajeros','1',1.00,'2026-03-01' WHERE NOT EXISTS (SELECT 1 FROM tabla_retencion WHERE codigo_retencion='312' AND vigente_desde='2026-03-01')",
            "INSERT INTO tabla_retencion (codigo_retencion, descripcion, tipo, porcentaje, vigente_desde) SELECT '725','IVA bienes','2',30.00,'2026-03-01' WHERE NOT EXISTS (SELECT 1 FROM tabla_retencion WHERE codigo_retencion='725' AND vigente_desde='2026-03-01')",
            "INSERT INTO tabla_retencion (codigo_retencion, descripcion, tipo, porcentaje, vigente_desde) SELECT '723','IVA servicios','2',70.00,'2026-03-01' WHERE NOT EXISTS (SELECT 1 FROM tabla_retencion WHERE codigo_retencion='723' AND vigente_desde='2026-03-01')",
            "INSERT INTO tabla_retencion (codigo_retencion, descripcion, tipo, porcentaje, vigente_desde) SELECT '322','Seguros y reaseguros','1',1.75,'2026-03-01' WHERE NOT EXISTS (SELECT 1 FROM tabla_retencion WHERE codigo_retencion='322' AND vigente_desde='2026-03-01')"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureRetencionSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureRetencionSchema conexión"); }
    }

    public static void ensureCertificadoEstadoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS certificado_estado (id SERIAL PRIMARY KEY, ruta_p12 VARCHAR(500) NOT NULL, titular VARCHAR(300), fecha_emision DATE, fecha_expiracion DATE NOT NULL, dias_restantes INTEGER NOT NULL, nivel_severidad VARCHAR(15) NOT NULL, ultima_verificacion TIMESTAMP NOT NULL DEFAULT now(), ultimo_email_enviado TIMESTAMP)",
            "CREATE INDEX IF NOT EXISTS idx_certificado_ruta ON certificado_estado(ruta_p12)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureCertificadoEstadoSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureCertificadoEstadoSchema conexión"); }
    }

    public static void ensureConfiguracionEmailSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS configuracion_email ("+
            "id SERIAL PRIMARY KEY, host_smtp VARCHAR(150) NOT NULL, puerto_smtp INTEGER NOT NULL DEFAULT 587, "+
            "usar_tls BOOLEAN NOT NULL DEFAULT true, email_remitente VARCHAR(150) NOT NULL, "+
            "nombre_remitente VARCHAR(150) NOT NULL, usuario_smtp VARCHAR(150) NOT NULL, "+
            "password_cifrado TEXT NOT NULL, reply_to VARCHAR(150), activo BOOLEAN NOT NULL DEFAULT true, "+
            "actualizado_en TIMESTAMP NOT NULL DEFAULT now(), actualizado_por INTEGER REFERENCES usuarios(id))",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_config_email_unica_activa ON configuracion_email(activo) WHERE activo=true"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureConfiguracionEmailSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureConfiguracionEmailSchema conexión"); }
        // HSQLDB fallback (SERIAL -> IDENTITY, partial index no soportado)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS configuracion_email ("+
                    "id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, host_smtp VARCHAR(150) NOT NULL, puerto_smtp INTEGER NOT NULL DEFAULT 587, "+
                    "usar_tls BOOLEAN NOT NULL DEFAULT true, email_remitente VARCHAR(150) NOT NULL, "+
                    "nombre_remitente VARCHAR(150) NOT NULL, usuario_smtp VARCHAR(150) NOT NULL, "+
                    "password_cifrado TEXT NOT NULL, reply_to VARCHAR(150), activo BOOLEAN NOT NULL DEFAULT true, "+
                    "actualizado_en TIMESTAMP NOT NULL DEFAULT now(), actualizado_por INTEGER)");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureConfiguracionEmailSchema hsqldb fallback"); }
        // seed inicial con credencial hardcodeada previa si tabla vacía (no corta envíos el día del deploy)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM configuracion_email WHERE activo=true")) {
            if (rs.next() && rs.getInt(1) == 0) {
                try {
                    String pwdCifrado = com.vendex.dao.ConfiguracionEmailDAOPostgres.cifrarParaGuardar("awnfnmidbtqyyclz");
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO configuracion_email (host_smtp, puerto_smtp, usar_tls, email_remitente, nombre_remitente, usuario_smtp, password_cifrado, activo) " +
                            "VALUES (?,?,?,?,?,?,?,true)")) {
                        ps.setString(1, "smtp.gmail.com");
                        ps.setInt(2, 587);
                        ps.setBoolean(3, true);
                        ps.setString(4, "tagrepuestosvick@gmail.com");
                        ps.setString(5, "Vendex Repuestos");
                        ps.setString(6, "tagrepuestosvick@gmail.com");
                        ps.setString(7, pwdCifrado);
                        ps.executeUpdate();
                    }
                } catch (Exception ignore) {}
            }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureConfiguracionEmailSchema seed conexión"); }
    }

    public static void ensureLoginBruteForceSchema() {
        String[] ddls = new String[]{
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_intento_fallido TIMESTAMP",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_bloqueado_hasta ON usuarios(bloqueado_hasta) WHERE bloqueado_hasta IS NOT NULL",
            "CREATE TABLE IF NOT EXISTS login_intento_log (id SERIAL PRIMARY KEY, usuario_input VARCHAR(150) NOT NULL, exitoso BOOLEAN NOT NULL, equipo VARCHAR(150), creado_en TIMESTAMP NOT NULL DEFAULT now())",
            "CREATE INDEX IF NOT EXISTS idx_login_log_usuario_input ON login_intento_log(usuario_input)",
            "CREATE INDEX IF NOT EXISTS idx_login_log_creado_en ON login_intento_log(creado_en)",
            "CREATE TABLE IF NOT EXISTS configuracion (clave VARCHAR(100) PRIMARY KEY, valor TEXT NOT NULL, descripcion VARCHAR(300), actualizado_en TIMESTAMP NOT NULL DEFAULT now())",
            "INSERT INTO configuracion (clave, valor, descripcion) VALUES ('bloqueo.intentos_permitidos','5','Intentos fallidos antes de bloquear') ON CONFLICT (clave) DO NOTHING",
            "INSERT INTO configuracion (clave, valor, descripcion) VALUES ('bloqueo.ventana_minutos','15','Ventana reseteo contador') ON CONFLICT (clave) DO NOTHING",
            "INSERT INTO configuracion (clave, valor, descripcion) VALUES ('bloqueo.duracion_minutos','15','Duracion inicial bloqueo backoff') ON CONFLICT (clave) DO NOTHING",
            "INSERT INTO configuracion (clave, valor, descripcion) VALUES ('bloqueo.max_horas','24','Tope max bloqueo') ON CONFLICT (clave) DO NOTHING"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureLoginBruteForceSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureLoginBruteForceSchema conexión"); }
        // HSQLDB fallback
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS login_intento_log (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, usuario_input VARCHAR(150) NOT NULL, exitoso BOOLEAN NOT NULL, equipo VARCHAR(150), creado_en TIMESTAMP NOT NULL DEFAULT now())");
            st.execute("CREATE TABLE IF NOT EXISTS configuracion (clave VARCHAR(100) PRIMARY KEY, valor TEXT NOT NULL, descripcion VARCHAR(300), actualizado_en TIMESTAMP NOT NULL DEFAULT now())");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureLoginBruteForceSchema hsqldb"); }
        // permisos mínimo privilegio
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT, INSERT ON TABLE login_intento_log TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant login_intento_log"); }
            try { st.execute("GRANT USAGE, SELECT ON SEQUENCE login_intento_log_id_seq TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant login_intento_log seq"); }
            try { st.execute("GRANT SELECT ON TABLE configuracion TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant configuracion"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureLoginBruteForceSchema grants"); }
    }

    public static void ensurePermisosGranularesSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS rol (id SERIAL PRIMARY KEY, nombre VARCHAR(50) UNIQUE NOT NULL, descripcion VARCHAR(200), limite_descuento_pct NUMERIC(5,2) DEFAULT NULL)",
            "CREATE TABLE IF NOT EXISTS permiso (codigo VARCHAR(50) PRIMARY KEY, descripcion VARCHAR(200) NOT NULL, categoria VARCHAR(50) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS rol_permiso (rol_id INT NOT NULL REFERENCES rol(id) ON DELETE CASCADE, permiso_codigo VARCHAR(50) NOT NULL REFERENCES permiso(codigo) ON DELETE CASCADE, PRIMARY KEY (rol_id, permiso_codigo))",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS rol_id INT REFERENCES rol(id)",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS limite_descuento_pct NUMERIC(5,2) DEFAULT NULL",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_rol_id ON usuarios(rol_id)",
            "CREATE TABLE IF NOT EXISTS auditoria_accion (id SERIAL PRIMARY KEY, usuario_id INT NOT NULL REFERENCES usuarios(id), permiso_codigo VARCHAR(50) NOT NULL, resultado VARCHAR(15) NOT NULL, detalle VARCHAR(300), creado_en TIMESTAMP NOT NULL DEFAULT now())",
            "CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON auditoria_accion(usuario_id)",
            "CREATE INDEX IF NOT EXISTS idx_auditoria_permiso ON auditoria_accion(permiso_codigo)",
            "CREATE INDEX IF NOT EXISTS idx_auditoria_creado ON auditoria_accion(creado_en)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensurePermisosGranularesSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePermisosGranularesSchema conexión"); }
        // HSQLDB fallback
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS rol (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, nombre VARCHAR(50) UNIQUE NOT NULL, descripcion VARCHAR(200), limite_descuento_pct NUMERIC(5,2) DEFAULT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS permiso (codigo VARCHAR(50) PRIMARY KEY, descripcion VARCHAR(200) NOT NULL, categoria VARCHAR(50) NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS rol_permiso (rol_id INT NOT NULL, permiso_codigo VARCHAR(50) NOT NULL, PRIMARY KEY (rol_id, permiso_codigo))");
            st.execute("CREATE TABLE IF NOT EXISTS auditoria_accion (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, usuario_id INT NOT NULL, permiso_codigo VARCHAR(50) NOT NULL, resultado VARCHAR(15) NOT NULL, detalle VARCHAR(300), creado_en TIMESTAMP NOT NULL DEFAULT now())");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePermisosGranularesSchema hsqldb"); }
        // seeds idempotentes (también en sql/migracion_permisos_granulares_20260922.sql para postgres)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : new String[]{
                "INSERT INTO rol (nombre, descripcion, limite_descuento_pct) VALUES ('ADMINISTRADOR','Acceso total', NULL) ON CONFLICT (nombre) DO NOTHING",
                "INSERT INTO rol (nombre, descripcion, limite_descuento_pct) VALUES ('VENDEDOR','Ventas y facturación', 5.00) ON CONFLICT (nombre) DO NOTHING",
                "INSERT INTO rol (nombre, descripcion, limite_descuento_pct) VALUES ('CAJERO','Caja y cobranza', 0.00) ON CONFLICT (nombre) DO NOTHING",
                "INSERT INTO rol (nombre, descripcion, limite_descuento_pct) VALUES ('BODEGUERO','Inventario y bodega', 0.00) ON CONFLICT (nombre) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('FACTURA_EMITIR','Emitir una factura nueva','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('FACTURA_ANULAR','Iniciar NC por anulación total','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('NOTA_CREDITO_EMITIR','Emitir NC','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('NOTA_DEBITO_EMITIR','Emitir ND','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('GUIA_REMISION_EMITIR','Emitir GR','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('RETENCION_EMITIR','Emitir retención','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('PRECIO_EDITAR','Modificar precio','INVENTARIO') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('INVENTARIO_AJUSTAR','Ajuste manual de stock','INVENTARIO') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('DESCUENTO_APLICAR','Aplicar descuentos','FACTURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('CAJA_ABRIR','Abrir caja','CAJA') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('CAJA_CERRAR','Cerrar caja','CAJA') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('CAJA_VER_HISTORICO','Ver cierres de caja','CAJA') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('USUARIO_GESTIONAR','Gestionar usuarios y roles','USUARIOS') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('CONFIGURACION_EMAIL_EDITAR','Editar correo','CONFIGURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('CONFIGURACION_BD_VER','Ver/editar conexión BD','CONFIGURACION') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('REPORTE_VER','Ver dashboard y reportes','REPORTES') ON CONFLICT (codigo) DO NOTHING",
                "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('REPORTE_VER_FINANCIERO','Ver reportes financieros','REPORTES') ON CONFLICT (codigo) DO NOTHING"
            }) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "seed rol/permiso"); }
            // matriz inicial
            for (String sql : new String[]{
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, p.codigo FROM rol r CROSS JOIN permiso p WHERE r.nombre='ADMINISTRADOR' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'FACTURA_EMITIR' FROM rol r WHERE r.nombre='VENDEDOR' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'GUIA_REMISION_EMITIR' FROM rol r WHERE r.nombre='VENDEDOR' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'DESCUENTO_APLICAR' FROM rol r WHERE r.nombre='VENDEDOR' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'REPORTE_VER' FROM rol r WHERE r.nombre='VENDEDOR' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'FACTURA_EMITIR' FROM rol r WHERE r.nombre='CAJERO' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'NOTA_CREDITO_EMITIR' FROM rol r WHERE r.nombre='CAJERO' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'CAJA_ABRIR' FROM rol r WHERE r.nombre='CAJERO' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'CAJA_CERRAR' FROM rol r WHERE r.nombre='CAJERO' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'REPORTE_VER' FROM rol r WHERE r.nombre='CAJERO' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'GUIA_REMISION_EMITIR' FROM rol r WHERE r.nombre='BODEGUERO' ON CONFLICT DO NOTHING",
                "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, 'INVENTARIO_AJUSTAR' FROM rol r WHERE r.nombre='BODEGUERO' ON CONFLICT DO NOTHING"
            }) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "seed rol_permiso"); }
            // HSQLDB no soporta ON CONFLICT, ignorar error y continuar
            // backfill rol_id
            try { st.execute("UPDATE usuarios SET rol_id = (SELECT id FROM rol WHERE rol.nombre = usuarios.rol) WHERE rol_id IS NULL AND rol IS NOT NULL"); } catch (SQLException e) { logIfPermissionDenied(e, "backfill rol_id"); }
            try { st.execute("UPDATE usuarios SET rol_id = (SELECT id FROM rol WHERE nombre='ADMINISTRADOR') WHERE rol_id IS NULL"); } catch (SQLException e) { logIfPermissionDenied(e, "backfill admin"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePermisosGranularesSchema seeds"); }
        // grants
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT ON TABLE rol TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant rol"); }
            try { st.execute("GRANT SELECT ON TABLE permiso TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant permiso"); }
            try { st.execute("GRANT SELECT ON TABLE rol_permiso TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant rol_permiso"); }
            try { st.execute("GRANT SELECT, INSERT ON TABLE auditoria_accion TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant auditoria"); }
            try { st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant seq auditoria"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePermisosGranulares grants"); }
    }

    public static void ensureComprobantePendienteSriSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS comprobante_pendiente_sri (" +
            "id SERIAL PRIMARY KEY, comprobante_id INTEGER REFERENCES comprobantes_electronicos(id), " +
            "tipo_comprobante VARCHAR(20) NOT NULL, clave_acceso VARCHAR(49) UNIQUE NOT NULL, " +
            "numero_comprobante VARCHAR(30), ambiente VARCHAR(10), intentos INTEGER NOT NULL DEFAULT 0, " +
            "ultimo_intento TIMESTAMP, proximo_intento TIMESTAMP NOT NULL DEFAULT now(), " +
            "estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', ultimo_mensaje_sri TEXT)",
            "CREATE INDEX IF NOT EXISTS idx_pendiente_estado_proximo ON comprobante_pendiente_sri(estado, proximo_intento)",
            "CREATE INDEX IF NOT EXISTS idx_pendiente_clave ON comprobante_pendiente_sri(clave_acceso)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureComprobantePendienteSriSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureComprobantePendienteSriSchema conexion"); }
        // HSQLDB fallback
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS comprobante_pendiente_sri (" +
                    "id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, comprobante_id INTEGER, " +
                    "tipo_comprobante VARCHAR(20) NOT NULL, clave_acceso VARCHAR(49) NOT NULL UNIQUE, " +
                    "numero_comprobante VARCHAR(30), ambiente VARCHAR(10), intentos INTEGER NOT NULL DEFAULT 0, " +
                    "ultimo_intento TIMESTAMP, proximo_intento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', ultimo_mensaje_sri TEXT)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_pendiente_estado_proximo ON comprobante_pendiente_sri(estado, proximo_intento)");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureComprobantePendienteSriSchema hsqldb"); }
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE comprobante_pendiente_sri TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant pendiente"); }
            try { st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant pendiente seq"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureComprobantePendienteSriSchema grants"); }
    }

    public static void ensureVehiculoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS vehiculo (id SERIAL PRIMARY KEY, marca VARCHAR(50) NOT NULL, modelo VARCHAR(50) NOT NULL, anio_desde INT, anio_hasta INT, motor VARCHAR(50), combustible VARCHAR(20), vin_prefijo VARCHAR(8), pais_origen VARCHAR(50), tipo_vehiculo VARCHAR(30), creado_en TIMESTAMP NOT NULL DEFAULT now(), UNIQUE(marca, modelo, COALESCE(anio_desde,0), COALESCE(motor,'')))",
            "CREATE TABLE IF NOT EXISTS inventario_vehiculo (inventario_id INTEGER NOT NULL REFERENCES inventario(id) ON DELETE CASCADE, vehiculo_id INTEGER NOT NULL REFERENCES vehiculo(id) ON DELETE CASCADE, notas VARCHAR(200), PRIMARY KEY (inventario_id, vehiculo_id))",
            "CREATE INDEX IF NOT EXISTS idx_vehiculo_marca_modelo ON vehiculo(marca, modelo)",
            "CREATE INDEX IF NOT EXISTS idx_vehiculo_anio ON vehiculo(anio_desde, anio_hasta)",
            "CREATE INDEX IF NOT EXISTS idx_iv_veh_vehiculo ON inventario_vehiculo(vehiculo_id)",
            "CREATE INDEX IF NOT EXISTS idx_iv_veh_inventario ON inventario_vehiculo(inventario_id)",
            "INSERT INTO vehiculo(marca, modelo, anio_desde, anio_hasta, motor, combustible, vin_prefijo, tipo_vehiculo) VALUES ('CHEVROLET','AVEO',2012,2017,'1.6','Gasolina','3G1','Sedán') ON CONFLICT (marca, modelo, COALESCE(anio_desde,0), COALESCE(motor,'')) DO NOTHING",
            "INSERT INTO vehiculo(marca, modelo, anio_desde, anio_hasta, motor, combustible, vin_prefijo, tipo_vehiculo) VALUES ('TOYOTA','HILUX',2015,2024,'2.8','Diésel','8AJ','Pickup') ON CONFLICT (marca, modelo, COALESCE(anio_desde,0), COALESCE(motor,'')) DO NOTHING"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureVehiculoSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureVehiculoSchema conexión"); }
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS vehiculo (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, marca VARCHAR(50) NOT NULL, modelo VARCHAR(50) NOT NULL, anio_desde INT, anio_hasta INT, motor VARCHAR(50), combustible VARCHAR(20), vin_prefijo VARCHAR(8), pais_origen VARCHAR(50), tipo_vehiculo VARCHAR(30), creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS inventario_vehiculo (inventario_id INTEGER NOT NULL, vehiculo_id INTEGER NOT NULL, notas VARCHAR(200), PRIMARY KEY (inventario_id, vehiculo_id))");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (!msg.contains("already exists") && !msg.contains("duplicate")) logIfPermissionDenied(e, "ensureVehiculoSchema hsqldb");
        }
    }

    public static void ensureAccesoDirectoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS acceso_directo (" +
            "id SERIAL PRIMARY KEY, usuario_id INT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, " +
            "vista_ruta VARCHAR(100) NOT NULL, titulo VARCHAR(80) NOT NULL, icono_literal VARCHAR(60) NOT NULL, " +
            "posicion INT NOT NULL DEFAULT 0, creado_en TIMESTAMP NOT NULL DEFAULT now(), " +
            "UNIQUE(usuario_id, vista_ruta))",
            "CREATE INDEX IF NOT EXISTS idx_acceso_directo_usuario ON acceso_directo(usuario_id)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureAccesoDirectoSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureAccesoDirectoSchema conexion"); }
        // HSQLDB fallback
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS acceso_directo (" +
                    "id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, usuario_id INTEGER NOT NULL, " +
                    "vista_ruta VARCHAR(100) NOT NULL, titulo VARCHAR(80) NOT NULL, icono_literal VARCHAR(60) NOT NULL, " +
                    "posicion INTEGER NOT NULL DEFAULT 0, creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(usuario_id, vista_ruta))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_acceso_directo_usuario ON acceso_directo(usuario_id)");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureAccesoDirectoSchema hsqldb"); }
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE acceso_directo TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant acceso_directo"); }
            try { st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant acceso_directo seq"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureAccesoDirectoSchema grants"); }
    }

    /** Fase 3 Vendexi: tablas de medición de consumo, periodo de capacidad y sesiones. */
    public static void ensureVendexiSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS vendexi_consumo (" +
            "id BIGSERIAL PRIMARY KEY, fecha TIMESTAMP NOT NULL DEFAULT now(), " +
            "modulo VARCHAR(40) NOT NULL, operacion VARCHAR(40) NOT NULL, modelo VARCHAR(80), " +
            "tokens_entrada BIGINT NOT NULL DEFAULT 0, tokens_salida BIGINT NOT NULL DEFAULT 0, " +
            "costo_estimado NUMERIC(12,6) NOT NULL DEFAULT 0)",
            "CREATE INDEX IF NOT EXISTS idx_vendexi_consumo_fecha ON vendexi_consumo(fecha)",
            "CREATE TABLE IF NOT EXISTS vendexi_periodo (" +
            "id INT PRIMARY KEY DEFAULT 1, inicio TIMESTAMP NOT NULL DEFAULT now(), " +
            "CONSTRAINT vendexi_periodo_unico CHECK (id = 1))",
            "INSERT INTO vendexi_periodo (id, inicio) VALUES (1, now()) ON CONFLICT DO NOTHING",
            "CREATE TABLE IF NOT EXISTS vendexi_sesion (" +
            "id BIGSERIAL PRIMARY KEY, inicio TIMESTAMP NOT NULL DEFAULT now(), fin TIMESTAMP, " +
            "duracion_seg BIGINT, modulo VARCHAR(40) NOT NULL, tipo VARCHAR(30) NOT NULL DEFAULT 'CONSULTA')",
            "CREATE INDEX IF NOT EXISTS idx_vendexi_sesion_inicio ON vendexi_sesion(inicio)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureVendexiSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureVendexiSchema conexion"); }
        // HSQLDB fallback (tests)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS vendexi_consumo (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "modulo VARCHAR(40) NOT NULL, operacion VARCHAR(40) NOT NULL, modelo VARCHAR(80), " +
                    "tokens_entrada BIGINT NOT NULL DEFAULT 0, tokens_salida BIGINT NOT NULL DEFAULT 0, " +
                    "costo_estimado NUMERIC(12,6) NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS vendexi_periodo (" +
                    "id INTEGER PRIMARY KEY DEFAULT 1, inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS vendexi_sesion (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "fin TIMESTAMP, duracion_seg BIGINT, modulo VARCHAR(40) NOT NULL, tipo VARCHAR(30) NOT NULL DEFAULT 'CONSULTA')");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureVendexiSchema hsqldb"); }
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE vendexi_consumo TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant vendexi_consumo"); }
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE vendexi_periodo TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant vendexi_periodo"); }
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE vendexi_sesion TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant vendexi_sesion"); }
            try { st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant vendexi seq"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureVendexiSchema grants"); }
    }

    /** Fase 10: historial persistente de Vetxi; SQL compatible con PostgreSQL y HSQLDB. */
    public static void ensureVetxiHistorialSchema() {
        String[] ddls = {
            "CREATE TABLE IF NOT EXISTS vetxi_chat_mensaje (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, " +
            "mensaje_id VARCHAR(36) NOT NULL, conversacion_id VARCHAR(36) NOT NULL, usuario_id INTEGER NOT NULL, " +
            "sucursal_id INTEGER NOT NULL, titulo VARCHAR(120) NOT NULL, es_usuario BOOLEAN NOT NULL, texto TEXT NOT NULL, " +
            "sin_ia BOOLEAN DEFAULT false NOT NULL, rol_ia VARCHAR(20), roles_bloqueados TEXT DEFAULT '[]' NOT NULL, " +
            "fuentes TEXT DEFAULT '[]' NOT NULL, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
            "CONSTRAINT uq_vetxi_chat_mensaje UNIQUE (usuario_id, sucursal_id, mensaje_id))",
            "CREATE INDEX IF NOT EXISTS idx_vetxi_chat_propietario ON vetxi_chat_mensaje(usuario_id, sucursal_id, id)",
            "CREATE INDEX IF NOT EXISTS idx_vetxi_chat_conversacion ON vetxi_chat_mensaje(usuario_id, sucursal_id, conversacion_id, id)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            boolean hsql = con.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT).contains("hsql");
            for (String ddl : ddls) st.execute(hsql ? ddl.replace("TEXT", "VARCHAR(1048576)") : ddl);
            if (con.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT).contains("postgres")) {
                try { st.execute("GRANT SELECT, INSERT ON TABLE vetxi_chat_mensaje TO app_vendex"); }
                catch (SQLException e) { logIfPermissionDenied(e, "grant historial Vetxi"); }
                try { st.execute("GRANT USAGE, SELECT ON SEQUENCE vetxi_chat_mensaje_id_seq TO app_vendex"); }
                catch (SQLException e) { logIfPermissionDenied(e, "grant secuencia historial Vetxi"); }
            }
        } catch (SQLException e) { LOG.log(Level.WARNING, "No se pudo preparar el historial de Vetxi", e); }
    }

    /** Fase 6b: tablas de administración central de licencias (emisión/revocación). */
    public static void ensureLicenciasCentralSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS licencia_emitida (" +
            "id BIGSERIAL PRIMARY KEY, machine_code VARCHAR(64) NOT NULL, clave_hash VARCHAR(64) NOT NULL, " +
            "modalidad VARCHAR(16) NOT NULL, incluye_ia BOOLEAN NOT NULL DEFAULT true, " +
            "roles VARCHAR(255) NOT NULL DEFAULT '-', capacidad BIGINT NOT NULL DEFAULT 0, " +
            "expiracion DATE, emitida_por INT, fecha TIMESTAMP NOT NULL DEFAULT now())",
            "CREATE INDEX IF NOT EXISTS idx_licencia_emitida_machine ON licencia_emitida(machine_code)",
            "CREATE TABLE IF NOT EXISTS licencia_revocada (" +
            "id BIGSERIAL PRIMARY KEY, machine_code VARCHAR(64) UNIQUE NOT NULL, " +
            "motivo VARCHAR(255), revocada_por INT, fecha TIMESTAMP NOT NULL DEFAULT now())"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureLicenciasCentralSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureLicenciasCentralSchema conexion"); }
        // HSQLDB fallback (tests)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS licencia_emitida (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, machine_code VARCHAR(64) NOT NULL, " +
                    "clave_hash VARCHAR(64) NOT NULL, modalidad VARCHAR(16) NOT NULL, " +
                    "incluye_ia BOOLEAN DEFAULT true, roles VARCHAR(255) DEFAULT '-', " +
                    "capacidad BIGINT DEFAULT 0, expiracion DATE, emitida_por INTEGER, " +
                    "fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS licencia_revocada (" +
                    "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, machine_code VARCHAR(64) UNIQUE NOT NULL, " +
                    "motivo VARCHAR(255), revocada_por INTEGER, fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureLicenciasCentralSchema hsqldb"); }
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE licencia_emitida TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant licencia_emitida"); }
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE licencia_revocada TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant licencia_revocada"); }
            try { st.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant licencias seq"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureLicenciasCentralSchema grants"); }
    }

    /** Fase 7: permisos por usuario, multi-sucursal, trazabilidad y alineación de usuarios. */
    public static void ensureFase7Schema() {
        String[] ddls = new String[]{
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT true",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email VARCHAR(120)",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255)",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP NOT NULL DEFAULT now()",
            "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_login TIMESTAMP",
            "CREATE TABLE IF NOT EXISTS usuario_permiso (" +
            "usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, " +
            "permiso_codigo VARCHAR(50) NOT NULL REFERENCES permiso(codigo) ON DELETE CASCADE, " +
            "otorgar BOOLEAN NOT NULL DEFAULT true, " +
            "PRIMARY KEY (usuario_id, permiso_codigo))",
            "CREATE TABLE IF NOT EXISTS usuario_sucursal (" +
            "usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, " +
            "sucursal_id INTEGER NOT NULL REFERENCES sucursal(id) ON DELETE CASCADE, " +
            "PRIMARY KEY (usuario_id, sucursal_id))",
            "CREATE INDEX IF NOT EXISTS idx_usuario_sucursal ON usuario_sucursal(sucursal_id)",
            "ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS usuario_id INTEGER",
            "ALTER TABLE nota_venta_registro ADD COLUMN IF NOT EXISTS usuario_id INTEGER",
            "CREATE INDEX IF NOT EXISTS idx_factura_usuario ON factura_registro(usuario_id)",
            "CREATE INDEX IF NOT EXISTS idx_nota_venta_usuario ON nota_venta_registro(usuario_id)",
            "ALTER TABLE auditoria_accion ADD COLUMN IF NOT EXISTS sucursal_id INTEGER"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureFase7Schema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureFase7Schema conexion"); }
        // HSQLDB fallback (sin IF NOT EXISTS en ALTER/INDEX; errores ya-existente se ignoran por sentencia)
        String[][] hsqldb = new String[][]{
            {"ALTER TABLE usuarios ADD COLUMN activo BOOLEAN NOT NULL DEFAULT true"},
            {"ALTER TABLE usuarios ADD COLUMN email VARCHAR(120)"},
            {"ALTER TABLE usuarios ADD COLUMN password_hash VARCHAR(255)"},
            {"ALTER TABLE usuarios ADD COLUMN fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP"},
            {"ALTER TABLE usuarios ADD COLUMN ultimo_login TIMESTAMP"},
            {"CREATE TABLE IF NOT EXISTS usuario_permiso (" +
                    "usuario_id INTEGER NOT NULL, permiso_codigo VARCHAR(50) NOT NULL, " +
                    "otorgar BOOLEAN NOT NULL DEFAULT true, PRIMARY KEY (usuario_id, permiso_codigo))"},
            {"CREATE TABLE IF NOT EXISTS usuario_sucursal (" +
                    "usuario_id INTEGER NOT NULL, sucursal_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (usuario_id, sucursal_id))"},
            {"ALTER TABLE factura_registro ADD COLUMN usuario_id INTEGER"},
            {"ALTER TABLE nota_venta_registro ADD COLUMN usuario_id INTEGER"},
            {"ALTER TABLE auditoria_accion ADD COLUMN sucursal_id INTEGER"}
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String[] sql : hsqldb) try { st.execute(sql[0]); } catch (SQLException e) { logIfPermissionDenied(e, "ensureFase7Schema hsqldb"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureFase7Schema hsqldb conexion"); }
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE usuario_permiso TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant usuario_permiso"); }
            try { st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE usuario_sucursal TO app_vendex"); } catch (SQLException e) { logIfPermissionDenied(e, "grant usuario_sucursal"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureFase7Schema grants"); }
    }

    public static void ensureSucursalSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS sucursal (id SERIAL PRIMARY KEY, codigo VARCHAR(3) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL, direccion VARCHAR(300), telefono VARCHAR(20), es_central BOOLEAN NOT NULL DEFAULT false, activo BOOLEAN NOT NULL DEFAULT true, creado_en TIMESTAMP NOT NULL DEFAULT now())",
            "INSERT INTO sucursal(codigo, nombre, direccion, es_central, activo) VALUES ('001', 'Matriz', 'Matriz', true, true) ON CONFLICT (codigo) DO NOTHING",
            "ALTER TABLE inventario ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE factura_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE nota_venta_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE nota_credito_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE nota_debito_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE guia_remision_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE retencion_registro ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "ALTER TABLE caja_sesion ADD COLUMN IF NOT EXISTS sucursal_id INTEGER DEFAULT 1",
            "CREATE INDEX IF NOT EXISTS idx_inventario_sucursal ON inventario(sucursal_id)",
            "CREATE INDEX IF NOT EXISTS idx_factura_sucursal ON factura_registro(sucursal_id)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensureSucursalSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureSucursalSchema conexión"); }
        // HSQLDB fallback
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS sucursal (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, codigo VARCHAR(3) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL, direccion VARCHAR(300), telefono VARCHAR(20), es_central BOOLEAN DEFAULT false, activo BOOLEAN DEFAULT true, creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("INSERT INTO sucursal(codigo, nombre, direccion, es_central, activo) VALUES ('001', 'Matriz', 'Matriz', true, true)");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (!msg.contains("unique") && !msg.contains("duplicate") && !msg.contains("already exists")) {
                logIfPermissionDenied(e, "ensureSucursalSchema hsqldb");
            }
        }
        // FKs opcionales (si existen tablas, no bloquear si falla por datos)
        try (Connection con = DatabaseConnection.getConnection(); java.sql.Statement st = con.createStatement()) {
            try { st.execute("ALTER TABLE inventario ADD CONSTRAINT IF NOT EXISTS fk_inventario_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id)"); } catch (SQLException e) { logIfPermissionDenied(e, "fk inventario sucursal"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensureSucursalSchema fk"); }
    }

    /** Módulo de préstamos financieros (V38). Idempotente, con fallback HSQLDB para tests. */
    public static void ensurePrestamoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS entidad_financiera (id SERIAL PRIMARY KEY, nombre VARCHAR(120) NOT NULL, tipo VARCHAR(20) DEFAULT 'BANCO', identificacion VARCHAR(20), telefono VARCHAR(30), contacto VARCHAR(80), notas TEXT, estado BOOLEAN DEFAULT TRUE, fecha_registro TIMESTAMP DEFAULT now())",
            "CREATE TABLE IF NOT EXISTS prestamo (id SERIAL PRIMARY KEY, entidad_id INT NOT NULL REFERENCES entidad_financiera(id), monto_principal NUMERIC(12,2) NOT NULL, tasa_periodo_pct NUMERIC(6,3) NOT NULL, periodicidad CHAR(1) NOT NULL, numero_cuotas INT NOT NULL, cuota_monto NUMERIC(12,2) NOT NULL, total_a_pagar NUMERIC(12,2) NOT NULL, total_interes NUMERIC(12,2) NOT NULL, fecha_inicio DATE NOT NULL, estado VARCHAR(15) DEFAULT 'ACTIVO', meta_periodicidad VARCHAR(10), meta_monto NUMERIC(12,2), notas TEXT, fecha_registro TIMESTAMP DEFAULT now())",
            "CREATE TABLE IF NOT EXISTS prestamo_cuota (id SERIAL PRIMARY KEY, prestamo_id INT NOT NULL REFERENCES prestamo(id) ON DELETE CASCADE, numero INT NOT NULL, fecha_vencimiento DATE NOT NULL, monto NUMERIC(12,2) NOT NULL, capital NUMERIC(12,2) NOT NULL, interes NUMERIC(12,2) NOT NULL, saldo_anterior NUMERIC(12,2) NOT NULL, saldo_restante NUMERIC(12,2) NOT NULL, monto_pagado NUMERIC(12,2) DEFAULT 0, interes_pagado NUMERIC(12,2) DEFAULT 0, estado VARCHAR(12) DEFAULT 'PENDIENTE', fecha_pago DATE, UNIQUE(prestamo_id, numero))",
            "CREATE TABLE IF NOT EXISTS prestamo_pago (id SERIAL PRIMARY KEY, prestamo_id INT NOT NULL REFERENCES prestamo(id), cuota_id INT REFERENCES prestamo_cuota(id), monto NUMERIC(12,2) NOT NULL, interes_parte NUMERIC(12,2) DEFAULT 0, capital_parte NUMERIC(12,2) DEFAULT 0, fuente VARCHAR(20) NOT NULL, referencia_id INT, usuario_id INT, notas TEXT, fecha TIMESTAMP DEFAULT now())",
            "CREATE INDEX IF NOT EXISTS idx_prestamo_estado ON prestamo(estado)",
            "CREATE INDEX IF NOT EXISTS idx_prestamo_cuota_prestamo ON prestamo_cuota(prestamo_id, estado, fecha_vencimiento)",
            "CREATE INDEX IF NOT EXISTS idx_prestamo_pago_prestamo ON prestamo_pago(prestamo_id)",
            "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('PRESTAMO_VER','Ver préstamos, cuotas y estados','PRESTAMOS') ON CONFLICT (codigo) DO NOTHING",
            "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('PRESTAMO_GESTIONAR','Crear y administrar préstamos y entidades financieras','PRESTAMOS') ON CONFLICT (codigo) DO NOTHING",
            "INSERT INTO permiso (codigo, descripcion, categoria) VALUES ('PRESTAMO_ABONAR','Registrar abonos a cuotas de préstamos','PRESTAMOS') ON CONFLICT (codigo) DO NOTHING",
            "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('PRESTAMO_VER'),('PRESTAMO_GESTIONAR'),('PRESTAMO_ABONAR')) AS p(codigo) JOIN permiso pp ON pp.codigo=p.codigo JOIN rol rr ON rr.nombre='ADMINISTRADOR' AND rr.id=r.id ON CONFLICT DO NOTHING",
            "INSERT INTO rol_permiso (rol_id, permiso_codigo) SELECT r.id, p.codigo FROM rol r CROSS JOIN (VALUES ('PRESTAMO_VER'),('PRESTAMO_ABONAR')) AS p(codigo) JOIN permiso pp ON pp.codigo=p.codigo JOIN rol rr ON rr.nombre='CAJERO' AND rr.id=r.id ON CONFLICT DO NOTHING"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensurePrestamoSchema"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePrestamoSchema conexión"); }
        // HSQLDB fallback (tests)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS entidad_financiera (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, nombre VARCHAR(120) NOT NULL, tipo VARCHAR(20) DEFAULT 'BANCO', identificacion VARCHAR(20), telefono VARCHAR(30), contacto VARCHAR(80), notas VARCHAR(2000), estado BOOLEAN DEFAULT TRUE, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS prestamo (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, entidad_id INTEGER NOT NULL, monto_principal NUMERIC(12,2) NOT NULL, tasa_periodo_pct NUMERIC(6,3) NOT NULL, periodicidad CHAR(1) NOT NULL, numero_cuotas INTEGER NOT NULL, cuota_monto NUMERIC(12,2) NOT NULL, total_a_pagar NUMERIC(12,2) NOT NULL, total_interes NUMERIC(12,2) NOT NULL, fecha_inicio DATE NOT NULL, estado VARCHAR(15) DEFAULT 'ACTIVO', meta_periodicidad VARCHAR(10), meta_monto NUMERIC(12,2), notas VARCHAR(2000), fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS prestamo_cuota (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, prestamo_id INTEGER NOT NULL, numero INTEGER NOT NULL, fecha_vencimiento DATE NOT NULL, monto NUMERIC(12,2) NOT NULL, capital NUMERIC(12,2) NOT NULL, interes NUMERIC(12,2) NOT NULL, saldo_anterior NUMERIC(12,2) NOT NULL, saldo_restante NUMERIC(12,2) NOT NULL, monto_pagado NUMERIC(12,2) DEFAULT 0, interes_pagado NUMERIC(12,2) DEFAULT 0, estado VARCHAR(12) DEFAULT 'PENDIENTE', fecha_pago DATE, UNIQUE(prestamo_id, numero))");
            st.execute("CREATE TABLE IF NOT EXISTS prestamo_pago (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, prestamo_id INTEGER NOT NULL, cuota_id INTEGER, monto NUMERIC(12,2) NOT NULL, interes_parte NUMERIC(12,2) DEFAULT 0, capital_parte NUMERIC(12,2) DEFAULT 0, fuente VARCHAR(20) NOT NULL, referencia_id INTEGER, usuario_id INTEGER, notas VARCHAR(2000), fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePrestamoSchema hsqldb"); }
        // Grants mínimo privilegio
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : new String[]{
                "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE entidad_financiera TO app_vendex",
                "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE prestamo TO app_vendex",
                "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE prestamo_cuota TO app_vendex",
                "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE prestamo_pago TO app_vendex",
                "GRANT USAGE, SELECT ON SEQUENCE entidad_financiera_id_seq TO app_vendex",
                "GRANT USAGE, SELECT ON SEQUENCE prestamo_id_seq TO app_vendex",
                "GRANT USAGE, SELECT ON SEQUENCE prestamo_cuota_id_seq TO app_vendex",
                "GRANT USAGE, SELECT ON SEQUENCE prestamo_pago_id_seq TO app_vendex"
            }) try { st.execute(sql); } catch (SQLException e) { logIfPermissionDenied(e, "ensurePrestamoSchema grant"); }
        } catch (SQLException e) { logIfPermissionDenied(e, "ensurePrestamoSchema grants"); }
    }

    /**
     * Flyway migraciones versionadas. Corre solo si hay postgres.password disponible
     * (solo host central). Usa usuario postgres, no app_vendex, con baseline manual por cliente.
     * Si no hay password (clientes), no hace nada y deja ensure* fallback.
     */
    public static void migrateWithFlywayIfAvailable() {
        String pgPass = com.vendex.config.PostgresConfig.getPassword();
        if (pgPass == null || pgPass.isBlank()) {
            LOG.info("Flyway omitido: sin postgres.password (solo host central migra). Usando ensure* fallback.");
            return;
        }
        try {
            String[] cfg = DbConfig.cargar();
            String url = cfg[0];
            // Flyway usa postgres superuser, mismo url/host vendex-db
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource(url, "postgres", pgPass)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(false) // manual por cliente: flyway baseline -baselineVersion=1
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    .load();
            flyway.migrate();
            LOG.info("Flyway migrate OK: " + url);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("baseline") || msg.contains("already baselined") || msg.contains("flyway_schema_history")) {
                LOG.warning("Flyway baseline pendiente: ejecutar manual por cliente `flyway baseline -baselineVersion=1` con postgres. Detalle: " + e.getMessage());
            } else if (msg.contains("already exists") || msg.contains("ya existe")) {
                LOG.warning("Flyway V1 baseline ya existe en BD — ejecutar manual `flyway baseline -baselineVersion=1` en este cliente antes de primer migrate. Detalle: " + e.getMessage());
            } else if (msg.contains("permission denied") || msg.contains("must be owner")) {
                LOG.log(Level.WARNING, "Flyway requiere postgres migrador (CREATE): " + e.getMessage());
            } else {
                LOG.log(Level.WARNING, "Flyway migrate fallo (se usara ensure* fallback): " + e.getMessage(), e);
            }
            // no propagar — ensure* fallback idempotente sigue
        }
    }

    public static void setConnectionParams(String url, String user, String password) {
        aplicarParams(url, user, password);
        configLoaded = true;
        // Si cambia a postgres real, recrear pool; si es hsqldb, cerrar pool
        if (url != null && (url.toLowerCase().contains("hsqldb") || url.contains("tc:"))) {
            synchronized (POOL_LOCK) { closePoolLocked(); }
        } else if (url != null && url.toLowerCase().contains("postgresql")) {
            ensurePool(url, user, password);
        }
    }

    public static void resetToDefault() {
        aplicarParams(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
        synchronized (POOL_LOCK) { closePoolLocked(); }
        configLoaded = false;
    }

    /** Para usar en tests Testcontainers: cerrar pool al final suite */
    public static void shutdown() {
        closePool();
    }
}

package com.vendex.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DEFAULT_URL = DbConfig.DEFAULT_URL;
    private static final String DEFAULT_USER = DbConfig.DEFAULT_USER;
    private static final String DEFAULT_PASSWORD = DbConfig.DEFAULT_PASSWORD;

    private static final ThreadLocal<String> URL = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();

    private static volatile boolean configLoaded = false;

    static {
        URL.set(DEFAULT_URL);
        USER.set(DEFAULT_USER);
        PASSWORD.set(DEFAULT_PASSWORD);
    }

    /**
     * Carga ~/.vendex/db.properties una vez al inicio. Llamado desde MainApp.start().
     * Si el archivo no existe lo crea con defaults (localhost). Para PC cliente
     * editar db.url a jdbc:postgresql://192.168.1.7:5432/dbVendex.
     */
    public static synchronized void initFromConfig() {
        if (configLoaded) return;
        try {
            String[] cfg = DbConfig.cargar();
            URL.set(cfg[0]);
            USER.set(cfg[1]);
            PASSWORD.set(cfg[2]);
            configLoaded = true;
        } catch (Exception e) {
            // fallback a defaults si el archivo esta corrupto
            e.printStackTrace();
            URL.set(DEFAULT_URL);
            USER.set(DEFAULT_USER);
            PASSWORD.set(DEFAULT_PASSWORD);
            configLoaded = true;
        }
    }

    public static Connection getConnection() throws SQLException {
        // lazy init por si alguien llama antes de MainApp (ej. TestConexion)
        if (!configLoaded) {
            synchronized (DatabaseConnection.class) {
                if (!configLoaded) initFromConfig();
            }
        }
        String url = URL.get();
        if (url == null) {
            url = DEFAULT_URL;
            URL.set(url);
            USER.set(DEFAULT_USER);
            PASSWORD.set(DEFAULT_PASSWORD);
        }
        return DriverManager.getConnection(url, USER.get(), PASSWORD.get());
    }

    public static void ensureNotaCreditoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS nota_credito_registro ("+
            "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, factura_registro_id INTEGER NOT NULL REFERENCES factura_registro(id),"+
            "establecimiento VARCHAR(3) NOT NULL DEFAULT '001', punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL,"+
            "fecha_emision TIMESTAMP NOT NULL DEFAULT now(), cliente_id INTEGER NOT NULL REFERENCES cliente(id), motivo VARCHAR(300) NOT NULL,"+
            "tipo_motivo VARCHAR(20) NOT NULL CHECK (tipo_motivo IN ('DEVOLUCION','DESCUENTO','ANULACION')),"+
            "total_sin_impuestos NUMERIC(12,2) NOT NULL, valor_iva NUMERIC(12,2) NOT NULL, valor_modificacion NUMERIC(12,2) NOT NULL,"+
            "reingresa_stock BOOLEAN NOT NULL DEFAULT false, estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', mensaje_sri VARCHAR(500),"+
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
             "estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', mensaje_sri VARCHAR(500), numero_autorizacion VARCHAR(49),"+
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
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException ignore) {}
        } catch (SQLException ignore) {}
    }

    public static void ensureGuiaRemisionSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS guia_remision_registro ("+
            "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, establecimiento VARCHAR(3) NOT NULL DEFAULT '001',"+
            "punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL, fecha_emision TIMESTAMP NOT NULL DEFAULT now(),"+
            "dir_partida VARCHAR(300) NOT NULL, razon_social_transportista VARCHAR(300) NOT NULL, tipo_identificacion_transportista VARCHAR(2) NOT NULL,"+
            "ruc_transportista VARCHAR(13) NOT NULL, placa VARCHAR(10) NOT NULL, fecha_ini_transporte DATE NOT NULL, fecha_fin_transporte DATE NOT NULL,"+
            "estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE', mensaje_sri VARCHAR(500), numero_autorizacion VARCHAR(49),"+
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
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException ignore) {}
        } catch (SQLException ignore) {}
    }

    public static void ensureRetencionSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS retencion_registro ("+
            "id SERIAL PRIMARY KEY, clave_acceso VARCHAR(49) UNIQUE NOT NULL, establecimiento VARCHAR(3) NOT NULL DEFAULT '001',"+
            "punto_emision VARCHAR(3) NOT NULL DEFAULT '001', secuencial VARCHAR(9) NOT NULL, fecha_emision TIMESTAMP NOT NULL DEFAULT now(),"+
            "periodo_fiscal VARCHAR(7) NOT NULL, proveedor_id INTEGER REFERENCES proveedor(id), tipo_identificacion_sujeto VARCHAR(2) NOT NULL,"+
            "razon_social_sujeto VARCHAR(300) NOT NULL, identificacion_sujeto VARCHAR(13) NOT NULL, estado_sri VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',"+
            "mensaje_sri VARCHAR(500), numero_autorizacion VARCHAR(49), fecha_autorizacion TIMESTAMP, xml_firmado TEXT,"+
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
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException ignore) {}
        } catch (SQLException ignore) {}
    }

    public static void ensureCertificadoEstadoSchema() {
        String[] ddls = new String[]{
            "CREATE TABLE IF NOT EXISTS certificado_estado (id SERIAL PRIMARY KEY, ruta_p12 VARCHAR(500) NOT NULL, titular VARCHAR(300), fecha_emision DATE, fecha_expiracion DATE NOT NULL, dias_restantes INTEGER NOT NULL, nivel_severidad VARCHAR(15) NOT NULL, ultima_verificacion TIMESTAMP NOT NULL DEFAULT now(), ultimo_email_enviado TIMESTAMP)",
            "CREATE INDEX IF NOT EXISTS idx_certificado_ruta ON certificado_estado(ruta_p12)"
        };
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException ignore) {}
        } catch (SQLException ignore) {}
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
            for (String sql : ddls) try { st.execute(sql); } catch (SQLException ignore) {}
        } catch (SQLException ignore) {}
        // HSQLDB fallback (SERIAL -> IDENTITY, partial index no soportado)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS configuracion_email ("+
                    "id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, host_smtp VARCHAR(150) NOT NULL, puerto_smtp INTEGER NOT NULL DEFAULT 587, "+
                    "usar_tls BOOLEAN NOT NULL DEFAULT true, email_remitente VARCHAR(150) NOT NULL, "+
                    "nombre_remitente VARCHAR(150) NOT NULL, usuario_smtp VARCHAR(150) NOT NULL, "+
                    "password_cifrado TEXT NOT NULL, reply_to VARCHAR(150), activo BOOLEAN NOT NULL DEFAULT true, "+
                    "actualizado_en TIMESTAMP NOT NULL DEFAULT now(), actualizado_por INTEGER)");
        } catch (SQLException ignore) {}
        // seed inicial con credencial hardcodeada previa si tabla vacía (no corta envíos el día del deploy)
        try (Connection con = getConnection(); java.sql.Statement st = con.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM configuracion_email WHERE activo=true")) {
            if (rs.next() && rs.getInt(1) == 0) {
                try {
                    String pwdCifrado = com.vendex.util.Cifrado.encriptar("awnfnmidbtqyyclz");
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
        } catch (SQLException ignore) {}
    }

    public static void setConnectionParams(String url, String user, String password) {
        URL.set(url);
        USER.set(user);
        PASSWORD.set(password);
        configLoaded = true;
    }

    public static void resetToDefault() {
        URL.set(DEFAULT_URL);
        USER.set(DEFAULT_USER);
        PASSWORD.set(DEFAULT_PASSWORD);
        configLoaded = false;
    }
}

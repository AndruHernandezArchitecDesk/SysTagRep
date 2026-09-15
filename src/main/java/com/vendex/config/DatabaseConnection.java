package com.vendex.config;

import java.sql.Connection;
import java.sql.DriverManager;
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

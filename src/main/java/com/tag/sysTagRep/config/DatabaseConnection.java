package com.tag.sysTagRep.config;

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
     * Carga ~/.systag/db.properties una vez al inicio. Llamado desde MainApp.start().
     * Si el archivo no existe lo crea con defaults (localhost). Para PC cliente
     * editar db.url a jdbc:postgresql://192.168.1.7:5432/dbTag.
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

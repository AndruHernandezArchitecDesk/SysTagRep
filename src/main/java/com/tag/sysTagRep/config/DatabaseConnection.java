package com.tag.sysTagRep.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/dbTag";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "admin";

    private static final ThreadLocal<String> URL = new ThreadLocal<>();
    private static final ThreadLocal<String> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> PASSWORD = new ThreadLocal<>();

    static {
        URL.set(DEFAULT_URL);
        USER.set(DEFAULT_USER);
        PASSWORD.set(DEFAULT_PASSWORD);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL.get(), USER.get(), PASSWORD.get());
    }

    public static void setConnectionParams(String url, String user, String password) {
        URL.set(url);
        USER.set(user);
        PASSWORD.set(password);
    }

    public static void resetToDefault() {
        URL.set(DEFAULT_URL);
        USER.set(DEFAULT_USER);
        PASSWORD.set(DEFAULT_PASSWORD);
    }
}

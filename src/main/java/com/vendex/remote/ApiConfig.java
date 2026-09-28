package com.vendex.remote;

import com.vendex.config.AppContext;

/**
 * Configura la conexión al backend REST.
 * Se lee de {@code -Dvendex.api.remote} / {@code API_MODE=remote} y {@code api.host}.
 */
public class ApiConfig {

    public static final String PROP_REMOTE = "vendex.api.remote";
    public static final String ENV_MODE = "API_MODE";
    public static final String PROP_HOST = "api.host";
    public static final String ENV_HOST = "API_HOST";
    public static final String DEFAULT_HOST = "http://localhost:7070";

    public static boolean isModoRemoto() {
        String v = System.getProperty(PROP_REMOTE, System.getenv(ENV_MODE));
        return "true".equalsIgnoreCase(v) || "remote".equalsIgnoreCase(v);
    }

    public static String host() {
        String h = System.getProperty(PROP_HOST, System.getenv(ENV_HOST));
        return h != null ? h : DEFAULT_HOST;
    }

    public static RestClient client() {
        return new RestClient(host());
    }

    public static String baseUrl() {
        return host() + "/api";
    }
}

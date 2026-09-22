package com.vendex.util;

import com.vendex.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cálculo puro de bloqueo anti fuerza bruta. Configurable vía tabla configuracion (no hardcodeado).
 * Defaults: 5 intentos, ventana 15min, duración 15min, backoff x2 hasta 24h.
 * Throttling progresivo: 1→0s,2→1s,3→2s,4→4s,5→8s→bloqueo.
 */
public final class PoliticaBloqueo {

    private static final Logger LOG = Logger.getLogger(PoliticaBloqueo.class.getName());

    // defaults si no hay fila en configuracion
    public static final int DEFAULT_INTENTOS_PERMITIDOS = 5;
    public static final int DEFAULT_VENTANA_MINUTOS = 15;
    public static final int DEFAULT_DURACION_MINUTOS = 15;
    public static final int DEFAULT_MAX_HORAS = 24;

    private static final long[] DEMORAS_MS = {0, 1000, 2000, 4000, 8000};

    private PoliticaBloqueo() {}

    public static int intentosPermitidos() { return leerInt("bloqueo.intentos_permitidos", DEFAULT_INTENTOS_PERMITIDOS); }
    public static int ventanaMinutos() { return leerInt("bloqueo.ventana_minutos", DEFAULT_VENTANA_MINUTOS); }
    public static int duracionMinutos() { return leerInt("bloqueo.duracion_minutos", DEFAULT_DURACION_MINUTOS); }
    public static int maxHoras() { return leerInt("bloqueo.max_horas", DEFAULT_MAX_HORAS); }

    private static int leerInt(String clave, int def) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT valor FROM configuracion WHERE clave=?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Integer.parseInt(rs.getString(1).trim());
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "No se pudo leer configuracion " + clave + ", usando default " + def, e);
        }
        return def;
    }

    /** Demora antes de permitir siguiente intento (server-side). intentoNum 1-indexed. */
    public static long demoraMs(int intentoNum) {
        if (intentoNum <= 1) return 0;
        if (intentoNum - 1 < DEMORAS_MS.length) return DEMORAS_MS[intentoNum - 1];
        return DEMORAS_MS[DEMORAS_MS.length - 1]; // 8s a partir del 5
    }

    /** Dentro de ventana de conteo? Si último fallo fue hace > ventana, se resetea. */
    public static boolean dentroVentana(LocalDateTime ultimoIntento) {
        if (ultimoIntento == null) return false;
        return ultimoIntento.isAfter(LocalDateTime.now().minusMinutes(ventanaMinutos()));
    }

    /** Calcula bloqueado_hasta con backoff exponencial. bloqueosPrevios = cuántos bloqueos ya sufrió esa cuenta (estimado). */
    public static LocalDateTime calcularBloqueadoHasta(int intentosActuales, int bloqueosPrevios) {
        int base = duracionMinutos();
        int maxMin = maxHoras() * 60;
        // bloqueosPrevios 0 → 15, 1→30, 2→60, 3→120...
        long minutos = (long) base * (1L << Math.min(bloqueosPrevios, 10));
        if (minutos > maxMin) minutos = maxMin;
        return LocalDateTime.now().plusMinutes(minutos);
    }

    /** Versión simple sin historial de bloqueos previos: usa bloqueosPrevios=0 y si intentos>=umbral duplica por exceso. */
    public static LocalDateTime calcularBloqueadoHastaSimple(int intentosFallidos) {
        int umbral = intentosPermitidos();
        int exceso = Math.max(0, intentosFallidos - umbral);
        return calcularBloqueadoHasta(intentosFallidos, exceso);
    }

    /** Minutos restantes de bloqueo (ceil). */
    public static long minutosRestantes(LocalDateTime bloqueadoHasta) {
        if (bloqueadoHasta == null) return 0;
        Duration d = Duration.between(LocalDateTime.now(), bloqueadoHasta);
        if (d.isNegative() || d.isZero()) return 0;
        return (d.toSeconds() + 59) / 60;
    }
}

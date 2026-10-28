package com.vendex.model;

import java.util.List;

/** DTOs compartidos por JDBC, REST y el panel. Tiempos UTC en milisegundos. */
public final class CuotaIA {
    private CuotaIA() {}
    public record Config(String modelo, String nivel, boolean habilitado, long rpm, long tpm, long rpd,
                         int maxSalida, boolean ahorro, java.math.BigDecimal precioEntrada, java.math.BigDecimal precioSalida) {
        public void validar() {
            if (!MODELOS.contains(modelo) || !("Gratuito".equals(nivel) || "Pago".equals(nivel))
                    || rpm < 0 || tpm < 0 || rpd < 0 || maxSalida < 256 || maxSalida > 8192
                    || (precioEntrada != null && precioEntrada.signum() < 0) || (precioSalida != null && precioSalida.signum() < 0))
                throw new IllegalArgumentException("Configuración inválida: límites >= 0 y salida entre 256 y 8192.");
        }
    }
    public static final List<String> MODELOS = List.of("gemini-3.1-flash-lite", "gemini-2.5-flash",
            "gemini-flash-latest", "gemini-flash-lite-latest");
    public record Reserva(String id, String modelo, String operacion, long estimados) {}
    public record Permiso(boolean permitido, String mensaje, int maxSalida, boolean ahorro) {}
    public record Fin(String id, int http, long duracion, long entrada, long salida, long pensamiento,
                      long total, long pausaMs, String detalle) {}
    public record Intento(String id, String modelo, String operacion, long fecha, int http, long duracion,
                          long entrada, long salida, long pensamiento, long total, String detalle, java.math.BigDecimal costo) {}
    public record Estado(Config config, long minuto, long dia, long tokensMinuto, long pausaHasta) {}
    public record Panel(List<Estado> modelos, List<Intento> intentos) {}
}

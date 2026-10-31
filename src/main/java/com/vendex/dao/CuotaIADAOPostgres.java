package com.vendex.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.config.DatabaseConnection;
import com.vendex.model.CuotaIA.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Reservas serializadas por modelo: todas las PCs cuentan antes de enviar a Google. */
public class CuotaIADAOPostgres implements CuotaIADAO {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void prepararEsquema() throws Exception {
        try (var in = CuotaIADAOPostgres.class.getResourceAsStream("/db/migration/V41__cuotas_ia.sql");
             var c = DatabaseConnection.getConnection(); var s = c.createStatement()) {
            String sql = new String(Objects.requireNonNull(in).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String parte : sql.split(";")) if (!parte.isBlank()) s.execute(parte);
            if (c.getMetaData().getDatabaseProductName().contains("PostgreSQL")) {
                try { s.execute("GRANT SELECT, INSERT, UPDATE ON ia_cuota, ia_solicitud TO app_vendex"); }
                catch (SQLException e) { java.util.logging.Logger.getLogger(CuotaIADAOPostgres.class.getName()).fine(e.getMessage()); }
            }
        }
    }

    private long ahora(Connection c) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT CURRENT_TIMESTAMP")) {
            r.next(); return r.getTimestamp(1).getTime();
        }
    }
    private Estado estado(Connection c, String modelo, boolean bloqueo) throws Exception {
        Config cfg; long pausa;
        try (var s = c.prepareStatement("SELECT configuracion, pausa_hasta FROM ia_cuota WHERE modelo=?" + (bloqueo ? " FOR UPDATE" : ""))) {
            s.setString(1, modelo);
            try (var r = s.executeQuery()) {
                if (!r.next()) throw new IllegalStateException("Modelo sin configuración de cuotas");
                cfg = JSON.readValue(r.getString(1), Config.class); pausa = r.getLong(2);
            }
        }
        long ahora = ahora(c);
        long dia = Instant.ofEpochMilli(ahora).atZone(ZoneId.of("America/Los_Angeles"))
                .toLocalDate().atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli();
        try (var s = c.prepareStatement("SELECT COUNT(*), COALESCE(SUM(CASE WHEN fecha>? THEN 1 ELSE 0 END),0), "
                + "COALESCE(SUM(CASE WHEN fecha>? THEN estimados ELSE 0 END),0) FROM ia_solicitud WHERE modelo=? AND fecha>=?")) {
            s.setLong(1, ahora - 60000); s.setLong(2, ahora - 60000); s.setString(3, modelo);
            // Incluir minuto anterior aunque cruce el límite del día.
            s.setLong(4, Math.min(dia, ahora - 60000));
            try (var r = s.executeQuery()) {
                r.next(); long minuto = r.getLong(2), tokens = r.getLong(3);
                try (var d = c.prepareStatement("SELECT COUNT(*) FROM ia_solicitud WHERE modelo=? AND fecha>=?")) {
                    d.setString(1, modelo); d.setLong(2, dia);
                    try (var dr = d.executeQuery()) { dr.next(); return new Estado(cfg, minuto, dr.getLong(1), tokens, pausa); }
                }
            }
        }
    }
    @Override public Permiso reservar(Reserva req) throws Exception {
        UUID.fromString(req.id());
        if (req.estimados() < 0 || req.operacion() == null || req.operacion().length() > 100)
            throw new IllegalArgumentException("Reserva inválida");
        try (var c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Estado e = estado(c, req.modelo(), true); Config cfg = e.config();
                // Un reenvío REST de la reserva no autoriza una segunda llamada.
                try (var s = c.prepareStatement("SELECT id FROM ia_solicitud WHERE id=?")) {
                    s.setString(1, req.id());
                    try (var r = s.executeQuery()) { if (r.next()) return new Permiso(false, "Reserva ya utilizada", cfg.maxSalida(), cfg.ahorro()); }
                }
                String motivo = !cfg.habilitado() ? "Modelo deshabilitado por el administrador"
                        : e.pausaHasta() > ahora(c) ? mensajePausa(c, e)
                        : cfg.rpm() > 0 && e.minuto() >= cfg.rpm() ? "Límite local RPM alcanzado"
                        : cfg.rpd() > 0 && e.dia() >= cfg.rpd() ? "Límite local RPD alcanzado (día de Los Ángeles)"
                        : cfg.tpm() > 0 && req.estimados() > cfg.tpm() - e.tokensMinuto() ? "Límite local TPM estimado alcanzado" : "";
                if (!motivo.isEmpty()) { c.rollback(); return new Permiso(false, motivo, cfg.maxSalida(), cfg.ahorro()); }
                try (var s = c.prepareStatement("INSERT INTO ia_solicitud(id,modelo,operacion,fecha,estimados) VALUES(?,?,?,?,?)")) {
                    s.setString(1, req.id()); s.setString(2, req.modelo()); s.setString(3, req.operacion());
                    s.setLong(4, ahora(c)); s.setLong(5, req.estimados()); s.executeUpdate();
                }
                c.commit(); return new Permiso(true, "", cfg.maxSalida(), cfg.ahorro());
            } catch (Exception ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        }
    }
    @Override public void finalizar(Fin f) throws Exception {
        if (f.entrada() < 0 || f.salida() < 0 || f.pensamiento() < 0 || f.total() < 0 || f.pausaMs() < 0)
            throw new IllegalArgumentException("Medición inválida");
        try (var c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String modelo;
                try (var s = c.prepareStatement("SELECT modelo FROM ia_solicitud WHERE id=?")) {
                    s.setString(1, f.id()); try (var r = s.executeQuery()) { if (!r.next()) throw new IllegalArgumentException("Reserva desconocida"); modelo = r.getString(1); }
                }
                Config cfg = estado(c, modelo, true).config();
                java.math.BigDecimal costo = null;
                if (f.http() == 200 && cfg.precioEntrada() != null && cfg.precioSalida() != null) {
                    costo = cfg.precioEntrada().multiply(java.math.BigDecimal.valueOf(f.entrada()))
                            .add(cfg.precioSalida().multiply(java.math.BigDecimal.valueOf(f.salida() + f.pensamiento())))
                            .divide(java.math.BigDecimal.valueOf(1000000), 8, java.math.RoundingMode.HALF_UP);
                }
                int cambios;
                try (var s = c.prepareStatement("UPDATE ia_solicitud SET http=?,duracion=?,entrada=?,salida=?,pensamiento=?,total=?,detalle=?,costo=? WHERE id=? AND http=-1")) {
                    s.setInt(1, f.http()); s.setLong(2, Math.max(0, f.duracion())); s.setLong(3, f.entrada()); s.setLong(4, f.salida());
                    s.setLong(5, f.pensamiento()); s.setLong(6, f.total());
                    s.setString(7, f.detalle() == null ? "" : f.detalle().substring(0, Math.min(300, f.detalle().length())));
                    s.setBigDecimal(8, costo); s.setString(9, f.id()); cambios = s.executeUpdate();
                }
                if (cambios > 0 && f.http() == 429) {
                    long hasta = ahora(c) + (f.pausaMs() == 0 ? 60000 : Math.max(1000, Math.min(f.pausaMs(), 7 * 86400000L)));
                    try (var s = c.prepareStatement("UPDATE ia_cuota SET pausa_hasta=CASE WHEN pausa_hasta>? THEN pausa_hasta ELSE ? END WHERE modelo=?")) {
                        s.setLong(1, hasta); s.setLong(2, hasta); s.setString(3, modelo); s.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        }
    }
    private String mensajePausa(Connection c, Estado e) throws SQLException {
        String detalle = "Cuota Google no especificada";
        try (var s = c.prepareStatement("SELECT detalle FROM ia_solicitud WHERE modelo=? AND http=429 ORDER BY fecha DESC LIMIT 1")) {
            s.setString(1, e.config().modelo());
            try (var r = s.executeQuery()) {
                if (r.next() && r.getString(1) != null && !r.getString(1).isBlank()) detalle = r.getString(1);
            }
        }
        return "Modelo " + e.config().modelo() + " en pausa compartida por respuesta 429 de Google. " + detalle
                + ". Reintento no antes de " + Instant.ofEpochMilli(e.pausaHasta())
                + " (UTC). No es un límite local de Vendex; la cuota de Google puede seguir agotada al terminar la espera.";
    }
    @Override public Panel panel() throws Exception {
        try (var c = DatabaseConnection.getConnection()) {
            List<Estado> estados = new ArrayList<>();
            for (String modelo : com.vendex.model.CuotaIA.MODELOS) estados.add(estado(c, modelo, false));
            List<Intento> intentos = new ArrayList<>();
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT * FROM ia_solicitud ORDER BY fecha DESC LIMIT 200")) {
                while (r.next()) intentos.add(new Intento(r.getString("id"), r.getString("modelo"), r.getString("operacion"),
                        r.getLong("fecha"), r.getInt("http"), r.getLong("duracion"), r.getLong("entrada"), r.getLong("salida"),
                        r.getLong("pensamiento"), r.getLong("total"), r.getString("detalle"), r.getBigDecimal("costo")));
            }
            return new Panel(estados, intentos);
        }
    }
    @Override public void guardar(Config cfg) throws Exception {
        cfg.validar();
        try (var c = DatabaseConnection.getConnection(); var s = c.prepareStatement("UPDATE ia_cuota SET configuracion=? WHERE modelo=?")) {
            s.setString(1, JSON.writeValueAsString(cfg)); s.setString(2, cfg.modelo());
            if (s.executeUpdate() != 1) throw new IllegalStateException("Modelo desconocido");
        }
    }
}

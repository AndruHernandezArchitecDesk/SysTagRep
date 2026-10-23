package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.ComprobantePendienteSri;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComprobantePendienteSriDAOPostgres implements ComprobantePendienteSriDAO {

    private static final Logger LOG = Logger.getLogger(ComprobantePendienteSriDAO.class.getName());

    public void encolar(String tipoComprobante, String claveAcceso, String numeroComprobante, String ambiente, String mensaje) {
        if (claveAcceso == null || claveAcceso.isBlank()) return;
        // resolver comprobante_id si existe
        Integer compId = null;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM comprobantes_electronicos WHERE clave_acceso = ? LIMIT 1")) {
            ps.setString(1, claveAcceso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) compId = rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "No se pudo resolver comprobante_id para " + claveAcceso, e);
        }
        // insert o actualizar si ya existe
        String sql = "INSERT INTO comprobante_pendiente_sri (comprobante_id, tipo_comprobante, clave_acceso, numero_comprobante, ambiente, intentos, ultimo_intento, proximo_intento, estado, ultimo_mensaje_sri) " +
                     "VALUES (?, ?, ?, ?, ?, 0, NULL, NOW(), 'PENDIENTE', ?) " +
                     "ON CONFLICT (clave_acceso) DO UPDATE SET estado='PENDIENTE', ultimo_mensaje_sri=EXCLUDED.ultimo_mensaje_sri, proximo_intento=NOW() WHERE comprobante_pendiente_sri.estado IN ('PENDIENTE','AGOTADA','RECHAZADA')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (compId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, compId);
            ps.setString(2, tipoComprobante);
            ps.setString(3, claveAcceso);
            ps.setString(4, numeroComprobante);
            ps.setString(5, ambiente);
            ps.setString(6, mensaje);
            ps.executeUpdate();
        } catch (SQLException e) {
            // HSQLDB no soporta ON CONFLICT — fallback
            try (Connection con = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT id FROM comprobante_pendiente_sri WHERE clave_acceso = ?")) {
                    ps.setString(1, claveAcceso);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            try (PreparedStatement upd = con.prepareStatement("UPDATE comprobante_pendiente_sri SET estado='PENDIENTE', ultimo_mensaje_sri=?, proximo_intento=NOW() WHERE clave_acceso=? AND estado IN ('PENDIENTE','AGOTADA','RECHAZADA')")) {
                                upd.setString(1, mensaje);
                                upd.setString(2, claveAcceso);
                                upd.executeUpdate();
                                return;
                            }
                        }
                    }
                }
                try (PreparedStatement ins = con.prepareStatement("INSERT INTO comprobante_pendiente_sri (comprobante_id, tipo_comprobante, clave_acceso, numero_comprobante, ambiente, intentos, proximo_intento, estado, ultimo_mensaje_sri) VALUES (?,?,?,?,?,0,NOW(),'PENDIENTE',?)")) {
                    if (compId == null) ins.setNull(1, Types.INTEGER); else ins.setInt(1, compId);
                    ins.setString(2, tipoComprobante);
                    ins.setString(3, claveAcceso);
                    ins.setString(4, numeroComprobante);
                    ins.setString(5, ambiente);
                    ins.setString(6, mensaje);
                    ins.executeUpdate();
                }
            } catch (SQLException e2) {
                LOG.log(Level.WARNING, "Error encolando " + claveAcceso, e2);
            }
        }
    }

    public void encolarFactura(String clave, String numero, String ambiente) {
        encolar("FACTURA", clave, numero, ambiente, "Pendiente autorización SRI");
    }

    public void encolar(String tipoComprobante, String claveAcceso, String numeroComprobante, String ambiente) {
        encolar(tipoComprobante, claveAcceso, numeroComprobante, ambiente, "Pendiente SRI - en cola contingencia");
    }

    public List<ComprobantePendienteSri> listarParaReintentar(int limite) {
        List<ComprobantePendienteSri> lista = new ArrayList<>();
        String sql = "SELECT id, comprobante_id, tipo_comprobante, clave_acceso, numero_comprobante, ambiente, intentos, ultimo_intento, proximo_intento, estado, ultimo_mensaje_sri FROM comprobante_pendiente_sri WHERE estado='PENDIENTE' AND proximo_intento <= NOW() ORDER BY proximo_intento ASC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "listarParaReintentar", e);
        }
        return lista;
    }

    public List<ComprobantePendienteSri> listarTodos(int limite) {
        List<ComprobantePendienteSri> lista = new ArrayList<>();
        String sql = "SELECT id, comprobante_id, tipo_comprobante, clave_acceso, numero_comprobante, ambiente, intentos, ultimo_intento, proximo_intento, estado, ultimo_mensaje_sri FROM comprobante_pendiente_sri ORDER BY proximo_intento DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "listarTodos", e);
        }
        return lista;
    }

    public int contarPendientes() {
        String sql = "SELECT COUNT(*) FROM comprobante_pendiente_sri WHERE estado='PENDIENTE'";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOG.log(Level.FINE, "contarPendientes", e);
        }
        return 0;
    }

    public void marcarResultado(int id, String estado, String mensaje, LocalDateTime proximoIntento, int intentos) {
        String sql = "UPDATE comprobante_pendiente_sri SET estado=?, ultimo_mensaje_sri=?, proximo_intento=?, intentos=?, ultimo_intento=NOW() WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            if (proximoIntento == null) ps.setNull(3, Types.TIMESTAMP); else ps.setTimestamp(3, Timestamp.valueOf(proximoIntento));
            ps.setInt(4, intentos);
            ps.setInt(5, id);
            try {
                ps.executeUpdate();
            } catch (SQLException e) {
                if (!esExcesoLargo(e)) throw e;
                // BD sin migrar a TEXT: reintentar recortado.
                try (PreparedStatement ps2 = con.prepareStatement(sql)) {
                    ps2.setString(1, estado);
                    ps2.setString(2, truncar(mensaje));
                    if (proximoIntento == null) ps2.setNull(3, Types.TIMESTAMP); else ps2.setTimestamp(3, Timestamp.valueOf(proximoIntento));
                    ps2.setInt(4, intentos);
                    ps2.setInt(5, id);
                    ps2.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "marcarResultado", e);
        }
    }

    public void marcarReintento(int id, int intentos, LocalDateTime proximo, String mensaje) {
        marcarResultado(id, "PENDIENTE", mensaje, proximo, intentos);
    }

    public void forzarReintentoAhora(String clave) {
        String sql = "UPDATE comprobante_pendiente_sri SET proximo_intento=NOW(), estado='PENDIENTE' WHERE clave_acceso=? AND estado IN ('PENDIENTE','AGOTADA','RECHAZADA')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "forzarReintentoAhora", e);
        }
    }

    @Override
    public boolean anularLocal(String claveAcceso, String mensaje) {
        String sql = "UPDATE comprobante_pendiente_sri SET estado='ANULADO', ultimo_mensaje_sri=? "
                + "WHERE clave_acceso=? AND estado IN ('PENDIENTE','AGOTADA')";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, mensaje);
            ps.setString(2, claveAcceso);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "anularLocal " + claveAcceso, e);
            throw new IllegalStateException("No se pudo anular localmente la tarea de reintento SRI", e);
        }
    }

    @Override
    public boolean siguePendiente(int id) {
        String sql = "SELECT 1 FROM comprobante_pendiente_sri WHERE id=? AND estado='PENDIENTE'";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "siguePendiente " + id, e);
            return false;
        }
    }

    private ComprobantePendienteSri mapear(ResultSet rs) throws SQLException {
        ComprobantePendienteSri c = new ComprobantePendienteSri();
        c.setId(rs.getInt("id"));
        int cid = rs.getInt("comprobante_id");
        if (!rs.wasNull()) c.setComprobanteId(cid);
        c.setTipoComprobante(rs.getString("tipo_comprobante"));
        c.setClaveAcceso(rs.getString("clave_acceso"));
        c.setNumeroComprobante(rs.getString("numero_comprobante"));
        c.setAmbiente(rs.getString("ambiente"));
        c.setIntentos(rs.getInt("intentos"));
        Timestamp ultimo = rs.getTimestamp("ultimo_intento");
        if (ultimo != null) c.setUltimoIntento(ultimo.toLocalDateTime());
        Timestamp prox = rs.getTimestamp("proximo_intento");
        if (prox != null) c.setProximoIntento(prox.toLocalDateTime());
        c.setEstado(rs.getString("estado"));
        c.setUltimoMensajeSri(rs.getString("ultimo_mensaje_sri"));
        return c;
    }

    /** Cinturon para ultimo_mensaje_sri VARCHAR(500) en BDs aun sin migrar a TEXT. */
    private static String truncar(String mensaje) {
        if (mensaje == null) return null;
        return mensaje.length() <= 450 ? mensaje : mensaje.substring(0, 450) + "...[truncado]";
    }

    /** SQLState 22001 = string data right truncation (columna VARCHAR corta). */
    private static boolean esExcesoLargo(SQLException e) {
        if (e == null) return false;
        if ("22001".equals(e.getSQLState())) return true;
        String m = e.getMessage();
        return m != null && (m.contains("too long") || m.contains("demasiado largo"));
    }
}

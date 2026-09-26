package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaCreditoRegistro;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotaCreditoRegistroDAOPostgres implements NotaCreditoRegistroDAO {

    private static final Logger LOGGER = Logger.getLogger(NotaCreditoRegistroDAO.class.getName());

    public int insertar(NotaCreditoRegistro nc) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertar(con, nc);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.insertar", e);
        }
        return -1;
    }

    public int insertar(Connection con, NotaCreditoRegistro nc) throws SQLException {
        String sql = "INSERT INTO nota_credito_registro(clave_acceso, factura_registro_id, establecimiento, punto_emision, secuencial, " +
                "fecha_emision, cliente_id, motivo, tipo_motivo, total_sin_impuestos, valor_iva, valor_modificacion, reingresa_stock, estado_sri, mensaje_sri, numero_autorizacion, fecha_autorizacion, xml_firmado, usuario_id, sucursal_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nc.getClaveAcceso());
            ps.setInt(2, nc.getFacturaRegistroId());
            ps.setString(3, nc.getEstablecimiento());
            ps.setString(4, nc.getPuntoEmision());
            ps.setString(5, nc.getSecuencial());
            ps.setObject(6, nc.getFechaEmision());
            ps.setInt(7, nc.getClienteId());
            ps.setString(8, nc.getMotivo());
            ps.setString(9, nc.getTipoMotivo());
            ps.setBigDecimal(10, nc.getTotalSinImpuestos());
            ps.setBigDecimal(11, nc.getValorIva());
            ps.setBigDecimal(12, nc.getValorModificacion());
            ps.setBoolean(13, nc.isReingresaStock());
            ps.setString(14, nc.getEstadoSri());
            ps.setString(15, nc.getMensajeSri());
            ps.setString(16, nc.getNumeroAutorizacion());
            ps.setObject(17, nc.getFechaAutorizacion());
            ps.setString(18, nc.getXmlFirmado());
            ps.setInt(19, nc.getUsuarioId());
            ps.setInt(20, nc.getSucursalId() > 0 ? nc.getSucursalId() : 1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException {
        String sql = "UPDATE nota_credito_registro SET estado_sri=?, mensaje_sri=?, numero_autorizacion=?, fecha_autorizacion=? WHERE clave_acceso=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, numeroAutorizacion);
            ps.setTimestamp(4, parsearFecha(fechaAutorizacion));
            ps.setString(5, claveAcceso);
            ps.executeUpdate();
        }
    }

    public NotaCreditoRegistro obtenerPorClave(String claveAcceso) {
        String sql = "SELECT ncr.*, c.nombre AS nombre_cliente FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id WHERE ncr.clave_acceso=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, claveAcceso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.obtenerPorClave", e);
        }
        return null;
    }

    public NotaCreditoRegistro obtenerPorId(int id) {
        String sql = "SELECT ncr.*, c.nombre AS nombre_cliente FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id WHERE ncr.id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.obtenerPorId", e);
        }
        return null;
    }

    public List<NotaCreditoRegistro> listarPorFactura(int facturaRegistroId) {
        List<NotaCreditoRegistro> lista = new ArrayList<>();
        String sql = "SELECT ncr.*, c.nombre AS nombre_cliente FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id WHERE ncr.factura_registro_id=? ORDER BY ncr.creado_en DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facturaRegistroId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.listarPorFactura", e);
        }
        return lista;
    }

    public BigDecimal sumarValorModificacionPorFactura(int facturaRegistroId, String estadoFiltro) {
        String sql = "SELECT COALESCE(SUM(valor_modificacion),0) FROM nota_credito_registro WHERE factura_registro_id=? AND estado_sri=?";
        if (estadoFiltro == null) sql = "SELECT COALESCE(SUM(valor_modificacion),0) FROM nota_credito_registro WHERE factura_registro_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facturaRegistroId);
            if (estadoFiltro != null) ps.setString(2, estadoFiltro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                // tabla aún no migrada (fresh install) - intentar auto-migrar una vez
                DatabaseConnection.ensureNotaCreditoSchema();
            } else {
                LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.sumarValorModificacionPorFactura", e);
            }
        }
        return BigDecimal.ZERO;
    }

    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        String sql = "UPDATE nota_credito_registro SET estado_sri=?, mensaje_sri=?, numero_autorizacion=?, fecha_autorizacion=? WHERE clave_acceso=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, numeroAutorizacion);
            ps.setTimestamp(4, parsearFecha(fechaAutorizacion));
            ps.setString(5, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.actualizarEstado", e);
        }
    }

    public void actualizarXmlFirmado(String claveAcceso, String xmlFirmado) {
        String sql = "UPDATE nota_credito_registro SET xml_firmado=? WHERE clave_acceso=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, xmlFirmado);
            ps.setString(2, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.actualizarXmlFirmado", e);
        }
    }

    public List<NotaCreditoRegistro> listarPendientesSri() {
        List<NotaCreditoRegistro> lista = new ArrayList<>();
        String sql = "SELECT ncr.*, c.nombre AS nombre_cliente FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id WHERE ncr.estado_sri IN ('PENDIENTE','ERROR') ORDER BY ncr.id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.listarPendientesSri", e);
        }
        return lista;
    }

    public List<NotaCreditoRegistro> listarPaginado(int page, int pageSize, String filtro) {
        List<NotaCreditoRegistro> lista = new ArrayList<>();
        String base = "SELECT ncr.*, c.nombre AS nombre_cliente FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id ";
        String where = buildWhere(filtro);
        String sql = base + where + " ORDER BY ncr.creado_en DESC LIMIT ? OFFSET ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = setFilter(ps, filtro, 1);
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.listarPaginado", e);
        }
        return lista;
    }

    public int contar(String filtro) {
        String sql = "SELECT COUNT(*) FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id " + buildWhere(filtro);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setFilter(ps, filtro, 1);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.contar", e); }
        return 0;
    }

    public List<NotaCreditoRegistro> listarTodas() {
        List<NotaCreditoRegistro> lista = new ArrayList<>();
        String sql = "SELECT ncr.*, c.nombre AS nombre_cliente FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id ORDER BY ncr.creado_en DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error en NotaCreditoRegistroDAO.listarTodas", e); }
        return lista;
    }

    private String buildWhere(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) return "";
        return "WHERE (LOWER(ncr.clave_acceso) LIKE ? OR LOWER(ncr.secuencial) LIKE ? OR LOWER(c.nombre) LIKE ? OR LOWER(COALESCE(ncr.estado_sri,'')) LIKE ? OR LOWER(COALESCE(ncr.motivo,'')) LIKE ?)";
    }

    private int setFilter(PreparedStatement ps, String filtro, int start) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) return start;
        String like = "%" + filtro.toLowerCase() + "%";
        int idx = start;
        for (int i=0;i<5;i++) ps.setString(idx++, like);
        return idx;
    }

    private NotaCreditoRegistro mapear(ResultSet rs) throws SQLException {
        NotaCreditoRegistro n = new NotaCreditoRegistro();
        n.setId(rs.getInt("id"));
        n.setClaveAcceso(rs.getString("clave_acceso"));
        n.setFacturaRegistroId(rs.getInt("factura_registro_id"));
        n.setEstablecimiento(rs.getString("establecimiento"));
        n.setPuntoEmision(rs.getString("punto_emision"));
        n.setSecuencial(rs.getString("secuencial"));
        // numComprobante derivado si no existe columna
        try { n.setNumComprobante(rs.getString("establecimiento") + "-" + rs.getString("punto_emision") + "-" + rs.getString("secuencial")); } catch (Exception ignore) {}
        Timestamp fe = rs.getTimestamp("fecha_emision");
        if (fe != null) n.setFechaEmision(fe.toLocalDateTime());
        n.setClienteId(rs.getInt("cliente_id"));
        n.setMotivo(rs.getString("motivo"));
        n.setTipoMotivo(rs.getString("tipo_motivo"));
        n.setTotalSinImpuestos(rs.getBigDecimal("total_sin_impuestos"));
        n.setValorIva(rs.getBigDecimal("valor_iva"));
        n.setValorModificacion(rs.getBigDecimal("valor_modificacion"));
        n.setReingresaStock(rs.getBoolean("reingresa_stock"));
        n.setEstadoSri(rs.getString("estado_sri"));
        n.setMensajeSri(rs.getString("mensaje_sri"));
        n.setNumeroAutorizacion(rs.getString("numero_autorizacion"));
        Timestamp fa = rs.getTimestamp("fecha_autorizacion");
        if (fa != null) n.setFechaAutorizacion(fa.toLocalDateTime());
        n.setXmlFirmado(rs.getString("xml_firmado"));
        n.setUsuarioId(rs.getInt("usuario_id"));
        try { n.setSucursalId(rs.getInt("sucursal_id")); if (rs.wasNull()) n.setSucursalId(1); } catch (Exception ignore) { n.setSucursalId(1); }
        Timestamp ce = rs.getTimestamp("creado_en");
        if (ce != null) n.setCreadoEn(ce.toLocalDateTime());
        try { n.setNombreCliente(rs.getString("nombre_cliente")); } catch (SQLException ignore) {}
        return n;
    }

    private Timestamp parsearFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return null;
        try {
            String v = fecha.trim().replace("Z", "+00:00");
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(v, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Timestamp.valueOf(odt.atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime());
        } catch (Exception e) { return null; }
    }
}

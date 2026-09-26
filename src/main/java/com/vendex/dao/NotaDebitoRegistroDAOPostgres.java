package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaDebitoRegistro;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vendex.util.AppConstants;

public class NotaDebitoRegistroDAOPostgres implements NotaDebitoRegistroDAO {

    private static final Logger LOGGER = Logger.getLogger(NotaDebitoRegistroDAO.class.getName());

    public int insertar(NotaDebitoRegistro nd) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertar(con, nd);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.insertar", e);
        }
        return -1;
    }

    public int insertar(Connection con, NotaDebitoRegistro nd) throws SQLException {
        String sql = "INSERT INTO nota_debito_registro(clave_acceso, factura_registro_id, establecimiento, punto_emision, secuencial, " +
                "fecha_emision, cliente_id, forma_pago, total_sin_impuestos, valor_iva, valor_total, " +
                "estado_sri, mensaje_sri, numero_autorizacion, fecha_autorizacion, xml_firmado, usuario_id, sucursal_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nd.getClaveAcceso());
            ps.setInt(2, nd.getFacturaRegistroId());
            ps.setString(3, nd.getEstablecimiento());
            ps.setString(4, nd.getPuntoEmision());
            ps.setString(5, nd.getSecuencial());
            ps.setObject(6, nd.getFechaEmision());
            ps.setInt(7, nd.getClienteId());
            ps.setString(8, nd.getFormaPago());
            ps.setBigDecimal(9, nd.getTotalSinImpuestos());
            ps.setBigDecimal(10, nd.getValorIva());
            ps.setBigDecimal(11, nd.getValorTotal());
            ps.setString(12, nd.getEstadoSri());
            ps.setString(13, nd.getMensajeSri());
            ps.setString(14, nd.getNumeroAutorizacion());
            ps.setObject(15, nd.getFechaAutorizacion());
            ps.setString(16, nd.getXmlFirmado());
            ps.setInt(17, nd.getUsuarioId());
            ps.setInt(18, nd.getSucursalId() > 0 ? nd.getSucursalId() : 1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException {
        String sql = "UPDATE nota_debito_registro SET estado_sri=?, mensaje_sri=?, numero_autorizacion=?, fecha_autorizacion=? WHERE clave_acceso=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, numeroAutorizacion);
            ps.setTimestamp(4, parsearFechaAutorizacion(fechaAutorizacion));
            ps.setString(5, claveAcceso);
            ps.executeUpdate();
        }
    }

    public NotaDebitoRegistro obtenerPorClave(String claveAcceso) {
        String sql = "SELECT ndr.*, c.nombre AS nombre_cliente FROM nota_debito_registro ndr LEFT JOIN cliente c ON c.id=ndr.cliente_id WHERE ndr.clave_acceso=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, claveAcceso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.obtenerPorClave", e);
        }
        return null;
    }

    public NotaDebitoRegistro obtenerPorId(int id) {
        String sql = "SELECT ndr.*, c.nombre AS nombre_cliente FROM nota_debito_registro ndr LEFT JOIN cliente c ON c.id=ndr.cliente_id WHERE ndr.id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.obtenerPorId", e);
        }
        return null;
    }

    public List<NotaDebitoRegistro> listarPorFactura(int facturaRegistroId) {
        List<NotaDebitoRegistro> lista = new ArrayList<>();
        String sql = "SELECT ndr.*, c.nombre AS nombre_cliente FROM nota_debito_registro ndr LEFT JOIN cliente c ON c.id=ndr.cliente_id WHERE ndr.factura_registro_id=? ORDER BY ndr.creado_en DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facturaRegistroId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.listarPorFactura", e);
        }
        return lista;
    }

    public BigDecimal sumarValorTotalPorFactura(int facturaRegistroId, String estadoFiltro) {
        String sql = "SELECT COALESCE(SUM(valor_total),0) FROM nota_debito_registro WHERE factura_registro_id=? AND estado_sri=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facturaRegistroId);
            ps.setString(2, estadoFiltro != null ? estadoFiltro : AppConstants.ESTADO_AUTORIZADO);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.sumarValorTotalPorFactura", e);
        }
        return BigDecimal.ZERO;
    }

    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) {
        String sql = "UPDATE nota_debito_registro SET estado_sri=?, mensaje_sri=?, numero_autorizacion=?, fecha_autorizacion=? WHERE clave_acceso=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, mensaje);
            ps.setString(3, numeroAutorizacion);
            ps.setTimestamp(4, parsearFechaAutorizacion(fechaAutorizacion));
            ps.setString(5, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.actualizarEstado", e);
        }
    }

    public List<NotaDebitoRegistro> listarPendientesSri() {
        List<NotaDebitoRegistro> lista = new ArrayList<>();
        String sql = "SELECT ndr.*, c.nombre AS nombre_cliente FROM nota_debito_registro ndr LEFT JOIN cliente c ON c.id=ndr.cliente_id WHERE ndr.estado_sri IN ('PENDIENTE','ERROR') ORDER BY ndr.id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoRegistroDAO.listarPendientesSri", e);
        }
        return lista;
    }

    private NotaDebitoRegistro mapear(ResultSet rs) throws SQLException {
        NotaDebitoRegistro n = new NotaDebitoRegistro();
        n.setId(rs.getInt("id"));
        n.setClaveAcceso(rs.getString("clave_acceso"));
        n.setFacturaRegistroId(rs.getInt("factura_registro_id"));
        n.setEstablecimiento(rs.getString("establecimiento"));
        n.setPuntoEmision(rs.getString("punto_emision"));
        n.setSecuencial(rs.getString("secuencial"));
        Timestamp fe = rs.getTimestamp("fecha_emision");
        if (fe != null) n.setFechaEmision(fe.toLocalDateTime());
        n.setClienteId(rs.getInt("cliente_id"));
        n.setFormaPago(rs.getString("forma_pago"));
        n.setTotalSinImpuestos(rs.getBigDecimal("total_sin_impuestos"));
        n.setValorIva(rs.getBigDecimal("valor_iva"));
        n.setValorTotal(rs.getBigDecimal("valor_total"));
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
        try { n.setNumComprobante(rs.getString("establecimiento") + "-" + rs.getString("punto_emision") + "-" + rs.getString("secuencial")); } catch (Exception ignore) {}
        return n;
    }

    private Timestamp parsearFechaAutorizacion(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return null;
        try {
            String v = fecha.trim().replace("Z", "+00:00");
            OffsetDateTime odt = OffsetDateTime.parse(v, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return Timestamp.from(odt.toInstant());
        } catch (Exception e) { return null; }
    }
}

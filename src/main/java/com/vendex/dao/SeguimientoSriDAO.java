package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.SeguimientoSriItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consulta unificada del Seguimiento SRI (FACTURA, NC, ND, GUIA, RETENCION).
 * Orden descendente: lo más reciente primero (fecha DESC).
 * Filtro por tipo: TODOS, FACTURA, NC, ND, GUIA, RETENCION.
 */
public class SeguimientoSriDAO {

    private static final Logger LOGGER = Logger.getLogger(SeguimientoSriDAO.class.getName());

    private static final String Q_FACTURA =
            "SELECT 'FACTURA' AS tipo, fr.id AS registro_id, fr.codigo AS codigo, fr.num_comprobante AS num_comprobante, "
            + "fr.fecha AS fecha, c.nombre AS cliente, fr.forma_pago AS forma_pago, fr.subtotal AS subtotal, fr.iva AS iva, fr.total AS total, "
            + "COALESCE(ce.estado_sri, fr.estado_sri) AS estado, COALESCE(ce.mensaje_sri, '') AS mensaje, fr.clave_acceso AS clave_acceso, fr.cliente_id AS ref_id "
            + "FROM factura_registro fr LEFT JOIN cliente c ON c.id=fr.cliente_id "
            + "LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=fr.clave_acceso";

    private static final String Q_NC =
            "SELECT 'NC' AS tipo, ncr.id AS registro_id, "
            + "(ncr.establecimiento||'-'||ncr.punto_emision||'-'||ncr.secuencial) AS codigo, "
            + "(ncr.establecimiento||'-'||ncr.punto_emision||'-'||ncr.secuencial) AS num_comprobante, "
            + "ncr.fecha_emision AS fecha, c.nombre AS cliente, ncr.tipo_motivo AS forma_pago, "
            + "ncr.total_sin_impuestos AS subtotal, ncr.valor_iva AS iva, ncr.valor_modificacion AS total, "
            + "COALESCE(ce.estado_sri, ncr.estado_sri) AS estado, COALESCE(ce.mensaje_sri, ncr.mensaje_sri, '') AS mensaje, "
            + "ncr.clave_acceso AS clave_acceso, ncr.cliente_id AS ref_id "
            + "FROM nota_credito_registro ncr LEFT JOIN cliente c ON c.id=ncr.cliente_id "
            + "LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=ncr.clave_acceso";

    private static final String Q_ND =
            "SELECT 'ND' AS tipo, ndr.id AS registro_id, "
            + "(ndr.establecimiento||'-'||ndr.punto_emision||'-'||ndr.secuencial) AS codigo, "
            + "(ndr.establecimiento||'-'||ndr.punto_emision||'-'||ndr.secuencial) AS num_comprobante, "
            + "ndr.fecha_emision AS fecha, c.nombre AS cliente, ndr.forma_pago AS forma_pago, "
            + "ndr.total_sin_impuestos AS subtotal, ndr.valor_iva AS iva, ndr.valor_total AS total, "
            + "COALESCE(ce.estado_sri, ndr.estado_sri) AS estado, COALESCE(ce.mensaje_sri, ndr.mensaje_sri, '') AS mensaje, "
            + "ndr.clave_acceso AS clave_acceso, ndr.cliente_id AS ref_id "
            + "FROM nota_debito_registro ndr LEFT JOIN cliente c ON c.id=ndr.cliente_id "
            + "LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=ndr.clave_acceso";

    private static final String Q_GUIA =
            "SELECT 'GUIA' AS tipo, gr.id AS registro_id, "
            + "(gr.establecimiento||'-'||gr.punto_emision||'-'||gr.secuencial) AS codigo, "
            + "(gr.establecimiento||'-'||gr.punto_emision||'-'||gr.secuencial) AS num_comprobante, "
            + "gr.fecha_emision AS fecha, gr.razon_social_transportista AS cliente, ('Placa '||gr.placa) AS forma_pago, "
            + "0::numeric AS subtotal, 0::numeric AS iva, 0::numeric AS total, "
            + "COALESCE(ce.estado_sri, gr.estado_sri) AS estado, COALESCE(ce.mensaje_sri, gr.mensaje_sri, '') AS mensaje, "
            + "gr.clave_acceso AS clave_acceso, 0 AS ref_id "
            + "FROM guia_remision_registro gr "
            + "LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=gr.clave_acceso";

    private static final String Q_RET =
            "SELECT 'RETENCION' AS tipo, r.id AS registro_id, "
            + "(r.establecimiento||'-'||r.punto_emision||'-'||r.secuencial) AS codigo, "
            + "(r.establecimiento||'-'||r.punto_emision||'-'||r.secuencial) AS num_comprobante, "
            + "r.fecha_emision AS fecha, r.razon_social_sujeto AS cliente, ('Periodo '||r.periodo_fiscal) AS forma_pago, "
            + "0::numeric AS subtotal, 0::numeric AS iva, 0::numeric AS total, "
            + "COALESCE(ce.estado_sri, r.estado_sri) AS estado, COALESCE(ce.mensaje_sri, r.mensaje_sri, '') AS mensaje, "
            + "r.clave_acceso AS clave_acceso, COALESCE(r.proveedor_id, 0) AS ref_id "
            + "FROM retencion_registro r "
            + "LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=r.clave_acceso";

    /** Arma el UNION solo con las tablas que existen (instalaciones sin migrar). */
    private static String union(Connection con) throws SQLException {
        StringBuilder sb = new StringBuilder(Q_FACTURA);
        if (existeTabla(con, "nota_credito_registro")) sb.append(" UNION ALL ").append(Q_NC);
        if (existeTabla(con, "nota_debito_registro")) sb.append(" UNION ALL ").append(Q_ND);
        if (existeTabla(con, "guia_remision_registro")) sb.append(" UNION ALL ").append(Q_GUIA);
        if (existeTabla(con, "retencion_registro")) sb.append(" UNION ALL ").append(Q_RET);
        return sb.toString();
    }

    private static boolean existeTabla(Connection con, String tabla) {
        try (ResultSet rs = con.getMetaData().getTables(null, null, tabla, new String[]{"TABLE"})) {
            return rs.next();
        } catch (SQLException e) {
            return true;
        }
    }

    private static String where(String tipo, String filtro) {
        if (filtro != null && !filtro.trim().isEmpty()) {
            return "WHERE (?='TODOS' OR tipo=?) AND (LOWER(COALESCE(codigo,'')) LIKE ? "
                    + "OR LOWER(COALESCE(num_comprobante,'')) LIKE ? OR LOWER(COALESCE(cliente,'')) LIKE ? "
                    + "OR LOWER(COALESCE(estado,'')) LIKE ? OR LOWER(COALESCE(tipo,'')) LIKE ?)";
        }
        return "WHERE (?='TODOS' OR tipo=?)";
    }

    private static int setParams(PreparedStatement ps, String tipo, String filtro, int start) throws SQLException {
        String t = tipo == null || tipo.isBlank() ? "TODOS" : tipo.trim().toUpperCase();
        int idx = start;
        ps.setString(idx++, t);
        ps.setString(idx++, t);
        if (filtro != null && !filtro.trim().isEmpty()) {
            String like = "%" + filtro.toLowerCase() + "%";
            for (int i = 0; i < 5; i++) ps.setString(idx++, like);
        }
        return idx;
    }

    public int contar(String tipo, String filtro) {
        try (Connection con = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM (" + union(con) + ") t " + where(tipo, filtro);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                setParams(ps, tipo, filtro, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "SeguimientoSriDAO.contar", e);
        }
        return 0;
    }

    public List<SeguimientoSriItem> listar(String tipo, int page, int pageSize, String filtro) {
        List<SeguimientoSriItem> lista = new ArrayList<>();
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 25;
        try (Connection con = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM (" + union(con) + ") t " + where(tipo, filtro)
                    + " ORDER BY fecha DESC NULLS LAST, registro_id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int idx = setParams(ps, tipo, filtro, 1);
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, (page - 1) * pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "SeguimientoSriDAO.listar", e);
        }
        return lista;
    }

    private SeguimientoSriItem mapear(ResultSet rs) throws SQLException {
        SeguimientoSriItem it = new SeguimientoSriItem();
        it.setTipo(rs.getString("tipo"));
        it.setRegistroId(rs.getInt("registro_id"));
        it.setCodigo(rs.getString("codigo"));
        it.setNumComprobante(rs.getString("num_comprobante"));
        try {
            it.setFecha(rs.getObject("fecha", LocalDateTime.class));
        } catch (Exception e) {
            java.sql.Timestamp ts = rs.getTimestamp("fecha");
            it.setFecha(ts != null ? ts.toLocalDateTime() : null);
        }
        it.setNombreCliente(rs.getString("cliente"));
        it.setFormaPago(rs.getString("forma_pago"));
        it.setSubtotal(rs.getBigDecimal("subtotal"));
        it.setIva(rs.getBigDecimal("iva"));
        it.setTotal(rs.getBigDecimal("total"));
        it.setEstadoSri(rs.getString("estado"));
        it.setMensajeSri(rs.getString("mensaje"));
        it.setClaveAcceso(rs.getString("clave_acceso"));
        it.setRefId(rs.getInt("ref_id"));
        return it;
    }
}

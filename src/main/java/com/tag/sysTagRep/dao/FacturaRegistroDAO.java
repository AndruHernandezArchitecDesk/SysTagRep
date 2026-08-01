package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.FacturaRegistro;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FacturaRegistroDAO {

    public List<FacturaRegistro> obtenerNumFactura() {
        List<FacturaRegistro> lista = new ArrayList<>();
        String sql = "SELECT * FROM factura_registro ORDER BY id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaRegistro f = new FacturaRegistro();
                f.setId(rs.getInt("id"));
                f.setCodigo(rs.getString("codigo"));
                lista.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public int insertar(FacturaRegistro fr) {
        String sql = "INSERT INTO factura_registro(empresa_id, cliente_id, fecha, codigo, forma_pago, " +
                     "subtotal, iva, descuento, total, clave_acceso, num_comprobante, ambiente_sri, estado_sri, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fr.getEmpresaId());
            ps.setInt(2, fr.getClienteId());
            ps.setObject(3, fr.getFecha());
            ps.setString(4, fr.getCodigo());
            ps.setString(5, fr.getFormaPago());
            ps.setBigDecimal(6, fr.getSubtotal());
            ps.setBigDecimal(7, fr.getIva());
            ps.setBigDecimal(8, fr.getDescuento() != null ? fr.getDescuento() : BigDecimal.ZERO);
            ps.setBigDecimal(9, fr.getTotal());
            ps.setString(10, fr.getClaveAcceso());
            ps.setString(11, fr.getNumComprobante());
            ps.setString(12, fr.getAmbienteSri());
            ps.setString(13, fr.getEstadoSri());
            ps.setObject(14, fr.getFechaRegistro());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void actualizarEstado(String claveAcceso, String estado) {
        String sql = "UPDATE factura_registro SET estado_sri = ? WHERE clave_acceso = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<FacturaRegistro> listarPendientesSri() {
        List<FacturaRegistro> lista = new ArrayList<>();
        String sql = "SELECT fr.clave_acceso, fr.ambiente_sri, ce.estado_sri, ce.numero_comprobante " +
                     "FROM comprobantes_electronicos ce " +
                     "JOIN factura_registro fr ON fr.clave_acceso = ce.clave_acceso " +
                     "WHERE ce.estado_sri IN ('PENDIENTE', 'ERROR') AND fr.clave_acceso IS NOT NULL " +
                     "ORDER BY fr.id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaRegistro f = new FacturaRegistro();
                f.setClaveAcceso(rs.getString("clave_acceso"));
                f.setAmbienteSri(rs.getString("ambiente_sri"));
                f.setEstadoSri(rs.getString("estado_sri"));
                f.setNumComprobante(rs.getString("numero_comprobante"));
                lista.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public int contar(String filtro) {
        String sql = "SELECT COUNT(*) FROM factura_registro fr " +
                     "LEFT JOIN cliente c ON c.id = fr.cliente_id " +
                     buildWhereClause(filtro);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setFilterParameters(ps, filtro, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<FacturaRegistro> listarPaginado(int page, int pageSize, String filtro) {
        List<FacturaRegistro> lista = new ArrayList<>();
        String sql = "SELECT fr.*, c.nombre AS nombre_cliente, ce.mensaje_sri AS mensaje_sri, " +
                     "COALESCE(ce.estado_sri, fr.estado_sri) AS estado_sri_actual " +
                     "FROM factura_registro fr " +
                     "LEFT JOIN cliente c ON c.id = fr.cliente_id " +
                     "LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso = fr.clave_acceso " +
                     buildWhereClause(filtro) +
                     " ORDER BY CAST(regexp_replace(fr.codigo, '\\D', '', 'g') AS INTEGER) ASC, fr.id ASC " +
                     "LIMIT ? OFFSET ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = setFilterParameters(ps, filtro, 1);
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaRegistro f = new FacturaRegistro();
                    mapear(rs, f);
                    f.setNombreCliente(rs.getString("nombre_cliente"));
                    lista.add(f);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    private String buildWhereClause(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) return "";
        return "WHERE (LOWER(fr.codigo) LIKE ? OR LOWER(COALESCE(fr.num_comprobante, '')) LIKE ? " +
               "OR LOWER(c.nombre) LIKE ? OR LOWER(COALESCE(fr.estado_sri, '')) LIKE ? " +
               "OR LOWER(COALESCE(fr.forma_pago, '')) LIKE ?)";
    }

    private int setFilterParameters(PreparedStatement ps, String filtro, int startIdx) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) return startIdx;
        String like = "%" + filtro.toLowerCase() + "%";
        int idx = startIdx;
        for (int i = 0; i < 5; i++) ps.setString(idx++, like);
        return idx;
    }

    private void mapear(ResultSet rs, FacturaRegistro f) throws SQLException {
        f.setId(rs.getInt("id"));
        f.setEmpresaId(rs.getInt("empresa_id"));
        f.setClienteId(rs.getInt("cliente_id"));
        f.setFecha(rs.getObject("fecha", LocalDateTime.class));
        f.setCodigo(rs.getString("codigo"));
        f.setFormaPago(rs.getString("forma_pago"));
        f.setSubtotal(rs.getBigDecimal("subtotal"));
        f.setIva(rs.getBigDecimal("iva"));
        f.setDescuento(rs.getBigDecimal("descuento"));
        f.setTotal(rs.getBigDecimal("total"));
        f.setClaveAcceso(rs.getString("clave_acceso"));
        f.setNumComprobante(rs.getString("num_comprobante"));
        f.setAmbienteSri(rs.getString("ambiente_sri"));
        f.setEstadoSri(rs.getString("estado_sri_actual"));
        f.setMensajeSri(rs.getString("mensaje_sri"));
        f.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
    }
}

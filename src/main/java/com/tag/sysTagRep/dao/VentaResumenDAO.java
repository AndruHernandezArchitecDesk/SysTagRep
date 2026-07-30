package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.VentaResumen;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VentaResumenDAO {

    public List<VentaResumen> listar() {
        return listarPorFecha(null);
    }

    public List<VentaResumen> listarPorFecha(LocalDate fecha) {
        return listarPorRango(fecha, fecha);
    }

    public List<VentaResumen> listarPorRango(LocalDate desde, LocalDate hasta) {
        List<VentaResumen> lista = new ArrayList<>();
        String sql = "SELECT * FROM (" +
                "SELECT nvr.id, 'NOTA_VENTA' AS tipo, nvr.codigo, nvr.fecha, c.nombre AS cliente, nvr.forma_pago, " +
                "  (SELECT COALESCE(COUNT(*), 0) FROM nota_venta_detalle nvd WHERE nvd.nota_venta_registro_id = nvr.id) AS items, " +
                "  (SELECT COALESCE(SUM(nvd.total), 0) FROM nota_venta_detalle nvd WHERE nvd.nota_venta_registro_id = nvr.id) AS total " +
                "FROM nota_venta_registro nvr " +
                "JOIN cliente c ON c.id = nvr.cliente_id " +
                "UNION ALL " +
                "SELECT fr.id, 'FACTURA' AS tipo, fr.codigo, fr.fecha, c.nombre AS cliente, fr.forma_pago, " +
                "  (SELECT COALESCE(COUNT(*), 0) FROM factura_detalle fd WHERE fd.factura_registro_id = fr.id) AS items, " +
                "  COALESCE(fr.total, 0) AS total " +
                "FROM factura_registro fr " +
                "JOIN cliente c ON c.id = fr.cliente_id " +
                ") AS ventas WHERE CAST(fecha AS DATE) BETWEEN ? AND ? ORDER BY fecha ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde != null ? desde : LocalDate.now());
            ps.setObject(2, hasta != null ? hasta : LocalDate.now());
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                VentaResumen v = new VentaResumen();
                v.setId(rs.getInt("id"));
                v.setTipo(rs.getString("tipo"));
                v.setCodigo(rs.getString("codigo"));
                v.setFecha(rs.getObject("fecha", LocalDateTime.class));
                v.setCliente(rs.getString("cliente"));
                v.setFormaPago(rs.getString("forma_pago"));
                v.setItems(rs.getInt("items"));
                v.setTotal(rs.getBigDecimal("total"));
                lista.add(v);
            }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}

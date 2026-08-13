package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.FacturaDetalle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FacturaDetalleDAO {

    public void insertarDetalle(int facturaRegistroId, List<FacturaDetalle> detalles) {
        String sql = "INSERT INTO factura_detalle(factura_registro_id, inventario_id, codigo, descripcion, " +
                     "cantidad, precio_unitario, precio_total, subtotal, iva, descuento, total, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (FacturaDetalle d : detalles) {
                BigDecimal subtotal = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
                BigDecimal iva = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

                ps.setInt(1, facturaRegistroId);
                ps.setInt(2, d.getInventarioId());
                ps.setString(3, d.getCodigo());
                ps.setString(4, d.getDescripcion());
                ps.setInt(5, d.getCantidad());
                ps.setBigDecimal(6, d.getPrecioUnitario());
                ps.setBigDecimal(7, d.getPrecioTotal());
                ps.setBigDecimal(8, subtotal);
                ps.setBigDecimal(9, iva);
                ps.setBigDecimal(10, BigDecimal.ZERO);
                ps.setBigDecimal(11, total);
                ps.setObject(12, LocalDateTime.now());
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<FacturaDetalle> listarPorFacturaRegistroId(int facturaRegistroId) {
        List<FacturaDetalle> lista = new ArrayList<>();
        String sql = "SELECT inventario_id, codigo, descripcion, cantidad, precio_unitario, precio_total " +
                     "FROM factura_detalle WHERE factura_registro_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facturaRegistroId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaDetalle d = new FacturaDetalle(
                            rs.getInt("inventario_id"),
                            rs.getString("codigo"),
                            rs.getString("descripcion"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio_unitario"));
                    d.setPrecioTotal(rs.getBigDecimal("precio_total"));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean existeVentaPorInventarioIds(List<Integer> inventarioIds) {
        if (inventarioIds == null || inventarioIds.isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM factura_detalle WHERE inventario_id IN (" +
                     inventarioIds.stream().map(i -> "?").collect(java.util.stream.Collectors.joining(",")) + ")";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : inventarioIds) {
                ps.setInt(i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

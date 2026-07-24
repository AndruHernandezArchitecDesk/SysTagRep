package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.DetalleVenta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class NotaVentaDetalleDAO {

    public void insertarDetalle(int notaVentaRegistroId, List<DetalleVenta> detalles) {
        String sql = "INSERT INTO nota_venta_detalle(nota_venta_registro_id, descripcion, cantidad, precio_unitario, precio_total, subtotal, iva, descuento, total, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (DetalleVenta d : detalles) {
                BigDecimal subtotal = d.getPrecioUnitario().multiply(new BigDecimal(d.getCantidad()));
                BigDecimal iva = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

                ps.setInt(1, notaVentaRegistroId);
                ps.setString(2, d.getDescripcion());
                ps.setInt(3, d.getCantidad());
                ps.setBigDecimal(4, d.getPrecioUnitario());
                ps.setBigDecimal(5, d.getPrecioTotal());
                ps.setBigDecimal(6, subtotal);
                ps.setBigDecimal(7, iva);
                ps.setBigDecimal(8, BigDecimal.ZERO);
                ps.setBigDecimal(9, total);
                ps.setObject(10, LocalDateTime.now());
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

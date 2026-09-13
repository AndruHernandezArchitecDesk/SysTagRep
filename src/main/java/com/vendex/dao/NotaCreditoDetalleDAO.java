package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaCreditoDetalle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotaCreditoDetalleDAO {

    private static final Logger LOGGER = Logger.getLogger(NotaCreditoDetalleDAO.class.getName());

    public void insertarDetalles(int notaCreditoId, List<NotaCreditoDetalle> detalles) {
        String sql = "INSERT INTO nota_credito_detalle(nota_credito_id, factura_detalle_id, inventario_id, descripcion, cantidad, precio_unitario, descuento, codigo_porcentaje_iva, precio_total_sin_impuesto) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (NotaCreditoDetalle d : detalles) {
                ps.setInt(1, notaCreditoId);
                if (d.getFacturaDetalleId() != null) ps.setInt(2, d.getFacturaDetalleId()); else ps.setNull(2, java.sql.Types.INTEGER);
                if (d.getInventarioId() != null) ps.setInt(3, d.getInventarioId()); else ps.setNull(3, java.sql.Types.INTEGER);
                ps.setString(4, d.getDescripcion());
                ps.setBigDecimal(5, d.getCantidad());
                ps.setBigDecimal(6, d.getPrecioUnitario());
                ps.setBigDecimal(7, d.getDescuento() != null ? d.getDescuento() : java.math.BigDecimal.ZERO);
                ps.setString(8, d.getCodigoPorcentajeIva() != null ? d.getCodigoPorcentajeIva() : "4");
                ps.setBigDecimal(9, d.getPrecioTotalSinImpuesto());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoDetalleDAO.insertarDetalles", e);
        }
    }

    public List<NotaCreditoDetalle> listarPorNotaCreditoId(int notaCreditoId) {
        List<NotaCreditoDetalle> lista = new ArrayList<>();
        String sql = "SELECT * FROM nota_credito_detalle WHERE nota_credito_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notaCreditoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotaCreditoDetalle d = new NotaCreditoDetalle();
                    d.setId(rs.getInt("id"));
                    d.setNotaCreditoId(rs.getInt("nota_credito_id"));
                    int fd = rs.getInt("factura_detalle_id"); d.setFacturaDetalleId(rs.wasNull()?null:fd);
                    int inv = rs.getInt("inventario_id"); d.setInventarioId(rs.wasNull()?null:inv);
                    d.setDescripcion(rs.getString("descripcion"));
                    d.setCantidad(rs.getBigDecimal("cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setDescuento(rs.getBigDecimal("descuento"));
                    d.setCodigoPorcentajeIva(rs.getString("codigo_porcentaje_iva"));
                    d.setPrecioTotalSinImpuesto(rs.getBigDecimal("precio_total_sin_impuesto"));
                    lista.add(d);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaCreditoDetalleDAO.listarPorNotaCreditoId", e);
        }
        return lista;
    }
}

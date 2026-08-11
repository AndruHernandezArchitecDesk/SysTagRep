package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.CuentaPorPagar;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CuentaPorPagarDAO {

    public void insertar(CuentaPorPagar cpp) {
        String sql = "INSERT INTO cuentas_por_pagar(inventario_id, proveedor_id, total, meses_plazo, interes, cuota_mensual, estado, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpp.getInventarioId());
            ps.setInt(2, cpp.getProveedorId());
            ps.setBigDecimal(3, cpp.getTotal());
            ps.setInt(4, cpp.getMesesPlazo());
            ps.setBigDecimal(5, cpp.getInteres());
            ps.setBigDecimal(6, cpp.getCuotaMensual());
            ps.setString(7, "Pendiente");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Object[]> listarCreditosActivos() {
        List<Object[]> lista = new ArrayList<>();
        String sql =                      "SELECT cpp.id, cpp.inventario_id, cpp.proveedor_id, pv.nombre, pv.identificacion, " +
                     "i.codigo, i.descripcion, cpp.total, cpp.meses_plazo, cpp.interes, cpp.cuota_mensual, " +
                     "cpp.adelanto, cpp.estado, cpp.fecha_registro " +
                     "FROM cuentas_por_pagar cpp " +
                     "INNER JOIN proveedor pv ON pv.id = cpp.proveedor_id " +
                     "INNER JOIN inventario i ON i.id = cpp.inventario_id " +
                     "WHERE cpp.estado = 'Pendiente' " +
                     "ORDER BY (cpp.fecha_registro + (cpp.meses_plazo || ' months')::interval) ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[]{
                    rs.getInt("id"),
                    rs.getInt("inventario_id"),
                    rs.getInt("proveedor_id"),
                    rs.getString("nombre"),
                    rs.getString("identificacion"),
                    rs.getString("codigo"),
                    rs.getString("descripcion"),
                    rs.getBigDecimal("total"),
                    rs.getInt("meses_plazo"),
                    rs.getBigDecimal("interes"),
                    rs.getBigDecimal("cuota_mensual"),
                    rs.getBigDecimal("adelanto"),
                    rs.getString("estado"),
                    rs.getTimestamp("fecha_registro")
                };
                lista.add(fila);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public void registrarAdelanto(int cppId, BigDecimal nuevoAdelanto) {
        String sql = "UPDATE cuentas_por_pagar SET adelanto = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, nuevoAdelanto);
            ps.setInt(2, cppId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void marcarPagado(int cppId) {
        String sql = "UPDATE cuentas_por_pagar SET estado = 'Pagado' WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cppId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void eliminarPorInventarios(List<Integer> inventarioIds) {
        if (inventarioIds == null || inventarioIds.isEmpty()) return;
        String sql = "DELETE FROM cuentas_por_pagar WHERE inventario_id IN (" +
                     inventarioIds.stream().map(i -> "?").collect(java.util.stream.Collectors.joining(",")) + ")";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Integer id : inventarioIds) {
                ps.setInt(i++, id);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String[]> obtenerDetallesInventario(int inventarioId) {
        List<String[]> detalles = new ArrayList<>();
        String sql = "SELECT descripcion, cantidad, costo_sin_iva, precio_venta FROM inventario WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inventarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal costo = rs.getBigDecimal("costo_sin_iva");
                    int cant = rs.getInt("cantidad");
                    BigDecimal subtotal = costo != null ? costo.multiply(BigDecimal.valueOf(cant)) : BigDecimal.ZERO;
                    detalles.add(new String[]{
                        rs.getString("descripcion"),
                        String.valueOf(cant),
                        costo != null ? costo.toString() : "0",
                        subtotal.setScale(2).toString()
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return detalles;
    }
}

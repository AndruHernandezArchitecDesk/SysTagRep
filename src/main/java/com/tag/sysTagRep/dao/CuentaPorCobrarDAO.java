package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.CuentaPorCobrar;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CuentaPorCobrarDAO {

    public void insertar(CuentaPorCobrar cpc) {
        String sql = "INSERT INTO cuentas_por_cobrar(nota_venta_id, cliente_id, total, meses_plazo, interes, cuota_mensual, estado, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpc.getNotaVentaId());
            ps.setInt(2, cpc.getClienteId());
            ps.setBigDecimal(3, cpc.getTotal());
            ps.setInt(4, cpc.getMesesPlazo());
            ps.setBigDecimal(5, cpc.getInteres());
            ps.setBigDecimal(6, cpc.getCuotaMensual());
            ps.setString(7, "Pendiente");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Object[]> listarCreditosActivos() {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT cpc.id, cpc.nota_venta_id, cpc.cliente_id, cl.nombre, cl.identificacion, " +
                     "nv.codigo, nv.fecha, cpc.total, cpc.meses_plazo, cpc.interes, cpc.cuota_mensual, " +
                     "cpc.adelanto, cpc.estado, cpc.fecha_registro " +
                     "FROM cuentas_por_cobrar cpc " +
                     "INNER JOIN cliente cl ON cl.id = cpc.cliente_id " +
                     "INNER JOIN nota_venta_registro nv ON nv.id = cpc.nota_venta_id " +
                     "WHERE cpc.estado = 'Pendiente' " +
                     "ORDER BY (cpc.fecha_registro + (cpc.meses_plazo || ' days')::interval) ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[]{
                    rs.getInt("id"),
                    rs.getInt("nota_venta_id"),
                    rs.getInt("cliente_id"),
                    rs.getString("nombre"),
                    rs.getString("identificacion"),
                    rs.getString("codigo"),
                    rs.getTimestamp("fecha"),
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

    public List<Object[]> listarPorCliente(int clienteId) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT cpc.id, cpc.nota_venta_id, cpc.cliente_id, cl.nombre, cl.identificacion, " +
                     "nv.codigo, nv.fecha, cpc.total, cpc.meses_plazo, cpc.interes, cpc.cuota_mensual, " +
                     "cpc.adelanto, cpc.estado, cpc.fecha_registro " +
                     "FROM cuentas_por_cobrar cpc " +
                     "INNER JOIN cliente cl ON cl.id = cpc.cliente_id " +
                     "INNER JOIN nota_venta_registro nv ON nv.id = cpc.nota_venta_id " +
                     "WHERE cpc.estado = 'Pendiente' AND cpc.cliente_id = ? " +
                     "ORDER BY cpc.fecha_registro ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[]{
                        rs.getInt("id"),
                        rs.getInt("nota_venta_id"),
                        rs.getInt("cliente_id"),
                        rs.getString("nombre"),
                        rs.getString("identificacion"),
                        rs.getString("codigo"),
                        rs.getTimestamp("fecha"),
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public void registrarAdelanto(int cpcId, BigDecimal nuevoAdelanto) {
        String sql = "UPDATE cuentas_por_cobrar SET adelanto = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, nuevoAdelanto);
            ps.setInt(2, cpcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void marcarPagado(int cpcId) {
        String sql = "UPDATE cuentas_por_cobrar SET estado = 'Pagado' WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String[]> obtenerDetallesVenta(int notaVentaId) {
        List<String[]> detalles = new ArrayList<>();
        String sql = "SELECT descripcion, cantidad, precio_unitario, subtotal FROM nota_venta_detalle WHERE nota_venta_registro_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notaVentaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(new String[]{
                        rs.getString("descripcion"),
                        String.valueOf(rs.getInt("cantidad")),
                        rs.getBigDecimal("precio_unitario").toString(),
                        rs.getBigDecimal("subtotal").toString()
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return detalles;
    }
}

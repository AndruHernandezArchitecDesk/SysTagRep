package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardDAO {

    /**
     * Ventas por día: suma de totales de nota_venta_detalle agrupada por fecha de registro de nota_venta_registro.
     */
    public Map<String, Double> ventasPorDia(int ultimosDias) {
        Map<String, Double> mapa = new LinkedHashMap<>();
        String sql = "SELECT CAST(nvr.fecha_registro AS DATE) AS dia, SUM(nvd.total) AS total_ventas " +
                     "FROM nota_venta_registro nvr " +
                     "JOIN nota_venta_detalle nvd ON nvd.nota_venta_registro_id = nvr.id " +
                     "WHERE nvr.fecha_registro >= NOW() - (? || ' days')::INTERVAL " +
                     "GROUP BY dia ORDER BY dia ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ultimosDias);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dia = rs.getDate("dia").toLocalDate().toString();
                double total = rs.getDouble("total_ventas");
                mapa.merge(dia, total, Double::sum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapa;
    }

    /**
     * Facturas electrónicas por día: suma de total de factura_registro agrupada por fecha.
     */
    public Map<String, Double> facturasPorDia(int ultimosDias) {
        Map<String, Double> mapa = new LinkedHashMap<>();
        String sql = "SELECT CAST(fecha_registro AS DATE) AS dia, SUM(total) AS total_facturas " +
                     "FROM factura_registro " +
                     "WHERE fecha_registro >= NOW() - (? || ' days')::INTERVAL " +
                     "GROUP BY dia ORDER BY dia ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ultimosDias);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dia = rs.getDate("dia").toLocalDate().toString();
                double total = rs.getDouble("total_facturas");
                mapa.merge(dia, total, Double::sum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapa;
    }

    /**
     * Inventario por marca: suma de cantidad agrupada por marca.
     */
    public Map<String, Integer> inventarioPorMarca() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT m.nombre AS marca, SUM(i.cantidad) AS total " +
                     "FROM inventario i JOIN marca m ON i.marca_id = m.id " +
                     "GROUP BY m.nombre ORDER BY total DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("marca"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapa;
    }

    /**
     * Inventario por grupo: suma de cantidad agrupada por grupo.
     */
    public Map<String, Integer> inventarioPorGrupo() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT g.nombre AS grupo, SUM(i.cantidad) AS total " +
                     "FROM inventario i JOIN grupo g ON i.grupo_id = g.id " +
                     "GROUP BY g.nombre ORDER BY total DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("grupo"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapa;
    }

    /**
     * Ventas del día: suma de total de factura_registro de la fecha indicada.
     */
    public double ventasDelDia(LocalDate fecha) {
        double total = 0.0;
        String sql = "SELECT COALESCE(SUM(total), 0) AS total_ventas " +
                     "FROM factura_registro WHERE CAST(fecha AS DATE) = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total_ventas");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * Facturas emitidas: cantidad de registros de factura_registro de la fecha indicada.
     */
    public int facturasEmitidasDelDia(LocalDate fecha) {
        int conteo = 0;
        String sql = "SELECT COUNT(*) AS cantidad FROM factura_registro WHERE CAST(fecha AS DATE) = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                conteo = rs.getInt("cantidad");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conteo;
    }

    /**
     * Productos vendidos: suma de cantidad de factura_detalle de las facturas de la fecha indicada.
     */
    public int productosVendidosDelDia(LocalDate fecha) {
        int cantidad = 0;
        String sql = "SELECT COALESCE(SUM(fd.cantidad), 0) AS cantidad " +
                     "FROM factura_detalle fd " +
                     "JOIN factura_registro fr ON fr.id = fd.factura_registro_id " +
                     "WHERE CAST(fr.fecha AS DATE) = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cantidad = rs.getInt("cantidad");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cantidad;
    }

    /**
     * Clientes atendidos: clientes distintos con facturas en la fecha indicada.
     */
    public int clientesAtendidosDelDia(LocalDate fecha) {
        int conteo = 0;
        String sql = "SELECT COUNT(DISTINCT fr.cliente_id) AS cantidad " +
                     "FROM factura_registro fr WHERE CAST(fr.fecha AS DATE) = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                conteo = rs.getInt("cantidad");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conteo;
    }

    /**
     * Compras por día: suma de (costo_sin_iva * cantidad) de inventario agrupada por fecha_ingreso.
     */
    public Map<String, Double> comprasPorDia(int ultimosDias) {
        Map<String, Double> mapa = new LinkedHashMap<>();
        String sql = "SELECT CAST(fecha_ingreso AS DATE) AS dia, SUM(costo_sin_iva * cantidad) AS total_compras " +
                     "FROM inventario " +
                     "WHERE fecha_ingreso IS NOT NULL " +
                     "AND fecha_ingreso >= NOW() - (? || ' days')::INTERVAL " +
                     "GROUP BY dia ORDER BY dia ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ultimosDias);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dia = rs.getDate("dia").toLocalDate().toString();
                double total = rs.getDouble("total_compras");
                mapa.merge(dia, total, Double::sum);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapa;
    }
}

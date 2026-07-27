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

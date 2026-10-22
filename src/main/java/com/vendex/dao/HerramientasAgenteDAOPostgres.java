package com.vendex.dao;

import com.vendex.chatbot.HerramientasAgente;
import com.vendex.config.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fase agente IA — herramientas de lectura del ERP (solo SELECT, sin efectos).
 * Todas las consultas son fail-soft: en error devuelven lista vacía/null y loguean,
 * nunca lanzan hacia el chatbot (el asistente no debe caer por una consulta).
 */
public class HerramientasAgenteDAOPostgres implements HerramientasAgente {

    private static final Logger LOGGER = Logger.getLogger(HerramientasAgenteDAOPostgres.class.getName());

    @Override
    public List<Producto> productos(String criterio, int limite) {
        if (criterio == null || criterio.isBlank()) return List.of();
        String sql = "SELECT codigo, descripcion, cantidad, precio_venta, costo_sin_iva FROM inventario " +
                     "WHERE estado=true AND (UPPER(codigo) = UPPER(?) OR UPPER(descripcion) LIKE UPPER(?)) " +
                     "ORDER BY (UPPER(codigo) = UPPER(?)) DESC, cantidad DESC, descripcion LIMIT ?";
        List<Producto> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + criterio.trim() + "%";
            ps.setString(1, criterio.trim());
            ps.setString(2, like);
            ps.setString(3, criterio.trim());
            ps.setInt(4, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.productos", e);
        }
        return out;
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        BigDecimal precio = rs.getBigDecimal("precio_venta");
        BigDecimal costo = rs.getBigDecimal("costo_sin_iva");
        double pct = -1;
        if (precio != null && precio.signum() > 0 && costo != null) {
            // Criterio Análisis IA: (precio venta - costo sin IVA) / precio venta * 100
            pct = precio.subtract(costo).multiply(new BigDecimal("100"))
                    .divide(precio, 1, RoundingMode.HALF_UP).doubleValue();
        }
        return new Producto(
                rs.getString("codigo"),
                rs.getString("descripcion"),
                rs.getInt("cantidad"),
                precio,
                costo,
                pct);
    }

    @Override
    public List<Cliente> clientes(String criterio, int limite) {
        if (criterio == null || criterio.isBlank()) return List.of();
        String sql = "SELECT id, nombre, identificacion FROM cliente " +
                     "WHERE estado=true AND (UPPER(nombre) LIKE UPPER(?) OR identificacion = ?) " +
                     "ORDER BY nombre LIMIT ?";
        List<Cliente> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + criterio.trim() + "%");
            ps.setString(2, criterio.trim());
            ps.setInt(3, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("identificacion")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.clientes", e);
        }
        return out;
    }

    @Override
    public Ventas ventas(int clienteId) {
        String sql = "SELECT COUNT(*), COALESCE(SUM(total),0), MAX(fecha) FROM factura_registro WHERE cliente_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ultima = rs.getTimestamp(3);
                    return new Ventas(rs.getInt(1), rs.getBigDecimal(2),
                            ultima != null ? ultima.toLocalDateTime().toLocalDate().toString() : null);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.ventas", e);
        }
        return null;
    }

    @Override
    public BigDecimal saldo(int clienteId) {
        String sql = "SELECT COALESCE(SUM(total - COALESCE(adelanto,0)),0) FROM cuentas_por_cobrar " +
                     "WHERE cliente_id = ? AND estado = 'Pendiente'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.saldo", e);
        }
        return null;
    }

    @Override
    public List<Top> masVendidos(int limite) {
        String sql = "SELECT COALESCE(NULLIF(TRIM(d.codigo),''), 'sin código') AS cod, d.descripcion, " +
                     "SUM(d.cantidad) AS q, SUM(d.precio_total) AS monto " +
                     "FROM factura_detalle d " +
                     "GROUP BY1, d.descripcion ORDER BY monto DESC LIMIT ?";
        List<Top> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Top(rs.getString("cod"), rs.getString("descripcion"),
                            rs.getInt("q"), rs.getBigDecimal("monto"), null));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.masVendidos", e);
        }
        return out;
    }

    @Override
    public List<Top> mejorMargen(int limite) {
        String sql = "SELECT codigo, descripcion, precio_venta, costo_sin_iva FROM inventario " +
                     "WHERE estado=true AND costo_sin_iva IS NOT NULL AND costo_sin_iva > 0 " +
                     "AND precio_venta IS NOT NULL AND precio_venta > 0 " +
                     "ORDER BY (precio_venta - costo_sin_iva) / precio_venta DESC LIMIT ?";
        List<Top> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal precio = rs.getBigDecimal("precio_venta");
                    BigDecimal costo = rs.getBigDecimal("costo_sin_iva");
                    double pct = precio.subtract(costo).multiply(new BigDecimal("100"))
                            .divide(precio, 1, RoundingMode.HALF_UP).doubleValue();
                    out.add(new Top(rs.getString("codigo"), rs.getString("descripcion"), 0, precio, pct));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.mejorMargen", e);
        }
        return out;
    }
}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Inventario;
import com.vendex.model.Vehiculo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventarioVehiculoDAOPostgres implements InventarioVehiculoDAO {

    private static final Logger LOG = Logger.getLogger(InventarioVehiculoDAOPostgres.class.getName());

    @Override
    public void asociar(int inventarioId, int vehiculoId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            asociar(con, inventarioId, vehiculoId);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.asociar", e);
        }
    }

    @Override
    public void asociar(Connection con, int inventarioId, int vehiculoId) throws SQLException {
        String sql = "INSERT INTO inventario_vehiculo(inventario_id, vehiculo_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inventarioId);
            ps.setInt(2, vehiculoId);
            ps.executeUpdate();
        }
    }

    @Override
    public void asociarMultiple(int inventarioId, List<Integer> vehiculoIds) {
        if (vehiculoIds == null || vehiculoIds.isEmpty()) return;
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                limpiarPorInventario(con, inventarioId);
                for (int vid : vehiculoIds) asociar(con, inventarioId, vid);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally { con.setAutoCommit(true); }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.asociarMultiple", e);
        }
    }

    @Override
    public void limpiarPorInventario(int inventarioId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            limpiarPorInventario(con, inventarioId);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.limpiarPorInventario", e);
        }
    }

    @Override
    public void limpiarPorInventario(Connection con, int inventarioId) throws SQLException {
        String sql = "DELETE FROM inventario_vehiculo WHERE inventario_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inventarioId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Integer> listarVehiculoIdsPorInventario(int inventarioId) {
        List<Integer> lista = new ArrayList<>();
        String sql = "SELECT vehiculo_id FROM inventario_vehiculo WHERE inventario_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inventarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.listarVehiculoIdsPorInventario", e);
        }
        return lista;
    }

    @Override
    public List<Integer> listarInventarioIdsPorVehiculo(int vehiculoId) {
        List<Integer> lista = new ArrayList<>();
        String sql = "SELECT inventario_id FROM inventario_vehiculo WHERE vehiculo_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehiculoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("does not exist") || msg.contains("no existe")) return lista;
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.listarInventarioIdsPorVehiculo", e);
        }
        return lista;
    }

    @Override
    public List<Inventario> listarPorVehiculo(int vehiculoId, int sucursalId) {
        List<Inventario> lista = new ArrayList<>();
        String sql = "SELECT i.*, p.nombre as nombre_proveedor, g.nombre as nombre_grupo, m.nombre as nombre_marca, COALESCE(ub.codigo_ubicacion, u.nombre) as nombre_ubicacion " +
                "FROM inventario i JOIN inventario_vehiculo iv ON iv.inventario_id=i.id " +
                "LEFT JOIN proveedor p ON p.id=i.proveedor_id LEFT JOIN grupo g ON g.id=i.grupo_id LEFT JOIN marca m ON m.id=i.marca_id " +
                "LEFT JOIN ubicacion_percha u ON u.id=i.ubicacion_percha_id " +
                "LEFT JOIN (SELECT id_producto, STRING_AGG(codigo_ubicacion, ', ' ORDER BY codigo_ubicacion) AS codigo_ubicacion FROM ubicacion WHERE id_producto IS NOT NULL GROUP BY id_producto) ub ON ub.id_producto=i.id " +
                "WHERE iv.vehiculo_id=? AND i.estado=true AND i.sucursal_id=? ORDER BY i.descripcion";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, vehiculoId);
            ps.setInt(2, sucursalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Inventario v = new Inventario();
                    // Reusar mapeo simple
                    v.setId(rs.getInt("id"));
                    v.setDescripcion(rs.getString("descripcion"));
                    v.setCodigo(rs.getString("codigo"));
                    v.setCantidad(rs.getInt("cantidad"));
                    v.setPrecioVenta(rs.getBigDecimal("precio_venta"));
                    v.setSucursalId(rs.getInt("sucursal_id"));
                    v.setUbicacionPercha(rs.getString("nombre_ubicacion"));
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("does not exist")) {
                try { DatabaseConnection.ensureSucursalSchema(); } catch (Exception ignore) {}
                return lista;
            }
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.listarPorVehiculo", e);
        }
        return lista;
    }

    @Override
    public List<Vehiculo> listarVehiculosPorInventario(int inventarioId) {
        List<Vehiculo> lista = new ArrayList<>();
        String sql = "SELECT v.* FROM vehiculo v JOIN inventario_vehiculo iv ON iv.vehiculo_id=v.id WHERE iv.inventario_id=? ORDER BY v.marca, v.modelo";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, inventarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vehiculo v = new Vehiculo();
                    v.setId(rs.getInt("id"));
                    v.setMarca(rs.getString("marca"));
                    v.setModelo(rs.getString("modelo"));
                    int desde = rs.getInt("anio_desde"); v.setAnioDesde(rs.wasNull()?null:desde);
                    int hasta = rs.getInt("anio_hasta"); v.setAnioHasta(rs.wasNull()?null:hasta);
                    v.setMotor(rs.getString("motor"));
                    v.setCombustible(rs.getString("combustible"));
                    v.setVinPrefijo(rs.getString("vin_prefijo"));
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "InventarioVehiculoDAO.listarVehiculosPorInventario", e);
        }
        return lista;
    }
}

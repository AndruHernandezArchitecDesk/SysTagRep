package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Vehiculo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VehiculoDAOPostgres implements VehiculoDAO {

    private static final Logger LOG = Logger.getLogger(VehiculoDAOPostgres.class.getName());

    @Override
    public List<Vehiculo> listar() {
        List<Vehiculo> lista = new ArrayList<>();
        String sql = "SELECT * FROM vehiculo ORDER BY marca, modelo, anio_desde";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOG.log(Level.SEVERE, "VehiculoDAO.listar", e);
        }
        return lista;
    }

    @Override
    public List<String> listarMarcas() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT marca FROM vehiculo WHERE marca IS NOT NULL ORDER BY marca";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(rs.getString(1));
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOG.log(Level.SEVERE, "VehiculoDAO.listarMarcas", e);
        }
        return lista;
    }

    @Override
    public List<String> listarModelosPorMarca(String marca) {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT modelo FROM vehiculo WHERE marca ILIKE ? ORDER BY modelo";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, marca);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(rs.getString(1));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOG.log(Level.SEVERE, "VehiculoDAO.listarModelosPorMarca", e);
        }
        return lista;
    }

    @Override
    public List<Integer> listarAniosPorModelo(String marca, String modelo) {
        List<Integer> lista = new ArrayList<>();
        String sql = "SELECT anio_desde, anio_hasta FROM vehiculo WHERE marca ILIKE ? AND modelo ILIKE ? ORDER BY anio_desde";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, marca);
            ps.setString(2, modelo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int desde = rs.getInt("anio_desde");
                    int hasta = rs.getInt("anio_hasta");
                    if (rs.wasNull() || hasta == 0) hasta = desde;
                    for (int a = desde; a <= hasta; a++) if (!lista.contains(a)) lista.add(a);
                }
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOG.log(Level.SEVERE, "VehiculoDAO.listarAniosPorModelo", e);
        }
        return lista;
    }

    @Override
    public Optional<Vehiculo> obtenerPorId(int id) {
        String sql = "SELECT * FROM vehiculo WHERE id=? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return Optional.empty();
            LOG.log(Level.SEVERE, "VehiculoDAO.obtenerPorId", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Vehiculo> buscarPorVin(String vin17) {
        if (vin17 == null || vin17.length() < 3) return Optional.empty();
        String wmi = vin17.substring(0, 3).toUpperCase();
        // VIN año: 10º char
        String sql = "SELECT * FROM vehiculo WHERE vin_prefijo ILIKE ? LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, wmi + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "VehiculoDAO.buscarPorVin", e);
        }
        // Fallback: buscar por marca en VIN (no preciso)
        return Optional.empty();
    }

    @Override
    public int guardar(Vehiculo v) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return guardar(con, v);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "VehiculoDAO.guardar", e);
        }
        return -1;
    }

    @Override
    public int guardar(Connection con, Vehiculo v) throws SQLException {
        String sql = "INSERT INTO vehiculo(marca, modelo, anio_desde, anio_hasta, motor, combustible, vin_prefijo, pais_origen, tipo_vehiculo) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getMarca());
            ps.setString(2, v.getModelo());
            if (v.getAnioDesde() != null) ps.setInt(3, v.getAnioDesde()); else ps.setNull(3, Types.INTEGER);
            if (v.getAnioHasta() != null) ps.setInt(4, v.getAnioHasta()); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, v.getMotor());
            ps.setString(6, v.getCombustible());
            ps.setString(7, v.getVinPrefijo());
            ps.setString(8, v.getPaisOrigen());
            ps.setString(9, v.getTipoVehiculo());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    @Override
    public void actualizar(Vehiculo v) {
        String sql = "UPDATE vehiculo SET marca=?, modelo=?, anio_desde=?, anio_hasta=?, motor=?, combustible=?, vin_prefijo=?, pais_origen=?, tipo_vehiculo=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getMarca());
            ps.setString(2, v.getModelo());
            if (v.getAnioDesde() != null) ps.setInt(3, v.getAnioDesde()); else ps.setNull(3, Types.INTEGER);
            if (v.getAnioHasta() != null) ps.setInt(4, v.getAnioHasta()); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, v.getMotor());
            ps.setString(6, v.getCombustible());
            ps.setString(7, v.getVinPrefijo());
            ps.setString(8, v.getPaisOrigen());
            ps.setString(9, v.getTipoVehiculo());
            ps.setInt(10, v.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "VehiculoDAO.actualizar", e);
        }
    }

    @Override
    public void eliminar(int id) {
        String sql = "DELETE FROM vehiculo WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "VehiculoDAO.eliminar", e);
        }
    }

    @Override
    public List<Vehiculo> buscar(String marca, String modelo, Integer anio) {
        List<Vehiculo> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM vehiculo WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (marca != null && !marca.isBlank()) { sql.append(" AND marca ILIKE ?"); params.add(marca); }
        if (modelo != null && !modelo.isBlank()) { sql.append(" AND modelo ILIKE ?"); params.add(modelo); }
        if (anio != null) { sql.append(" AND ? BETWEEN COALESCE(anio_desde, ?) AND COALESCE(anio_hasta, ?)"); params.add(anio); params.add(anio); params.add(anio); }
        sql.append(" ORDER BY marca, modelo LIMIT 50");
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            if (isNoExiste(e)) return lista;
            LOG.log(Level.SEVERE, "VehiculoDAO.buscar", e);
        }
        return lista;
    }

    private Vehiculo mapear(ResultSet rs) throws SQLException {
        Vehiculo v = new Vehiculo();
        v.setId(rs.getInt("id"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));
        int desde = rs.getInt("anio_desde"); v.setAnioDesde(rs.wasNull() ? null : desde);
        int hasta = rs.getInt("anio_hasta"); v.setAnioHasta(rs.wasNull() ? null : hasta);
        v.setMotor(rs.getString("motor"));
        v.setCombustible(rs.getString("combustible"));
        v.setVinPrefijo(rs.getString("vin_prefijo"));
        v.setPaisOrigen(rs.getString("pais_origen"));
        v.setTipoVehiculo(rs.getString("tipo_vehiculo"));
        Timestamp ts = rs.getTimestamp("creado_en");
        if (ts != null) v.setCreadoEn(ts.toLocalDateTime());
        return v;
    }

    private boolean isNoExiste(SQLException e) {
        String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return m.contains("does not exist") || m.contains("no existe");
    }
}

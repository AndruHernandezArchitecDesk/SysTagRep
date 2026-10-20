package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.VendexiConsumo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VendexiConsumoDAOPostgres implements VendexiConsumoDAO {

    private static final Logger LOGGER = Logger.getLogger(VendexiConsumoDAOPostgres.class.getName());

    @Override
    public void insertar(VendexiConsumo c) {
        String sql = "INSERT INTO vendexi_consumo (fecha, modulo, operacion, modelo, tokens_entrada, tokens_salida, costo_estimado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(c.getFecha() != null ? c.getFecha() : LocalDateTime.now()));
            ps.setString(2, c.getModulo());
            ps.setString(3, c.getOperacion());
            ps.setString(4, c.getModelo());
            ps.setLong(5, c.getTokensEntrada());
            ps.setLong(6, c.getTokensSalida());
            ps.setBigDecimal(7, c.getCostoEstimado());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo registrar consumo Vendexi", e);
        }
    }

    @Override
    public long sumarTokensDesde(LocalDateTime desde) {
        String sql = "SELECT COALESCE(SUM(tokens_entrada + tokens_salida), 0) FROM vendexi_consumo WHERE fecha >= ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo sumar tokens Vendexi", e);
            return 0;
        }
    }

    @Override
    public BigDecimal sumarCostoDesde(LocalDateTime desde) {
        String sql = "SELECT COALESCE(SUM(costo_estimado), 0) FROM vendexi_consumo WHERE fecha >= ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo sumar costo Vendexi", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public List<VendexiConsumo> listarReciente(int limite) {
        String sql = "SELECT id, fecha, modulo, operacion, modelo, tokens_entrada, tokens_salida, costo_estimado " +
                "FROM vendexi_consumo ORDER BY fecha DESC LIMIT ?";
        List<VendexiConsumo> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VendexiConsumo c = new VendexiConsumo();
                    c.setId(rs.getLong("id"));
                    Timestamp f = rs.getTimestamp("fecha");
                    c.setFecha(f != null ? f.toLocalDateTime() : null);
                    c.setModulo(rs.getString("modulo"));
                    c.setOperacion(rs.getString("operacion"));
                    c.setModelo(rs.getString("modelo"));
                    c.setTokensEntrada(rs.getLong("tokens_entrada"));
                    c.setTokensSalida(rs.getLong("tokens_salida"));
                    c.setCostoEstimado(rs.getBigDecimal("costo_estimado"));
                    out.add(c);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo listar consumo Vendexi", e);
        }
        return out;
    }

    @Override
    public LocalDateTime obtenerInicioPeriodo() {
        String sql = "SELECT inicio FROM vendexi_periodo WHERE id = 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Timestamp t = rs.getTimestamp("inicio");
                if (t != null) return t.toLocalDateTime();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo leer periodo Vendexi", e);
        }
        return null;
    }

    @Override
    public void actualizarInicioPeriodo(LocalDateTime inicio) {
        String sql = "UPDATE vendexi_periodo SET inicio = ? WHERE id = 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            int n = ps.executeUpdate();
            if (n == 0) {
                try (PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO vendexi_periodo (id, inicio) VALUES (1, ?)")) {
                    ins.setTimestamp(1, Timestamp.valueOf(inicio));
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo actualizar periodo Vendexi", e);
        }
    }

    @Override
    public long iniciarSesion(String modulo, String tipo) {
        String sql = "INSERT INTO vendexi_sesion (modulo, tipo) VALUES (?, ?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, modulo);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo iniciar sesión Vendexi", e);
            return -1;
        }
    }

    @Override
    public void finalizarSesion(long idSesion) {
        String sql = "UPDATE vendexi_sesion SET fin = now(), " +
                "duracion_seg = GREATEST(0, EXTRACT(EPOCH FROM (now() - inicio))) WHERE id = ? AND fin IS NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idSesion);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "No se pudo finalizar sesión Vendexi", e);
        }
    }
}

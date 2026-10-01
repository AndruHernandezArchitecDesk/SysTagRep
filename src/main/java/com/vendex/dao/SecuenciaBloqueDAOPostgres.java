package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.SecuenciaBloque;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecuenciaBloqueDAOPostgres implements SecuenciaBloqueDAO {

    private static final Logger LOG = Logger.getLogger(SecuenciaBloqueDAOPostgres.class.getName());

    @Override
    public SecuenciaBloque obtenerBloqueDisponible(int puntoEmisionId, String tipo) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return obtenerBloqueDisponible(con, puntoEmisionId, tipo);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error obteniendo bloque disponible", e);
            return null;
        }
    }

    @Override
    public SecuenciaBloque obtenerBloqueDisponible(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM secuencia_bloque WHERE punto_emision_id=? AND tipo=? AND usado_hasta < numero_fin ORDER BY reservado_en ASC LIMIT 1"
        );
        ps.setInt(1, puntoEmisionId);
        ps.setString(2, tipo);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapear(rs);
        }
        return null;
    }

    @Override
    public SecuenciaBloque reservarBloque(int puntoEmisionId, String tipo, int tamano) {
        try (Connection con = DatabaseConnection.getConnection()) {
            return reservarBloque(con, puntoEmisionId, tipo, tamano);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error reservando bloque", e);
            return null;
        }
    }

    @Override
    public SecuenciaBloque reservarBloque(Connection con, int puntoEmisionId, String tipo, int tamano) throws SQLException {
        int maxFin;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(numero_fin), 0) FROM secuencia_bloque WHERE punto_emision_id=" + puntoEmisionId + " AND tipo='" + tipo + "'")) {
            rs.next();
            maxFin = rs.getInt(1);
        }
        int inicio = maxFin + 1;
        int fin = inicio + tamano - 1;
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO secuencia_bloque (punto_emision_id, tipo, numero_inicio, numero_fin, usado_hasta, reservado_en) VALUES (?, ?, ?, ?, ?, ?)"
        );
        ps.setInt(1, puntoEmisionId);
        ps.setString(2, tipo);
        ps.setInt(3, inicio);
        ps.setInt(4, fin);
        ps.setInt(5, inicio);
        ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
        ps.executeUpdate();
        SecuenciaBloque bloque = new SecuenciaBloque(puntoEmisionId, tipo, inicio, fin, inicio);
        bloque.setId(obtenerUltimoId(con));
        return bloque;
    }

    @Override
    public void marcarUsado(SecuenciaBloque bloque, int numeroUsado) {
        try (Connection con = DatabaseConnection.getConnection()) {
            marcarUsado(con, bloque, numeroUsado);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error marcando usado", e);
        }
    }

    @Override
    public void marcarUsado(Connection con, SecuenciaBloque bloque, int numeroUsado) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "UPDATE secuencia_bloque SET usado_hasta=? WHERE id=?"
        );
        ps.setInt(1, numeroUsado);
        ps.setInt(2, bloque.getId());
        ps.executeUpdate();
    }

    @Override
    public List<SecuenciaBloque> listarPorPuntoEmision(int puntoEmisionId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM secuencia_bloque WHERE punto_emision_id=? ORDER BY reservado_en DESC"
            );
            ps.setInt(1, puntoEmisionId);
            ResultSet rs = ps.executeQuery();
            List<SecuenciaBloque> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error listando bloques", e);
            return List.of();
        }
    }

    @Override
    public void liberarBloquesExpirados() {
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM secuencia_bloque WHERE reservado_en < NOW() - INTERVAL '24 hours' AND usado_hasta >= numero_fin");
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error liberando bloques expirados", e);
        }
    }

    private SecuenciaBloque mapear(ResultSet rs) throws SQLException {
        SecuenciaBloque b = new SecuenciaBloque();
        b.setId(rs.getInt("id"));
        b.setPuntoEmisionId(rs.getInt("punto_emision_id"));
        b.setTipo(rs.getString("tipo"));
        b.setNumeroInicio(rs.getInt("numero_inicio"));
        b.setNumeroFin(rs.getInt("numero_fin"));
        b.setUsadoHasta(rs.getInt("usado_hasta"));
        b.setReservadoEn(rs.getTimestamp("reservado_en").toLocalDateTime());
        return b;
    }

    private int obtenerUltimoId(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT LASTVAL()")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}

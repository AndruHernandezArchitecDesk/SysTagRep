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
                "SELECT * FROM secuencia_bloque WHERE punto_emision_id=? AND tipo=? AND usado_hasta < numero_fin ORDER BY reservado_en ASC LIMIT 1 FOR UPDATE"
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
            boolean autoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                SecuenciaBloque bloque = reservarBloque(con, puntoEmisionId, tipo, tamano);
                con.commit();
                return bloque;
            } catch (SQLException e) {
                try { con.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                try { con.setAutoCommit(autoCommit); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error reservando bloque", e);
            return null;
        }
    }

    @Override
    public SecuenciaBloque reservarBloque(Connection con, int puntoEmisionId, String tipo, int tamano) throws SQLException {
        if (tamano <= 0 || tamano > 10000) throw new SQLException("Tamaño de bloque inválido: " + tamano);
        // Serializa reservas concurrentes: dos PCs no pueden calcular el mismo MAX.
        try (Statement lock = con.createStatement()) {
            lock.execute("LOCK TABLE secuencia_bloque IN EXCLUSIVE MODE");
        }
        int maxFin;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(MAX(numero_fin), 0) FROM secuencia_bloque WHERE punto_emision_id=? AND tipo=?")) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                maxFin = rs.getInt(1);
            }
        }
        int inicio = maxFin + 1;
        int fin = inicio + tamano - 1;
        // Guardia anti-solape (defensa en profundidad si el lock no aplicó).
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM secuencia_bloque WHERE punto_emision_id=? AND tipo=? AND NOT (numero_fin < ? OR numero_inicio > ?) LIMIT 1")) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            ps.setInt(3, inicio);
            ps.setInt(4, fin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new SQLException("Solape de bloque detectado, reintente la reserva");
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO secuencia_bloque (punto_emision_id, tipo, numero_inicio, numero_fin, usado_hasta, reservado_en) VALUES (?, ?, ?, ?, ?, ?) RETURNING id")) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            ps.setInt(3, inicio);
            ps.setInt(4, fin);
            ps.setInt(5, inicio);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                SecuenciaBloque bloque = new SecuenciaBloque(puntoEmisionId, tipo, inicio, fin, inicio);
                bloque.setId(rs.getInt(1));
                return bloque;
            }
        }
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
        // Monótono: un avance obsoleto (menor) nunca retrocede el contador.
        PreparedStatement ps = con.prepareStatement(
                "UPDATE secuencia_bloque SET usado_hasta=? WHERE id=? AND usado_hasta < ?"
        );
        ps.setInt(1, numeroUsado);
        ps.setInt(2, bloque.getId());
        ps.setInt(3, numeroUsado);
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
            // Solo rangos muertos de verdad: consumidos, viejos y ya superados por la
            // secuencia online. Borrar sin esa guarda reciclaría números emitidos,
            // porque las reservas nuevas parten de MAX(numero_fin).
            // En esquemas legacy sin punto_emision_id en secuencia_documento no se borra.
            try (ResultSet rs = st.executeQuery("SELECT 1 FROM information_schema.columns "
                    + "WHERE table_name='secuencia_documento' AND column_name='punto_emision_id'")) {
                if (!rs.next()) return;
            }
            st.executeUpdate("DELETE FROM secuencia_bloque b WHERE b.reservado_en < NOW() - INTERVAL '24 hours' "
                    + "AND b.usado_hasta >= b.numero_fin AND b.numero_fin < (SELECT COALESCE(MAX(s.siguiente_numero), 0) "
                    + "FROM secuencia_documento s WHERE s.punto_emision_id = b.punto_emision_id AND s.tipo = b.tipo)");
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

}

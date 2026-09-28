package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.SecuenciaDocumento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecuenciaDocumentoDAOPostgres implements SecuenciaDocumentoDAO {

    private static final Logger LOGGER = Logger.getLogger(SecuenciaDocumentoDAOPostgres.class.getName());

    @Override
    public SecuenciaDocumento obtener(int puntoEmisionId, String tipo) {
        String sql = "SELECT tipo, establecimiento, punto_emision, siguiente_numero FROM secuencia_documento WHERE punto_emision_id=? AND tipo=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SecuenciaDocumento(puntoEmisionId,
                            rs.getString("tipo"), rs.getString("establecimiento"),
                            rs.getString("punto_emision"), rs.getInt("siguiente_numero"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.obtener", e);
        }
        return new SecuenciaDocumento(puntoEmisionId, tipo, "001", "001", 1);
    }

    @Override
    public boolean establecer(int puntoEmisionId, String tipo, String prefijo, String establecimiento, String puntoEmision, int numero) {
        String sql = "INSERT INTO secuencia_documento(punto_emision_id, tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (punto_emision_id, tipo) DO UPDATE SET prefijo=EXCLUDED.prefijo, " +
                     "establecimiento=EXCLUDED.establecimiento, punto_emision=EXCLUDED.punto_emision, siguiente_numero=EXCLUDED.siguiente_numero";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            ps.setString(3, prefijo);
            ps.setString(4, establecimiento);
            ps.setString(5, puntoEmision);
            ps.setInt(6, numero);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.establecer", e);
        }
        return false;
    }

    @Override
    public int marcarUsado(int puntoEmisionId, String tipo) {
        asegurarFila(puntoEmisionId, tipo);
        String sql = "UPDATE secuencia_documento SET siguiente_numero = siguiente_numero + 1 WHERE punto_emision_id=? AND tipo=? RETURNING siguiente_numero - 1 AS usado";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("usado");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.marcarUsado", e);
        }
        return -1;
    }

    @Override
    public int marcarUsado(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        asegurarFila(con, puntoEmisionId, tipo);
        String sql = "UPDATE secuencia_documento SET siguiente_numero = siguiente_numero + 1 WHERE punto_emision_id=? AND tipo=? RETURNING siguiente_numero - 1 AS usado";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("usado");
            }
        }
        return -1;
    }

    @Override
    public SecuenciaDocumento obtener(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        String sql = "SELECT tipo, establecimiento, punto_emision, siguiente_numero FROM secuencia_documento WHERE punto_emision_id=? AND tipo=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SecuenciaDocumento(puntoEmisionId, rs.getString("tipo"),
                            rs.getString("establecimiento"), rs.getString("punto_emision"), rs.getInt("siguiente_numero"));
                }
            }
        }
        return new SecuenciaDocumento(puntoEmisionId, tipo, "001", "001", 1);
    }

    private void asegurarFila(int puntoEmisionId, String tipo) {
        String sql = "INSERT INTO secuencia_documento(punto_emision_id, tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                     "VALUES (?, ?, '001', '001', '001', 1) ON CONFLICT (punto_emision_id, tipo) DO NOTHING";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.asegurarFila", e);
        }
    }

    private void asegurarFila(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        String sql = "INSERT INTO secuencia_documento(punto_emision_id, tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                     "VALUES (?, ?, '001', '001', '001', 1) ON CONFLICT (punto_emision_id, tipo) DO NOTHING";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntoEmisionId);
            ps.setString(2, tipo);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean existeCodigoNotaVenta(String codigo) {
        return existeCodigo("nota_venta_registro", "codigo", codigo);
    }

    @Override
    public boolean existeCodigoFactura(String codigo) {
        return existeCodigo("factura_registro", "codigo", codigo);
    }

    @Override
    public boolean existeCodigoNotaCredito(String codigo) {
        return existeCodigo("nota_credito_registro", "clave_acceso", codigo);
    }

    private boolean existeCodigo(String tabla, String columna, String codigo) {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + columna + "=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.existeCodigo", e);
        }
        return false;
    }
}

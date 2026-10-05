package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.SecuenciaDocumento;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecuenciaDocumentoDAOPostgres implements SecuenciaDocumentoDAO {

    private static final Logger LOGGER = Logger.getLogger(SecuenciaDocumentoDAOPostgres.class.getName());
    private static final AtomicBoolean PUNTO_EMISION_ID_DETECTADO = new AtomicBoolean(false);
    private static final AtomicBoolean PUNTO_EMISION_ID_EXISTE = new AtomicBoolean(false);

    @Override
    public SecuenciaDocumento obtener(int puntoEmisionId, String tipo) {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
            return obtenerPorPuntoEmisionId(puntoEmisionId, tipo);
        }
        return obtenerLegacy(tipo);
    }

    private void detectarEsquema() {
        PUNTO_EMISION_ID_DETECTADO.set(true);
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'secuencia_documento' AND column_name = 'punto_emision_id'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                PUNTO_EMISION_ID_EXISTE.set(rs.getInt(1) > 0);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.detectarEsquema", e);
        }
    }

    private SecuenciaDocumento obtenerPorPuntoEmisionId(int puntoEmisionId, String tipo) {
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

    private SecuenciaDocumento obtenerLegacy(String tipo) {
        String sql = "SELECT tipo, establecimiento, punto_emision, siguiente_numero FROM secuencia_documento WHERE tipo=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String establecimiento = rs.getString("establecimiento");
                    String puntoEmision = rs.getString("punto_emision");
                    return new SecuenciaDocumento(1,
                            rs.getString("tipo"),
                            establecimiento != null ? establecimiento : "001",
                            puntoEmision != null ? puntoEmision : "001",
                            rs.getInt("siguiente_numero"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.obtenerLegacy", e);
        }
        return new SecuenciaDocumento(1, tipo, "001", "001", 1);
    }

    @Override
    public boolean establecer(int puntoEmisionId, String tipo, String prefijo, String establecimiento, String puntoEmision, int numero) {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
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
        String sql = "INSERT INTO secuencia_documento(tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (tipo) DO UPDATE SET prefijo=EXCLUDED.prefijo, " +
                "establecimiento=EXCLUDED.establecimiento, punto_emision=EXCLUDED.punto_emision, siguiente_numero=EXCLUDED.siguiente_numero";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, prefijo);
            ps.setString(3, establecimiento);
            ps.setString(4, puntoEmision);
            ps.setInt(5, numero);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.establecer", e);
        }
        return false;
    }

    @Override
    public int marcarUsado(int puntoEmisionId, String tipo) {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
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
        String sql = "UPDATE secuencia_documento SET siguiente_numero = siguiente_numero + 1 WHERE tipo=? RETURNING siguiente_numero - 1 AS usado";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("usado");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.marcarUsadoLegacy", e);
        }
        return -1;
    }

    @Override
    public int marcarUsado(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
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
        String sql = "UPDATE secuencia_documento SET siguiente_numero = siguiente_numero + 1 WHERE tipo=? RETURNING siguiente_numero - 1 AS usado";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("usado");
            }
        }
        return -1;
    }

    @Override
    public SecuenciaDocumento obtener(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
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
        String sql = "SELECT tipo, establecimiento, punto_emision, siguiente_numero FROM secuencia_documento WHERE tipo=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SecuenciaDocumento(1, rs.getString("tipo"),
                            rs.getString("establecimiento"), rs.getString("punto_emision"), rs.getInt("siguiente_numero"));
                }
            }
        }
        return new SecuenciaDocumento(1, tipo, "001", "001", 1);
    }

    private void asegurarFila(int puntoEmisionId, String tipo) {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
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
            return;
        }
        String sql = "INSERT INTO secuencia_documento(tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                "VALUES (?, '001', '001', '001', 1) ON CONFLICT (tipo) DO NOTHING";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SecuenciaDocumentoDAO.asegurarFilaLegacy", e);
        }
    }

    private void asegurarFila(Connection con, int puntoEmisionId, String tipo) throws SQLException {
        if (!PUNTO_EMISION_ID_DETECTADO.get()) {
            detectarEsquema();
        }
        if (PUNTO_EMISION_ID_EXISTE.get()) {
            String sql = "INSERT INTO secuencia_documento(punto_emision_id, tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                    "VALUES (?, ?, '001', '001', '001', 1) ON CONFLICT (punto_emision_id, tipo) DO NOTHING";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, puntoEmisionId);
                ps.setString(2, tipo);
                ps.executeUpdate();
            }
            return;
        }
        String sql = "INSERT INTO secuencia_documento(tipo, prefijo, establecimiento, punto_emision, siguiente_numero) " +
                "VALUES (?, '001', '001', '001', 1) ON CONFLICT (tipo) DO NOTHING";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
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

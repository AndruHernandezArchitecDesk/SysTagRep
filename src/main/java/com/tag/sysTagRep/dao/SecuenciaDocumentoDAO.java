package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.SecuenciaDocumento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SecuenciaDocumentoDAO {

    public SecuenciaDocumento obtener(String tipo) {
        String sql = "SELECT tipo, establecimiento, punto_emision, siguiente_numero FROM secuencia_documento WHERE tipo=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SecuenciaDocumento(
                            rs.getString("tipo"),
                            rs.getString("establecimiento"),
                            rs.getString("punto_emision"),
                            rs.getInt("siguiente_numero"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new SecuenciaDocumento(tipo, "001", "001", 1);
    }

    public void actualizarConfiguracion(String tipo, String establecimiento, String puntoEmision) {
        String sql = "UPDATE secuencia_documento SET establecimiento=?, punto_emision=? WHERE tipo=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, establecimiento);
            ps.setString(2, puntoEmision);
            ps.setString(3, tipo);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int marcarUsado(String tipo) {
        String sql = "UPDATE secuencia_documento SET siguiente_numero = siguiente_numero + 1 WHERE tipo=? RETURNING siguiente_numero - 1 AS usado";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("usado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean existeCodigoNotaVenta(String codigo) {
        return existeCodigo("nota_venta_registro", "codigo", codigo);
    }

    public boolean existeCodigoFactura(String codigo) {
        return existeCodigo("factura_registro", "codigo", codigo);
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
            e.printStackTrace();
        }
        return false;
    }

    public boolean iniciarEn(String tipo, int numero) {
        String sql = "UPDATE secuencia_documento SET siguiente_numero=? WHERE tipo=? AND siguiente_numero <= ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, numero);
            ps.setString(2, tipo);
            ps.setInt(3, numero);
            int filas = ps.executeUpdate();
            return filas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

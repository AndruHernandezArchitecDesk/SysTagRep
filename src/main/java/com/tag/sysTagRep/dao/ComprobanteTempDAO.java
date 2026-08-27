package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.ComprobanteTemp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComprobanteTempDAO {

    private static final Logger LOGGER = Logger.getLogger(ComprobanteTempDAO.class.getName());

    public ComprobanteTempDAO() {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() {
        String sql = "CREATE TABLE IF NOT EXISTS comprobante_temp (" +
                "id SERIAL PRIMARY KEY, " +
                "proforma_id INT REFERENCES nota_venta_registro(id) ON DELETE CASCADE, " +
                "codigo VARCHAR(50) NOT NULL, " +
                "descripcion VARCHAR(500) NOT NULL, " +
                "cantidad INT NOT NULL DEFAULT 1, " +
                "precio_unitario NUMERIC(12,2) NOT NULL, " +
                "precio_total NUMERIC(12,2) NOT NULL, " +
                "fecha_creacion TIMESTAMP DEFAULT NOW()" +
                ")";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creando tabla comprobante_temp", e);
        }
    }

    public int insertar(int proformaId, String codigo, String descripcion, int cantidad,
                        java.math.BigDecimal precioUnitario, java.math.BigDecimal precioTotal) {
        String sql = "INSERT INTO comprobante_temp(proforma_id, codigo, descripcion, cantidad, precio_unitario, precio_total) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (proformaId > 0) ps.setInt(1, proformaId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, codigo);
            ps.setString(3, descripcion);
            ps.setInt(4, cantidad);
            ps.setBigDecimal(5, precioUnitario);
            ps.setBigDecimal(6, precioTotal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error insertando comprobante_temp", e);
        }
        return -1;
    }

    public List<ComprobanteTemp> listarPorProforma(int proformaId) {
        List<ComprobanteTemp> lista = new ArrayList<>();
        String sql = "SELECT * FROM comprobante_temp WHERE proforma_id = ? ORDER BY id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, proformaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComprobanteTemp c = new ComprobanteTemp();
                    c.setId(rs.getInt("id"));
                    c.setProformaId(rs.getInt("proforma_id"));
                    c.setCodigo(rs.getString("codigo"));
                    c.setDescripcion(rs.getString("descripcion"));
                    c.setCantidad(rs.getInt("cantidad"));
                    c.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    c.setPrecioTotal(rs.getBigDecimal("precio_total"));
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    if (ts != null) c.setFechaCreacion(ts.toLocalDateTime());
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error listando comprobante_temp", e);
        }
        return lista;
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM comprobante_temp WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error eliminando comprobante_temp", e);
        }
    }

    public void limpiarPorProforma(int proformaId) {
        String sql = "DELETE FROM comprobante_temp WHERE proforma_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, proformaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error limpiando comprobante_temp", e);
        }
    }
}

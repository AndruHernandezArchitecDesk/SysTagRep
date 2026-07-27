package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.FacturaRegistro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacturaRegistroDAO {

    public List<FacturaRegistro> obtenerNumFactura() {
        List<FacturaRegistro> lista = new ArrayList<>();
        String sql = "SELECT * FROM factura_registro ORDER BY id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaRegistro f = new FacturaRegistro();
                f.setId(rs.getInt("id"));
                f.setCodigo(rs.getString("codigo"));
                lista.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public int insertar(FacturaRegistro fr) {
        String sql = "INSERT INTO factura_registro(empresa_id, cliente_id, fecha, codigo, forma_pago, " +
                     "subtotal, iva, total, clave_acceso, num_comprobante, ambiente_sri, estado_sri, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fr.getEmpresaId());
            ps.setInt(2, fr.getClienteId());
            ps.setObject(3, fr.getFecha());
            ps.setString(4, fr.getCodigo());
            ps.setString(5, fr.getFormaPago());
            ps.setBigDecimal(6, fr.getSubtotal());
            ps.setBigDecimal(7, fr.getIva());
            ps.setBigDecimal(8, fr.getTotal());
            ps.setString(9, fr.getClaveAcceso());
            ps.setString(10, fr.getNumComprobante());
            ps.setString(11, fr.getAmbienteSri());
            ps.setString(12, fr.getEstadoSri());
            ps.setObject(13, fr.getFechaRegistro());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void actualizarEstado(String claveAcceso, String estado) {
        String sql = "UPDATE factura_registro SET estado_sri = ? WHERE clave_acceso = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, claveAcceso);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

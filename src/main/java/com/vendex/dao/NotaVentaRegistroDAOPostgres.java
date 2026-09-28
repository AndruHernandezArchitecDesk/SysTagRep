package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaVentaRegistro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotaVentaRegistroDAOPostgres implements NotaVentaRegistroDAO {
    public List<NotaVentaRegistro> obtenerNumNotaVenta() {
        List<NotaVentaRegistro> lista = new ArrayList<>();
        String sql = "SELECT * FROM nota_venta_registro ORDER BY id";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                NotaVentaRegistro n = new NotaVentaRegistro();
                n.setId(rs.getInt("id"));
                n.setCodigo(rs.getString("codigo"));
                try { n.setSucursalId(rs.getInt("sucursal_id")); if (rs.wasNull()) n.setSucursalId(1); } catch (Exception ignore) {}
                lista.add(n);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public int insertar(NotaVentaRegistro nvr) {
        String sql = "INSERT INTO nota_venta_registro(empresa_id, cliente_id, fecha, codigo, forma_pago, fecha_registro, sucursal_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nvr.getEmpresaId());
            ps.setInt(2, nvr.getClienteId());
            ps.setObject(3, nvr.getFecha());
            ps.setString(4, nvr.getCodigo());
            ps.setString(5, nvr.getFormaPago());
            ps.setObject(6, nvr.getFechaRegistro());
            if (nvr.getSucursalId() != null) ps.setInt(7, nvr.getSucursalId()); else ps.setNull(7, java.sql.Types.INTEGER);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

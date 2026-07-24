package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.NotaVentaRegistro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotaVentaRegistroDAO {
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
                lista.add(n);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public int insertar(NotaVentaRegistro nvr) {
        String sql = "INSERT INTO nota_venta_registro(empresa_id, cliente_id, fecha, codigo, forma_pago, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nvr.getEmpresaId());
            ps.setInt(2, nvr.getClienteId());
            ps.setObject(3, nvr.getFecha());
            ps.setString(4, nvr.getCodigo());
            ps.setString(5, nvr.getFormaPago());
            ps.setObject(6, nvr.getFechaRegistro());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}

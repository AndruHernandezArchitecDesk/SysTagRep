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
                n.setCodigo(rs.getInt("codigo"));
                lista.add(n);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

}

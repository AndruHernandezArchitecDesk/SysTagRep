package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Empresa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmpresaDAO {
    public List<Empresa> listar() {
        List<Empresa> lista = new ArrayList<>();
        String sql = "SELECT * FROM empresa WHERE estado = true ORDER BY id";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Empresa e = new Empresa();
                e.setId(rs.getInt("id"));
                e.setRuc(rs.getString("ruc"));
                e.setRazonSocial(rs.getString("razon_social"));
                e.setSucursal(rs.getString("sucursal"));
                e.setDireccionCallePrincipal(rs.getString("direccion_calle_principal"));
                e.setDireccionCalleSecundaria(rs.getString("direccion_calle_secundaria"));
                e.setTelefono(rs.getString("telefono"));
                e.setCelular(rs.getString("celular"));
                e.setCorreo(rs.getString("correo"));
                e.setLogoUrl(rs.getString("logo_url"));
                e.setAgenteRetencion(rs.getString("agente_retencion"));
                e.setResolucion(rs.getString("resolucion"));
                lista.add(e);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}

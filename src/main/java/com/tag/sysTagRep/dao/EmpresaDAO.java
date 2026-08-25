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
    public Empresa obtenerPorId(int id) {
        String sql = "SELECT * FROM empresa WHERE id = ?";
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Empresa> listar() {
        List<Empresa> lista = new ArrayList<>();
        String sql = "SELECT * FROM empresa WHERE estado = true ORDER BY id";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public boolean actualizar(Empresa empresa) {
        String sql = "UPDATE empresa SET razon_social = ?, logo_url = ?, agente_retencion = ?, resolucion = ?, estado = ? WHERE id = ?";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empresa.getRazonSocial());
            ps.setString(2, empresa.getLogoUrl());
            ps.setString(3, empresa.getAgenteRetencion());
            ps.setString(4, empresa.getResolucion());
            ps.setBoolean(5, empresa.isEstado());
            ps.setInt(6, empresa.getId());

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Empresa mapear(ResultSet rs) throws SQLException {
        Empresa e = new Empresa();
        e.setId(rs.getInt("id"));
        e.setRuc(rs.getString("ruc"));
        e.setRazonSocial(rs.getString("razon_social"));
        e.setTitulo(rs.getString("titulo"));
        e.setSucursal(rs.getString("sucursal"));
        e.setDireccionCallePrincipal(rs.getString("direccion_calle_principal"));
        e.setDireccionCalleSecundaria(rs.getString("direccion_calle_secundaria"));
        e.setTelefono(rs.getString("telefono"));
        e.setCelular(rs.getString("celular"));
        e.setCorreo(rs.getString("correo"));
        e.setLogoUrl(rs.getString("logo_url"));
        e.setAgenteRetencion(rs.getString("agente_retencion"));
        e.setResolucion(rs.getString("resolucion"));
        e.setEstado(rs.getBoolean("estado"));
        return e;
    }
}

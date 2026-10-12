package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Empresa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmpresaDAOPostgres implements EmpresaDAO {
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
        String sql = "UPDATE empresa SET ruc = ?, razon_social = ?, titulo = ?, sucursal = ?, "
                + "direccion_calle_principal = ?, direccion_calle_secundaria = ?, telefono = ?, celular = ?, "
                + "correo = ?, logo_url = ?, agente_retencion = ?, resolucion = ?, obligado_contabilidad = ?, estado = ? "
                + "WHERE id = ?";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empresa.getRuc());
            ps.setString(2, empresa.getRazonSocial());
            ps.setString(3, empresa.getTitulo());
            ps.setString(4, empresa.getSucursal());
            ps.setString(5, empresa.getDireccionCallePrincipal());
            ps.setString(6, empresa.getDireccionCalleSecundaria());
            ps.setString(7, empresa.getTelefono());
            ps.setString(8, empresa.getCelular());
            ps.setString(9, empresa.getCorreo());
            ps.setString(10, empresa.getLogoUrl());
            ps.setString(11, empresa.getAgenteRetencion());
            ps.setString(12, empresa.getResolucion());
            ps.setString(13, empresa.getObligadoContabilidad());
            ps.setBoolean(14, empresa.isEstado());
            ps.setInt(15, empresa.getId());

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Empresa crear(Empresa empresa) {
        String sql = "INSERT INTO empresa(ruc, razon_social, titulo, sucursal, direccion_calle_principal, "
                + "direccion_calle_secundaria, telefono, celular, correo, logo_url, agente_retencion, resolucion, "
                + "obligado_contabilidad, estado) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, empresa.getRuc());
            ps.setString(2, empresa.getRazonSocial());
            ps.setString(3, empresa.getTitulo());
            ps.setString(4, empresa.getSucursal());
            ps.setString(5, empresa.getDireccionCallePrincipal());
            ps.setString(6, empresa.getDireccionCalleSecundaria());
            ps.setString(7, empresa.getTelefono());
            ps.setString(8, empresa.getCelular());
            ps.setString(9, empresa.getCorreo());
            ps.setString(10, empresa.getLogoUrl());
            ps.setString(11, empresa.getAgenteRetencion());
            ps.setString(12, empresa.getResolucion());
            ps.setString(13, empresa.getObligadoContabilidad());
            ps.setBoolean(14, empresa.isEstado());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) empresa.setId(rs.getInt(1));
            }
            return empresa;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Empresa mapear(ResultSet rs) throws SQLException {
        Empresa e = new Empresa();
        e.setId(rs.getInt("id"));
        e.setRuc(rs.getString("ruc"));
        e.setRazonSocial(rs.getString("razon_social"));
        try { e.setTitulo(rs.getString("titulo")); } catch (SQLException ignore) {}
        e.setSucursal(rs.getString("sucursal"));
        e.setDireccionCallePrincipal(rs.getString("direccion_calle_principal"));
        e.setDireccionCalleSecundaria(rs.getString("direccion_calle_secundaria"));
        e.setTelefono(rs.getString("telefono"));
        e.setCelular(rs.getString("celular"));
        e.setCorreo(rs.getString("correo"));
        try { e.setLogoUrl(rs.getString("logo_url")); } catch (SQLException ignore) {}
        e.setAgenteRetencion(rs.getString("agente_retencion"));
        e.setResolucion(rs.getString("resolucion"));
        try { e.setObligadoContabilidad(rs.getString("obligado_contabilidad")); } catch (SQLException ignore) {}
        try { e.setEstado(rs.getBoolean("estado")); } catch (SQLException ignore) { e.setEstado(true); }
        return e;
    }
}

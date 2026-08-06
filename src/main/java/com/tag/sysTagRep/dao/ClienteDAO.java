package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public List<Cliente> obtenerListaClientes() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id,nombre,identificacion,direccion,correo,telefono,celular FROM cliente ORDER BY nombre ASC";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente c = new Cliente();
                c.setId(rs.getInt("id"));
                c.setNombre(rs.getString("nombre"));
                c.setIdentificacion(rs.getString("identificacion"));
                c.setDireccion(rs.getString("direccion"));
                c.setCorreo(rs.getString("correo"));
                c.setTelefono(rs.getString("telefono"));
                c.setCelular(rs.getString("celular"));
                lista.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    public List<Cliente> listar() {

        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM cliente ORDER BY nombre ASC";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente c = new Cliente();
                c.setId(rs.getInt("id"));
                c.setNombre(rs.getString("nombre"));
                c.setIdentificacion(rs.getString("identificacion"));
                c.setDireccion(rs.getString("direccion"));
                c.setCorreo(rs.getString("correo"));
                c.setTelefono(rs.getString("telefono"));
                c.setCelular(rs.getString("celular"));
                c.setFecha_registro(rs.getObject("fecha_registro", LocalDateTime.class));
                c.setEstado(rs.getBoolean("estado"));
                lista.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    private static final String INSERT =
            "INSERT INTO cliente(nombre, identificacion, direccion, correo, telefono, celular, fecha_registro, estado) VALUES (?, ?, ?, ?, ?, ?, now(), ?)";

    public void guardar(Cliente cliente) {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getIdentificacion());
            ps.setString(3, cliente.getDireccion());
            ps.setString(4, cliente.getCorreo());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getCelular());
            ps.setBoolean(7, cliente.isEstado());

            ps.executeUpdate();

            System.out.println("✔ Cliente guardado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar cliente");
            e.printStackTrace();
        }
    }

    private static final String UPDATE =
            "UPDATE cliente SET nombre=?, identificacion=?, direccion=?, correo=?, telefono=?, celular=?, estado=? WHERE id=?";

    public void actualizar(Cliente cliente) {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getIdentificacion());
            ps.setString(3, cliente.getDireccion());
            ps.setString(4, cliente.getCorreo());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getCelular());
            ps.setBoolean(7, cliente.isEstado());
            ps.setInt(8, cliente.getId());
            ps.executeUpdate();

            System.out.println("✔ Cliente actualizado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar cliente");
            e.printStackTrace();
        }
    }

    public Cliente obtenerPorId(int id) {
        String sql = "SELECT id,nombre,identificacion,direccion,correo,telefono,celular FROM cliente WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente c = new Cliente();
                    c.setId(rs.getInt("id"));
                    c.setNombre(rs.getString("nombre"));
                    c.setIdentificacion(rs.getString("identificacion"));
                    c.setDireccion(rs.getString("direccion"));
                    c.setCorreo(rs.getString("correo"));
                    c.setTelefono(rs.getString("telefono"));
                    c.setCelular(rs.getString("celular"));
                    return c;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void eliminar(int id) {

        String SQL = "DELETE FROM cliente WHERE id=?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✔ Cliente eliminado");

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar cliente");
            e.printStackTrace();
        }
    }
}

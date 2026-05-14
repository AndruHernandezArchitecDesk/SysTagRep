package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    public List<Proveedor> listar() {

        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM proveedor ORDER BY id";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Proveedor v = new Proveedor();
                v.setId(rs.getInt("id"));
                v.setNombre(rs.getString("nombre"));
                v.setIdentificacion(rs.getString("identificacion"));
                v.setDireccion(rs.getString("direccion"));
                v.setCorreo(rs.getString("correo"));
                v.setTelefono(rs.getString("telefono"));
                v.setCelular(rs.getString("celular"));
                v.setFecha_registro(rs.getObject("fecha_registro", LocalDateTime.class));
                v.setEstado(rs.getBoolean("estado"));

                lista.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    private static final String INSERT =
            "INSERT INTO proveedor(nombre, identificacion, direccion, correo, telefono, celular, fecha_registro, estado) VALUES (?, ?, ?, ?,?,?,now(),?)";

    public void guardar(Proveedor proveedor) {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {

            ps.setString(1, proveedor.getNombre());
            ps.setString(2, proveedor.getIdentificacion());
            ps.setString(3, proveedor.getDireccion());
            ps.setString(4, proveedor.getCorreo());
            ps.setString(5, proveedor.getTelefono());
            ps.setString(6, proveedor.getCelular());
            ps.setBoolean(7, proveedor.isEstado());

            ps.executeUpdate();

            System.out.println("✔ Proveedor guardado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar proveedor");
            e.printStackTrace();
        }
    }

    private static final String UPDATE =
            "UPDATE proveedor SET nombre=?, identificacion=?, direccion=?, correo=?, telefono=?, celular=?, estado=? WHERE id=?";

    public void actualizar(Proveedor proveedor) {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {

            ps.setString(1, proveedor.getNombre());
            ps.setString(2, proveedor.getIdentificacion());
            ps.setString(3, proveedor.getDireccion());
            ps.setString(4, proveedor.getCorreo());
            ps.setString(5, proveedor.getTelefono());
            ps.setString(6, proveedor.getCelular());
            ps.setBoolean(7, proveedor.isEstado());
            ps.setInt(8, proveedor.getId());
            ps.executeUpdate();

            System.out.println("✔ Proveedor actualizado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizado  proveedor");
            e.printStackTrace();
        }
    }

    public void eliminar(int id) {

        String SQL = "DELETE FROM proveedor WHERE id=?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✔ Proveedor eliminado");

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar proveedor");
            e.printStackTrace();
        }
    }
}

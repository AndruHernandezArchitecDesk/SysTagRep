package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Vendedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendedorDAO {

    public List<Vendedor> listar() {

        List<Vendedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM vendedor ORDER BY id";

        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Vendedor v = new Vendedor();
                v.setId(rs.getInt("id"));
                v.setNombre(rs.getString("nombre"));
                v.setIdentificacion(rs.getString("identificacion"));
                v.setCorreo(rs.getString("correo"));
                v.setEstado(rs.getBoolean("estado"));

                lista.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    private static final String INSERT =
            "INSERT INTO vendedor(nombre, identificacion, correo, estado) VALUES (?, ?, ?, ?)";

    public void guardar(Vendedor vendedor) {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {

            ps.setString(1, vendedor.getNombre());
            ps.setString(2, vendedor.getIdentificacion());
            ps.setString(3, vendedor.getCorreo());
            ps.setBoolean(4, vendedor.isEstado());

            ps.executeUpdate();

            System.out.println("✔ Vendedor guardado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar vendedor");
            e.printStackTrace();
        }
    }

    private static final String UPDATE =
            "UPDATE vendedor SET nombre=?, identificacion=?, correo=?, estado=? WHERE id=?";

    public void actualizar(Vendedor vendedor) {

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {

            ps.setString(1, vendedor.getNombre());
            ps.setString(2, vendedor.getIdentificacion());
            ps.setString(3, vendedor.getCorreo());
            ps.setBoolean(4, vendedor.isEstado());
            ps.setInt(5, vendedor.getId());
            ps.executeUpdate();

            System.out.println("✔ Vendedor actualizado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizado  vendedor");
            e.printStackTrace();
        }
    }

    public void eliminar(int id) {

        String SQL = "DELETE FROM vendedor WHERE id=?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✔ Vendedor eliminado");

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar vendedor");
            e.printStackTrace();
        }
    }

}

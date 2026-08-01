package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Usuario;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }

    public Usuario autenticar(String username, String password) {
        String sql = "SELECT * FROM usuarios WHERE username=? AND password=? AND activo=true";
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error de conexión a BD: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY id ASC";

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

    public Usuario obtenerPorId(int id) {
        String sql = "SELECT * FROM usuarios WHERE id=?";
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

    public int guardar(Usuario u) {
        String hash = hashPassword(u.getPassword());
        String sql = "INSERT INTO usuarios (nombre, apellido, email, username, password, password_hash, rol, activo, permisos, fecha_creacion) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getUsername());
            ps.setString(5, hash);
            ps.setString(6, hash);
            ps.setString(7, u.getRol());
            ps.setBoolean(8, u.isEstado());
            ps.setString(9, u.getPermisos());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void actualizar(Usuario u) {
        String hash = u.getPassword() != null && !u.getPassword().isEmpty() ? hashPassword(u.getPassword()) : null;
        String sql;
        if (hash != null) {
            sql = "UPDATE usuarios SET nombre=?, apellido=?, email=?, username=?, password=?, password_hash=?, rol=?, activo=?, permisos=? WHERE id=?";
        } else {
            sql = "UPDATE usuarios SET nombre=?, apellido=?, email=?, username=?, rol=?, activo=?, permisos=? WHERE id=?";
        }
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getUsername());
            if (hash != null) {
                ps.setString(5, hash);
                ps.setString(6, hash);
                ps.setString(7, u.getRol());
                ps.setBoolean(8, u.isEstado());
                ps.setString(9, u.getPermisos());
                ps.setInt(10, u.getId());
            } else {
                ps.setString(5, u.getRol());
                ps.setBoolean(6, u.isEstado());
                ps.setString(7, u.getPermisos());
                ps.setInt(8, u.getId());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM usuarios WHERE id=?";
        try (Connection con = new DatabaseConnection().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setApellido(rs.getString("apellido"));
        u.setCorreo(rs.getString("email"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRol(rs.getString("rol"));
        u.setFecha_creacion(rs.getTimestamp("fecha_creacion") != null ? rs.getTimestamp("fecha_creacion").toLocalDateTime() : null);
        u.setUltimo_login(rs.getTimestamp("ultimo_login") != null ? rs.getTimestamp("ultimo_login").toLocalDateTime() : null);
        u.setEstado(rs.getBoolean("activo"));
        u.setPermisos(rs.getString("permisos"));
        return u;
    }
}

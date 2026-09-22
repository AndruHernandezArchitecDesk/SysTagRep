package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Usuario;
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

    // --- Anti fuerza bruta ---

    public Usuario buscarPorUsername(String username) {
        String sql = "SELECT * FROM usuarios WHERE username=? FOR UPDATE";
        // HSQLDB no soporta FOR UPDATE con todos los tipos; fallback sin lock
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            // fallback sin FOR UPDATE
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps2 = con.prepareStatement("SELECT * FROM usuarios WHERE username=?")) {
                ps2.setString(1, username);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) return mapear(rs);
                }
            } catch (SQLException e2) { e2.printStackTrace(); }
        }
        return null;
    }

    public boolean verificarPassword(Usuario u, String passwordPlano) {
        String hash = hashPassword(passwordPlano);
        String stored = u.getPassword();
        if (stored == null) return false;
        return stored.equals(hash);
    }

    public void incrementarIntentoFallido(int id) {
        String sql = "UPDATE usuarios SET intentos_fallidos = intentos_fallidos + 1, ultimo_intento_fallido = NOW() WHERE id=?";
        // HSQLDB: NOW() -> CURRENT_TIMESTAMP
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE usuarios SET intentos_fallidos = intentos_fallidos + 1, ultimo_intento_fallido = CURRENT_TIMESTAMP WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e2) { e2.printStackTrace(); }
        }
    }

    public void bloquear(int id, java.time.LocalDateTime hasta) {
        String sql = "UPDATE usuarios SET bloqueado_hasta=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, hasta == null ? null : java.sql.Timestamp.valueOf(hasta));
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void resetearIntentos(int id) {
        String sql = "UPDATE usuarios SET intentos_fallidos=0, bloqueado_hasta=NULL, ultimo_intento_fallido=NULL, ultimo_login=NOW() WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE usuarios SET intentos_fallidos=0, bloqueado_hasta=NULL, ultimo_intento_fallido=NULL, ultimo_login=CURRENT_TIMESTAMP WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e2) { e2.printStackTrace(); }
        }
    }

    public void resetearSiVentanaExpirada(int id, java.time.LocalDateTime ultimoIntento, int ventanaMinutos) {
        if (ultimoIntento == null) return;
        if (ultimoIntento.isBefore(java.time.LocalDateTime.now().minusMinutes(ventanaMinutos))) {
            String sql = "UPDATE usuarios SET intentos_fallidos=0, bloqueado_hasta=NULL WHERE id=?";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public void desbloquear(int id) {
        String sql = "UPDATE usuarios SET intentos_fallidos=0, bloqueado_hasta=NULL, ultimo_intento_fallido=NULL WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
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
        // columnas anti fuerza bruta (pueden no existir en instalaciones viejas)
        try { u.setIntentosFallidos(rs.getInt("intentos_fallidos")); } catch (SQLException ignored) { u.setIntentosFallidos(0); }
        try { java.sql.Timestamp t = rs.getTimestamp("ultimo_intento_fallido"); u.setUltimoIntentoFallido(t != null ? t.toLocalDateTime() : null); } catch (SQLException ignored) {}
        try { java.sql.Timestamp t = rs.getTimestamp("bloqueado_hasta"); u.setBloqueadoHasta(t != null ? t.toLocalDateTime() : null); } catch (SQLException ignored) {}
        return u;
    }
}

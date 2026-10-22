package com.vendex.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.config.DatabaseConnection;
import com.vendex.model.VetxiHistorial.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Todas las lecturas y escrituras incluyen el propietario y la sucursal capturados. */
public class VetxiHistorialDAOPostgres implements VetxiHistorialDAO {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override public void guardar(Ambito a, Mensaje m) throws Exception {
        String sql = "INSERT INTO vetxi_chat_mensaje (mensaje_id, conversacion_id, usuario_id, sucursal_id, titulo, " +
                "es_usuario, texto, sin_ia, rol_ia, roles_bloqueados, fuentes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection()) {
            if (existe(con, a, m.id())) return;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, m.id()); ps.setString(2, m.conversacionId());
                ps.setInt(3, a.usuarioId()); ps.setInt(4, a.sucursalId()); ps.setString(5, m.titulo());
                ps.setBoolean(6, m.usuario()); ps.setString(7, m.texto()); ps.setBoolean(8, m.sinIA());
                ps.setString(9, m.rolIA()); ps.setString(10, JSON.writeValueAsString(m.rolesBloqueados()));
                ps.setString(11, JSON.writeValueAsString(m.fuentes()));
                try { ps.executeUpdate(); }
                catch (SQLException e) {
                    if (!"23505".equals(e.getSQLState()) || !existe(con, a, m.id())) throw e;
                }
            }
        }
    }

    private boolean existe(Connection con, Ambito a, String mensajeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM vetxi_chat_mensaje WHERE usuario_id=? AND sucursal_id=? AND mensaje_id=?")) {
            ps.setInt(1, a.usuarioId()); ps.setInt(2, a.sucursalId()); ps.setString(3, mensajeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override public List<Conversacion> conversaciones(Ambito a, int limite) throws Exception {
        String sql = "SELECT conversacion_id, MIN(titulo) AS titulo, MAX(fecha) AS actualizada, MAX(id) AS ultimo " +
                "FROM vetxi_chat_mensaje WHERE usuario_id=? AND sucursal_id=? GROUP BY conversacion_id " +
                "ORDER BY ultimo DESC LIMIT ?";
        List<Conversacion> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.usuarioId()); ps.setInt(2, a.sucursalId()); ps.setInt(3, Math.max(1, Math.min(50, limite)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Conversacion(rs.getString("conversacion_id"), rs.getString("titulo"),
                        rs.getTimestamp("actualizada").toLocalDateTime().toString()));
            }
        }
        return out;
    }

    @Override public List<Mensaje> mensajes(Ambito a, String conversacionId, int limite) throws Exception {
        UUID.fromString(conversacionId);
        List<Mensaje> out = new ArrayList<>();
        String sql = "SELECT * FROM vetxi_chat_mensaje WHERE usuario_id=? AND sucursal_id=? AND conversacion_id=? " +
                "ORDER BY id DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.usuarioId()); ps.setInt(2, a.sucursalId()); ps.setString(3, conversacionId);
            ps.setInt(4, Math.max(1, Math.min(200, limite)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Mensaje(rs.getString("mensaje_id"), rs.getString("conversacion_id"),
                        rs.getString("titulo"), rs.getBoolean("es_usuario"), rs.getString("texto"), rs.getBoolean("sin_ia"),
                        rs.getString("rol_ia"), JSON.readValue(rs.getString("roles_bloqueados"), new TypeReference<List<String>>() {}),
                        JSON.readValue(rs.getString("fuentes"), new TypeReference<List<Fuente>>() {}),
                        rs.getTimestamp("fecha").toLocalDateTime().toString()));
            }
        }
        Collections.reverse(out);
        return out;
    }
}

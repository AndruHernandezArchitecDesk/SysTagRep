package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.GuiaRemisionDetalle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiaRemisionDetalleDAOPostgres implements GuiaRemisionDetalleDAO {

    private static final Logger LOGGER = Logger.getLogger(GuiaRemisionDetalleDAO.class.getName());

    public void insertarDetalles(int destinatarioId, List<GuiaRemisionDetalle> lista) {
        try (Connection con = DatabaseConnection.getConnection()) { insertarDetalles(con, destinatarioId, lista); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "insertarDetalles GR", e); }
    }
    public void insertarDetalles(Connection con, int destinatarioId, List<GuiaRemisionDetalle> lista) throws SQLException {
        if (lista == null || lista.isEmpty()) return;
        String sql = "INSERT INTO guia_remision_detalle(guia_remision_destinatario_id, inventario_id, codigo_interno, descripcion, cantidad) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (GuiaRemisionDetalle d : lista) {
                ps.setInt(1, destinatarioId);
                if (d.getInventarioId() != null) ps.setInt(2, d.getInventarioId()); else ps.setObject(2, null);
                ps.setString(3, d.getCodigoInterno());
                ps.setString(4, d.getDescripcion());
                ps.setBigDecimal(5, d.getCantidad());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<GuiaRemisionDetalle> listarPorDestinatarioId(int destinatarioId) {
        String sql = "SELECT * FROM guia_remision_detalle WHERE guia_remision_destinatario_id=? ORDER BY id ASC";
        List<GuiaRemisionDetalle> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, destinatarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuiaRemisionDetalle d = new GuiaRemisionDetalle();
                d.setId(rs.getInt("id"));
                d.setGuiaRemisionDestinatarioId(rs.getInt("guia_remision_destinatario_id"));
                int iid = rs.getInt("inventario_id"); d.setInventarioId(rs.wasNull() ? null : iid);
                d.setCodigoInterno(rs.getString("codigo_interno"));
                d.setDescripcion(rs.getString("descripcion"));
                d.setCantidad(rs.getBigDecimal("cantidad"));
                lista.add(d);
            }
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarPorDestinatarioId", e); }
        return lista;
    }

    public List<GuiaRemisionDetalle> listarPorGuiaId(int guiaId) {
        String sql = "SELECT gd.* FROM guia_remision_detalle gd JOIN guia_remision_destinatario dest ON dest.id=gd.guia_remision_destinatario_id WHERE dest.guia_remision_id=? ORDER BY dest.id, gd.id";
        List<GuiaRemisionDetalle> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, guiaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuiaRemisionDetalle d = new GuiaRemisionDetalle();
                d.setId(rs.getInt("id"));
                d.setGuiaRemisionDestinatarioId(rs.getInt("guia_remision_destinatario_id"));
                int iid = rs.getInt("inventario_id"); d.setInventarioId(rs.wasNull() ? null : iid);
                d.setCodigoInterno(rs.getString("codigo_interno"));
                d.setDescripcion(rs.getString("descripcion"));
                d.setCantidad(rs.getBigDecimal("cantidad"));
                lista.add(d);
            }
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarPorGuiaId detalle", e); }
        return lista;
    }
}

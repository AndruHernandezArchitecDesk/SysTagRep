package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.RetencionDetalle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetencionDetalleDAOPostgres implements RetencionDetalleDAO {

    private static final Logger LOGGER = Logger.getLogger(RetencionDetalleDAO.class.getName());

    public void insertarDetalles(int docSustentoId, List<RetencionDetalle> lista) {
        try (Connection con = DatabaseConnection.getConnection()) { insertarDetalles(con, docSustentoId, lista); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "insertarDetalles Retencion", e); }
    }
    public void insertarDetalles(Connection con, int docSustentoId, List<RetencionDetalle> lista) throws SQLException {
        if (lista == null || lista.isEmpty()) return;
        String sql = "INSERT INTO retencion_detalle(doc_sustento_id, codigo, codigo_retencion, base_imponible, porcentaje_retener, valor_retenido) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (RetencionDetalle d : lista) {
                ps.setInt(1, docSustentoId);
                ps.setString(2, d.getCodigo());
                ps.setString(3, d.getCodigoRetencion());
                ps.setBigDecimal(4, d.getBaseImponible());
                ps.setBigDecimal(5, d.getPorcentajeRetener());
                ps.setBigDecimal(6, d.getValorRetenido());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<RetencionDetalle> listarPorDocumentoId(int docId) {
        String sql = "SELECT * FROM retencion_detalle WHERE doc_sustento_id=? ORDER BY id ASC";
        List<RetencionDetalle> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, docId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RetencionDetalle d = new RetencionDetalle();
                d.setId(rs.getInt("id"));
                d.setDocSustentoId(rs.getInt("doc_sustento_id"));
                d.setCodigo(rs.getString("codigo"));
                d.setCodigoRetencion(rs.getString("codigo_retencion"));
                d.setBaseImponible(rs.getBigDecimal("base_imponible"));
                d.setPorcentajeRetener(rs.getBigDecimal("porcentaje_retener"));
                d.setValorRetenido(rs.getBigDecimal("valor_retenido"));
                lista.add(d);
            }
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarPorDocumentoId", e); }
        return lista;
    }
}

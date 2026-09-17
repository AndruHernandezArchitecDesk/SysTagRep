package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.RetencionDocumentoSustento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetencionDocumentoSustentoDAO {

    private static final Logger LOGGER = Logger.getLogger(RetencionDocumentoSustentoDAO.class.getName());

    public void insertarDocumentos(int retencionId, List<RetencionDocumentoSustento> lista) {
        if (lista == null || lista.isEmpty()) return;
        String sql = "INSERT INTO retencion_documento_sustento(retencion_id, cod_sustento, cod_doc_sustento, num_doc_sustento, fecha_emision_doc_sustento, total_sin_impuestos) VALUES (?,?,?,?,?,?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection()) {
            for (RetencionDocumentoSustento d : lista) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, retencionId);
                    ps.setString(2, d.getCodSustento());
                    ps.setString(3, d.getCodDocSustento());
                    ps.setString(4, d.getNumDocSustento());
                    ps.setObject(5, d.getFechaEmisionDocSustento());
                    ps.setBigDecimal(6, d.getTotalSinImpuestos());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) d.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "insertarDocumentos Retencion", e); }
    }

    public List<RetencionDocumentoSustento> listarPorRetencionId(int retencionId) {
        String sql = "SELECT * FROM retencion_documento_sustento WHERE retencion_id=? ORDER BY id ASC";
        List<RetencionDocumentoSustento> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, retencionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RetencionDocumentoSustento d = new RetencionDocumentoSustento();
                d.setId(rs.getInt("id"));
                d.setRetencionId(rs.getInt("retencion_id"));
                d.setCodSustento(rs.getString("cod_sustento"));
                d.setCodDocSustento(rs.getString("cod_doc_sustento"));
                d.setNumDocSustento(rs.getString("num_doc_sustento"));
                try { d.setFechaEmisionDocSustento(rs.getObject("fecha_emision_doc_sustento", LocalDate.class)); } catch (Exception e) { java.sql.Date date = rs.getDate("fecha_emision_doc_sustento"); d.setFechaEmisionDocSustento(date != null ? date.toLocalDate() : null); }
                d.setTotalSinImpuestos(rs.getBigDecimal("total_sin_impuestos"));
                lista.add(d);
            }
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarPorRetencionId", e); }
        return lista;
    }
}

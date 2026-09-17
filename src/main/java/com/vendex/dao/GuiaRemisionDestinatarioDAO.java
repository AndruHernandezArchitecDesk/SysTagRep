package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.GuiaRemisionDestinatario;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiaRemisionDestinatarioDAO {

    private static final Logger LOGGER = Logger.getLogger(GuiaRemisionDestinatarioDAO.class.getName());

    public void insertarDestinatarios(int guiaId, List<GuiaRemisionDestinatario> lista) {
        if (lista == null || lista.isEmpty()) return;
        String sql = "INSERT INTO guia_remision_destinatario(guia_remision_id, identificacion_destinatario, razon_social_destinatario, direccion_destinatario, motivo_traslado, factura_registro_id, cod_doc_sustento, num_doc_sustento, num_aut_doc_sustento, fecha_emision_doc_sustento) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection()) {
            for (GuiaRemisionDestinatario d : lista) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, guiaId);
                    ps.setString(2, d.getIdentificacionDestinatario());
                    ps.setString(3, d.getRazonSocialDestinatario());
                    ps.setString(4, d.getDireccionDestinatario());
                    ps.setString(5, d.getMotivoTraslado());
                    if (d.getFacturaRegistroId() != null) ps.setInt(6, d.getFacturaRegistroId()); else ps.setObject(6, null);
                    ps.setString(7, d.getCodDocSustento());
                    ps.setString(8, d.getNumDocSustento());
                    ps.setString(9, d.getNumAutDocSustento());
                    if (d.getFechaEmisionDocSustento() != null) ps.setObject(10, d.getFechaEmisionDocSustento()); else ps.setObject(10, null);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) d.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "insertarDestinatarios", e); }
    }

    public List<GuiaRemisionDestinatario> listarPorGuiaId(int guiaId) {
        String sql = "SELECT * FROM guia_remision_destinatario WHERE guia_remision_id=? ORDER BY id ASC";
        List<GuiaRemisionDestinatario> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, guiaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GuiaRemisionDestinatario d = new GuiaRemisionDestinatario();
                d.setId(rs.getInt("id"));
                d.setGuiaRemisionId(rs.getInt("guia_remision_id"));
                d.setIdentificacionDestinatario(rs.getString("identificacion_destinatario"));
                d.setRazonSocialDestinatario(rs.getString("razon_social_destinatario"));
                d.setDireccionDestinatario(rs.getString("direccion_destinatario"));
                d.setMotivoTraslado(rs.getString("motivo_traslado"));
                int fid = rs.getInt("factura_registro_id"); d.setFacturaRegistroId(rs.wasNull() ? null : fid);
                d.setCodDocSustento(rs.getString("cod_doc_sustento"));
                d.setNumDocSustento(rs.getString("num_doc_sustento"));
                d.setNumAutDocSustento(rs.getString("num_aut_doc_sustento"));
                try { d.setFechaEmisionDocSustento(rs.getObject("fecha_emision_doc_sustento", LocalDate.class)); } catch (Exception e) { java.sql.Date date = rs.getDate("fecha_emision_doc_sustento"); d.setFechaEmisionDocSustento(date != null ? date.toLocalDate() : null); }
                lista.add(d);
            }
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarPorGuiaId", e); }
        return lista;
    }
}

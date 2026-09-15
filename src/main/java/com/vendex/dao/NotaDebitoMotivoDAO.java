package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaDebitoMotivo;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotaDebitoMotivoDAO {

    private static final Logger LOGGER = Logger.getLogger(NotaDebitoMotivoDAO.class.getName());

    public void insertarMotivos(int notaDebitoId, List<NotaDebitoMotivo> motivos) {
        String sql = "INSERT INTO nota_debito_motivo(nota_debito_id, razon, valor, grava_iva, codigo_porcentaje_iva) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (NotaDebitoMotivo m : motivos) {
                ps.setInt(1, notaDebitoId);
                ps.setString(2, m.getRazon());
                ps.setBigDecimal(3, m.getValor());
                ps.setBoolean(4, m.isGravaIva());
                ps.setString(5, m.getCodigoPorcentajeIva());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoMotivoDAO.insertarMotivos", e);
        }
    }

    public List<NotaDebitoMotivo> listarPorNotaDebitoId(int notaDebitoId) {
        List<NotaDebitoMotivo> lista = new ArrayList<>();
        String sql = "SELECT * FROM nota_debito_motivo WHERE nota_debito_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notaDebitoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotaDebitoMotivo m = new NotaDebitoMotivo();
                    m.setId(rs.getInt("id"));
                    m.setNotaDebitoId(rs.getInt("nota_debito_id"));
                    m.setRazon(rs.getString("razon"));
                    m.setValor(rs.getBigDecimal("valor"));
                    m.setGravaIva(rs.getBoolean("grava_iva"));
                    m.setCodigoPorcentajeIva(rs.getString("codigo_porcentaje_iva"));
                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en NotaDebitoMotivoDAO.listarPorNotaDebitoId", e);
        }
        return lista;
    }
}

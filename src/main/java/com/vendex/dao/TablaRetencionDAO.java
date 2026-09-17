package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.TablaRetencion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TablaRetencionDAO {

    private static final Logger LOGGER = Logger.getLogger(TablaRetencionDAO.class.getName());

    public List<TablaRetencion> listarVigentes() {
        String sql = "SELECT * FROM tabla_retencion WHERE vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE ORDER BY tipo, codigo_retencion";
        List<TablaRetencion> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarVigentes", e); }
        return lista;
    }

    public List<TablaRetencion> listarTodas() {
        String sql = "SELECT * FROM tabla_retencion ORDER BY vigente_desde DESC, codigo_retencion";
        List<TablaRetencion> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "listarTodas", e); }
        return lista;
    }

    public java.math.BigDecimal obtenerPorcentajeVigente(String codigoRetencion) {
        String sql = "SELECT porcentaje FROM tabla_retencion WHERE codigo_retencion=? AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE) ORDER BY vigente_desde DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoRetencion);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal("porcentaje");
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "obtenerPorcentaje", e); }
        return null;
    }

    private TablaRetencion mapear(ResultSet rs) throws SQLException {
        TablaRetencion t = new TablaRetencion();
        t.setId(rs.getInt("id"));
        t.setCodigoRetencion(rs.getString("codigo_retencion"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setTipo(rs.getString("tipo"));
        t.setPorcentaje(rs.getBigDecimal("porcentaje"));
        try { t.setVigenteDesde(rs.getObject("vigente_desde", java.time.LocalDate.class)); } catch (Exception e) { java.sql.Date d = rs.getDate("vigente_desde"); t.setVigenteDesde(d!=null?d.toLocalDate():null); }
        try { t.setVigenteHasta(rs.getObject("vigente_hasta", java.time.LocalDate.class)); } catch (Exception e) { java.sql.Date d = rs.getDate("vigente_hasta"); t.setVigenteHasta(d!=null?d.toLocalDate():null); }
        return t;
    }
}

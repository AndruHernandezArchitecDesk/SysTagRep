package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.MarcaAguaPDFConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Fila singleton compartida por todos los clientes de la base central. */
public final class MarcaAguaPDFDAOPostgres implements MarcaAguaPDFDAO {
    @Override
    public MarcaAguaPDFConfig obtener() throws SQLException {
        String sql = "SELECT factura_activa, proforma_activa, distribucion, opacidad, logo_bytes "
                + "FROM configuracion_marca_agua WHERE id=1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return MarcaAguaPDFConfig.desactivada();
            MarcaAguaPDFConfig.Distribucion distribucion;
            try {
                distribucion = MarcaAguaPDFConfig.Distribucion.valueOf(rs.getString("distribucion"));
            } catch (Exception ignored) {
                distribucion = MarcaAguaPDFConfig.Distribucion.CENTRO;
            }
            return new MarcaAguaPDFConfig(rs.getBoolean("factura_activa"),
                    rs.getBoolean("proforma_activa"), distribucion,
                    rs.getFloat("opacidad"), rs.getBytes("logo_bytes"));
        }
    }

    @Override
    public void guardar(MarcaAguaPDFConfig cfg) throws SQLException {
        if (cfg == null) throw new IllegalArgumentException("Configuración de marca de agua requerida");
        String update = "UPDATE configuracion_marca_agua SET factura_activa=?, proforma_activa=?, "
                + "distribucion=?, opacidad=?, logo_bytes=?, actualizado_en=CURRENT_TIMESTAMP WHERE id=1";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = con.prepareStatement(update)) {
                    bind(ps, cfg);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO configuracion_marca_agua "
                                    + "(id, factura_activa, proforma_activa, distribucion, opacidad, logo_bytes) "
                                    + "VALUES (1,?,?,?,?,?)")) {
                        bind(ps, cfg);
                        ps.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); } catch (SQLException ignored) { }
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (SQLException ignored) { }
            }
        }
    }

    private static void bind(PreparedStatement ps, MarcaAguaPDFConfig cfg) throws SQLException {
        ps.setBoolean(1, cfg.isFacturaActiva());
        ps.setBoolean(2, cfg.isProformaActiva());
        ps.setString(3, cfg.getDistribucion().name());
        ps.setFloat(4, cfg.getOpacidad());
        byte[] logo = cfg.getLogoBytes();
        if (logo == null) ps.setNull(5, Types.VARBINARY);
        else ps.setBytes(5, logo);
    }
}

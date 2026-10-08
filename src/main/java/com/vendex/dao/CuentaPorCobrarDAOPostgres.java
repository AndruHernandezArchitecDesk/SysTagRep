package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CuentaPorCobrar;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CuentaPorCobrarDAOPostgres implements CuentaPorCobrarDAO {

    private static final Logger LOGGER = Logger.getLogger(CuentaPorCobrarDAO.class.getName());

    public void insertar(CuentaPorCobrar cpc) {
        try (Connection con = DatabaseConnection.getConnection()) {
            insertar(con, cpc);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
    }

    public void insertar(Connection con, CuentaPorCobrar cpc) throws SQLException {
        String sql = "INSERT INTO cuentas_por_cobrar(nota_venta_id, factura_registro_id, cliente_id, total, meses_plazo, interes, cuota_mensual, estado, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (cpc.getNotaVentaId() != null) ps.setInt(1, cpc.getNotaVentaId());
            else ps.setNull(1, java.sql.Types.INTEGER);
            if (cpc.getFacturaRegistroId() != null) ps.setInt(2, cpc.getFacturaRegistroId());
            else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setInt(3, cpc.getClienteId());
            ps.setBigDecimal(4, cpc.getTotal());
            ps.setInt(5, cpc.getMesesPlazo());
            ps.setBigDecimal(6, cpc.getInteres());
            ps.setBigDecimal(7, cpc.getCuotaMensual());
            ps.setString(8, "Pendiente");
            ps.executeUpdate();
        }
    }

    public List<Object[]> listarCreditosActivos() {
        List<Object[]> lista = new ArrayList<>();
        // Posiciones 0..13 identicas al layout legacy; [1]/[5]/[6] resuelven el doc de origen
        // (proforma o factura). [14]=factura_registro_id, [15]=tipo ("PROFORMA"/"FACTURA").
        String sql = "SELECT cpc.id, COALESCE(cpc.nota_venta_id, cpc.factura_registro_id) AS doc_id, cpc.cliente_id, cl.nombre, cl.identificacion, " +
                     "COALESCE(nv.codigo, fr.codigo) AS codigo, COALESCE(nv.fecha, fr.fecha) AS fecha, cpc.total, cpc.meses_plazo, cpc.interes, cpc.cuota_mensual, " +
                     "cpc.adelanto, cpc.estado, cpc.fecha_registro, cpc.factura_registro_id, " +
                     "CASE WHEN cpc.factura_registro_id IS NOT NULL THEN 'FACTURA' ELSE 'PROFORMA' END AS tipo_doc " +
                     "FROM cuentas_por_cobrar cpc " +
                     "INNER JOIN cliente cl ON cl.id = cpc.cliente_id " +
                     "LEFT JOIN nota_venta_registro nv ON nv.id = cpc.nota_venta_id " +
                     "LEFT JOIN factura_registro fr ON fr.id = cpc.factura_registro_id " +
                     "WHERE cpc.estado = 'Pendiente' " +
                     "ORDER BY (cpc.fecha_registro + (cpc.meses_plazo || ' days')::interval) ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[]{
                    rs.getInt("id"),
                    rs.getInt("doc_id"),
                    rs.getInt("cliente_id"),
                    rs.getString("nombre"),
                    rs.getString("identificacion"),
                    rs.getString("codigo"),
                    rs.getTimestamp("fecha"),
                    rs.getBigDecimal("total"),
                    rs.getInt("meses_plazo"),
                    rs.getBigDecimal("interes"),
                    rs.getBigDecimal("cuota_mensual"),
                    rs.getBigDecimal("adelanto"),
                    rs.getString("estado"),
                    rs.getTimestamp("fecha_registro"),
                    rs.getObject("factura_registro_id") != null ? rs.getInt("factura_registro_id") : null,
                    rs.getString("tipo_doc")
                };
                lista.add(fila);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
        return lista;
    }

    public List<Object[]> listarPorCliente(int clienteId) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT cpc.id, COALESCE(cpc.nota_venta_id, cpc.factura_registro_id) AS doc_id, cpc.cliente_id, cl.nombre, cl.identificacion, " +
                     "COALESCE(nv.codigo, fr.codigo) AS codigo, COALESCE(nv.fecha, fr.fecha) AS fecha, cpc.total, cpc.meses_plazo, cpc.interes, cpc.cuota_mensual, " +
                     "cpc.adelanto, cpc.estado, cpc.fecha_registro, cpc.factura_registro_id, " +
                     "CASE WHEN cpc.factura_registro_id IS NOT NULL THEN 'FACTURA' ELSE 'PROFORMA' END AS tipo_doc " +
                     "FROM cuentas_por_cobrar cpc " +
                     "INNER JOIN cliente cl ON cl.id = cpc.cliente_id " +
                     "LEFT JOIN nota_venta_registro nv ON nv.id = cpc.nota_venta_id " +
                     "LEFT JOIN factura_registro fr ON fr.id = cpc.factura_registro_id " +
                     "WHERE cpc.estado = 'Pendiente' AND cpc.cliente_id = ? " +
                     "ORDER BY cpc.fecha_registro ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
             ps.setInt(1, clienteId);
             try (ResultSet rs = ps.executeQuery()) {
                 while (rs.next()) {
                     Object[] fila = new Object[]{
                         rs.getInt("id"),
                         rs.getInt("doc_id"),
                         rs.getInt("cliente_id"),
                         rs.getString("nombre"),
                         rs.getString("identificacion"),
                         rs.getString("codigo"),
                         rs.getTimestamp("fecha"),
                         rs.getBigDecimal("total"),
                         rs.getInt("meses_plazo"),
                         rs.getBigDecimal("interes"),
                         rs.getBigDecimal("cuota_mensual"),
                         rs.getBigDecimal("adelanto"),
                         rs.getString("estado"),
                         rs.getTimestamp("fecha_registro"),
                         rs.getObject("factura_registro_id") != null ? rs.getInt("factura_registro_id") : null,
                         rs.getString("tipo_doc")
                    };
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
        return lista;
    }

    public void registrarAdelanto(int cpcId, BigDecimal nuevoAdelanto) {
        String sql = "UPDATE cuentas_por_cobrar SET adelanto = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, nuevoAdelanto);
            ps.setInt(2, cpcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
    }

    public void marcarPagado(int cpcId) {
        String sql = "UPDATE cuentas_por_cobrar SET estado = 'Pagado' WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cpcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
    }

    public List<String[]> obtenerDetallesVenta(int notaVentaId) {        List<String[]> detalles = new ArrayList<>();
        String sql = "SELECT descripcion, cantidad, precio_unitario, subtotal FROM nota_venta_detalle WHERE nota_venta_registro_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, notaVentaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(new String[]{
                        rs.getString("descripcion"),
                        String.valueOf(rs.getInt("cantidad")),
                        rs.getBigDecimal("precio_unitario").toString(),
                        rs.getBigDecimal("subtotal").toString()
                    });
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
        return detalles;
    }

    @Override
    public List<String[]> obtenerDetallesFactura(int facturaRegistroId) {
        List<String[]> detalles = new ArrayList<>();
        String sql = "SELECT descripcion, cantidad, precio_unitario, subtotal FROM factura_detalle WHERE factura_registro_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, facturaRegistroId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(new String[]{
                        rs.getString("descripcion"),
                        String.valueOf(rs.getInt("cantidad")),
                        rs.getBigDecimal("precio_unitario").toString(),
                        rs.getBigDecimal("subtotal").toString()
                    });
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de CuentaPorCobrarDAO", e);
        }
        return detalles;
    }
}

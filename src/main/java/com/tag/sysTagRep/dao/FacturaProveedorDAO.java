package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.CompraResumen;
import com.tag.sysTagRep.model.FacturaProveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FacturaProveedorDAO {

    public void insertar(List<FacturaProveedor> lineas) {
        String sql = "INSERT INTO factura_proveedor(numero_factura, proveedor_id, codigo, codigo_manual, descripcion, " +
                     "grupo_id, marca_id, costo_sin_iva, iva, cantidad, total_linea, fecha) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (FacturaProveedor l : lineas) {
                ps.setString(1, l.getNumeroFactura());
                if (l.getProveedorId() > 0) ps.setInt(2, l.getProveedorId());
                else ps.setNull(2, java.sql.Types.INTEGER);
                ps.setString(3, l.getCodigo());
                ps.setString(4, l.getCodigoManual());
                ps.setString(5, l.getDescripcion());
                if (l.getGrupoId() > 0) ps.setInt(6, l.getGrupoId());
                else ps.setNull(6, java.sql.Types.INTEGER);
                if (l.getMarcaId() > 0) ps.setInt(7, l.getMarcaId());
                else ps.setNull(7, java.sql.Types.INTEGER);
                ps.setBigDecimal(8, l.getCostoSinIVA());
                ps.setBigDecimal(9, l.getIva());
                ps.setInt(10, l.getCantidad());
                ps.setBigDecimal(11, l.getTotalLinea());
                ps.setObject(12, LocalDateTime.now());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<FacturaProveedor> listarFacturas(LocalDate desde, LocalDate hasta) {
        List<FacturaProveedor> lista = new ArrayList<>();
        String sql = "SELECT fp.numero_factura, COALESCE(p.nombre, '') AS proveedor, MIN(fp.fecha) AS fecha, SUM(fp.total_linea) AS total " +
                     "FROM factura_proveedor fp " +
                     "LEFT JOIN proveedor p ON p.id = fp.proveedor_id " +
                     "WHERE CAST(fp.fecha AS DATE) BETWEEN ? AND ? " +
                     "GROUP BY fp.numero_factura, p.nombre " +
                     "ORDER BY MIN(fp.fecha) DESC, fp.numero_factura DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde != null ? desde : LocalDate.now());
            ps.setObject(2, hasta != null ? hasta : LocalDate.now());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaProveedor f = new FacturaProveedor();
                    f.setNumeroFactura(rs.getString("numero_factura"));
                    f.setProveedor(rs.getString("proveedor"));
                    f.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    f.setTotalLinea(rs.getBigDecimal("total"));
                    lista.add(f);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<CompraResumen> listarComprasPorFecha(LocalDate fecha) {
        List<CompraResumen> lista = new ArrayList<>();
        String sql = "SELECT COALESCE(fp.numero_factura, '') AS numero_factura, COALESCE(p.nombre, '') AS proveedor, " +
                     "MIN(fp.fecha) AS fecha, COUNT(*) AS items, SUM(fp.total_linea) AS total " +
                     "FROM factura_proveedor fp " +
                     "LEFT JOIN proveedor p ON p.id = fp.proveedor_id " +
                     "WHERE CAST(fp.fecha AS DATE) = ? " +
                     "GROUP BY fp.numero_factura, p.nombre " +
                     "ORDER BY MIN(fp.fecha) DESC, fp.numero_factura DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fecha != null ? fecha : LocalDate.now());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CompraResumen c = new CompraResumen();
                    c.setNumeroFactura(rs.getString("numero_factura"));
                    c.setProveedor(rs.getString("proveedor"));
                    c.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    c.setItems(rs.getInt("items"));
                    c.setTotal(rs.getBigDecimal("total"));
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<FacturaProveedor> listarDetallePorRango(LocalDate desde, LocalDate hasta) {
        List<FacturaProveedor> lista = new ArrayList<>();
        String sql = "SELECT fp.*, COALESCE(p.nombre, '') AS proveedor FROM factura_proveedor fp " +
                     "LEFT JOIN proveedor p ON p.id = fp.proveedor_id " +
                     "WHERE CAST(fp.fecha AS DATE) BETWEEN ? AND ? " +
                     "ORDER BY fp.fecha DESC, fp.numero_factura DESC, fp.id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde != null ? desde : LocalDate.now());
            ps.setObject(2, hasta != null ? hasta : LocalDate.now());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaProveedor l = new FacturaProveedor();
                    l.setId(rs.getInt("id"));
                    l.setNumeroFactura(rs.getString("numero_factura"));
                    l.setProveedorId(rs.getInt("proveedor_id"));
                    l.setProveedor(rs.getString("proveedor"));
                    l.setCodigo(rs.getString("codigo"));
                    l.setCodigoManual(rs.getString("codigo_manual"));
                    l.setDescripcion(rs.getString("descripcion"));
                    l.setGrupoId(rs.getInt("grupo_id"));
                    l.setMarcaId(rs.getInt("marca_id"));
                    l.setCostoSinIVA(rs.getBigDecimal("costo_sin_iva"));
                    l.setIva(rs.getBigDecimal("iva"));
                    l.setCantidad(rs.getInt("cantidad"));
                    l.setTotalLinea(rs.getBigDecimal("total_linea"));
                    l.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    lista.add(l);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<FacturaProveedor> listarPorFactura(String numeroFactura) {
        List<FacturaProveedor> lista = new ArrayList<>();
        String sql = "SELECT fp.*, COALESCE(p.nombre, '') AS proveedor FROM factura_proveedor fp " +
                     "LEFT JOIN proveedor p ON p.id = fp.proveedor_id " +
                     "WHERE fp.numero_factura = ? ORDER BY fp.id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroFactura);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaProveedor l = new FacturaProveedor();
                    l.setId(rs.getInt("id"));
                    l.setNumeroFactura(rs.getString("numero_factura"));
                    l.setProveedorId(rs.getInt("proveedor_id"));
                    l.setProveedor(rs.getString("proveedor"));
                    l.setCodigo(rs.getString("codigo"));
                    l.setCodigoManual(rs.getString("codigo_manual"));
                    l.setDescripcion(rs.getString("descripcion"));
                    l.setGrupoId(rs.getInt("grupo_id"));
                    l.setMarcaId(rs.getInt("marca_id"));
                    l.setCostoSinIVA(rs.getBigDecimal("costo_sin_iva"));
                    l.setIva(rs.getBigDecimal("iva"));
                    l.setCantidad(rs.getInt("cantidad"));
                    l.setTotalLinea(rs.getBigDecimal("total_linea"));
                    l.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    lista.add(l);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}

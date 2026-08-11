package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Inventario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventarioDAO {

    private static final Logger LOGGER = Logger.getLogger(InventarioDAO.class.getName());

    public List<Inventario> listar() {
        return listarPaginado(-1, -1, null);
    }

    public List<Inventario> listarPaginado(int page, int pageSize, String filtro) {
        List<Inventario> lista = new ArrayList<>();
        String base = "SELECT i.*, p.nombre as nombre_proveedor, g.nombre as nombre_grupo, m.nombre as nombre_marca, COALESCE(ub.codigo_ubicacion, u.nombre) as nombre_ubicacion " +
                      "FROM inventario i " +
                      "LEFT JOIN proveedor p ON p.id = i.proveedor_id " +
                      "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                      "LEFT JOIN marca m ON m.id = i.marca_id " +
                      "LEFT JOIN ubicacion_percha u ON u.id = i.ubicacion_percha_id " +
                      "LEFT JOIN ubicacion ub ON ub.id_producto = i.id";
        String where = buildWhereClause(filtro);
        if (where.isEmpty()) {
            where = " WHERE i.estado = true";
        } else {
            where = " WHERE i.estado = true AND " + where.substring("WHERE ".length());
        }
        boolean paginar = page > 0 && pageSize > 0;
        String sql = base + " " + where + " ORDER BY i.fecha_ingreso DESC, i.id DESC" + (paginar ? " LIMIT ? OFFSET ?" : "");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = setFilterParameters(ps, filtro, 1);
            if (paginar) {
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, (page - 1) * pageSize);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Inventario v = new Inventario();
                    mapearInventario(rs, v);
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
        return lista;
    }

    public int contar(String filtro) {
        String base = "SELECT COUNT(*) FROM inventario i " +
                      "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                      "LEFT JOIN marca m ON m.id = i.marca_id";
        String where = buildWhereClause(filtro);
        if (where.isEmpty()) {
            where = " WHERE i.estado = true";
        } else {
            where = " WHERE i.estado = true AND " + where.substring("WHERE ".length());
        }
        String sql = base + " " + where;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setFilterParameters(ps, filtro, 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
        return 0;
    }

    private String buildWhereClause(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) return "";
        String[] partes = filtro.toLowerCase().split("%");
        List<String> conds = new ArrayList<>();
        for (String p : partes) {
            if (!p.isBlank()) {
                conds.add("(LOWER(i.descripcion) LIKE ? OR LOWER(g.nombre) LIKE ? OR LOWER(m.nombre) LIKE ? OR LOWER(i.codigo) LIKE ?)");
            }
        }
        return conds.isEmpty() ? "" : " WHERE " + String.join(" AND ", conds);
    }

    private int setFilterParameters(PreparedStatement ps, String filtro, int startIdx) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) return startIdx;
        String[] partes = filtro.toLowerCase().split("%");
        int idx = startIdx;
        for (String p : partes) {
            if (!p.isBlank()) {
                String like = "%" + p + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
        }
        return idx;
    }

    public List<Inventario> listarPorRango(LocalDate desde, LocalDate hasta) {
        List<Inventario> lista = new ArrayList<>();
        String sql = "SELECT i.*, p.nombre as nombre_proveedor, g.nombre as nombre_grupo, m.nombre as nombre_marca, COALESCE(ub.codigo_ubicacion, u.nombre) as nombre_ubicacion " +
                     "FROM inventario i " +
                     "LEFT JOIN proveedor p ON p.id = i.proveedor_id " +
                     "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                     "LEFT JOIN marca m ON m.id = i.marca_id " +
                     "LEFT JOIN ubicacion_percha u ON u.id = i.ubicacion_percha_id " +
                     "LEFT JOIN (SELECT id_producto, STRING_AGG(codigo_ubicacion, ', ' ORDER BY codigo_ubicacion) AS codigo_ubicacion FROM ubicacion WHERE id_producto IS NOT NULL GROUP BY id_producto) ub ON ub.id_producto = i.id " +
                     "WHERE CAST(i.fecha_ingreso AS DATE) BETWEEN ? AND ? ORDER BY i.fecha_ingreso ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, desde != null ? desde : LocalDate.now());
            ps.setObject(2, hasta != null ? hasta : LocalDate.now());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Inventario v = new Inventario();
                    mapearInventario(rs, v);
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
        return lista;
    }

    public int guardar(Inventario inv) {
        String sql = "INSERT INTO inventario(descripcion, grupo_id, marca_id, costo_sin_iva, cantidad, ubicacion_percha_id, precio_venta, fecha_ingreso, estado, tag_codigo, codigo, proveedor_id, forma_pago, meses_plazo, interes, numero_factura) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, inv.getDescripcion());
            if (inv.getGrupoId() > 0) ps.setInt(2, inv.getGrupoId()); else ps.setNull(2, java.sql.Types.INTEGER);
            if (inv.getMarcaId() > 0) ps.setInt(3, inv.getMarcaId()); else ps.setNull(3, java.sql.Types.INTEGER);
            ps.setBigDecimal(4, inv.getCostoSinIVA());
            ps.setInt(5, inv.getCantidad());
            if (inv.getUbicacionPerchaId() > 0) ps.setInt(6, inv.getUbicacionPerchaId()); else ps.setNull(6, java.sql.Types.INTEGER);
            ps.setBigDecimal(7, inv.getPrecioVenta());
            ps.setObject(8, LocalDateTime.now());
            ps.setObject(9, inv.getEstado() != null ? inv.getEstado() : true);
            ps.setString(10, inv.getTagCodigo());
            ps.setString(11, inv.getCodigo());
            if (inv.getProveedorId() > 0) ps.setInt(12, inv.getProveedorId());
            else ps.setNull(12, java.sql.Types.INTEGER);
            ps.setString(13, inv.getFormaPago());
            if (inv.getMesesPlazo() > 0) ps.setInt(14, inv.getMesesPlazo()); else ps.setNull(14, java.sql.Types.INTEGER);
            if (inv.getInteres() != null && inv.getInteres().compareTo(BigDecimal.ZERO) > 0) ps.setBigDecimal(15, inv.getInteres()); else ps.setNull(15, java.sql.Types.DECIMAL);
            ps.setString(16, inv.getNumeroFactura());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
        return 0;
    }

    public void actualizar(Inventario inv) {
        String sql = "UPDATE inventario SET descripcion=?, grupo_id=?, marca_id=?, costo_sin_iva=?, cantidad=?, ubicacion_percha_id=?, precio_venta=?, fecha_ingreso=?, tag_codigo=?, codigo=?, proveedor_id=?, forma_pago=?, meses_plazo=?, interes=?, numero_factura=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, inv.getDescripcion());
            if (inv.getGrupoId() > 0) ps.setInt(2, inv.getGrupoId()); else ps.setNull(2, java.sql.Types.INTEGER);
            if (inv.getMarcaId() > 0) ps.setInt(3, inv.getMarcaId()); else ps.setNull(3, java.sql.Types.INTEGER);
            ps.setBigDecimal(4, inv.getCostoSinIVA());
            ps.setInt(5, inv.getCantidad());
            if (inv.getUbicacionPerchaId() > 0) ps.setInt(6, inv.getUbicacionPerchaId()); else ps.setNull(6, java.sql.Types.INTEGER);
            ps.setBigDecimal(7, inv.getPrecioVenta());
            ps.setObject(8, inv.getFecha_ingreso());
            ps.setString(9, inv.getTagCodigo());
            ps.setString(10, inv.getCodigo());
            if (inv.getProveedorId() > 0) ps.setInt(11, inv.getProveedorId());
            else ps.setNull(11, java.sql.Types.INTEGER);
            ps.setString(12, inv.getFormaPago());
            if (inv.getMesesPlazo() > 0) ps.setInt(13, inv.getMesesPlazo()); else ps.setNull(13, java.sql.Types.INTEGER);
            if (inv.getInteres() != null && inv.getInteres().compareTo(BigDecimal.ZERO) > 0) ps.setBigDecimal(14, inv.getInteres()); else ps.setNull(14, java.sql.Types.DECIMAL);
            ps.setString(15, inv.getNumeroFactura());
            ps.setInt(16, inv.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
    }

    public void actualizarPrecioVenta(int id, java.math.BigDecimal precio) {
        String sql = "UPDATE inventario SET precio_venta=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, precio);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM inventario WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
    }

    public String obtenerProveedorNombre(int productoId) {
        String sql = "SELECT p.nombre FROM inventario i JOIN proveedor p ON p.id = i.proveedor_id WHERE i.id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("nombre");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    public void descontarStock(int productoId, int cantidad) {
        String sql = "UPDATE inventario SET cantidad = cantidad - ? WHERE id = ? AND cantidad >= ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            ps.setInt(3, cantidad);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
    }

    public List<Inventario> listarActivosConStock() {
        List<Inventario> lista = new ArrayList<>();
        String sql = "SELECT i.*, p.nombre as nombre_proveedor, g.nombre as nombre_grupo, m.nombre as nombre_marca, COALESCE(ub.codigo_ubicacion, u.nombre) as nombre_ubicacion " +
                     "FROM inventario i " +
                     "LEFT JOIN proveedor p ON p.id = i.proveedor_id " +
                     "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                     "LEFT JOIN marca m ON m.id = i.marca_id " +
                     "LEFT JOIN ubicacion_percha u ON u.id = i.ubicacion_percha_id " +
                     "LEFT JOIN (SELECT id_producto, STRING_AGG(codigo_ubicacion, ', ' ORDER BY codigo_ubicacion) AS codigo_ubicacion FROM ubicacion WHERE id_producto IS NOT NULL GROUP BY id_producto) ub ON ub.id_producto = i.id " +
                     "WHERE i.estado=true AND i.cantidad >= 1 ORDER BY i.descripcion";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Inventario v = new Inventario();
                mapearInventario(rs, v);
                lista.add(v);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
        return lista;
    }

    public Inventario obtenerPorId(int id) {
        String sql = "SELECT i.*, p.nombre as nombre_proveedor, g.nombre as nombre_grupo, m.nombre as nombre_marca, COALESCE(ub.codigo_ubicacion, u.nombre) as nombre_ubicacion " +
                     "FROM inventario i " +
                     "LEFT JOIN proveedor p ON p.id = i.proveedor_id " +
                     "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                     "LEFT JOIN marca m ON m.id = i.marca_id " +
                     "LEFT JOIN ubicacion_percha u ON u.id = i.ubicacion_percha_id " +
                     "LEFT JOIN (SELECT id_producto, STRING_AGG(codigo_ubicacion, ', ' ORDER BY codigo_ubicacion) AS codigo_ubicacion FROM ubicacion WHERE id_producto IS NOT NULL GROUP BY id_producto) ub ON ub.id_producto = i.id " +
                     "WHERE i.id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Inventario v = new Inventario();
                    mapearInventario(rs, v);
                    return v;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error en operacion de InventarioDAO", e);
        }
        return null;
    }

    private void mapearInventario(ResultSet rs, Inventario v) throws SQLException {
        v.setId(rs.getInt("id"));
        v.setDescripcion(rs.getString("descripcion"));
        v.setGrupo(rs.getString("nombre_grupo"));
        v.setMarca(rs.getString("nombre_marca"));
        v.setGrupoId(rs.getInt("grupo_id"));
        v.setMarcaId(rs.getInt("marca_id"));
        v.setUbicacionPercha(rs.getString("nombre_ubicacion"));
        v.setUbicacionPerchaId(rs.getInt("ubicacion_percha_id"));
        v.setCostoSinIVA(rs.getBigDecimal("costo_sin_iva"));
        v.setCantidad(rs.getInt("cantidad"));
        v.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        v.setFecha_ingreso(rs.getObject("fecha_ingreso", LocalDateTime.class));
        v.setEstado(rs.getBoolean("estado"));
        v.setCodigo(rs.getString("codigo"));
        v.setTagCodigo(rs.getString("tag_codigo"));
        v.setProveedor(rs.getString("nombre_proveedor"));
        v.setProveedorId(rs.getInt("proveedor_id"));
        v.setFormaPago(rs.getString("forma_pago"));
        v.setMesesPlazo(rs.getInt("meses_plazo"));
        v.setInteres(rs.getBigDecimal("interes"));
        v.setNumeroFactura(rs.getString("numero_factura"));
    }
}
package com.tag.sysTagRep.dao;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.model.Inventario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventarioDAO {

    public List<Inventario> listar() {
        List<Inventario> lista = new ArrayList<>();
        String sql = "SELECT i.*, p.nombre as nombre_proveedor, g.nombre as nombre_grupo, m.nombre as nombre_marca, u.nombre as nombre_ubicacion " +
                     "FROM inventario i " +
                     "LEFT JOIN proveedor p ON p.id = i.proveedor_id " +
                     "LEFT JOIN grupo g ON g.id = i.grupo_id " +
                     "LEFT JOIN marca m ON m.id = i.marca_id " +
                     "LEFT JOIN ubicacion_percha u ON u.id = i.ubicacion_percha_id " +
                     "ORDER BY i.id";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Inventario v = new Inventario();
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
                v.setProveedor(rs.getString("nombre_proveedor"));
                v.setProveedorId(rs.getInt("proveedor_id"));
                v.setFormaPago(rs.getString("forma_pago"));
                v.setMesesPlazo(rs.getInt("meses_plazo"));
                v.setInteres(rs.getBigDecimal("interes"));
                lista.add(v);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public int guardar(Inventario inv) {
        String sql = "INSERT INTO inventario(descripcion, grupo_id, marca_id, costo_sin_iva, cantidad, ubicacion_percha_id, precio_venta, fecha_ingreso, estado, codigo, proveedor_id, forma_pago, meses_plazo, interes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            ps.setObject(9, true);
            ps.setString(10, inv.getCodigo());
            if (inv.getProveedorId() > 0) ps.setInt(11, inv.getProveedorId());
            else ps.setNull(11, java.sql.Types.INTEGER);
            ps.setString(12, inv.getFormaPago());
            if (inv.getMesesPlazo() > 0) ps.setInt(13, inv.getMesesPlazo()); else ps.setNull(13, java.sql.Types.INTEGER);
            if (inv.getInteres() != null && inv.getInteres().compareTo(BigDecimal.ZERO) > 0) ps.setBigDecimal(14, inv.getInteres()); else ps.setNull(14, java.sql.Types.DECIMAL);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void actualizar(Inventario inv) {
        String sql = "UPDATE inventario SET descripcion=?, grupo_id=?, marca_id=?, costo_sin_iva=?, cantidad=?, ubicacion_percha_id=?, precio_venta=?, fecha_ingreso=?, codigo=?, proveedor_id=?, forma_pago=?, meses_plazo=?, interes=? WHERE id=?";
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
            ps.setString(9, inv.getCodigo());
            if (inv.getProveedorId() > 0) ps.setInt(10, inv.getProveedorId());
            else ps.setNull(10, java.sql.Types.INTEGER);
            ps.setString(11, inv.getFormaPago());
            if (inv.getMesesPlazo() > 0) ps.setInt(12, inv.getMesesPlazo()); else ps.setNull(12, java.sql.Types.INTEGER);
            if (inv.getInteres() != null && inv.getInteres().compareTo(BigDecimal.ZERO) > 0) ps.setBigDecimal(13, inv.getInteres()); else ps.setNull(13, java.sql.Types.DECIMAL);
            ps.setInt(14, inv.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM inventario WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
        }
    }
}

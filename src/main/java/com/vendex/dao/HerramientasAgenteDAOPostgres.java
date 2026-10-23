package com.vendex.dao;

import com.vendex.chatbot.HerramientasAgente;
import com.vendex.config.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fase agente IA — herramientas de lectura del ERP (solo SELECT, sin efectos).
 * Todas las consultas son fail-soft: en error devuelven lista vacía/null y loguean,
 * nunca lanzan hacia el chatbot (el asistente no debe caer por una consulta).
 */
public class HerramientasAgenteDAOPostgres implements HerramientasAgente {

    private static final Logger LOGGER = Logger.getLogger(HerramientasAgenteDAOPostgres.class.getName());
    private final Integer sucursalId;

    /** Consultas locales sin restricción explícita de sucursal (compatibilidad legacy). */
    public HerramientasAgenteDAOPostgres() { this(null); }

    /** Consultas de agente usadas desde API: siempre acotadas a la sucursal de la sesión. */
    public HerramientasAgenteDAOPostgres(Integer sucursalId) { this.sucursalId = sucursalId; }

    @Override
    public List<com.vendex.model.Inventario> complementarios(String codigo, int limite) {
        if (codigo == null || codigo.isBlank() || limite <= 0) return List.of();
        // Solo datos necesarios para el selector compartido; incluye la base aunque no tenga stock.
        String sql = "SELECT i.id, i.codigo, i.descripcion, i.cantidad, i.precio_venta, " +
                "g.nombre AS grupo, m.nombre AS marca FROM inventario i " +
                "LEFT JOIN grupo g ON g.id=i.grupo_id LEFT JOIN marca m ON m.id=i.marca_id " +
                "WHERE i.estado=true " + (sucursalId == null ? "" : "AND i.sucursal_id=? ")
                + "AND (i.cantidad>0 OR UPPER(i.codigo)=UPPER(?)) ORDER BY i.id";
        List<com.vendex.model.Inventario> inventario = new ArrayList<>();
        com.vendex.model.Inventario base = null;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int param = 1;
            if (sucursalId != null) ps.setInt(param++, sucursalId);
            ps.setString(param, codigo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.vendex.model.Inventario i = new com.vendex.model.Inventario();
                    i.setId(rs.getInt("id"));
                    i.setCodigo(rs.getString("codigo"));
                    i.setDescripcion(rs.getString("descripcion"));
                    i.setCantidad(rs.getInt("cantidad"));
                    i.setPrecioVenta(rs.getBigDecimal("precio_venta"));
                    i.setGrupo(rs.getString("grupo"));
                    i.setMarca(rs.getString("marca"));
                    inventario.add(i);
                    if (codigo.trim().equalsIgnoreCase(i.getCodigo())) {
                        if (base != null) return List.of(); // código ambiguo: no asociar a otra referencia
                        base = i;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.complementarios", e);
            return List.of();
        }
        return new com.vendex.service.VendexiSugerenciaService().seleccionarCandidatos(
                base, inventario, java.util.Set.of(), Math.min(2, limite));
    }

    @Override
    public List<Producto> productos(String criterio, int limite) {
        if (criterio == null || criterio.isBlank()) return List.of();
        String sql = "SELECT i.codigo, i.descripcion, i.cantidad, i.precio_venta, i.costo_sin_iva, p.nombre AS proveedor " +
                     "FROM inventario i LEFT JOIN proveedor p ON p.id=i.proveedor_id " +
                     "WHERE i.estado=true " + (sucursalId == null ? "" : "AND i.sucursal_id=? ") +
                     "AND (UPPER(i.codigo) = UPPER(?) OR UPPER(i.descripcion) LIKE UPPER(?)) " +
                     "ORDER BY CASE WHEN UPPER(i.codigo) = UPPER(?) THEN 1 ELSE 0 END DESC, i.cantidad DESC, i.descripcion LIMIT ?";
        List<Producto> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + criterio.trim() + "%";
            int param = 1;
            if (sucursalId != null) ps.setInt(param++, sucursalId);
            ps.setString(param++, criterio.trim());
            ps.setString(param++, like);
            ps.setString(param++, criterio.trim());
            ps.setInt(param, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.productos", e);
        }
        return out;
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        BigDecimal precio = rs.getBigDecimal("precio_venta");
        BigDecimal costo = rs.getBigDecimal("costo_sin_iva");
        double pct = -1;
        if (precio != null && precio.signum() > 0 && costo != null) {
            // Criterio Análisis IA: (precio venta - costo sin IVA) / precio venta * 100
            pct = precio.subtract(costo).multiply(new BigDecimal("100"))
                    .divide(precio, 1, RoundingMode.HALF_UP).doubleValue();
        }
        return new Producto(
                rs.getString("codigo"),
                rs.getString("descripcion"),
                rs.getInt("cantidad"),
                precio,
                costo,
                pct,
                rs.getString("proveedor"));
    }

    @Override
    public List<Producto> compatibles(String descripcion, String marca, String modelo,
                                     Integer anio, String motor, int limite) {
        if (((marca == null || marca.isBlank()) && (modelo == null || modelo.isBlank())) || anio == null) return List.of();
        if (limite <= 0) return List.of();
        StringBuilder sql = new StringBuilder("SELECT i.codigo, i.descripcion, i.cantidad, i.precio_venta, "
                + "i.costo_sin_iva, p.nombre AS proveedor FROM inventario i "
                + "JOIN inventario_vehiculo iv ON iv.inventario_id=i.id "
                + "JOIN vehiculo v ON v.id=iv.vehiculo_id LEFT JOIN proveedor p ON p.id=i.proveedor_id "
                + "WHERE i.estado=true");
        List<Object> params = new ArrayList<>();
        if (sucursalId != null) { sql.append(" AND i.sucursal_id=?"); params.add(sucursalId); }
        if (marca != null && !marca.isBlank()) { sql.append(" AND UPPER(v.marca) LIKE UPPER(?)"); params.add("%" + marca.trim() + "%"); }
        if (modelo != null && !modelo.isBlank()) { sql.append(" AND UPPER(v.modelo) LIKE UPPER(?)"); params.add("%" + modelo.trim() + "%"); }
        // Sin año inicial registrado no se puede afirmar la compatibilidad para un año concreto.
        sql.append(" AND v.anio_desde IS NOT NULL AND ? >= v.anio_desde AND (v.anio_hasta IS NULL OR ? <= v.anio_hasta)");
        params.add(anio); params.add(anio);
        if (motor != null && !motor.isBlank()) { sql.append(" AND UPPER(COALESCE(v.motor,'')) LIKE UPPER(?)"); params.add("%" + motor.trim() + "%"); }
        for (String token : tokens(descripcion)) {
            sql.append(" AND (UPPER(i.descripcion) LIKE UPPER(?) OR UPPER(i.codigo) LIKE UPPER(?))");
            String like = "%" + token + "%";
            params.add(like); params.add(like);
        }
        sql.append(" ORDER BY i.cantidad DESC, i.descripcion LIMIT ?");
        params.add(Math.max(1, Math.min(20, limite)));
        List<Producto> out = new ArrayList<>();
        java.util.Set<String> vistos = new java.util.LinkedHashSet<>();
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto producto = mapearProducto(rs);
                    String key = producto.codigo() == null ? producto.descripcion() : producto.codigo().toUpperCase(java.util.Locale.ROOT);
                    if (vistos.add(key)) out.add(producto);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.compatibles", e);
        }
        return out;
    }

    private static List<String> tokens(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) return List.of();
        String normalizada = java.text.Normalizer.normalize(descripcion, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT);
        return java.util.Arrays.stream(normalizada.split("[^\\p{L}\\p{N}]+"))
                .filter(t -> t.length() > 2 && !java.util.Set.of("para", "por", "del", "con", "una", "uno", "que").contains(t))
                .distinct().toList();
    }

    @Override
    public List<Cliente> clientes(String criterio, int limite) {
        if (criterio == null || criterio.isBlank()) return List.of();
        String sql = "SELECT id, nombre, identificacion FROM cliente " +
                     "WHERE estado=true AND (UPPER(nombre) LIKE UPPER(?) OR identificacion = ?) " +
                     "ORDER BY nombre LIMIT ?";
        List<Cliente> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + criterio.trim() + "%");
            ps.setString(2, criterio.trim());
            ps.setInt(3, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("identificacion")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.clientes", e);
        }
        return out;
    }

    @Override
    public Ventas ventas(int clienteId) {
        String sql = "SELECT COUNT(*), COALESCE(SUM(total),0), MAX(fecha) FROM factura_registro WHERE cliente_id = ?"
                + (sucursalId == null ? "" : " AND sucursal_id=?");
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            if (sucursalId != null) ps.setInt(2, sucursalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ultima = rs.getTimestamp(3);
                    return new Ventas(rs.getInt(1), rs.getBigDecimal(2),
                            ultima != null ? ultima.toLocalDateTime().toLocalDate().toString() : null);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.ventas", e);
        }
        return null;
    }

    @Override
    public BigDecimal saldo(int clienteId) {
        String sql = "SELECT COALESCE(SUM(cpc.total - COALESCE(cpc.adelanto,0)),0) FROM cuentas_por_cobrar cpc " +
                     "WHERE cpc.cliente_id = ? AND cpc.estado = 'Pendiente'" + (sucursalId == null ? "" :
                     " AND ((cpc.factura_registro_id IS NOT NULL AND EXISTS (SELECT 1 FROM factura_registro fr WHERE fr.id=cpc.factura_registro_id AND fr.sucursal_id=?)) " +
                     "OR (cpc.nota_venta_id IS NOT NULL AND EXISTS (SELECT 1 FROM nota_venta_registro nvr WHERE nvr.id=cpc.nota_venta_id AND nvr.sucursal_id=?)))");
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            if (sucursalId != null) { ps.setInt(2, sucursalId); ps.setInt(3, sucursalId); }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.saldo", e);
        }
        return null;
    }

    @Override
    public List<Top> masVendidos(int limite) {
        String sql = "SELECT COALESCE(NULLIF(TRIM(d.codigo),''), 'sin código') AS cod, d.descripcion, " +
                     "SUM(d.cantidad) AS q, SUM(d.precio_total) AS monto " +
                     "FROM factura_detalle d JOIN factura_registro fr ON fr.id=d.factura_registro_id " +
                     (sucursalId == null ? "" : "WHERE fr.sucursal_id=? ") +
                     "GROUP BY COALESCE(NULLIF(TRIM(d.codigo),''), 'sin código'), d.descripcion " +
                     "ORDER BY monto DESC LIMIT ?";
        List<Top> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int param = 1;
            if (sucursalId != null) ps.setInt(param++, sucursalId);
            ps.setInt(param, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Top(rs.getString("cod"), rs.getString("descripcion"),
                            rs.getInt("q"), rs.getBigDecimal("monto"), null));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.masVendidos", e);
        }
        return out;
    }

    @Override
    public List<Top> mejorMargen(int limite) {
        String sql = "SELECT codigo, descripcion, precio_venta, costo_sin_iva FROM inventario " +
                     "WHERE estado=true " + (sucursalId == null ? "" : "AND sucursal_id=? ") +
                     "AND costo_sin_iva IS NOT NULL AND costo_sin_iva > 0 " +
                     "AND precio_venta IS NOT NULL AND precio_venta > 0 " +
                     "ORDER BY (precio_venta - costo_sin_iva) / precio_venta DESC LIMIT ?";
        List<Top> out = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int param = 1;
            if (sucursalId != null) ps.setInt(param++, sucursalId);
            ps.setInt(param, Math.max(1, limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal precio = rs.getBigDecimal("precio_venta");
                    BigDecimal costo = rs.getBigDecimal("costo_sin_iva");
                    double pct = precio.subtract(costo).multiply(new BigDecimal("100"))
                            .divide(precio, 1, RoundingMode.HALF_UP).doubleValue();
                    out.add(new Top(rs.getString("codigo"), rs.getString("descripcion"), 0, precio, pct));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "HerramientasAgente.mejorMargen", e);
        }
        return out;
    }
}

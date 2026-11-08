package com.vendex.service;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.InventarioDAOPostgres;
import com.vendex.model.Inventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;

/** Confirma compra, existencias y crédito en una sola transacción. */
public class IngresoStockService {
    public static String codigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public List<Integer> guardar(List<Inventario> productos, String facturaAnterior, int proveedorAnterior) throws SQLException {
        if (productos == null || productos.isEmpty()) throw new IllegalArgumentException("Agregue al menos un producto.");
        Inventario cabecera = productos.get(0);
        int sucursal = cabecera.getSucursalId();
        if (sucursal <= 0) throw new IllegalArgumentException("Sucursal obligatoria.");
        String numero = cabecera.getNumeroFactura() == null ? "" : cabecera.getNumeroFactura().trim();
        if (numero.isEmpty()) throw new IllegalArgumentException("Ingrese el número de factura del proveedor.");
        for (Inventario producto : productos) {
            if (producto.getSucursalId() != sucursal || producto.getProveedorId() != cabecera.getProveedorId()
                    || producto.getNumeroFactura() == null || !numero.equals(producto.getNumeroFactura().trim()))
                throw new IllegalArgumentException("Los productos deben pertenecer a la misma factura y sucursal.");
            if (producto.getCantidad() <= 0 || producto.getCostoSinIVA() == null || producto.getCostoSinIVA().signum() <= 0)
                throw new IllegalArgumentException("Cantidad y costo de compra deben ser mayores a cero.");
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                bloquear(con);
                if (facturaAnterior != null && !facturaAnterior.isBlank()) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT id FROM factura_proveedor WHERE numero_factura=? "
                            + "AND COALESCE(proveedor_id,0)=? AND (sucursal_id=? OR sucursal_id IS NULL)")) {
                        ps.setString(1, facturaAnterior); ps.setInt(2, proveedorAnterior); ps.setInt(3, sucursal);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new IllegalArgumentException("La factura original no existe en esta sucursal.");
                        }
                    }
                    new InventarioDAOPostgres().eliminarFacturaConDependencias(con, facturaAnterior, proveedorAnterior, sucursal);
                }
                try (PreparedStatement ps = con.prepareStatement("SELECT id FROM factura_proveedor WHERE numero_factura=? "
                        + "AND COALESCE(proveedor_id,0)=? AND (sucursal_id=? OR sucursal_id IS NULL)")) {
                    ps.setString(1, numero); ps.setInt(2, cabecera.getProveedorId()); ps.setInt(3, sucursal);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) throw new IllegalArgumentException("Ya existe la factura " + numero + " de este proveedor.");
                    }
                }
                List<Integer> ids = new ArrayList<>();
                for (Inventario producto : productos) {
                    List<Aplicacion> aplicaciones = aplicarIngreso(con, producto);
                    producto.setId(aplicaciones.get(0).id());
                    producto.setTagCodigo(aplicaciones.get(0).tag());
                    Integer credito = crearCredito(con, producto);
                    BigDecimal iva = producto.getCostoSinIVA().multiply(new BigDecimal("0.15")).setScale(3, RoundingMode.HALF_UP);
                    BigDecimal total = producto.getCostoSinIVA().add(iva).multiply(BigDecimal.valueOf(producto.getCantidad()))
                            .setScale(3, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
                    int linea = insertarLinea(con, producto, credito, total);
                    for (Aplicacion aplicacion : aplicaciones) {
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO ingreso_stock_movimiento "
                                + "(factura_proveedor_id,inventario_id,cantidad) VALUES (?,?,?)")) {
                            ps.setInt(1, linea); ps.setInt(2, aplicacion.id()); ps.setInt(3, aplicacion.cantidad()); ps.executeUpdate();
                        }
                    }
                    ids.add(producto.getId());
                }
                con.commit();
                com.vendex.util.InventarioEventos.notificar();
                return ids;
            } catch (Exception e) {
                con.rollback();
                if (e instanceof SQLException sql) throw sql;
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new SQLException("No se pudo confirmar el ingreso", e);
            }
        }
    }

    /** Orden de bloqueo común a ingresos, reemplazos y anulaciones. */
    public static void bloquear(Connection con) throws SQLException {
        if (con.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres")) {
            // Coordina también con el alta temporal en ventas y las escrituras legacy.
            try (Statement st = con.createStatement()) {
                st.execute("LOCK TABLE inventario, factura_proveedor IN SHARE ROW EXCLUSIVE MODE");
            }
        }
    }

    private record Aplicacion(int id, int cantidad, String tag) { }
    private record Existente(int id, int cantidad, String tag) { }

    private List<Aplicacion> aplicarIngreso(Connection con, Inventario producto) throws SQLException {
        String cod = codigo(producto.getCodigo());
        producto.setCodigo(cod);
        boolean servicio = Boolean.FALSE.equals(producto.getEstado()) || "000".equals(cod)
                || "SERVICIO LOGISTICO".equalsIgnoreCase(producto.getDescripcion());
        if (!servicio && !cod.isEmpty()) {
            List<Existente> existentes = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT id, tag_codigo, cantidad FROM inventario "
                    + "WHERE sucursal_id=? AND estado=true AND UPPER(TRIM(codigo))=? "
                    + "ORDER BY CASE WHEN cantidad<0 THEN 0 ELSE 1 END, id FOR UPDATE")) {
                ps.setInt(1, producto.getSucursalId()); ps.setString(2, cod);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) existentes.add(new Existente(rs.getInt("id"), rs.getInt("cantidad"), rs.getString("tag_codigo")));
                }
            }
            if (!existentes.isEmpty()) {
                // Si quedaron lotes duplicados de versiones anteriores, compensar
                // primero todos sus negativos antes de dejar un excedente positivo.
                int restante = producto.getCantidad();
                var cantidades = new LinkedHashMap<Integer, Integer>();
                for (Existente existente : existentes) {
                    if (restante == 0 || existente.cantidad() >= 0) break;
                    int cantidad = (int) Math.min((long) restante, -(long) existente.cantidad());
                    cantidades.put(existente.id(), cantidad);
                    restante -= cantidad;
                }
                if (restante > 0) cantidades.merge(existentes.get(0).id(), restante, Math::addExact);
                List<Aplicacion> aplicaciones = new ArrayList<>();
                for (Existente existente : existentes) {
                    Integer cantidad = cantidades.get(existente.id());
                    if (cantidad == null) continue;
                    try (PreparedStatement update = con.prepareStatement("UPDATE inventario SET cantidad=cantidad+?, "
                            + "costo_sin_iva=?, precio_venta=?, descripcion=?, "
                            + "grupo_id=COALESCE(?,grupo_id), marca_id=COALESCE(?,marca_id) WHERE id=? AND sucursal_id=?")) {
                        update.setInt(1, cantidad); update.setBigDecimal(2, producto.getCostoSinIVA());
                        update.setBigDecimal(3, producto.getPrecioVenta()); update.setString(4, producto.getDescripcion());
                        nullableId(update, 5, producto.getGrupoId()); nullableId(update, 6, producto.getMarcaId());
                        update.setInt(7, existente.id()); update.setInt(8, producto.getSucursalId());
                        if (update.executeUpdate() != 1) throw new SQLException("El producto cambió durante el ingreso.");
                    }
                    aplicaciones.add(new Aplicacion(existente.id(), cantidad, existente.tag()));
                }
                return aplicaciones;
            }
        }
        int id = new InventarioDAOPostgres().guardar(con, producto);
        return List.of(new Aplicacion(id, producto.getCantidad(), producto.getTagCodigo()));
    }

    private Integer crearCredito(Connection con, Inventario producto) throws SQLException {
        if (!"TAG Crédito".equals(producto.getFormaPago()) || producto.getProveedorId() <= 0) return null;
        int meses = Math.max(1, producto.getMesesPlazo());
        BigDecimal interes = producto.getInteres() == null ? BigDecimal.ZERO : producto.getInteres();
        BigDecimal total = producto.getCostoSinIVA().multiply(BigDecimal.valueOf(producto.getCantidad()))
                .multiply(BigDecimal.ONE.add(interes.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO cuentas_por_pagar(inventario_id, proveedor_id, total, "
                + "meses_plazo, interes, cuota_mensual, estado, fecha_registro) VALUES (?,?,?,?,?,?,'Pendiente',CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, producto.getId()); ps.setInt(2, producto.getProveedorId()); ps.setBigDecimal(3, total);
            ps.setInt(4, meses); ps.setBigDecimal(5, interes);
            ps.setBigDecimal(6, total.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("No se pudo registrar el crédito del proveedor.");
    }

    private int insertarLinea(Connection con, Inventario producto, Integer credito, BigDecimal importe) throws SQLException {
        String sql = "INSERT INTO factura_proveedor(numero_factura,proveedor_id,codigo,codigo_manual,descripcion,grupo_id,marca_id,"
                + "costo_sin_iva,iva,cantidad,total_linea,fecha,inventario_id,sucursal_id,cuenta_por_pagar_id,precio_venta,forma_pago,meses_plazo,interes) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            BigDecimal costo = producto.getCostoSinIVA();
            BigDecimal iva = costo.multiply(new BigDecimal("0.15")).setScale(3, RoundingMode.HALF_UP);
            ps.setString(1, producto.getNumeroFactura().trim()); nullableId(ps, 2, producto.getProveedorId());
            ps.setString(3, producto.getTagCodigo()); ps.setString(4, producto.getCodigo());
            ps.setString(5, producto.getDescripcion()); nullableId(ps, 6, producto.getGrupoId()); nullableId(ps, 7, producto.getMarcaId());
            ps.setBigDecimal(8, costo); ps.setBigDecimal(9, iva); ps.setInt(10, producto.getCantidad());
            ps.setBigDecimal(11, importe);
            ps.setObject(12, producto.getFecha_ingreso() == null ? LocalDateTime.now() : producto.getFecha_ingreso());
            ps.setInt(13, producto.getId()); ps.setInt(14, producto.getSucursalId());
            if (credito == null) ps.setNull(15, Types.INTEGER); else ps.setInt(15, credito);
            ps.setBigDecimal(16, producto.getPrecioVenta()); ps.setString(17, producto.getFormaPago());
            ps.setInt(18, producto.getMesesPlazo()); ps.setBigDecimal(19, producto.getInteres());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("No se pudo registrar la línea de factura del ingreso.");
    }

    /** Deshace solo la cantidad aportada por esta compra, conservando ventas y otros ingresos. */
    public boolean revertirRegistrada(Connection con, String numero, int proveedor, int sucursal) throws SQLException {
        List<int[]> lineas = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT id,inventario_id,cantidad,cuenta_por_pagar_id FROM factura_proveedor "
                + "WHERE numero_factura=? AND COALESCE(proveedor_id,0)=? AND sucursal_id=? ORDER BY id FOR UPDATE")) {
            ps.setString(1, numero); ps.setInt(2, proveedor); ps.setInt(3, sucursal);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int inventario = rs.getInt("inventario_id");
                    if (inventario <= 0) throw new SQLException("La factura mezcla ingresos sin trazabilidad de stock.");
                    lineas.add(new int[]{rs.getInt("id"), inventario, rs.getInt("cantidad"), rs.getInt("cuenta_por_pagar_id")});
                }
            }
        }
        if (lineas.isEmpty()) return false;
        for (int[] linea : lineas) {
            if (linea[3] > 0) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM cuentas_por_pagar WHERE id=? "
                        + "AND COALESCE(adelanto,0)=0 AND estado='Pendiente'")) {
                    ps.setInt(1, linea[3]);
                    if (ps.executeUpdate() != 1) throw new IllegalStateException("La factura tiene pagos de proveedor registrados; no se puede reemplazar.");
                }
            }
            List<int[]> movimientos = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT inventario_id,cantidad FROM ingreso_stock_movimiento "
                    + "WHERE factura_proveedor_id=? ORDER BY inventario_id FOR UPDATE")) {
                ps.setInt(1, linea[0]);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) movimientos.add(new int[]{rs.getInt(1), rs.getInt(2)});
                }
            }
            if (movimientos.isEmpty()) movimientos.add(new int[]{linea[1], linea[2]});
            for (int[] movimiento : movimientos) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE inventario SET cantidad=cantidad-? WHERE id=? AND sucursal_id=?")) {
                    ps.setInt(1, movimiento[1]); ps.setInt(2, movimiento[0]); ps.setInt(3, sucursal);
                    if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el producto original del ingreso.");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM factura_proveedor WHERE id=?")) {
                ps.setInt(1, linea[0]); ps.executeUpdate();
            }
        }
        return true;
    }

    private static void nullableId(PreparedStatement ps, int index, int id) throws SQLException {
        if (id > 0) ps.setInt(index, id); else ps.setNull(index, Types.INTEGER);
    }
}

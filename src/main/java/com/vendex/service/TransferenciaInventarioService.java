package com.vendex.service;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.Inventario;
import com.vendex.model.TransferenciaInventario;
import com.vendex.util.SesionActual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransferenciaInventarioService {

    private final InventarioDAO inventarioDAO;
    private final SucursalDAO sucursalDAO;
    private final TransferenciaInventarioDAO transferenciaDAO;
    private final LogDAO logDAO;

    public TransferenciaInventarioService() {
        this.inventarioDAO = new InventarioDAOPostgres();
        this.sucursalDAO = new SucursalDAOPostgres();
        this.transferenciaDAO = new TransferenciaInventarioDAOPostgres();
        this.logDAO = new LogDAOPostgres();
    }

    public TransferenciaInventarioService(InventarioDAO inventarioDAO, SucursalDAO sucursalDAO, TransferenciaInventarioDAO transferenciaDAO, LogDAO logDAO) {
        this.inventarioDAO = inventarioDAO;
        this.sucursalDAO = sucursalDAO;
        this.transferenciaDAO = transferenciaDAO;
        this.logDAO = logDAO;
    }

    public TransferenciaInventario transferir(int inventarioIdOrigen, int destinoSucursalId, int cantidad, String motivo) throws Exception {
        SesionActual.exigirPermiso("INVENTARIO_AJUSTAR");
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad debe ser > 0");
        if (motivo == null || motivo.trim().isEmpty()) motivo = "Transferencia entre sucursales";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Inventario origen = inventarioDAO.obtenerPorId(con, inventarioIdOrigen);
                if (origen == null) throw new IllegalArgumentException("Inventario origen no encontrado id=" + inventarioIdOrigen);
                int origenSucursalId = origen.getSucursalId();
                if (origenSucursalId == destinoSucursalId) throw new IllegalArgumentException("Origen y destino no pueden ser la misma sucursal");
                if (origen.getCantidad() < cantidad) throw new IllegalStateException("Stock insuficiente en origen: " + origen.getCantidad() + " < " + cantidad);

                // Verificar sucursales existen y activas
                var origenSuc = sucursalDAO.obtenerPorId(origenSucursalId);
                var destinoSuc = sucursalDAO.obtenerPorId(destinoSucursalId);
                if (origenSuc.isEmpty() || !origenSuc.get().isActivo()) throw new IllegalArgumentException("Sucursal origen no existe o inactiva");
                if (destinoSuc.isEmpty() || !destinoSuc.get().isActivo()) throw new IllegalArgumentException("Sucursal destino no existe o inactiva");

                // Buscar o crear inventario destino por mismo código
                String codigo = origen.getCodigo();
                Inventario destino = inventarioDAO.obtenerPorCodigoYSucursal(con, codigo, destinoSucursalId);
                if (destino == null) {
                    // Crear nuevo registro en destino con mismo datos pero cantidad 0, luego se sumará
                    Inventario nuevo = new Inventario();
                    nuevo.setDescripcion(origen.getDescripcion());
                    nuevo.setGrupoId(origen.getGrupoId());
                    nuevo.setMarcaId(origen.getMarcaId());
                    nuevo.setCostoSinIVA(origen.getCostoSinIVA());
                    nuevo.setCantidad(0);
                    nuevo.setUbicacionPerchaId(origen.getUbicacionPerchaId());
                    nuevo.setPrecioVenta(origen.getPrecioVenta());
                    nuevo.setFecha_ingreso(origen.getFecha_ingreso());
                    nuevo.setEstado(true);
                    nuevo.setCodigo(codigo);
                    nuevo.setTagCodigo(origen.getTagCodigo());
                    nuevo.setProveedorId(origen.getProveedorId());
                    nuevo.setFormaPago(origen.getFormaPago());
                    nuevo.setMesesPlazo(origen.getMesesPlazo());
                    nuevo.setInteres(origen.getInteres());
                    nuevo.setNumeroFactura(origen.getNumeroFactura());
                    nuevo.setSucursalId(destinoSucursalId);
                    // Insertar
                    String sql = "INSERT INTO inventario(descripcion, grupo_id, marca_id, costo_sin_iva, cantidad, ubicacion_percha_id, precio_venta, fecha_ingreso, estado, tag_codigo, codigo, proveedor_id, forma_pago, meses_plazo, interes, numero_factura, sucursal_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, nuevo.getDescripcion());
                        if (nuevo.getGrupoId() > 0) ps.setInt(2, nuevo.getGrupoId()); else ps.setNull(2, java.sql.Types.INTEGER);
                        if (nuevo.getMarcaId() > 0) ps.setInt(3, nuevo.getMarcaId()); else ps.setNull(3, java.sql.Types.INTEGER);
                        ps.setBigDecimal(4, nuevo.getCostoSinIVA());
                        ps.setInt(5, 0);
                        if (nuevo.getUbicacionPerchaId() > 0) ps.setInt(6, nuevo.getUbicacionPerchaId()); else ps.setNull(6, java.sql.Types.INTEGER);
                        ps.setBigDecimal(7, nuevo.getPrecioVenta());
                        ps.setObject(8, nuevo.getFecha_ingreso());
                        ps.setBoolean(9, true);
                        ps.setString(10, nuevo.getTagCodigo());
                        ps.setString(11, nuevo.getCodigo());
                        if (nuevo.getProveedorId() > 0) ps.setInt(12, nuevo.getProveedorId()); else ps.setNull(12, java.sql.Types.INTEGER);
                        ps.setString(13, nuevo.getFormaPago());
                        if (nuevo.getMesesPlazo() > 0) ps.setInt(14, nuevo.getMesesPlazo()); else ps.setNull(14, java.sql.Types.INTEGER);
                        if (nuevo.getInteres() != null) ps.setBigDecimal(15, nuevo.getInteres()); else ps.setNull(15, java.sql.Types.DECIMAL);
                        ps.setString(16, nuevo.getNumeroFactura());
                        ps.setInt(17, destinoSucursalId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                destino = new Inventario();
                                destino.setId(rs.getInt(1));
                                destino.setCodigo(codigo);
                                destino.setSucursalId(destinoSucursalId);
                                destino.setCantidad(0);
                            } else throw new SQLException("No se pudo crear inventario destino");
                        }
                    }
                }

                // Descontar origen, incrementar destino
                inventarioDAO.descontarStock(con, origen.getId(), cantidad);
                // Para destino, usamos devolverStock (incrementa) o update directo
                String sqlDest = "UPDATE inventario SET cantidad = cantidad + ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlDest)) {
                    ps.setInt(1, cantidad);
                    ps.setInt(2, destino.getId());
                    ps.executeUpdate();
                }

                // Registrar transferencia
                TransferenciaInventario t = new TransferenciaInventario(inventarioIdOrigen, origenSucursalId, destinoSucursalId, cantidad, SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : null, motivo);
                int transId = transferenciaDAO.guardar(con, t);
                t.setId(transId);

                con.commit();
                return t;
            } catch (Exception e) {
                try { con.rollback(); } catch (SQLException re) {}
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        }
    }
}

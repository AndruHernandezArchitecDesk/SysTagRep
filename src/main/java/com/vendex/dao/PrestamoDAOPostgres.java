package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Prestamo;
import com.vendex.model.PrestamoCuota;
import com.vendex.model.PrestamoPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrestamoDAOPostgres implements PrestamoDAO {

    private static final Logger LOGGER = Logger.getLogger(PrestamoDAOPostgres.class.getName());
    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    @Override
    public int insertar(Prestamo p) {
        String sql = "INSERT INTO prestamo(entidad_id, monto_principal, tasa_periodo_pct, periodicidad, numero_cuotas, " +
                     "cuota_monto, total_a_pagar, total_interes, fecha_inicio, estado, meta_periodicidad, meta_monto, notas, fecha_registro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) RETURNING id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getEntidadId());
            ps.setBigDecimal(2, p.getMontoPrincipal());
            ps.setBigDecimal(3, p.getTasaPeriodoPct());
            ps.setString(4, p.getPeriodicidad());
            ps.setInt(5, p.getNumeroCuotas());
            ps.setBigDecimal(6, p.getCuotaMonto());
            ps.setBigDecimal(7, p.getTotalAPagar());
            ps.setBigDecimal(8, p.getTotalInteres());
            ps.setDate(9, Date.valueOf(p.getFechaInicio()));
            ps.setString(10, p.getEstado() != null ? p.getEstado() : "ACTIVO");
            ps.setString(11, p.getMetaPeriodicidad());
            if (p.getMetaMonto() != null) ps.setBigDecimal(12, p.getMetaMonto()); else ps.setNull(12, java.sql.Types.NUMERIC);
            ps.setString(13, p.getNotas());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return insertar(p);
            }
            LOGGER.log(Level.SEVERE, "insertar Prestamo", ex);
        }
        return -1;
    }

    @Override
    public void insertarCuotas(List<PrestamoCuota> cuotas) {
        if (cuotas == null || cuotas.isEmpty()) return;
        String sql = "INSERT INTO prestamo_cuota(prestamo_id, numero, fecha_vencimiento, monto, capital, interes, " +
                     "saldo_anterior, saldo_restante, monto_pagado, interes_pagado, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 'PENDIENTE')";
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (PrestamoCuota c : cuotas) {
                    ps.setInt(1, c.getPrestamoId());
                    ps.setInt(2, c.getNumero());
                    ps.setDate(3, Date.valueOf(c.getFechaVencimiento()));
                    ps.setBigDecimal(4, c.getMonto());
                    ps.setBigDecimal(5, c.getCapital());
                    ps.setBigDecimal(6, c.getInteres());
                    ps.setBigDecimal(7, c.getSaldoAnterior());
                    ps.setBigDecimal(8, c.getSaldoRestante());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                insertarCuotas(cuotas);
                return;
            }
            LOGGER.log(Level.SEVERE, "insertarCuotas Prestamo", ex);
        }
    }

    @Override
    public List<Object[]> listarActivos() {
        List<Object[]> lista = new ArrayList<>();
        String sql =
            "SELECT p.id, p.entidad_id, ef.nombre AS entidad, p.monto_principal, p.tasa_periodo_pct, p.periodicidad, " +
            "p.numero_cuotas, p.cuota_monto, p.total_a_pagar, p.total_interes, p.fecha_inicio, p.estado, " +
            "p.meta_periodicidad, p.meta_monto, p.fecha_registro, " +
            "COALESCE((SELECT SUM(c.monto - c.monto_pagado) FROM prestamo_cuota c WHERE c.prestamo_id = p.id AND c.estado <> 'PAGADA'), 0) AS saldo_pendiente, " +
            "COALESCE((SELECT COUNT(*) FROM prestamo_cuota c WHERE c.prestamo_id = p.id AND c.estado = 'PAGADA'), 0) AS cuotas_pagadas, " +
            "(SELECT MIN(c.fecha_vencimiento) FROM prestamo_cuota c WHERE c.prestamo_id = p.id AND c.estado <> 'PAGADA') AS proximo_vencimiento " +
            "FROM prestamo p " +
            "INNER JOIN entidad_financiera ef ON ef.id = p.entidad_id " +
            "ORDER BY p.estado = 'ACTIVO' DESC, p.fecha_registro DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[]{
                    rs.getInt("id"),
                    rs.getInt("entidad_id"),
                    rs.getString("entidad"),
                    rs.getBigDecimal("monto_principal"),
                    rs.getBigDecimal("tasa_periodo_pct"),
                    rs.getString("periodicidad"),
                    rs.getInt("numero_cuotas"),
                    rs.getBigDecimal("cuota_monto"),
                    rs.getBigDecimal("total_a_pagar"),
                    rs.getBigDecimal("total_interes"),
                    rs.getDate("fecha_inicio") != null ? rs.getDate("fecha_inicio").toLocalDate() : null,
                    rs.getString("estado"),
                    rs.getString("meta_periodicidad"),
                    rs.getBigDecimal("meta_monto"),
                    rs.getTimestamp("fecha_registro"),
                    rs.getBigDecimal("saldo_pendiente"),
                    rs.getInt("cuotas_pagadas"),
                    rs.getDate("proximo_vencimiento") != null ? rs.getDate("proximo_vencimiento").toLocalDate() : null
                };
                lista.add(fila);
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return listarActivos();
            }
            LOGGER.log(Level.SEVERE, "listarActivos Prestamo", ex);
        }
        return lista;
    }

    @Override
    public Prestamo obtenerPorId(int id) {
        String sql = "SELECT p.*, ef.nombre AS entidad FROM prestamo p INNER JOIN entidad_financiera ef ON ef.id = p.entidad_id WHERE p.id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearPrestamo(rs);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "obtenerPorId Prestamo", ex);
        }
        return null;
    }

    @Override
    public List<PrestamoCuota> listarCuotas(int prestamoId) {
        List<PrestamoCuota> lista = new ArrayList<>();
        String sql = "SELECT * FROM prestamo_cuota WHERE prestamo_id = ? ORDER BY numero";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearCuota(rs));
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return listarCuotas(prestamoId);
            }
            LOGGER.log(Level.SEVERE, "listarCuotas Prestamo", ex);
        }
        return lista;
    }

    @Override
    public List<PrestamoPago> listarPagos(int prestamoId) {
        List<PrestamoPago> lista = new ArrayList<>();
        String sql = "SELECT * FROM prestamo_pago WHERE prestamo_id = ? ORDER BY fecha DESC, id DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrestamoPago pg = new PrestamoPago();
                    pg.setId(rs.getInt("id"));
                    pg.setPrestamoId(rs.getInt("prestamo_id"));
                    int cuotaId = rs.getInt("cuota_id");
                    pg.setCuotaId(rs.wasNull() ? null : cuotaId);
                    pg.setMonto(rs.getBigDecimal("monto"));
                    pg.setInteresParte(rs.getBigDecimal("interes_parte"));
                    pg.setCapitalParte(rs.getBigDecimal("capital_parte"));
                    pg.setFuente(rs.getString("fuente"));
                    int ref = rs.getInt("referencia_id");
                    pg.setReferenciaId(rs.wasNull() ? null : ref);
                    int uid = rs.getInt("usuario_id");
                    pg.setUsuarioId(rs.wasNull() ? null : uid);
                    pg.setNotas(rs.getString("notas"));
                    Timestamp ts = rs.getTimestamp("fecha");
                    if (ts != null) pg.setFecha(ts.toLocalDateTime());
                    lista.add(pg);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "listarPagos Prestamo", ex);
        }
        return lista;
    }

    @Override
    public BigDecimal registrarAbono(int prestamoId, BigDecimal monto, Integer cuotaIdPreferida,
                                     String fuente, Integer referenciaId, Integer usuarioId, String notas) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Prestamo prestamo = obtenerPorIdCon(con, prestamoId);
                if (prestamo == null || !"ACTIVO".equals(prestamo.getEstado())) {
                    con.rollback();
                    return BigDecimal.ZERO;
                }
                List<PrestamoCuota> cuotas = listarCuotasCon(con, prestamoId);
                // Orden: desde cuota preferida si existe, si no desde la primera
                int desde = 1;
                if (cuotaIdPreferida != null) {
                    for (PrestamoCuota c : cuotas) {
                        if (c.getId() == cuotaIdPreferida) { desde = c.getNumero(); break; }
                    }
                }
                com.vendex.util.Amortizacion.Resultado res =
                        com.vendex.util.Amortizacion.aplicarPago(cuotas, monto, desde);
                for (com.vendex.util.Amortizacion.Aplicacion a : res.aplicaciones) {
                    PrestamoCuota cuota = null;
                    for (PrestamoCuota c : cuotas) {
                        if (c.getId() == a.cuotaId) { cuota = c; break; }
                    }
                    if (cuota == null) continue;
                    actualizarCuotaCon(con, cuota.getId(), cuota.getMontoPagado(), cuota.getInteresPagado(),
                            cuota.getSaldoRestante(), cuota.getEstado(), cuota.getFechaPago());
                    insertarPagoCon(con, prestamoId, cuota.getId(), a.monto, a.interes, a.capital,
                            fuente, referenciaId, usuarioId, notas);
                }
                // ¿Préstamo terminado?
                if (!com.vendex.util.Amortizacion.quedanPendientes(listarCuotasCon(con, prestamoId))) {
                    try (PreparedStatement ps = con.prepareStatement("UPDATE prestamo SET estado='PAGADO' WHERE id=?")) {
                        ps.setInt(1, prestamoId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return res.totalAplicado();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return registrarAbono(prestamoId, monto, cuotaIdPreferida, fuente, referenciaId, usuarioId, notas);
            }
            LOGGER.log(Level.SEVERE, "registrarAbono Prestamo", ex);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<Object[]> listarCuotasPendientes() {
        List<Object[]> lista = new ArrayList<>();
        String sql =
            "SELECT c.id, c.prestamo_id, ef.nombre AS entidad, c.numero, c.fecha_vencimiento, c.monto, c.monto_pagado, " +
            "(c.monto - c.monto_pagado) AS falta, c.estado, p.periodicidad, p.cuota_monto " +
            "FROM prestamo_cuota c " +
            "INNER JOIN prestamo p ON p.id = c.prestamo_id AND p.estado = 'ACTIVO' " +
            "INNER JOIN entidad_financiera ef ON ef.id = p.entidad_id " +
            "WHERE c.estado <> 'PAGADA' AND (c.monto - c.monto_pagado) > 0 " +
            "ORDER BY c.fecha_vencimiento ASC, c.numero ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[]{
                    rs.getInt("id"),
                    rs.getInt("prestamo_id"),
                    rs.getString("entidad"),
                    rs.getInt("numero"),
                    rs.getDate("fecha_vencimiento") != null ? rs.getDate("fecha_vencimiento").toLocalDate() : null,
                    rs.getBigDecimal("monto"),
                    rs.getBigDecimal("monto_pagado"),
                    rs.getBigDecimal("falta"),
                    rs.getString("estado"),
                    rs.getString("periodicidad"),
                    rs.getBigDecimal("cuota_monto")
                };
                lista.add(fila);
            }
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return listarCuotasPendientes();
            }
            LOGGER.log(Level.SEVERE, "listarCuotasPendientes Prestamo", ex);
        }
        return lista;
    }

    @Override
    public List<Prestamo> listarPrestamosConMeta() {
        List<Prestamo> lista = new ArrayList<>();
        String sql = "SELECT p.*, ef.nombre AS entidad FROM prestamo p " +
                     "INNER JOIN entidad_financiera ef ON ef.id = p.entidad_id " +
                     "WHERE p.estado = 'ACTIVO' AND p.meta_periodicidad IS NOT NULL AND p.meta_monto IS NOT NULL";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearPrestamo(rs));
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("does not exist")) {
                try { DatabaseConnection.ensurePrestamoSchema(); } catch (Exception ignore) {}
                return listarPrestamosConMeta();
            }
            LOGGER.log(Level.SEVERE, "listarPrestamosConMeta", ex);
        }
        return lista;
    }

    @Override
    public void actualizarMeta(int prestamoId, String metaPeriodicidad, BigDecimal metaMonto) {
        String sql = "UPDATE prestamo SET meta_periodicidad = ?, meta_monto = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, metaPeriodicidad);
            if (metaMonto != null) ps.setBigDecimal(2, metaMonto); else ps.setNull(2, java.sql.Types.NUMERIC);
            ps.setInt(3, prestamoId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "actualizarMeta Prestamo", ex);
        }
    }

    @Override
    public void actualizarEstado(int prestamoId, String estado) {
        String sql = "UPDATE prestamo SET estado = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, prestamoId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "actualizarEstado Prestamo", ex);
        }
    }

    // ---------- helpers transaccionales ----------

    private Prestamo obtenerPorIdCon(Connection con, int id) throws SQLException {
        String sql = "SELECT p.*, ef.nombre AS entidad FROM prestamo p INNER JOIN entidad_financiera ef ON ef.id = p.entidad_id WHERE p.id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearPrestamo(rs);
            }
        }
        return null;
    }

    private List<PrestamoCuota> listarCuotasCon(Connection con, int prestamoId) throws SQLException {
        List<PrestamoCuota> lista = new ArrayList<>();
        String sql = "SELECT * FROM prestamo_cuota WHERE prestamo_id = ? ORDER BY numero";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearCuota(rs));
            }
        }
        return lista;
    }

    private void actualizarCuotaCon(Connection con, int cuotaId, BigDecimal montoPagado, BigDecimal interesPagado,
                                    BigDecimal saldoRestante, String estado, java.time.LocalDate fechaPago) throws SQLException {
        String sql = "UPDATE prestamo_cuota SET monto_pagado=?, interes_pagado=?, saldo_restante=?, estado=?, fecha_pago=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, montoPagado);
            ps.setBigDecimal(2, interesPagado);
            ps.setBigDecimal(3, saldoRestante);
            ps.setString(4, estado);
            if (fechaPago != null) ps.setDate(5, Date.valueOf(fechaPago)); else ps.setNull(5, java.sql.Types.DATE);
            ps.setInt(6, cuotaId);
            ps.executeUpdate();
        }
    }

    private void insertarPagoCon(Connection con, int prestamoId, Integer cuotaId, BigDecimal monto,
                                 BigDecimal interesParte, BigDecimal capitalParte, String fuente,
                                 Integer referenciaId, Integer usuarioId, String notas) throws SQLException {
        String sql = "INSERT INTO prestamo_pago(prestamo_id, cuota_id, monto, interes_parte, capital_parte, fuente, referencia_id, usuario_id, notas, fecha) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            if (cuotaId != null) ps.setInt(2, cuotaId); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setBigDecimal(3, monto);
            ps.setBigDecimal(4, interesParte);
            ps.setBigDecimal(5, capitalParte);
            ps.setString(6, fuente);
            if (referenciaId != null) ps.setInt(7, referenciaId); else ps.setNull(7, java.sql.Types.INTEGER);
            if (usuarioId != null) ps.setInt(8, usuarioId); else ps.setNull(8, java.sql.Types.INTEGER);
            ps.setString(9, notas);
            ps.executeUpdate();
        }
    }

    private Prestamo mapearPrestamo(ResultSet rs) throws SQLException {
        Prestamo p = new Prestamo();
        p.setId(rs.getInt("id"));
        p.setEntidadId(rs.getInt("entidad_id"));
        try { p.setEntidadNombre(rs.getString("entidad")); } catch (SQLException ignore) {}
        p.setMontoPrincipal(rs.getBigDecimal("monto_principal"));
        p.setTasaPeriodoPct(rs.getBigDecimal("tasa_periodo_pct"));
        p.setPeriodicidad(rs.getString("periodicidad"));
        p.setNumeroCuotas(rs.getInt("numero_cuotas"));
        p.setCuotaMonto(rs.getBigDecimal("cuota_monto"));
        p.setTotalAPagar(rs.getBigDecimal("total_a_pagar"));
        p.setTotalInteres(rs.getBigDecimal("total_interes"));
        Date fi = rs.getDate("fecha_inicio");
        if (fi != null) p.setFechaInicio(fi.toLocalDate());
        p.setEstado(rs.getString("estado"));
        p.setMetaPeriodicidad(rs.getString("meta_periodicidad"));
        p.setMetaMonto(rs.getBigDecimal("meta_monto"));
        p.setNotas(rs.getString("notas"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) p.setFechaRegistro(ts.toLocalDateTime());
        return p;
    }

    private PrestamoCuota mapearCuota(ResultSet rs) throws SQLException {
        PrestamoCuota c = new PrestamoCuota();
        c.setId(rs.getInt("id"));
        c.setPrestamoId(rs.getInt("prestamo_id"));
        c.setNumero(rs.getInt("numero"));
        Date fv = rs.getDate("fecha_vencimiento");
        if (fv != null) c.setFechaVencimiento(fv.toLocalDate());
        c.setMonto(rs.getBigDecimal("monto"));
        c.setCapital(rs.getBigDecimal("capital"));
        c.setInteres(rs.getBigDecimal("interes"));
        c.setSaldoAnterior(rs.getBigDecimal("saldo_anterior"));
        c.setSaldoRestante(rs.getBigDecimal("saldo_restante"));
        c.setMontoPagado(rs.getBigDecimal("monto_pagado"));
        c.setInteresPagado(rs.getBigDecimal("interes_pagado"));
        c.setEstado(rs.getString("estado"));
        Date fp = rs.getDate("fecha_pago");
        if (fp != null) c.setFechaPago(fp.toLocalDate());
        return c;
    }
}

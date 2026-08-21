package com.tag.sysTagRep.service;

import com.tag.sysTagRep.dao.CajaMovimientoDAO;
import com.tag.sysTagRep.dao.CajaSesionDAO;
import com.tag.sysTagRep.model.CajaMovimiento;
import com.tag.sysTagRep.model.CajaSesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CajaService {

    private final CajaSesionDAO sesionDAO = new CajaSesionDAO();
    private final CajaMovimientoDAO movimientoDAO = new CajaMovimientoDAO();

    public int abrirCaja(int usuarioId, BigDecimal montoInicial, String observaciones) {
        CajaSesion abierta = sesionDAO.obtenerAbierta();
        if (abierta != null) {
            throw new IllegalStateException("Ya existe una sesión de caja abierta (ID: " + abierta.getId() + ")");
        }
        CajaSesion s = new CajaSesion(usuarioId, montoInicial, observaciones);
        return sesionDAO.abrir(s);
    }

    public boolean cerrarCaja(int sesionId, BigDecimal montoFisico, String observaciones) {
        CajaSesion s = sesionDAO.obtenerPorId(sesionId);
        if (s == null || !"ABIERTA".equals(s.getEstado())) {
            throw new IllegalStateException("La sesión no existe o ya está cerrada");
        }
        BigDecimal totalMovimientos = sesionDAO.calcularTotalMovimientos(sesionId);
        BigDecimal esperado = s.getMontoInicial().add(totalMovimientos);
        BigDecimal diferencia = montoFisico.subtract(esperado);
        return sesionDAO.cerrar(sesionId, montoFisico, diferencia, observaciones != null ? observaciones : "");
    }

    public int registrarMovimiento(int sesionId, String tipo, BigDecimal monto, String descripcion, int usuarioId) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        CajaSesion s = sesionDAO.obtenerPorId(sesionId);
        if (s == null || !"ABIERTA".equals(s.getEstado())) {
            throw new IllegalStateException("Sesión de caja no válida o cerrada");
        }
        CajaMovimiento m = new CajaMovimiento(sesionId, tipo, monto, descripcion, usuarioId);
        return movimientoDAO.insertar(m);
    }

    public CajaSesion obtenerSesionAbierta() {
        return sesionDAO.obtenerAbierta();
    }

    public CajaSesion obtenerSesion(int id) {
        return sesionDAO.obtenerPorId(id);
    }

    public List<CajaMovimiento> obtenerMovimientos(int sesionId) {
        return movimientoDAO.listarPorSesion(sesionId);
    }

    public List<CajaSesion> obtenerSesionesPorFecha(LocalDate desde, LocalDate hasta) {
        return sesionDAO.listarPorFecha(desde, hasta);
    }

    public Map<String, BigDecimal> obtenerResumen(int sesionId) {
        Map<String, BigDecimal> resumen = new HashMap<>();
        BigDecimal ingresos = movimientoDAO.totalPorTipo(sesionId, "INGRESO");
        BigDecimal egresos = movimientoDAO.totalPorTipo(sesionId, "EGRESO");
        BigDecimal retiros = movimientoDAO.totalPorTipo(sesionId, "RETIRO");
        BigDecimal ajustes = movimientoDAO.totalPorTipo(sesionId, "AJUSTE");
        resumen.put("INGRESO", ingresos != null ? ingresos : BigDecimal.ZERO);
        resumen.put("EGRESO", egresos != null ? egresos : BigDecimal.ZERO);
        resumen.put("RETIRO", retiros != null ? retiros : BigDecimal.ZERO);
        resumen.put("AJUSTE", ajustes != null ? ajustes : BigDecimal.ZERO);
        BigDecimal totalEgresos = resumen.get("EGRESO").add(resumen.get("RETIRO")).add(resumen.get("AJUSTE"));
        resumen.put("TOTAL_EGRESOS", totalEgresos);
        resumen.put("NETO", resumen.get("INGRESO").subtract(totalEgresos));
        return resumen;
    }

    public BigDecimal calcularEsperado(int sesionId) {
        CajaSesion s = sesionDAO.obtenerPorId(sesionId);
        if (s == null) return BigDecimal.ZERO;
        BigDecimal movimientos = sesionDAO.calcularTotalMovimientos(sesionId);
        return s.getMontoInicial().add(movimientos);
    }
}

package com.tag.sysTagRep.service;

import com.tag.sysTagRep.dao.AlertaDAO;
import com.tag.sysTagRep.dao.CuentaPorCobrarDAO;
import com.tag.sysTagRep.dao.InventarioDAO;
import com.tag.sysTagRep.model.Alerta;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlertaService {

    private static final int UMBRAL_STOCK_BAJO = 5;

    private final AlertaDAO alertaDAO = new AlertaDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final CuentaPorCobrarDAO cuentaPorCobrarDAO = new CuentaPorCobrarDAO();

    public void regenerarAlertas() {
        alertaDAO.limpiar();
        List<Alerta> alertas = new ArrayList<>();
        alertas.addAll(generarAlertasStockBajo());
        alertas.addAll(generarAlertasCuentasVencidas());
        for (Alerta a : alertas) {
            alertaDAO.insertar(a);
        }
    }

    public int obtenerCantidadNoLeidas() {
        return alertaDAO.contarNoLeidas();
    }

    public List<Alerta> obtenerTodas() {
        return alertaDAO.listarTodas();
    }

    public List<Alerta> obtenerNoLeidas() {
        return alertaDAO.listarNoLeidas();
    }

    public void marcarComoLeida(int id) {
        alertaDAO.marcarComoLeida(id);
    }

    public void marcarTodasComoLeidas() {
        alertaDAO.marcarTodasComoLeidas();
    }

    public void eliminar(int id) {
        alertaDAO.eliminar(id);
    }

    public void eliminarLeidas() {
        alertaDAO.eliminarLeidas();
    }

    private List<Alerta> generarAlertasStockBajo() {
        List<Alerta> lista = new ArrayList<>();
        var inventarios = inventarioDAO.listar();
        for (var inv : inventarios) {
            if (inv.getCantidad() < UMBRAL_STOCK_BAJO && inv.getEstado() != null && inv.getEstado()) {
                String msg = String.format("Stock bajo: %s (Código: %s) — Cantidad actual: %d",
                        inv.getDescripcion(),
                        inv.getCodigo() != null && !inv.getCodigo().isEmpty() ? inv.getCodigo() : inv.getTagCodigo(),
                        inv.getCantidad());
                Alerta a = new Alerta("STOCK_BAJO", msg, inv.getId(), "INVENTARIO");
                lista.add(a);
            }
        }
        return lista;
    }

    private List<Alerta> generarAlertasCuentasVencidas() {
        List<Alerta> lista = new ArrayList<>();
        List<Object[]> creditos = cuentaPorCobrarDAO.listarCreditosActivos();
        LocalDateTime ahora = LocalDateTime.now();
        for (Object[] fila : creditos) {
            Timestamp fechaReg = (Timestamp) fila[12];
            if (fechaReg == null) continue;
            LocalDateTime fechaInicio = fechaReg.toLocalDateTime();
            int mesesPlazo = ((Number) fila[7]).intValue();
            LocalDateTime fechaVencimiento = fechaInicio.plusMonths(mesesPlazo);
            if (fechaVencimiento.isBefore(ahora)) {
                long diasVencidos = java.time.temporal.ChronoUnit.DAYS.between(fechaVencimiento, ahora);
                String cliente = (String) fila[3];
                BigDecimal total = (BigDecimal) fila[6];
                String codigo = (String) fila[4];
                String msg = String.format("Cuenta vencida: %s (RUC: %s) — $%s — Vencida hace %d día(s)",
                        cliente, codigo, total, diasVencidos);
                int id = ((Number) fila[0]).intValue();
                Alerta a = new Alerta("CUENTA_VENCIDA", msg, id, "CUENTA_POR_COBRAR");
                lista.add(a);
            }
        }
        return lista;
    }
}

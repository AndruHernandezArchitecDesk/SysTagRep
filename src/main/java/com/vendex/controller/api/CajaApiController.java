package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.CajaMovimientoDAO;
import com.vendex.dao.CajaSesionDAO;
import com.vendex.model.CajaMovimiento;
import com.vendex.model.CajaSesion;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CajaApiController {

    private final CajaSesionDAO sesionDAO;
    private final CajaMovimientoDAO movimientoDAO;

    public CajaApiController() {
        this(AppContext.getInstance());
    }

    public CajaApiController(AppContext ctx) {
        this.sesionDAO = ctx.cajaSesionDAO;
        this.movimientoDAO = ctx.cajaMovimientoDAO;
    }

    public void abierta(Context ctx) {
        CajaSesion s = sesionDAO.obtenerAbierta();
        if (s == null) ctx.json(Map.of("abierta", false));
        else ctx.json(Map.of(
                "abierta", true,
                "id", s.getId(),
                "usuarioId", s.getUsuarioId(),
                "montoInicial", s.getMontoInicial() != null ? s.getMontoInicial().toString() : "0",
                "fechaApertura", s.getFechaApertura() != null ? s.getFechaApertura().toString() : null,
                "sucursalId", s.getSucursalId()
        ));
    }

    public void abrir(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            BigDecimal monto = body.get("montoInicial") != null ? new BigDecimal(body.get("montoInicial").toString()) : BigDecimal.ZERO;
            String obs = (String) body.getOrDefault("observaciones", "");
            int usuarioId = SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : 1;
            int id = sesionDAO.abrir(new CajaSesion(usuarioId, monto, obs));
            if (id == -1) ctx.status(500).json(Map.of("error", "No se pudo abrir caja"));
            else ctx.json(Map.of("id", id, "montoInicial", monto.toString()));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void cerrar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int sesionId = ((Number) body.get("sesionId")).intValue();
            BigDecimal montoFisico = new BigDecimal(body.get("montoFisico").toString());
            BigDecimal diferencia = body.get("diferencia") != null ? new BigDecimal(body.get("diferencia").toString()) : BigDecimal.ZERO;
            String obs = (String) body.getOrDefault("observaciones", "");
            boolean ok = sesionDAO.cerrar(sesionId, montoFisico, diferencia, obs);
            ctx.json(Map.of("cerrado", ok));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        CajaSesion s = sesionDAO.obtenerPorId(id);
        if (s == null) ctx.status(404).json(Map.of("error", "Sesion de caja no encontrada"));
        else ctx.json(Map.of(
                "id", s.getId(),
                "usuarioId", s.getUsuarioId(),
                "montoInicial", s.getMontoInicial() != null ? s.getMontoInicial().toString() : "0",
                "estado", s.getEstado(),
                "fechaApertura", s.getFechaApertura() != null ? s.getFechaApertura().toString() : null,
                "fechaCierre", s.getFechaCierre() != null ? s.getFechaCierre().toString() : null,
                "sucursalId", s.getSucursalId()
        ));
    }

    public void totalMovimientos(Context ctx) {
        int sesionId = Integer.parseInt(ctx.pathParam("sesionId"));
        BigDecimal total = movimientoDAO.totalPorTipo(sesionId, "INGRESO");
        ctx.json(Map.of("total", total != null ? total.toString() : "0"));
    }

    public void movimientos(Context ctx) {
        int sesionId = Integer.parseInt(ctx.pathParam("sesionId"));
        List<CajaMovimiento> movs = movimientoDAO.listarPorSesion(sesionId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (CajaMovimiento m : movs) {
            out.add(Map.of(
                    "id", m.getId(),
                    "tipo", m.getTipo(),
                    "monto", m.getMonto() != null ? m.getMonto().toString() : "0",
                    "descripcion", m.getDescripcion(),
                    "fecha", m.getFecha() != null ? m.getFecha().toString() : null,
                    "usuarioId", m.getUsuarioId()
            ));
        }
        ctx.json(out);
    }

    public void resumen(Context ctx) {
        int sesionId = Integer.parseInt(ctx.pathParam("sesionId"));
        BigDecimal ingresos = movimientoDAO.totalPorTipo(sesionId, "INGRESO");
        BigDecimal egresos = movimientoDAO.totalPorTipo(sesionId, "EGRESO");
        BigDecimal retiros = movimientoDAO.totalPorTipo(sesionId, "RETIRO");
        BigDecimal ajustes = movimientoDAO.totalPorTipo(sesionId, "AJUSTE");
        BigDecimal totalEgresos = (egresos != null ? egresos : BigDecimal.ZERO).add(retiros != null ? retiros : BigDecimal.ZERO).add(ajustes != null ? ajustes : BigDecimal.ZERO);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ingresos", ingresos != null ? ingresos.toString() : "0");
        res.put("egresos", egresos != null ? egresos.toString() : "0");
        res.put("retiros", retiros != null ? retiros.toString() : "0");
        res.put("ajustes", ajustes != null ? ajustes.toString() : "0");
        res.put("totalEgresos", totalEgresos.toString());
        res.put("neto", (ingresos != null ? ingresos : BigDecimal.ZERO).subtract(totalEgresos).toString());
        ctx.json(res);
    }

    public void listarPorFecha(Context ctx) {
        String desdeStr = ctx.queryParam("desde");
        String hastaStr = ctx.queryParam("hasta");
        LocalDate desde = desdeStr != null ? LocalDate.parse(desdeStr) : LocalDate.now().minusDays(30);
        LocalDate hasta = hastaStr != null ? LocalDate.parse(hastaStr) : LocalDate.now();
        List<CajaSesion> sesiones = sesionDAO.listarPorFecha(desde, hasta);
        List<Map<String, Object>> out = new ArrayList<>();
        for (CajaSesion s : sesiones) {
            out.add(Map.of(
                    "id", s.getId(),
                    "usuarioId", s.getUsuarioId(),
                    "montoInicial", s.getMontoInicial() != null ? s.getMontoInicial().toString() : "0",
                    "estado", s.getEstado(),
                    "fechaApertura", s.getFechaApertura() != null ? s.getFechaApertura().toString() : null,
                    "fechaCierre", s.getFechaCierre() != null ? s.getFechaCierre().toString() : null,
                    "sucursalId", s.getSucursalId()
            ));
        }
        ctx.json(out);
    }
}

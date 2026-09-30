package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.DashboardDAO;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.Map;

public class DashboardApiController {

    private final DashboardDAO dao;

    public DashboardApiController() {
        this(AppContext.getInstance());
    }

    public DashboardApiController(AppContext ctx) {
        this.dao = ctx.dashboardDAO;
    }

    public void metricas(Context ctx) {
        String diasStr = ctx.queryParam("dias");
        int dias = diasStr != null ? Integer.parseInt(diasStr) : 7;
        String fechaStr = ctx.queryParam("fecha");
        LocalDate fecha = fechaStr != null ? LocalDate.parse(fechaStr) : LocalDate.now();
        try {
            Map<String, Object> resp = java.util.Map.of(
                    "ventasPorDia", dao.ventasPorDia(dias),
                    "facturasPorDia", dao.facturasPorDia(dias),
                    "notasCreditoPorDia", dao.notasCreditoPorDia(dias),
                    "notasDebitoPorDia", dao.notasDebitoPorDia(dias),
                    "ventasDelDia", dao.ventasDelDia(fecha),
                    "facturasEmitidasDelDia", dao.facturasEmitidasDelDia(fecha),
                    "productosVendidosDelDia", dao.productosVendidosDelDia(fecha),
                    "clientesAtendidosDelDia", dao.clientesAtendidosDelDia(fecha),
                    "inventarioPorMarca", dao.inventarioPorMarca(),
                    "inventarioPorGrupo", dao.inventarioPorGrupo()
            );
            ctx.json(resp);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

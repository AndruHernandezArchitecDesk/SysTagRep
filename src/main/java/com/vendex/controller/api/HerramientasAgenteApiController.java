package com.vendex.controller.api;

import com.vendex.api.ApiAuthException;
import com.vendex.chatbot.HerramientasAgente;
import com.vendex.dao.HerramientasAgenteDAOPostgres;
import com.vendex.model.Inventario;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.IntFunction;

/** Endpoints de solo lectura para el agente remoto, siempre limitados a la sucursal de sesión. */
public final class HerramientasAgenteApiController {
    public record SaldoRespuesta(BigDecimal saldo) {}
    public record ComplementarioRespuesta(int id, String codigo, String descripcion, int cantidad,
                                          BigDecimal precioVenta, String grupo, String marca) {
        static ComplementarioRespuesta desde(Inventario i) {
            return new ComplementarioRespuesta(i.getId(), i.getCodigo(), i.getDescripcion(), i.getCantidad(),
                    i.getPrecioVenta(), i.getGrupo(), i.getMarca());
        }
    }

    private final IntFunction<HerramientasAgente> herramientasPorSucursal;

    public HerramientasAgenteApiController() {
        this(HerramientasAgenteDAOPostgres::new);
    }

    HerramientasAgenteApiController(IntFunction<HerramientasAgente> herramientasPorSucursal) {
        this.herramientasPorSucursal = herramientasPorSucursal;
    }

    private HerramientasAgente herramientas() {
        if (SesionActual.getUsuario() == null) throw new ApiAuthException(401, "Sesión requerida para el agente Vetxi");
        return herramientasPorSucursal.apply(SesionActual.getSucursalId());
    }

    private int limite(Context ctx) {
        String valor = ctx.queryParam("limite");
        if (valor == null || valor.isBlank()) return 5;
        try { return Math.max(1, Math.min(20, Integer.parseInt(valor))); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("limite debe ser numérico"); }
    }

    private Integer anio(Context ctx) {
        String valor = ctx.queryParam("anio");
        if (valor == null || valor.isBlank()) return null;
        try { return Integer.valueOf(valor); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("anio debe ser numérico"); }
    }

    public void productos(Context ctx) {
        ctx.json(herramientas().productos(ctx.queryParam("criterio"), limite(ctx)));
    }

    public void complementarios(Context ctx) {
        List<ComplementarioRespuesta> salida = herramientas().complementarios(
                ctx.queryParam("codigo"), limite(ctx)).stream().map(ComplementarioRespuesta::desde).toList();
        ctx.json(salida);
    }

    public void compatibles(Context ctx) {
        ctx.json(herramientas().compatibles(ctx.queryParam("descripcion"), ctx.queryParam("marca"),
                ctx.queryParam("modelo"), anio(ctx), ctx.queryParam("motor"), limite(ctx)));
    }

    public void clientes(Context ctx) {
        ctx.json(herramientas().clientes(ctx.queryParam("criterio"), limite(ctx)));
    }

    public void ventas(Context ctx) {
        ctx.json(herramientas().ventas(idCliente(ctx)));
    }

    public void saldo(Context ctx) {
        ctx.json(new SaldoRespuesta(herramientas().saldo(idCliente(ctx))));
    }

    private int idCliente(Context ctx) {
        try { return Integer.parseInt(ctx.pathParam("clienteId")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("clienteId debe ser numérico"); }
    }

    public void masVendidos(Context ctx) { ctx.json(herramientas().masVendidos(limite(ctx))); }
    public void mejorMargen(Context ctx) { ctx.json(herramientas().mejorMargen(limite(ctx))); }
}

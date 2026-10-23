package com.vendex.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vendex.chatbot.HerramientasAgente;
import com.vendex.controller.api.HerramientasAgenteApiController;
import com.vendex.model.Inventario;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Implementación REST de las herramientas de lectura del agente, equivalente a su DAO local. */
public final class HerramientasAgenteDAORest implements HerramientasAgente {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(HerramientasAgenteDAORest.class.getName());
    private final RestClient rest;

    public HerramientasAgenteDAORest(RestClient rest) { this.rest = rest; }

    private String q(String valor) { return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8); }
    private int limite(int valor) { return Math.max(1, Math.min(20, valor)); }

    @Override public List<Producto> productos(String criterio, int limite) {
        if (criterio == null || criterio.isBlank()) return List.of();
        try { return rest.get("/api/vetxi/herramientas/productos?criterio=" + q(criterio) + "&limite=" + limite(limite),
                new TypeReference<List<Producto>>() {}); }
        catch (Exception e) { throw fallo("productos", e); }
    }

    @Override public List<Inventario> complementarios(String codigo, int limite) {
        if (codigo == null || codigo.isBlank() || limite <= 0) return List.of();
        try {
            List<HerramientasAgenteApiController.ComplementarioRespuesta> items = rest.get(
                    "/api/vetxi/herramientas/complementarios?codigo=" + q(codigo) + "&limite=" + limite(limite),
                    new TypeReference<List<HerramientasAgenteApiController.ComplementarioRespuesta>>() {});
            List<Inventario> salida = new ArrayList<>();
            for (var item : items) {
                Inventario i = new Inventario();
                i.setId(item.id()); i.setCodigo(item.codigo()); i.setDescripcion(item.descripcion());
                i.setCantidad(item.cantidad()); i.setPrecioVenta(item.precioVenta());
                i.setGrupo(item.grupo()); i.setMarca(item.marca());
                salida.add(i);
            }
            return salida;
        } catch (Exception e) { throw fallo("complementarios", e); }
    }

    @Override public List<Producto> compatibles(String descripcion, String marca, String modelo,
                                                 Integer anio, String motor, int limite) {
        if (anio == null || ((marca == null || marca.isBlank()) && (modelo == null || modelo.isBlank()))) return List.of();
        String path = "/api/vetxi/herramientas/compatibles?descripcion=" + q(descripcion)
                + "&marca=" + q(marca) + "&modelo=" + q(modelo)
                + (anio == null ? "" : "&anio=" + anio) + "&motor=" + q(motor) + "&limite=" + limite(limite);
        try { return rest.get(path, new TypeReference<List<Producto>>() {}); }
        catch (Exception e) { throw fallo("compatibles", e); }
    }

    @Override public List<Cliente> clientes(String criterio, int limite) {
        if (criterio == null || criterio.isBlank()) return List.of();
        try { return rest.get("/api/vetxi/herramientas/clientes?criterio=" + q(criterio) + "&limite=" + limite(limite),
                new TypeReference<List<Cliente>>() {}); }
        catch (Exception e) { throw fallo("clientes", e); }
    }

    @Override public Ventas ventas(int clienteId) {
        try { return rest.get("/api/vetxi/herramientas/ventas/" + clienteId, Ventas.class); }
        catch (Exception e) { throw fallo("ventas", e); }
    }

    @Override public BigDecimal saldo(int clienteId) {
        try { return rest.get("/api/vetxi/herramientas/saldo/" + clienteId,
                HerramientasAgenteApiController.SaldoRespuesta.class).saldo(); }
        catch (Exception e) { throw fallo("saldo", e); }
    }

    @Override public List<Top> masVendidos(int limite) {
        try { return rest.get("/api/vetxi/herramientas/mas-vendidos?limite=" + limite(limite),
                new TypeReference<List<Top>>() {}); }
        catch (Exception e) { throw fallo("masVendidos", e); }
    }

    @Override public List<Top> mejorMargen(int limite) {
        try { return rest.get("/api/vetxi/herramientas/mejor-margen?limite=" + limite(limite),
                new TypeReference<List<Top>>() {}); }
        catch (Exception e) { throw fallo("mejorMargen", e); }
    }

    private RuntimeException fallo(String operacion, Exception e) {
        LOG.log(java.util.logging.Level.FINE, "Herramienta remota " + operacion + " no disponible; se usará el fallback local", e);
        return new IllegalStateException("Herramienta remota Vetxi no disponible: " + operacion, e);
    }
}

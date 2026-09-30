package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class NotaVentaApiController {

    private final NotaVentaRegistroDAO nvrDAO;
    private final NotaVentaDetalleDAO nvdDAO;
    private final ClienteDAO clienteDAO;
    private final EmpresaDAO empresaDAO;
    private final InventarioDAO inventarioDAO;
    private final SecuenciaDocumentoDAO secuenciaDAO;

    public NotaVentaApiController() {
        this(AppContext.getInstance());
    }

    public NotaVentaApiController(AppContext ctx) {
        this.nvrDAO = ctx.notaVentaRegistroDAO;
        this.nvdDAO = ctx.notaVentaDetalleDAO;
        this.clienteDAO = ctx.clienteDAO;
        this.empresaDAO = ctx.empresaDAO;
        this.inventarioDAO = ctx.inventarioDAO;
        this.secuenciaDAO = ctx.secuenciaDAO;
    }

    public void listar(Context ctx) {
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        try {
            List<NotaVentaRegistro> all = nvrDAO.obtenerNumNotaVenta();
            int from = (page - 1) * size;
            int to = Math.min(from + size, all.size());
            List<NotaVentaRegistro> pageItems = from < all.size() ? all.subList(from, to) : Collections.emptyList();
            List<Map<String, Object>> out = new ArrayList<>();
            for (NotaVentaRegistro n : pageItems) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", n.getId());
                m.put("codigo", n.getCodigo());
                m.put("fechaRegistro", n.getFechaRegistro() != null ? n.getFechaRegistro().toString() : null);
                m.put("sucursalId", n.getSucursalId());
                out.add(m);
            }
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("items", out);
            resp.put("total", all.size());
            resp.put("page", page);
            resp.put("size", size);
            ctx.json(resp);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        NotaVentaRegistro n = nvrDAO.obtenerNumNotaVenta().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
        if (n == null) ctx.status(404).json(Map.of("error", "Nota de venta no encontrada"));
        else {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", n.getId());
            resp.put("codigo", n.getCodigo());
            resp.put("fechaRegistro", n.getFechaRegistro() != null ? n.getFechaRegistro().toString() : null);
            resp.put("sucursalId", n.getSucursalId());
            ctx.json(resp);
        }
    }

    public void emitir(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int clienteId = ((Number) body.get("clienteId")).intValue();
            String formaPago = (String) body.getOrDefault("formaPago", "Efectivo");
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) body.getOrDefault("items", List.of());
            if (itemsRaw.isEmpty()) { ctx.status(400).json(Map.of("error", "Debe agregar al menos un producto")); return; }

            Cliente cliente = clienteDAO.obtenerPorId(clienteId);
            if (cliente == null) { ctx.status(400).json(Map.of("error", "Cliente no encontrado")); return; }
            List<Empresa> empresas = empresaDAO.listar();
            if (empresas.isEmpty()) { ctx.status(500).json(Map.of("error", "No hay empresa configurada")); return; }
            Empresa empresa = empresas.get(0);

            int puntoEmisionId = SesionActual.getPuntoEmisionId();
            int secuencial = secuenciaDAO.marcarUsado(puntoEmisionId, "PROFORMA");
            if (secuencial == -1) { ctx.status(500).json(Map.of("error", "No se pudo obtener secuencial PROFORMA")); return; }
            SecuenciaDocumento sec = secuenciaDAO.obtener(puntoEmisionId, "PROFORMA");
            String estab = sec.getEstablecimiento() != null ? sec.getEstablecimiento() : "001";
            String pto = sec.getPuntoEmision() != null ? sec.getPuntoEmision() : "001";
            String codigo = estab + "-" + pto + "-" + String.format("%09d", secuencial);

            NotaVentaRegistro nvr = new NotaVentaRegistro(empresa.getId(), clienteId, LocalDateTime.now(), codigo, formaPago, LocalDateTime.now());
            nvr.setSucursalId(SesionActual.getSucursalId());
            int nvrId = nvrDAO.insertar(nvr);
            if (nvrId == -1) { ctx.status(500).json(Map.of("error", "Error al registrar proforma " + codigo)); return; }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", nvrId);
            resp.put("codigo", codigo);
            resp.put("clienteId", clienteId);
            resp.put("sucursalId", SesionActual.getSucursalId());
            ctx.json(resp);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

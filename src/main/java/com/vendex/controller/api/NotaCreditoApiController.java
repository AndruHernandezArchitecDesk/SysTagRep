package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.NotaCreditoRegistroDAO;
import com.vendex.dao.FacturaRegistroDAO;
import com.vendex.dao.FacturaDetalleDAO;
import com.vendex.model.NotaCreditoRegistro;
import com.vendex.model.FacturaRegistro;
import com.vendex.model.FacturaDetalle;
import com.vendex.service.NotaCreditoService;
import com.vendex.util.AppConstants;
import com.vendex.util.ConfigFirma;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.File;
import java.math.BigDecimal;

public class NotaCreditoApiController {

    private final NotaCreditoRegistroDAO dao;
    private final FacturaRegistroDAO facturaRegistroDAO;
    private final FacturaDetalleDAO facturaDetalleDAO;
    private final NotaCreditoService notaCreditoService;

    public NotaCreditoApiController() {
        this(AppContext.getInstance());
    }

    public NotaCreditoApiController(AppContext ctx) {
        this.dao = ctx.notaCreditoRegistroDAO;
        this.facturaRegistroDAO = ctx.facturaRegistroDAO;
        this.facturaDetalleDAO = ctx.facturaDetalleDAO;
        this.notaCreditoService = ctx.notaCreditoService;
    }

    public void listar(Context ctx) {
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        try {
            List<NotaCreditoRegistro> all = dao.listarPaginado(page, size, ctx.queryParam("q"));
            int from = (page - 1) * size;
            int to = Math.min(from + size, all.size());
            List<NotaCreditoRegistro> pageItems = from < all.size() ? all.subList(from, to) : List.of();
            List<Map<String, Object>> out = new ArrayList<>();
            for (NotaCreditoRegistro nc : pageItems) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", nc.getId());
                m.put("claveAcceso", nc.getClaveAcceso());
                m.put("numComprobante", nc.getNumComprobante());
                m.put("fechaEmision", nc.getFechaEmision() != null ? nc.getFechaEmision().toString() : null);
                m.put("estadoSri", nc.getEstadoSri());
                m.put("facturaRegistroId", nc.getFacturaRegistroId());
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
        NotaCreditoRegistro nc = dao.obtenerPorId(id);
        if (nc == null) ctx.status(404).json(Map.of("error", "Nota de crédito no encontrada"));
        else ctx.json(Map.of(
                "id", nc.getId(),
                "claveAcceso", nc.getClaveAcceso(),
                "numComprobante", nc.getNumComprobante(),
                "fechaEmision", nc.getFechaEmision() != null ? nc.getFechaEmision().toString() : null,
                "estadoSri", nc.getEstadoSri(),
                "facturaRegistroId", nc.getFacturaRegistroId()
        ));
    }

    public void obtenerPorClave(Context ctx) {
        String clave = ctx.queryParam("claveAcceso");
        if (clave == null || clave.isBlank()) { ctx.status(400).json(Map.of("error", "claveAcceso requerida")); return; }
        NotaCreditoRegistro nc = dao.obtenerPorClave(clave);
        if (nc == null) ctx.status(404).json(Map.of("error", "Nota de crédito no encontrada"));
        else ctx.json(Map.of(
                "id", nc.getId(),
                "claveAcceso", nc.getClaveAcceso(),
                "numComprobante", nc.getNumComprobante(),
                "fechaEmision", nc.getFechaEmision() != null ? nc.getFechaEmision().toString() : null,
                "estadoSri", nc.getEstadoSri(),
                "facturaRegistroId", nc.getFacturaRegistroId()
        ));
    }

    public void actualizarEstado(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String claveAcceso = (String) body.get("claveAcceso");
            String estado = (String) body.get("estado");
            String mensaje = (String) body.getOrDefault("mensaje", "");
            String numeroAutorizacion = (String) body.getOrDefault("numeroAutorizacion", "");
            String fechaAutorizacion = (String) body.getOrDefault("fechaAutorizacion", "");
            dao.actualizarEstado(claveAcceso, estado, mensaje, numeroAutorizacion, fechaAutorizacion);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void listarPorFactura(Context ctx) {
        int facturaRegistroId = Integer.parseInt(ctx.pathParam("facturaRegistroId"));
        try {
            List<NotaCreditoRegistro> base = dao.listarPorFactura(facturaRegistroId);
            List<Map<String, Object>> out = new ArrayList<>();
            for (NotaCreditoRegistro nc : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", nc.getId());
                m.put("claveAcceso", nc.getClaveAcceso());
                m.put("numComprobante", nc.getNumComprobante());
                m.put("fechaEmision", nc.getFechaEmision() != null ? nc.getFechaEmision().toString() : null);
                m.put("estadoSri", nc.getEstadoSri());
                out.add(m);
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void listarPendientesSri(Context ctx) {
        try {
            List<NotaCreditoRegistro> base = dao.listarPendientesSri();
            List<Map<String, Object>> out = new ArrayList<>();
            for (NotaCreditoRegistro nc : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", nc.getId());
                m.put("claveAcceso", nc.getClaveAcceso());
                m.put("numComprobante", nc.getNumComprobante());
                m.put("fechaEmision", nc.getFechaEmision() != null ? nc.getFechaEmision().toString() : null);
                m.put("estadoSri", nc.getEstadoSri());
                out.add(m);
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void contar(Context ctx) {
        String filtro = ctx.queryParam("filtro");
        try {
            int total = dao.contar(filtro);
            ctx.json(Map.of("total", total));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void emitir(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int facturaRegistroId = ((Number) body.get("facturaRegistroId")).intValue();
            String motivo = (String) body.get("motivo");
            String tipoMotivo = (String) body.getOrDefault("tipoMotivo", "DEVOLUCION");
            boolean reingresaStock = Boolean.TRUE.equals(body.get("reingresaStock"));
            String ambienteSri = (String) body.getOrDefault("ambienteSri", AppConstants.AMBIENTE_PRODUCCION);
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) body.getOrDefault("items", List.of());
            if (itemsRaw.isEmpty()) { ctx.status(400).json(Map.of("error", "Debe agregar al menos un item")); return; }

            FacturaRegistro fr = facturaRegistroDAO.obtenerPorId(facturaRegistroId);
            if (fr == null) { ctx.status(400).json(Map.of("error", "Factura no encontrada")); return; }

            List<FacturaDetalle> detallesFactura = facturaDetalleDAO.listarPorFacturaRegistroId(facturaRegistroId);
            List<NotaCreditoService.DetalleNCInput> detalles = new ArrayList<>();
            for (Map<String, Object> it : itemsRaw) {
                int invId = ((Number) it.get("inventarioId")).intValue();
                int cant = ((Number) it.get("cantidad")).intValue();
                BigDecimal pUnit = new BigDecimal(it.get("precioUnitario").toString());
                Integer facturaDetalleId = null;
                String codigo = null;
                String descripcion = null;
                for (FacturaDetalle fd : detallesFactura) {
                    if (fd.getInventarioId() == invId && fd.getCantidad() == cant) {
                        facturaDetalleId = fd.getId();
                        codigo = fd.getCodigo();
                        descripcion = fd.getDescripcion();
                        break;
                    }
                }
                detalles.add(new NotaCreditoService.DetalleNCInput(
                        invId, facturaDetalleId, codigo, descripcion, new BigDecimal(cant), pUnit
                ));
            }

            int usuarioId = SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : 1;
            File dir = new File(System.getProperty("java.io.tmpdir"));
            NotaCreditoService.ResultadoNotaCredito res = notaCreditoService.emitirNotaCredito(
                    facturaRegistroId, detalles, motivo, tipoMotivo, reingresaStock, ambienteSri,
                    ConfigFirma.cargar()[0], ConfigFirma.cargar()[1], dir, usuarioId
            );

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("claveAcceso", res.claveAcceso);
            resp.put("numComprobante", res.numComprobante);
            resp.put("rutaPDF", res.rutaPDF);
            resp.put("rutaXML", res.rutaXML);
            ctx.json(resp);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void insertar(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            NotaCreditoRegistro nc = new NotaCreditoRegistro();
            nc.setFacturaRegistroId(((Number) body.get("facturaRegistroId")).intValue());
            nc.setClaveAcceso((String) body.get("claveAcceso"));
            nc.setEstablecimiento((String) body.getOrDefault("establecimiento", "001"));
            nc.setPuntoEmision((String) body.getOrDefault("puntoEmision", "001"));
            nc.setSecuencial((String) body.get("secuencial"));
            String fechaStr = (String) body.get("fechaEmision");
            if (fechaStr != null) nc.setFechaEmision(java.time.LocalDateTime.parse(fechaStr));
            nc.setMotivo((String) body.get("motivo"));
            nc.setTipoMotivo((String) body.getOrDefault("tipoMotivo", "DEVOLUCION"));
            nc.setEstadoSri((String) body.getOrDefault("estadoSri", "PENDIENTE"));
            nc.setSucursalId(com.vendex.util.SucursalActual.getId());
            int id = dao.insertar(nc);
            ctx.json(Map.of("id", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void actualizarXmlFirmado(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String claveAcceso = (String) body.get("claveAcceso");
            String xmlFirmado = (String) body.get("xmlFirmado");
            dao.actualizarXmlFirmado(claveAcceso, xmlFirmado);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }
}

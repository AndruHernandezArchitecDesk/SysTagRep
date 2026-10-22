package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.service.FacturaService;
import com.vendex.service.ProductoTemporalFactura;
import com.vendex.util.*;
import io.javalin.http.Context;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class FacturaApiController {

    private final FacturaService facturaService;
    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final InventarioDAO inventarioDAO;
    private final FacturaRegistroDAO facturaRegistroDAO;

    public FacturaApiController() {
        this(AppContext.getInstance());
    }

    public FacturaApiController(AppContext ctx) {
        this(ctx.facturaService, ctx.empresaDAO, ctx.clienteDAO, ctx.inventarioDAO, ctx.facturaRegistroDAO);
    }

    public FacturaApiController(FacturaService facturaService, EmpresaDAO empresaDAO, ClienteDAO clienteDAO,
                                InventarioDAO inventarioDAO, FacturaRegistroDAO facturaRegistroDAO) {
        this.facturaService = facturaService;
        this.empresaDAO = empresaDAO;
        this.clienteDAO = clienteDAO;
        this.inventarioDAO = inventarioDAO;
        this.facturaRegistroDAO = facturaRegistroDAO;
    }

    public void listar(Context ctx) {
        String sucursalIdStr = ctx.queryParam("sucursalId");
        int sucursalId = sucursalIdStr != null && !sucursalIdStr.isBlank() ? Integer.parseInt(sucursalIdStr) : SesionActual.getSucursalId();
        String q = ctx.queryParam("q");
        String pageStr = ctx.queryParam("page");
        String sizeStr = ctx.queryParam("size");
        int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : 25;
        try {
            List<FacturaRegistro> base = facturaRegistroDAO.listarPaginado(page, size, q);
            List<Map<String, Object>> out = new ArrayList<>();
            for (FacturaRegistro fr : base) {
                if (fr.getSucursalId() == 0 || fr.getSucursalId() == sucursalId) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", fr.getId());
                    m.put("codigo", fr.getCodigo());
                    m.put("numComprobante", fr.getNumComprobante());
                    m.put("fecha", fr.getFecha() != null ? fr.getFecha().toString() : null);
                    m.put("total", fr.getTotal() != null ? fr.getTotal().toString() : "0");
                    m.put("estadoSri", fr.getEstadoSri());
                    m.put("clienteId", fr.getClienteId());
                    out.add(m);
                }
            }
            ctx.json(out);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void obtenerPorId(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        FacturaRegistro fr = facturaRegistroDAO.obtenerPorId(id);
        if (fr == null) ctx.status(404).json(Map.of("error", "Factura no encontrada"));
        else {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", fr.getId());
            resp.put("codigo", fr.getCodigo());
            resp.put("numComprobante", fr.getNumComprobante());
            resp.put("fecha", fr.getFecha() != null ? fr.getFecha().toString() : null);
            resp.put("total", fr.getTotal() != null ? fr.getTotal().toString() : "0");
            resp.put("estadoSri", fr.getEstadoSri());
            resp.put("clienteId", fr.getClienteId());
            ctx.json(resp);
        }
    }

    public void obtenerPorClave(Context ctx) {
        String clave = ctx.queryParam("claveAcceso");
        if (clave == null || clave.isBlank()) { ctx.status(400).json(Map.of("error", "claveAcceso requerida")); return; }
        FacturaRegistro fr = facturaRegistroDAO.obtenerPorClaveAcceso(clave);
        if (fr == null) ctx.status(404).json(Map.of("error", "Factura no encontrada"));
        else ctx.json(Map.of(
                "id", fr.getId(),
                "codigo", fr.getCodigo(),
                "numComprobante", fr.getNumComprobante(),
                "fecha", fr.getFecha() != null ? fr.getFecha().toString() : null,
                "total", fr.getTotal() != null ? fr.getTotal().toString() : "0",
                "estadoSri", fr.getEstadoSri(),
                "clienteId", fr.getClienteId()
        ));
    }

    public void insertarRegistro(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int clienteId = ((Number) body.get("clienteId")).intValue();
            int empresaId = ((Number) body.get("empresaId")).intValue();
            String codigo = (String) body.get("codigo");
            String claveAcceso = (String) body.get("claveAcceso");
            String numComprobante = (String) body.get("numComprobante");
            if (codigo == null || codigo.isBlank() || claveAcceso == null || claveAcceso.isBlank()
                    || numComprobante == null || numComprobante.isBlank()) {
                ctx.status(400).json(Map.of("error", "codigo, claveAcceso y numComprobante son obligatorios (no se aceptan NULL: el UNIQUE no los frena)"));
                return;
            }
            String estadoSri = (String) body.get("estadoSri");
            String fechaStr = (String) body.get("fecha");
            String totalStr = (String) body.get("total");
            java.math.BigDecimal total = totalStr != null ? new java.math.BigDecimal(totalStr) : java.math.BigDecimal.ZERO;

            FacturaRegistro fr = new FacturaRegistro();
            fr.setEmpresaId(empresaId);
            fr.setClienteId(clienteId);
            fr.setCodigo(codigo);
            fr.setClaveAcceso(claveAcceso);
            fr.setNumComprobante(numComprobante);
            fr.setEstadoSri(estadoSri);
            if (fechaStr != null) fr.setFecha(java.time.LocalDateTime.parse(fechaStr));
            fr.setTotal(total);
            fr.setSucursalId(SesionActual.getSucursalId());

            int id = facturaRegistroDAO.insertar(fr);
            ctx.json(Map.of("id", id));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void actualizarEstado(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String claveAcceso = (String) body.get("claveAcceso");
            String estado = (String) body.get("estado");
            facturaRegistroDAO.actualizarEstado(claveAcceso, estado);
            ctx.json(Map.of("ok", true));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void listarPendientesSri(Context ctx) {
        try {
            List<FacturaRegistro> base = facturaRegistroDAO.listarPendientesSri();
            List<Map<String, Object>> out = new ArrayList<>();
            for (FacturaRegistro fr : base) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", fr.getId());
                m.put("claveAcceso", fr.getClaveAcceso());
                m.put("numComprobante", fr.getNumComprobante());
                m.put("fecha", fr.getFecha() != null ? fr.getFecha().toString() : null);
                m.put("total", fr.getTotal() != null ? fr.getTotal().toString() : "0");
                m.put("estadoSri", fr.getEstadoSri());
                m.put("clienteId", fr.getClienteId());
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
            int total = facturaRegistroDAO.contar(filtro);
            ctx.json(Map.of("total", total));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    public void emitir(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            int clienteId = ((Number) body.get("clienteId")).intValue();
            String formaPago = (String) body.getOrDefault("formaPago", "Efectivo");
            String ambienteSri = (String) body.getOrDefault("ambienteSri", AppConstants.AMBIENTE_PRODUCCION);
            Object descRaw = body.get("descuentoValor") != null ? body.get("descuentoValor") : body.get("descuentoPct");
            BigDecimal descuentoValor = descRaw != null ? new BigDecimal(descRaw.toString()) : BigDecimal.ZERO;

            Cliente cliente = clienteDAO.obtenerPorId(clienteId);
            if (cliente == null) { ctx.status(400).json(Map.of("error", "Cliente no encontrado")); return; }

            List<Empresa> empresas = empresaDAO.listar();
            if (empresas.isEmpty()) { ctx.status(500).json(Map.of("error", "No hay empresa configurada")); return; }
            Empresa empresa = empresas.get(0);

            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) body.getOrDefault("items", List.of());
            if (itemsRaw.isEmpty()) { ctx.status(400).json(Map.of("error", "Debe agregar al menos un producto")); return; }

            List<FacturaDetalle> items = new ArrayList<>();
            for (Map<String, Object> it : itemsRaw) {
                if (it == null) throw new IllegalArgumentException("Detalle de factura inválido.");
                int invId, cant;
                BigDecimal pUnit;
                try {
                    invId = new BigDecimal(String.valueOf(it.get("inventarioId"))).intValueExact();
                    cant = new BigDecimal(String.valueOf(it.get("cantidad"))).intValueExact();
                    pUnit = new BigDecimal(String.valueOf(it.get("precioUnitario")));
                } catch (ArithmeticException | NumberFormatException e) {
                    throw new IllegalArgumentException("Identificador y cantidad enteros, y precio numérico obligatorios.");
                }
                if (cant <= 0 || pUnit.signum() <= 0 || invId < 0) throw new IllegalArgumentException("Cantidad y precio deben ser mayores a cero.");
                boolean temporal = Boolean.TRUE.equals(it.get("temporal"));
                if (temporal && invId == 0) {
                    items.add(ProductoTemporalFactura.crear(it.get("codigo") instanceof String cod ? cod : null,
                            it.get("descripcion") instanceof String desc ? desc : null, cant, pUnit));
                } else {
                    if (temporal || invId == 0) throw new IllegalArgumentException("El producto temporal debe indicar temporal=true e inventarioId=0.");
                    Inventario inv = inventarioDAO.obtenerPorId(invId);
                    if (inv == null) throw new IllegalArgumentException("Inventario no encontrado id=" + invId);
                    if (inv.getSucursalId() != SesionActual.getSucursalId())
                        throw new IllegalArgumentException("El producto no pertenece a la sucursal de la sesión.");
                    items.add(new FacturaDetalle(invId, inv.getCodigo(), inv.getDescripcion(), cant, pUnit));
                }
            }
            items = ProductoTemporalFactura.copiarYValidar(items);

            String rutaP12 = ConfigFirma.cargar()[0];
            String claveP12 = ConfigFirma.cargar()[1];
            File dir = new File(System.getProperty("java.io.tmpdir"));
            int usuarioId = com.vendex.util.SesionActual.getUsuario() != null ? com.vendex.util.SesionActual.getUsuario().getId() : 1;

            FacturaService.ResultadoFactura resultado = facturaService.guardarFactura(
                    cliente, empresa, null, items, formaPago, null, null,
                    ambienteSri, rutaP12, claveP12, dir, descuentoValor
            );

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", resultado.itemsDetalle != null && !resultado.itemsDetalle.isEmpty() ? resultado.itemsDetalle.get(0).getFacturaRegistroId() : -1);
            resp.put("claveAcceso", resultado.claveAcceso);
            resp.put("numComprobante", resultado.numComprobante);
            resp.put("ambienteSri", resultado.ambienteSri);
            resp.put("codEstab", resultado.codEstab);
            resp.put("codPtoEmi", resultado.codPtoEmi);
            resp.put("secuencial", resultado.secuencialFE);
            resp.put("fechaEmision", resultado.fechaEmisionFE);
            resp.put("total", resultado.totCalc != null ? resultado.totCalc.toString() : "0");
            resp.put("firmaOk", resultado.firmaOk);
            resp.put("rutaPDF", resultado.rutaPDF);
            resp.put("rutaXML", resultado.rutaXML);
            ctx.json(resp);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Datos de factura inválidos"));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno", "type", e.getClass().getName()));
        }
    }
}

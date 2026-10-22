package com.vendex.controller.api;

import com.vendex.config.AppContext;
import com.vendex.model.CuentaPorPagar;
import com.vendex.model.FacturaProveedor;
import com.vendex.model.Inventario;
import com.vendex.service.NotaDebitoService;
import com.vendex.util.AppConstants;
import com.vendex.util.ConfigFirma;
import com.vendex.util.SRIWebService;
import com.vendex.util.SesionActual;
import io.javalin.http.Context;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fase 7: endpoints que cierran los huecos del SyncService offline
 * (NOTA_DEBITO, INVENTARIO_ELIMINAR, INGRESO_MERCADERIA, CUENTA_POR_COBRAR/PAGAR).
 * Reproducen el mismo flujo de los controllers online.
 */
public class OfflineSyncApiController {

    private final AppContext ctx;

    public OfflineSyncApiController() {
        this(AppContext.getInstance());
    }

    public OfflineSyncApiController(AppContext ctx) {
        this.ctx = ctx;
    }

    // ===== NOTA_DEBITO =====
    public void emitirNotaDebito(Context http) {
        try {
            Map<String, Object> body = http.bodyAsClass(Map.class);
            int facturaId = ((Number) body.get("facturaId")).intValue();
            String formaPago = str(body.get("formaPago"));
            String ambiente = str(body.getOrDefault("ambienteSri", AppConstants.AMBIENTE_PRUEBAS));
            List<NotaDebitoService.MotivoNDInput> motivos = new ArrayList<>();
            Object raw = body.get("motivos");
            if (raw instanceof List<?> lista) {
                for (Object o : lista) {
                    Map<?, ?> m = (Map<?, ?>) o;
                    motivos.add(new NotaDebitoService.MotivoNDInput(
                            str(m.get("razon")), new BigDecimal(String.valueOf(m.get("valor")))));
                }
            }
            int usuarioId = SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : 1;
            File dir = new File(System.getProperty("java.io.tmpdir"));
            NotaDebitoService.ResultadoNotaDebito res = ctx.notaDebitoService.emitirNotaDebito(
                    facturaId, motivos, formaPago, ambiente,
                    ConfigFirma.cargar()[0], ConfigFirma.cargar()[1], dir, usuarioId);

            // Contingencia todo a cola: provisional + encolar SRI (igual que el controller online)
            SRIWebService.SRIResponse provisional = new SRIWebService.SRIResponse();
            provisional.setEstado(AppConstants.ESTADO_PENDIENTE);
            provisional.setMensaje("Pendiente SRI - en cola");
            try { ctx.notaDebitoService.finalizarEnvioSRI(provisional, res, dir); } catch (Exception ignore) {}
            try { ctx.comprobantePendienteSriDAO.encolar("NOTA_DEBITO", res.claveAcceso, res.numComprobante, ambiente); } catch (Exception ignore) {}

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("claveAcceso", res.claveAcceso);
            resp.put("numComprobante", res.numComprobante);
            http.json(resp);
        } catch (IllegalArgumentException | IllegalStateException e) {
            http.status(400).json(Map.of("error", msg(e)));
        } catch (Exception e) {
            http.status(500).json(Map.of("error", msg(e)));
        }
    }

    // ===== INVENTARIO_ELIMINAR =====
    public void eliminarInventario(Context http) {
        try {
            SesionActual.exigirPermiso("INVENTARIO_AJUSTAR");
            Map<String, Object> body = http.bodyAsClass(Map.class);
            int id = ((Number) body.get("inventarioId")).intValue();
            ctx.inventarioDAO.eliminar(id);
            http.json(Map.of("eliminado", true, "inventarioId", id));
        } catch (com.vendex.exception.SinPermisoException e) {
            http.status(403).json(Map.of("error", msg(e)));
        } catch (Exception e) {
            http.status(500).json(Map.of("error", msg(e)));
        }
    }

    // ===== CUENTA_POR_COBRAR (adelanto) =====
    public void adelantoCobrar(Context http) {
        try {
            Map<String, Object> body = http.bodyAsClass(Map.class);
            int id = ((Number) body.get("cuentaPorCobrarId")).intValue();
            BigDecimal adelanto = new BigDecimal(String.valueOf(body.get("adelanto")));
            ctx.cuentaPorCobrarDAO.registrarAdelanto(id, adelanto);
            if (Boolean.TRUE.equals(body.get("pagado"))) ctx.cuentaPorCobrarDAO.marcarPagado(id);
            http.json(Map.of("cuentaPorCobrarId", id, "adelanto", adelanto, "pagado",
                    Boolean.TRUE.equals(body.get("pagado"))));
        } catch (Exception e) {
            http.status(500).json(Map.of("error", msg(e)));
        }
    }

    // ===== CUENTA_POR_PAGAR (abono) =====
    public void adelantoPagar(Context http) {
        try {
            Map<String, Object> body = http.bodyAsClass(Map.class);
            int id = ((Number) body.get("cuentaPorPagarId")).intValue();
            BigDecimal adelanto = new BigDecimal(String.valueOf(body.get("adelanto")));
            ctx.cuentaPorPagarDAO.registrarAdelanto(id, adelanto);
            if (Boolean.TRUE.equals(body.get("pagado"))) ctx.cuentaPorPagarDAO.marcarPagado(id);
            http.json(Map.of("cuentaPorPagarId", id, "adelanto", adelanto, "pagado",
                    Boolean.TRUE.equals(body.get("pagado"))));
        } catch (Exception e) {
            http.status(500).json(Map.of("error", msg(e)));
        }
    }

    // ===== INGRESO_MERCADERIA =====
    public void ingresoMercaderia(Context http) {
        try {
            Map<String, Object> b = http.bodyAsClass(Map.class);
            boolean modoEdicion = Boolean.TRUE.equals(b.get("modoEdicion"));
            Integer proveedorId = b.get("proveedorId") != null ? ((Number) b.get("proveedorId")).intValue() : null;
            String numFactura = str(b.getOrDefault("numeroFactura", ""));
            LocalDateTime fecha = b.get("fecha") != null ? LocalDateTime.parse(String.valueOf(b.get("fecha")))
                    : LocalDateTime.now();
            String formaPago = str(b.get("formaPago"));
            int meses = b.get("mesesPlazo") != null ? ((Number) b.get("mesesPlazo")).intValue() : 0;
            BigDecimal interes = b.get("interes") != null ? new BigDecimal(String.valueOf(b.get("interes"))) : BigDecimal.ZERO;

            if (modoEdicion) {
                Inventario i = new Inventario();
                i.setId(Integer.parseInt(String.valueOf(b.get("inventarioId"))));
                i.setDescripcion(str(b.get("descripcion")));
                i.setGrupoId(b.get("grupoId") != null ? ((Number) b.get("grupoId")).intValue() : 0);
                i.setMarcaId(b.get("marcaId") != null ? ((Number) b.get("marcaId")).intValue() : 0);
                i.setCostoSinIVA(new BigDecimal(String.valueOf(b.get("costoSinIVA"))));
                i.setCantidad(((Number) b.get("cantidad")).intValue());
                i.setPrecioVenta(new BigDecimal(String.valueOf(b.get("precioVenta"))));
                i.setTagCodigo(str(b.get("codigo")));
                i.setCodigo(str(b.get("codigoManual")));
                i.setFecha_ingreso(fecha);
                i.setNumeroFactura(numFactura);
                i.setProveedorId(proveedorId != null ? proveedorId : 0);
                i.setFormaPago(formaPago);
                i.setMesesPlazo("TAG Crédito".equals(formaPago) ? meses : 0);
                i.setInteres("TAG Crédito".equals(formaPago) ? interes : BigDecimal.ZERO);
                ctx.inventarioDAO.actualizar(i);
                http.json(Map.of("actualizado", true, "inventarioId", i.getId()));
                return;
            }

            Object rawProd = b.get("productos");
            if (!(rawProd instanceof List<?> productos) || productos.isEmpty()) {
                http.status(400).json(Map.of("error", "Debe agregar al menos un producto"));
                return;
            }
            if (ctx.facturaProveedorDAO.existeNumeroFactura(numFactura, proveedorId != null ? proveedorId : 0)) {
                http.status(409).json(Map.of("error", "Ya existe una factura registrada con el N° " + numFactura));
                return;
            }

            List<FacturaProveedor> lineasFactura = new ArrayList<>();
            for (Object o : productos) {
                Map<?, ?> p = (Map<?, ?>) o;
                BigDecimal costo = new BigDecimal(String.valueOf(p.get("costoSinIVA")));
                int cant = ((Number) p.get("cantidad")).intValue();
                BigDecimal precioVenta = p.get("precioVenta") != null
                        ? new BigDecimal(String.valueOf(p.get("precioVenta"))) : costo;

                Inventario i = new Inventario();
                i.setDescripcion(str(p.get("descripcion")));
                i.setGrupoId(p.get("grupoId") != null ? ((Number) p.get("grupoId")).intValue() : 0);
                i.setMarcaId(p.get("marcaId") != null ? ((Number) p.get("marcaId")).intValue() : 0);
                i.setCostoSinIVA(costo);
                i.setCantidad(cant);
                i.setPrecioVenta(precioVenta);
                i.setTagCodigo(str(p.get("codigo")));
                i.setCodigo(str(p.get("codigoManual")));
                String desc = str(p.get("descripcion"));
                String codManual = str(p.get("codigoManual"));
                boolean servicio = "SERVICIO LOGISTICO".equalsIgnoreCase(desc) || "000".equals(codManual);
                i.setEstado(!servicio);
                i.setFecha_ingreso(fecha);
                i.setNumeroFactura(numFactura);
                i.setProveedorId(proveedorId != null ? proveedorId : 0);
                i.setFormaPago(formaPago);
                i.setMesesPlazo("TAG Crédito".equals(formaPago) ? meses : 0);
                i.setInteres("TAG Crédito".equals(formaPago) ? interes : BigDecimal.ZERO);

                int id = ctx.inventarioDAO.guardar(i);
                if ("TAG Crédito".equals(formaPago) && proveedorId != null && id > 0) {
                    BigDecimal total = costo.multiply(BigDecimal.valueOf(cant));
                    crearCreditoProveedor(id, proveedorId, total, meses, interes);
                }

                BigDecimal iva = costo.multiply(new BigDecimal("0.15")).setScale(3, RoundingMode.HALF_UP);
                BigDecimal totalLinea = costo.add(iva).multiply(BigDecimal.valueOf(cant)).setScale(3, RoundingMode.HALF_UP);
                FacturaProveedor linea = new FacturaProveedor();
                linea.setNumeroFactura(numFactura);
                linea.setProveedorId(proveedorId != null ? proveedorId : 0);
                linea.setCodigo(str(p.get("codigo")));
                linea.setCodigoManual(codManual);
                linea.setDescripcion(desc);
                linea.setGrupoId(i.getGrupoId());
                linea.setMarcaId(i.getMarcaId());
                linea.setCostoSinIVA(costo);
                linea.setIva(iva);
                linea.setCantidad(cant);
                linea.setTotalLinea(totalLinea);
                linea.setFecha(fecha);
                lineasFactura.add(linea);
            }
            ctx.facturaProveedorDAO.insertar(lineasFactura);
            http.json(Map.of("guardado", true, "numeroFactura", numFactura, "lineas", lineasFactura.size()));
        } catch (Exception e) {
            http.status(500).json(Map.of("error", msg(e)));
        }
    }

    private void crearCreditoProveedor(int inventarioId, int proveedorId, BigDecimal total, int mesesPlazo, BigDecimal interesPct) {
        if (mesesPlazo <= 0) mesesPlazo = 1;
        BigDecimal tasa = interesPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalConInteres = total.multiply(BigDecimal.ONE.add(tasa));
        BigDecimal cuotaMensual = totalConInteres.divide(BigDecimal.valueOf(mesesPlazo), 2, RoundingMode.HALF_UP);
        ctx.cuentaPorPagarDAO.insertar(new CuentaPorPagar(inventarioId, proveedorId, totalConInteres, mesesPlazo, interesPct, cuotaMensual));
    }

    private static String str(Object o) { return o != null ? String.valueOf(o) : null; }

    private static String msg(Exception e) { return e.getMessage() != null ? e.getMessage() : "Error interno"; }
}

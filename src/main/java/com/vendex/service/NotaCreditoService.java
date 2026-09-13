package com.vendex.service;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.util.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotaCreditoService {

    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final InventarioDAO inventarioDAO;
    private final FacturaRegistroDAO facturaRegistroDAO;
    private final FacturaDetalleDAO facturaDetalleDAO;
    private final SecuenciaDocumentoDAO secuenciaDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final NotaCreditoRegistroDAO notaCreditoDAO;
    private final NotaCreditoDetalleDAO notaCreditoDetalleDAO;
    private final HistorialProductoDAO historialProductoDAO;
    private final CajaSesionDAO cajaSesionDAO;
    private final CajaMovimientoDAO cajaMovimientoDAO;
    private final LogDAO logDAO;

    public NotaCreditoService() {
        this.empresaDAO = new EmpresaDAO();
        this.clienteDAO = new ClienteDAO();
        this.inventarioDAO = new InventarioDAO();
        this.facturaRegistroDAO = new FacturaRegistroDAO();
        this.facturaDetalleDAO = new FacturaDetalleDAO();
        this.secuenciaDAO = new SecuenciaDocumentoDAO();
        this.comprobanteDAO = new ComprobanteDAO();
        this.notaCreditoDAO = new NotaCreditoRegistroDAO();
        this.notaCreditoDetalleDAO = new NotaCreditoDetalleDAO();
        this.historialProductoDAO = new HistorialProductoDAO();
        this.cajaSesionDAO = new CajaSesionDAO();
        this.cajaMovimientoDAO = new CajaMovimientoDAO();
        this.logDAO = new LogDAO();
    }

    public static class DetalleNCInput {
        public Integer inventarioId;
        public Integer facturaDetalleId;
        public String codigo;
        public String descripcion;
        public BigDecimal cantidad;
        public BigDecimal precioUnitario;
        public BigDecimal descuento = BigDecimal.ZERO;
        public DetalleNCInput(Integer inventarioId, Integer facturaDetalleId, String codigo, String descripcion, BigDecimal cantidad, BigDecimal precioUnitario) {
            this.inventarioId = inventarioId;
            this.facturaDetalleId = facturaDetalleId;
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }
    }

    public ResultadoNotaCredito emitirNotaCredito(int facturaRegistroId, List<DetalleNCInput> detallesInput,
                                                  String motivo, String tipoMotivo, boolean reingresaStock,
                                                  String ambienteSri, String rutaP12, String claveP12,
                                                  File directorioEscritorio, int usuarioId) throws Exception {
        if (motivo == null || motivo.trim().isEmpty()) throw new IllegalArgumentException("El motivo es obligatorio (SRI).");
        if (detallesInput == null || detallesInput.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un item a acreditar.");
        if (tipoMotivo == null || !(tipoMotivo.equals("DEVOLUCION") || tipoMotivo.equals("DESCUENTO") || tipoMotivo.equals("ANULACION")))
            throw new IllegalArgumentException("tipoMotivo invalido: " + tipoMotivo);

        FacturaRegistro factura = obtenerFacturaPorId(facturaRegistroId);
        if (factura == null) throw new IllegalArgumentException("Factura no encontrada id=" + facturaRegistroId);
        if (!AppConstants.ESTADO_AUTORIZADO.equals(factura.getEstadoSri())) throw new IllegalStateException("Solo se permite NC sobre factura AUTORIZADA. Estado actual: " + factura.getEstadoSri());

        // Validar saldo disponible
        BigDecimal totalFactura = factura.getTotal() != null ? factura.getTotal() : BigDecimal.ZERO;
        BigDecimal sumaPrevias = notaCreditoDAO.sumarValorModificacionPorFactura(facturaRegistroId, AppConstants.ESTADO_AUTORIZADO);
        // Calcular nuevo valorModificacion
        BigDecimal subSinIva = BigDecimal.ZERO;
        List<NotaCreditoDetalle> detallesParaDao = new ArrayList<>();
        List<Object[]> detallesParaXml = new ArrayList<>();
        for (DetalleNCInput in : detallesInput) {
            BigDecimal precioSinIva = in.precioUnitario.divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            BigDecimal totalSinIva = precioSinIva.multiply(in.cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal desc = in.descuento != null ? in.descuento : BigDecimal.ZERO;
            String descTxt = in.descripcion != null && in.descripcion.length() > AppConstants.MAX_DESCRIPCION_XML ? in.descripcion.substring(0, AppConstants.MAX_DESCRIPCION_XML) : in.descripcion;
            subSinIva = subSinIva.add(totalSinIva).subtract(desc);
            NotaCreditoDetalle nd = new NotaCreditoDetalle();
            nd.setFacturaDetalleId(in.facturaDetalleId);
            nd.setInventarioId(in.inventarioId);
            nd.setDescripcion(descTxt);
            nd.setCantidad(in.cantidad);
            nd.setPrecioUnitario(precioSinIva.setScale(4, RoundingMode.HALF_UP));
            nd.setDescuento(desc.setScale(2, RoundingMode.HALF_UP));
            nd.setCodigoPorcentajeIva("4");
            nd.setPrecioTotalSinImpuesto(totalSinIva.subtract(desc).setScale(2, RoundingMode.HALF_UP));
            detallesParaDao.add(nd);
            detallesParaXml.add(new Object[]{in.codigo != null ? in.codigo : "NC", descTxt, in.cantidad.toPlainString(), precioSinIva.setScale(2, RoundingMode.HALF_UP).toPlainString(), desc.toPlainString(), totalSinIva.subtract(desc).setScale(2, RoundingMode.HALF_UP).toPlainString()});
        }
        if (subSinIva.compareTo(BigDecimal.ZERO) < 0) subSinIva = BigDecimal.ZERO;
        subSinIva = subSinIva.setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = subSinIva.multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorModificacion = subSinIva.add(iva).setScale(2, RoundingMode.HALF_UP);

        if (sumaPrevias.add(valorModificacion).compareTo(totalFactura) > 0) {
            throw new IllegalStateException("La NC excede el saldo disponible. Total factura: " + totalFactura + ", ya acreditado: " + sumaPrevias + ", nuevo: " + valorModificacion);
        }

        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (empresa == null) throw new IllegalStateException("No se encontraron datos de la empresa.");
        Cliente cliente = clienteDAO.obtenerPorId(factura.getClienteId());
        if (cliente == null) throw new IllegalStateException("Cliente no encontrado para la factura.");

        int secuencialNC = secuenciaDAO.marcarUsado("NOTA_CREDITO");
        if (secuencialNC == -1) throw new IllegalStateException("No se pudo obtener secuencial NOTA_CREDITO.");
        SecuenciaDocumento sec = secuenciaDAO.obtener("NOTA_CREDITO");
        String codEstab = sec.getEstablecimiento() != null ? sec.getEstablecimiento() : AppConstants.ESTABLECIMIENTO_DEFAULT;
        String codPtoEmi = sec.getPuntoEmision() != null ? sec.getPuntoEmision() : AppConstants.PUNTO_EMISION_DEFAULT;
        String claveAcceso = ClaveAcceso.generar(AppConstants.TIPO_COMPROBANTE_NOTA_CREDITO, empresa.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialNC);
        String secuencialStr = String.format("%09d", secuencialNC);
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + secuencialStr;
        LocalDateTime ahora = LocalDateTime.now();
        String fechaEmision = ahora.format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
        String fechaEmisionDocSustento = factura.getFecha() != null ? factura.getFecha().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : fechaEmision;
        String numDocModificado = factura.getNumComprobante() != null ? factura.getNumComprobante() : factura.getCodigo();

        String tipoIdComp;
        String identTrim = cliente.getIdentificacion() != null ? cliente.getIdentificacion().trim() : "";
        if (AppConstants.esConsumidorFinal(identTrim)) tipoIdComp = AppConstants.TIPO_ID_CONSUMIDOR_FINAL;
        else tipoIdComp = identTrim.length() == AppConstants.MAX_LONGITUD_IDENTIFICACION_JURIDICA ? "04" : "05";

        String dirMatriz = empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria();
        String xmlGenerado = XmlNotaCreditoBuilder.construirNotaCredito(
                ambienteSri, claveAcceso, empresa.getRuc(), empresa.getRazonSocial(),
                codEstab, codPtoEmi, secuencialNC, dirMatriz, dirMatriz,
                "", "NO", tipoIdComp, cliente.getNombre(), cliente.getIdentificacion(),
                AppConstants.COD_DOC_MODIFICADO_FACTURA, numDocModificado, fechaEmisionDocSustento,
                fechaEmision, subSinIva.toPlainString(), valorModificacion.toPlainString(), iva.toPlainString(),
                motivo.length() > 300 ? motivo.substring(0,300) : motivo,
                detallesParaXml
        );

        if (xmlGenerado == null) throw new IllegalStateException("No se pudo generar el XML de nota de credito.");

        String xmlFirmado = xmlGenerado;
        if (rutaP12 == null || rutaP12.trim().isEmpty() || claveP12 == null || claveP12.trim().isEmpty()) {
            throw new IllegalStateException("No se configuró la firma electrónica (.p12 y contraseña).");
        }
        try {
            FirmaDigital firma = new FirmaDigital();
            if (!firma.cargarCertificado(rutaP12, claveP12)) throw new IllegalStateException("No se pudo cargar el certificado.");
            xmlFirmado = firma.firmarXml(xmlGenerado);
        } catch (Exception e) {
            logDAO.guardar("NotaCreditoService", "firmarXml", e.getMessage(), e);
            throw new Exception("Error al firmar XML: " + e.getMessage(), e);
        }

        NotaCreditoRegistro nc = new NotaCreditoRegistro();
        nc.setClaveAcceso(claveAcceso);
        nc.setFacturaRegistroId(facturaRegistroId);
        nc.setEstablecimiento(codEstab);
        nc.setPuntoEmision(codPtoEmi);
        nc.setSecuencial(secuencialStr);
        nc.setFechaEmision(ahora);
        nc.setClienteId(cliente.getId());
        nc.setMotivo(motivo);
        nc.setTipoMotivo(tipoMotivo);
        nc.setTotalSinImpuestos(subSinIva);
        nc.setValorIva(iva);
        nc.setValorModificacion(valorModificacion);
        nc.setReingresaStock(reingresaStock);
        nc.setEstadoSri(AppConstants.ESTADO_PENDIENTE);
        nc.setXmlFirmado(xmlFirmado);
        nc.setUsuarioId(usuarioId);
        int ncId = notaCreditoDAO.insertar(nc);
        if (ncId == -1) throw new IllegalStateException("Error al registrar la nota de credito.");

        notaCreditoDetalleDAO.insertarDetalles(ncId, detallesParaDao);
        comprobanteDAO.insertar(claveAcceso, ncId, numComprobante, ambienteSri, xmlFirmado, AppConstants.TIPO_COMPROBANTE_NOTA_CREDITO);

        if (reingresaStock) {
            for (NotaCreditoDetalle d : detallesParaDao) {
                if (d.getInventarioId() != null) {
                    inventarioDAO.devolverStock(d.getInventarioId(), d.getCantidad());
                }
            }
            List<HistorialProducto> hist = new ArrayList<>();
            for (NotaCreditoDetalle d : detallesParaDao) {
                if (d.getInventarioId() != null) {
                    String provNombre = inventarioDAO.obtenerProveedorNombre(d.getInventarioId());
                    hist.add(new HistorialProducto(d.getInventarioId(), d.getInventarioId().toString(), d.getDescripcion(), d.getCantidad().intValue(), d.getPrecioUnitario(), "DEVOLUCION_NC", numComprobante, cliente.getNombre(), provNombre, ahora));
                }
            }
            if (!hist.isEmpty()) historialProductoDAO.insertar(hist);
        }

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_NOTA_CREDITO + numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
        String rutaXML = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_NOTA_CREDITO + numComprobante.replace("-", "") + AppConstants.EXTENSION_XML;
        try { Files.write(Paths.get(rutaXML), xmlFirmado.getBytes(StandardCharsets.UTF_8)); } catch (IOException e) { logDAO.guardar("NotaCreditoService","guardarXML", e.getMessage(), e); }

        return new ResultadoNotaCredito(claveAcceso, numComprobante, xmlFirmado, ambienteSri, codEstab, codPtoEmi, secuencialNC, fechaEmision, fechaEmisionDocSustento, numDocModificado, tipoIdComp, rutaPDF, rutaXML, subSinIva, iva, valorModificacion, motivo, tipoMotivo, reingresaStock, detallesParaXml, cliente, empresa, ncId);
    }

    public SRIWebService.SRIResponse enviarYSolicitarAutorizacion(String ambienteSri, String xmlFirmado, String claveAcceso) {
        try {
            SRIWebService sriWs = new SRIWebService(ambienteSri);
            return sriWs.validarComprobante(xmlFirmado, claveAcceso);
        } catch (Exception e) {
            logDAO.guardar("NotaCreditoService", "validarSRI", e.getMessage(), e);
            SRIWebService.SRIResponse r = new SRIWebService.SRIResponse();
            r.setEstado(AppConstants.ESTADO_PENDIENTE);
            r.setMensaje("Error de conexion: " + e.getMessage());
            return r;
        }
    }

    public void finalizarEnvioSRI(SRIWebService.SRIResponse sriResp, ResultadoNotaCredito resultado, File directorioEscritorio) {
        String estado = sriResp.getEstado();
        String numAut = sriResp.getNumeroAutorizacion();
        String fechaAut = sriResp.getFechaAutorizacion();
        notaCreditoDAO.actualizarEstado(resultado.claveAcceso, estado, sriResp.getMensaje(), numAut, fechaAut);
        comprobanteDAO.actualizarEstado(resultado.claveAcceso, estado, sriResp.getMensaje(), resultado.xmlFirmado, numAut, fechaAut);
        comprobanteDAO.guardarEnvio(resultado.claveAcceso, resultado.numComprobante, resultado.ambienteSri, resultado.xmlFirmado, sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(), estado, sriResp.getMensaje(), numAut, fechaAut, AppConstants.TIPO_COMPROBANTE_NOTA_CREDITO);

        actualizarEstadoFactura(resultado, estado);

        if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
            // Movimiento EGRESO caja si hay sesion abierta
            try {
                CajaSesion sesionAbierta = cajaSesionDAO.obtenerAbierta();
                if (sesionAbierta != null) {
                    CajaMovimiento mov = new CajaMovimiento(sesionAbierta.getId(), "EGRESO", resultado.valorModificacion, "NC " + resultado.numComprobante + " modifica Factura " + resultado.numDocModificado + " - " + resultado.motivo, resultado.cliente != null ? sesionAbierta.getUsuarioId() : sesionAbierta.getUsuarioId());
                    mov.setReferenciaId(resultado.notaCreditoId);
                    mov.setReferenciaTipo("NOTA_CREDITO");
                    cajaMovimientoDAO.insertar(mov);
                }
            } catch (Exception e) { logDAO.guardar("NotaCreditoService","cajaMovimiento", e.getMessage(), e); }
        }

        // regenerar PDF
        try {
            String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_NOTA_CREDITO + resultado.numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
            PdfNotaCredito.generar(rutaPDF, resultado.claveAcceso, numAut, fechaAut, resultado.ambienteSri,
                    resultado.empresa.getRuc(), resultado.empresa.getRazonSocial(),
                    resultado.empresa.getDireccionCallePrincipal() + " y " + resultado.empresa.getDireccionCalleSecundaria(),
                    resultado.empresa.getTelefono(), resultado.empresa.getCorreo(),
                    resultado.codEstab, resultado.codPtoEmi, resultado.secuencial,
                    resultado.fechaEmision, resultado.fechaEmisionDocSustento, resultado.numDocModificado,
                    resultado.tipoIdComp, resultado.cliente.getNombre(), resultado.cliente.getIdentificacion(),
                    resultado.cliente.getDireccion(), resultado.cliente.getCorreo(), resultado.cliente.getTelefono(),
                    resultado.motivo, resultado.detallesParaXml,
                    resultado.subSinIva, resultado.iva, resultado.valorModificacion);
        } catch (Exception e) { logDAO.guardar("NotaCreditoService","generarPDF", e.getMessage(), e); }
    }

    private void actualizarEstadoFactura(ResultadoNotaCredito resultado, String estado) {
        if (!AppConstants.ESTADO_AUTORIZADO.equals(estado)) return;
        try (java.sql.Connection con = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(valor_modificacion),0) FROM nota_credito_registro WHERE factura_registro_id=? AND estado_sri='AUTORIZADO'")) {
            ps.setInt(1, resultado.facturaRegistroIdForEstado());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal sum = rs.getBigDecimal(1);
                    FacturaRegistro fac = obtenerFacturaPorId(resultado.facturaRegistroIdForEstado());
                    String nuevoEstadoNc = "PARCIAL";
                    if (fac != null && fac.getTotal() != null && sum.compareTo(fac.getTotal()) >= 0) nuevoEstadoNc = "ANULADA";
                    try (java.sql.PreparedStatement upd = con.prepareStatement("UPDATE factura_registro SET estado_nc=? WHERE id=?")) {
                        upd.setString(1, nuevoEstadoNc);
                        upd.setInt(2, resultado.facturaRegistroIdForEstado());
                        upd.executeUpdate();
                    }
                }
            }
        } catch (Exception e) { logDAO.guardar("NotaCreditoService","actualizarEstadoFactura", e.getMessage(), e); }
    }

    private FacturaRegistro obtenerFacturaPorId(int id) {
        String sql = "SELECT fr.*, c.nombre AS nombre_cliente, ce.mensaje_sri AS mensaje_sri, COALESCE(ce.estado_sri, fr.estado_sri) AS estado_sri_actual FROM factura_registro fr LEFT JOIN cliente c ON c.id=fr.cliente_id LEFT JOIN comprobantes_electronicos ce ON ce.clave_acceso=fr.clave_acceso WHERE fr.id=? LIMIT 1";
        try (java.sql.Connection con = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FacturaRegistro f = new FacturaRegistro();
                    f.setId(rs.getInt("id"));
                    f.setEmpresaId(rs.getInt("empresa_id"));
                    f.setClienteId(rs.getInt("cliente_id"));
                    f.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    f.setCodigo(rs.getString("codigo"));
                    f.setFormaPago(rs.getString("forma_pago"));
                    f.setSubtotal(rs.getBigDecimal("subtotal"));
                    f.setIva(rs.getBigDecimal("iva"));
                    f.setDescuento(rs.getBigDecimal("descuento"));
                    f.setTotal(rs.getBigDecimal("total"));
                    f.setClaveAcceso(rs.getString("clave_acceso"));
                    f.setNumComprobante(rs.getString("num_comprobante"));
                    f.setAmbienteSri(rs.getString("ambiente_sri"));
                    f.setEstadoSri(rs.getString("estado_sri_actual"));
                    f.setMensajeSri(rs.getString("mensaje_sri"));
                    return f;
                }
            }
        } catch (Exception e) { logDAO.guardar("NotaCreditoService","obtenerFacturaPorId", e.getMessage(), e); }
        return null;
    }

    public String regenerarRide(String claveAcceso, String numeroAutorizacion, String fechaAutorizacion, File directorioEscritorio) {
        NotaCreditoRegistro nc = notaCreditoDAO.obtenerPorClave(claveAcceso);
        if (nc == null) { logDAO.guardar("NotaCreditoService","regenerarRide","NC no encontrada "+claveAcceso); return null; }
        Cliente cliente = clienteDAO.obtenerPorId(nc.getClienteId());
        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (cliente == null || empresa == null) return null;
        FacturaRegistro factura = obtenerFacturaPorId(nc.getFacturaRegistroId());
        String numDocMod = factura != null ? factura.getNumComprobante() : "";
        String fechaDocSust = factura != null && factura.getFecha()!=null ? factura.getFecha().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";
        String tipoIdComp = AppConstants.esConsumidorFinal(cliente.getIdentificacion()!=null?cliente.getIdentificacion().trim():"")?AppConstants.TIPO_ID_CONSUMIDOR_FINAL: (cliente.getIdentificacion()!=null&&cliente.getIdentificacion().trim().length()==13?"04":"05");
        String fechaEmision = nc.getFechaEmision()!=null? nc.getFechaEmision().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";
        String[] partes = nc.getSecuencial()!=null? new String[]{nc.getEstablecimiento(), nc.getPuntoEmision(), nc.getSecuencial()} : (nc.getNumComprobante()!=null? nc.getNumComprobante().split("-"): new String[]{"001","001","000000000"});
        String codEstab = partes.length>0?partes[0]:"001"; String codPtoEmi = partes.length>1?partes[1]:"001"; int sec = partes.length>2? Integer.parseInt(partes[2].replaceAll("\\D","")):0;
        List<NotaCreditoDetalle> dets = notaCreditoDetalleDAO.listarPorNotaCreditoId(nc.getId());
        List<Object[]> detallesXml = new ArrayList<>();
        for (NotaCreditoDetalle d: dets) detallesXml.add(new Object[]{d.getInventarioId()!=null? String.valueOf(d.getInventarioId()):"NC", d.getDescripcion(), d.getCantidad().toPlainString(), d.getPrecioUnitario().setScale(2,RoundingMode.HALF_UP).toPlainString(), d.getDescuento()!=null?d.getDescuento().toPlainString():"0.00", d.getPrecioTotalSinImpuesto().toPlainString()});
        String rutaPDF = directorioEscritorio.getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_NOTA_CREDITO+nc.getNumComprobante().replace("-","")+AppConstants.EXTENSION_PDF;
        PdfNotaCredito.generar(rutaPDF, claveAcceso, numeroAutorizacion, fechaAutorizacion, nc.getEstadoSri()!=null? "PRUEBAS": "PRUEBAS",
                empresa.getRuc(), empresa.getRazonSocial(), empresa.getDireccionCallePrincipal()+" y "+empresa.getDireccionCalleSecundaria(),
                empresa.getTelefono(), empresa.getCorreo(),
                codEstab, codPtoEmi, sec, fechaEmision, fechaDocSust, numDocMod,
                tipoIdComp, cliente.getNombre(), cliente.getIdentificacion(), cliente.getDireccion(), cliente.getCorreo(), cliente.getTelefono(),
                nc.getMotivo(), detallesXml,
                nc.getTotalSinImpuestos()!=null?nc.getTotalSinImpuestos():BigDecimal.ZERO,
                nc.getValorIva()!=null?nc.getValorIva():BigDecimal.ZERO,
                nc.getValorModificacion()!=null?nc.getValorModificacion():BigDecimal.ZERO);
        return rutaPDF;
    }

    public boolean enviarCorreoAutorizacion(String destinatario, String nombreCliente, String codigo, String rutaPDF, String rutaXML) {
        if (destinatario==null||destinatario.trim().isEmpty()) return false;
        try {
            EmailService emailService = new EmailService();
            return emailService.enviarCorreoConArchivos(destinatario.trim(), nombreCliente, codigo, AppConstants.TIPO_DOCUMENTO_NOTA_CREDITO, new File(rutaPDF), new File(rutaXML));
        } catch (Exception e) { logDAO.guardar("NotaCreditoService","enviarCorreo","Error enviando correo a "+destinatario+": "+e.getMessage(), e); return false; }
    }

    public static class ResultadoNotaCredito {
        public final String claveAcceso; public final String numComprobante; public final String xmlFirmado; public final String ambienteSri;
        public final String codEstab; public final String codPtoEmi; public final int secuencial; public final String fechaEmision; public final String fechaEmisionDocSustento; public final String numDocModificado;
        public final String tipoIdComp; public final String rutaPDF; public final String rutaXML;
        public final BigDecimal subSinIva; public final BigDecimal iva; public final BigDecimal valorModificacion;
        public final String motivo; public final String tipoMotivo; public final boolean reingresaStock;
        public final List<Object[]> detallesParaXml; public final Cliente cliente; public final Empresa empresa; public final int notaCreditoId;
        private final int facturaRegistroId;
        public ResultadoNotaCredito(String claveAcceso, String numComprobante, String xmlFirmado, String ambienteSri, String codEstab, String codPtoEmi, int secuencial, String fechaEmision, String fechaEmisionDocSustento, String numDocModificado, String tipoIdComp, String rutaPDF, String rutaXML, BigDecimal subSinIva, BigDecimal iva, BigDecimal valorModificacion, String motivo, String tipoMotivo, boolean reingresaStock, List<Object[]> detallesParaXml, Cliente cliente, Empresa empresa, int notaCreditoId) {
            this.claveAcceso=claveAcceso; this.numComprobante=numComprobante; this.xmlFirmado=xmlFirmado; this.ambienteSri=ambienteSri; this.codEstab=codEstab; this.codPtoEmi=codPtoEmi; this.secuencial=secuencial; this.fechaEmision=fechaEmision; this.fechaEmisionDocSustento=fechaEmisionDocSustento; this.numDocModificado=numDocModificado; this.tipoIdComp=tipoIdComp; this.rutaPDF=rutaPDF; this.rutaXML=rutaXML; this.subSinIva=subSinIva; this.iva=iva; this.valorModificacion=valorModificacion; this.motivo=motivo; this.tipoMotivo=tipoMotivo; this.reingresaStock=reingresaStock; this.detallesParaXml=detallesParaXml; this.cliente=cliente; this.empresa=empresa; this.notaCreditoId=notaCreditoId; this.facturaRegistroId=-1;
        }
        // helper for estado update - recuperar id via clave is messy; store via notaCreditoId lookup would be needed; instead fallback query by clave
        public int facturaRegistroIdForEstado() {
            // intento via NotaCreditoRegistro
            try (java.sql.Connection con = DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = con.prepareStatement("SELECT factura_registro_id FROM nota_credito_registro WHERE clave_acceso=?")) {
                ps.setString(1, claveAcceso);
                try (java.sql.ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            } catch (Exception ignore) {}
            return -1;
        }
    }
}

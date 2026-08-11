package com.tag.sysTagRep.service;

import com.tag.sysTagRep.dao.*;
import com.tag.sysTagRep.model.*;
import com.tag.sysTagRep.util.*;
import com.tag.sysTagRep.util.AppConstants;

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

public class FacturaService {

    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final InventarioDAO inventarioDAO;
    private final FacturaRegistroDAO facturaRegistroDAO;
    private final FacturaDetalleDAO facturaDetalleDAO;
    private final SecuenciaDocumentoDAO secuenciaDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    private final HistorialProductoDAO historialProductoDAO;
    private final LogDAO logDAO;

    public FacturaService() {
        this.empresaDAO = new EmpresaDAO();
        this.clienteDAO = new ClienteDAO();
        this.inventarioDAO = new InventarioDAO();
        this.facturaRegistroDAO = new FacturaRegistroDAO();
        this.facturaDetalleDAO = new FacturaDetalleDAO();
        this.secuenciaDAO = new SecuenciaDocumentoDAO();
        this.comprobanteDAO = new ComprobanteDAO();
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAO();
        this.historialProductoDAO = new HistorialProductoDAO();
        this.logDAO = new LogDAO();
    }

    public ResultadoFactura guardarFactura(Cliente cliente, Empresa empresa, String codigo,
                                           List<FacturaDetalle> itemsDetalle,
                                           String formaPago, Integer mesesPlazo, String interes,
                                           String ambienteSri, String rutaP12, String claveP12,
                                           File directorioEscritorio,
                                           BigDecimal descuentoPct) throws Exception {
        if (cliente == null) {
            throw new IllegalArgumentException("Debe seleccionar un cliente.");
        }
        if (itemsDetalle == null || itemsDetalle.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto.");
        }
        if (empresa == null) {
            throw new IllegalArgumentException("No se encontraron datos de la empresa.");
        }

        int clienteId = cliente.getId();
        int empresaId = empresa.getId();
        LocalDateTime ahora = LocalDateTime.now();

        BigDecimal sub = itemsDetalle.stream()
                .map(FacturaDetalle::getPrecioTotal)
                .reduce(AppConstants.CERO, BigDecimal::add);
        BigDecimal ivaCalc = sub.multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBrutoCalc = sub.add(ivaCalc);
        BigDecimal descCalc = calcularDescuento(totalBrutoCalc, descuentoPct);
        BigDecimal totCalc = totalBrutoCalc.subtract(descCalc).setScale(2, RoundingMode.HALF_UP);

        String codEstab = AppConstants.ESTABLECIMIENTO_DEFAULT;
        String codPtoEmi = AppConstants.PUNTO_EMISION_DEFAULT;
        int secuencialFE = comprobanteDAO.obtenerSecuencial(AppConstants.TIPO_COMPROBANTE_FACTURA);
        String claveAcceso = ClaveAcceso.generar(
                AppConstants.TIPO_COMPROBANTE_FACTURA,
                empresa.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialFE
        );
        String fechaEmisionFE = ahora.format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + String.format(AppConstants.PATRON_NUMERO_COMPROBANTE, secuencialFE);

        FacturaRegistro fr = new FacturaRegistro(
                empresaId, clienteId, ahora, codigo, formaPago,
                sub, ivaCalc, descCalc, totCalc,
                claveAcceso, numComprobante, ambienteSri
        );
        fr.setEstadoSri(AppConstants.ESTADO_PENDIENTE);
        int facturaId = facturaRegistroDAO.insertar(fr);

        if (facturaId == -1) {
            throw new IllegalStateException("Error al registrar la factura.");
        }

        secuenciaDAO.marcarUsado("FACTURA");

        List<FacturaDetalle> detallesDb = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            FacturaDetalle fd = new FacturaDetalle(
                    d.getInventarioId(), d.getCodigo(), d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario()
            );
            fd.setFacturaRegistroId(facturaId);
            detallesDb.add(fd);
        }
        facturaDetalleDAO.insertarDetalle(facturaId, detallesDb);

        for (FacturaDetalle d : itemsDetalle) {
            inventarioDAO.descontarStock(d.getInventarioId(), d.getCantidad());
        }

        String clienteNombre = cliente.getNombre();
        List<HistorialProducto> historial = new ArrayList<>();
        for (FacturaDetalle d : itemsDetalle) {
            String provNombre = inventarioDAO.obtenerProveedorNombre(d.getInventarioId());
            historial.add(new HistorialProducto(
                    d.getInventarioId(), d.getCodigo(), d.getDescripcion(),
                    d.getCantidad(), d.getPrecioUnitario(), "FACTURA", codigo,
                    clienteNombre, provNombre, ahora
            ));
        }
        historialProductoDAO.insertar(historial);

        if (AppConstants.FORMA_PAGO_CREDITO.equals(formaPago) && mesesPlazo != null && interes != null) {
            int dias = mesesPlazo;
            BigDecimal tasaInteres = new BigDecimal(interes).divide(AppConstants.CIEN);
            BigDecimal totalConInteres = totCalc.multiply(BigDecimal.ONE.add(tasaInteres)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cuotaMensual = totalConInteres.divide(new BigDecimal(dias), 2, RoundingMode.HALF_UP);

            CuentaPorCobrar cpc = new CuentaPorCobrar(
                    facturaId, clienteId, totCalc, dias,
                    new BigDecimal(interes), cuotaMensual
            );
            cuentaPorCobrarDAO.insertar(cpc);
        }

        String tipoIdComp = cliente.getIdentificacion().length() == AppConstants.MAX_LONGITUD_IDENTIFICACION_JURIDICA ? "05" : "04";

        String xmlGenerado = XmlSriBuilder.construirFactura(
                ambienteSri, claveAcceso, empresa.getRuc(), empresa.getRazonSocial(),
                codEstab, codPtoEmi, secuencialFE,
                empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria(),
                "", "NO",
                tipoIdComp, cliente.getNombre(), cliente.getIdentificacion(),
                cliente.getDireccion(),
                sub.setScale(2, RoundingMode.HALF_UP).toString(), descCalc.setScale(2, RoundingMode.HALF_UP).toString(),
                ivaCalc.setScale(2, RoundingMode.HALF_UP).toString(), totCalc.setScale(2, RoundingMode.HALF_UP).toString(),
                "0.00", formaPago, fechaEmisionFE, armarDetalles(itemsDetalle, descCalc)
        );

        String xmlFirmado = xmlGenerado;
        boolean firmaOk = false;
        if (rutaP12 == null || rutaP12.trim().isEmpty() || claveP12 == null || claveP12.trim().isEmpty()) {
            throw new IllegalStateException("No se configuró la firma electrónica (.p12 y contraseña).");
        } else {
            try {
                FirmaDigital firma = new FirmaDigital();
                if (!firma.cargarCertificado(rutaP12, claveP12)) {
                    throw new IllegalStateException("No se pudo cargar el certificado. Verifique la ruta y la contraseña.");
                }
                xmlFirmado = firma.firmarXml(xmlGenerado);
                firmaOk = true;
            } catch (Exception e) {
                logDAO.guardar("FacturaService", "firmarXml", e.getMessage(), e);
                throw new Exception("Error al firmar el XML: " + e.getMessage(), e);
            }
        }

        comprobanteDAO.insertar(claveAcceso, null, numComprobante, ambienteSri, xmlFirmado);

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator
                + AppConstants.PREFIJO_PDF_FACTURA + numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
        String rutaXML = directorioEscritorio.getAbsolutePath() + File.separator
                + AppConstants.PREFIJO_PDF_FACTURA + numComprobante.replace("-", "") + AppConstants.EXTENSION_XML;

        try {
            Files.write(Paths.get(rutaXML), xmlFirmado.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logDAO.guardar("FacturaService", "guardarXML", e.getMessage(), e);
        }

        return new ResultadoFactura(
                claveAcceso, numComprobante, xmlFirmado, ambienteSri,
                codEstab, codPtoEmi, secuencialFE, fechaEmisionFE, tipoIdComp,
                rutaPDF, rutaXML, firmaOk, sub, ivaCalc, descCalc, totCalc,
                itemsDetalle, cliente, empresa, formaPago
        );
    }

    public SRIWebService.SRIResponse enviarYSolicitarAutorizacion(String ambienteSri, String xmlFirmado, String claveAcceso) {
        try {
            SRIWebService sriWs = new SRIWebService(ambienteSri);
            return sriWs.validarComprobante(xmlFirmado, claveAcceso);
        } catch (Exception e) {
            logDAO.guardar("FacturaService", "validarSRI", e.getMessage(), e);
            SRIWebService.SRIResponse r = new SRIWebService.SRIResponse();
            r.setEstado(AppConstants.ESTADO_PENDIENTE);
            r.setMensaje("Error de conexión: " + e.getMessage());
            return r;
        }
    }

    public void finalizarEnvioSRI(SRIWebService.SRIResponse sriResp, ResultadoFactura resultado,
                                  File directorioEscritorio) {
        String estadoSri = sriResp.getEstado();
        String numeroAutorizacion = sriResp.getNumeroAutorizacion();
        String fechaAutorizacion = sriResp.getFechaAutorizacion();

        if (AppConstants.ESTADO_AUTORIZADO.equals(estadoSri)) {
            comprobanteDAO.actualizarEstado(resultado.claveAcceso, AppConstants.ESTADO_AUTORIZADO,
                    sriResp.getMensaje(), resultado.xmlFirmado, numeroAutorizacion, fechaAutorizacion);
            facturaRegistroDAO.actualizarEstado(resultado.claveAcceso, AppConstants.ESTADO_AUTORIZADO);
        } else if (AppConstants.ESTADO_RECHAZADA.equals(estadoSri) || AppConstants.ESTADO_DEVUELTA.equals(estadoSri)) {
            comprobanteDAO.actualizarEstado(resultado.claveAcceso, estadoSri, sriResp.getMensaje(),
                    resultado.xmlFirmado, numeroAutorizacion, null);
            facturaRegistroDAO.actualizarEstado(resultado.claveAcceso, estadoSri);
        } else {
            comprobanteDAO.actualizarEstado(resultado.claveAcceso, estadoSri, sriResp.getMensaje(),
                    resultado.xmlFirmado, numeroAutorizacion, fechaAutorizacion);
        }

        comprobanteDAO.guardarEnvio(resultado.claveAcceso, resultado.numComprobante, resultado.ambienteSri, resultado.xmlFirmado,
                sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(),
                estadoSri, sriResp.getMensaje(), numeroAutorizacion, fechaAutorizacion);

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator
                + AppConstants.PREFIJO_PDF_FACTURA + resultado.numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;

        PdfElectronico.generar(rutaPDF, resultado.claveAcceso, numeroAutorizacion, fechaAutorizacion, resultado.ambienteSri,
                resultado.empresa.getRuc(), resultado.empresa.getRazonSocial(),
                resultado.empresa.getDireccionCallePrincipal() + " y " + resultado.empresa.getDireccionCalleSecundaria(),
                resultado.empresa.getTelefono(), resultado.empresa.getCorreo(),
                "", "NO", resultado.empresa.getSucursal(),
                resultado.empresa.getAgenteRetencion(), resultado.empresa.getResolucion(),
                resultado.codEstab, resultado.codPtoEmi, resultado.secuencialFE,
                resultado.fechaEmisionFE, resultado.tipoIdComp,
                resultado.cliente.getNombre(), resultado.cliente.getIdentificacion(),
                resultado.cliente.getDireccion(), resultado.cliente.getCorreo(), resultado.cliente.getTelefono(),
                resultado.formaPago,
                armarDetalles(resultado.itemsDetalle, resultado.descCalc),
                resultado.sub, resultado.descCalc, resultado.ivaCalc, resultado.totCalc);
    }

    public boolean enviarCorreoAutorizacion(String destinatario, String nombreCliente, String codigo,
                                            String rutaPDF, String rutaXML) {
        if (destinatario == null || destinatario.trim().isEmpty()) return false;
        try {
            EmailService emailService = new EmailService();
            return emailService.enviarCorreoConArchivos(
                    destinatario.trim(), nombreCliente, codigo,
                    AppConstants.TIPO_DOCUMENTO_FACTURA,
                    new File(rutaPDF), new File(rutaXML)
            );
        } catch (Exception e) {
            logDAO.guardar("FacturaService", "enviarCorreo", "Error enviando correo a " + destinatario + ": " + e.getMessage(), e);
            return false;
        }
    }

    private BigDecimal calcularDescuento(BigDecimal totalBruto, BigDecimal descuentoPct) {
        if (descuentoPct == null) descuentoPct = AppConstants.CERO;
        return totalBruto.multiply(descuentoPct).divide(AppConstants.CIEN, 2, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> distribuirDescuento(BigDecimal descuentoTotal, List<BigDecimal> bases) {
        List<BigDecimal> resultado = new ArrayList<>();
        for (BigDecimal b : bases) resultado.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        if (descuentoTotal == null || descuentoTotal.signum() == 0 || bases.isEmpty()) {
            return resultado;
        }
        BigDecimal sumaBases = BigDecimal.ZERO;
        for (BigDecimal b : bases) sumaBases = sumaBases.add(b);
        if (sumaBases.signum() == 0) return resultado;

        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < bases.size(); i++) {
            if (i == bases.size() - 1) {
                resultado.set(i, descuentoTotal.subtract(acumulado).setScale(2, RoundingMode.HALF_UP));
            } else {
                BigDecimal d = descuentoTotal.multiply(bases.get(i)).divide(sumaBases, 2, RoundingMode.HALF_UP);
                acumulado = acumulado.add(d);
                resultado.set(i, d);
            }
        }
        return resultado;
    }

    private List<Object[]> armarDetalles(List<FacturaDetalle> items, BigDecimal descuentoTotal) {
        List<BigDecimal> bases = new ArrayList<>();
        for (FacturaDetalle d : items) {
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            bases.add(precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP));
        }
        List<BigDecimal> descLinea = distribuirDescuento(descuentoTotal, bases);

        List<Object[]> resultado = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            FacturaDetalle d = items.get(i);
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            BigDecimal totalDetSinIva = precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            String desc = d.getDescripcion();
            if (desc.length() > AppConstants.MAX_DESCRIPCION_XML) desc = desc.substring(0, AppConstants.MAX_DESCRIPCION_XML);
            resultado.add(new Object[]{
                    d.getCodigo(), desc, String.valueOf(d.getCantidad()),
                    precioSinIva.setScale(2, RoundingMode.HALF_UP).toString(),
                    descLinea.get(i).toString(),
                    totalDetSinIva.toString()
            });
        }
        return resultado;
    }

    public static class ResultadoFactura {
        public final String claveAcceso;
        public final String numComprobante;
        public final String xmlFirmado;
        public final String ambienteSri;
        public final String codEstab;
        public final String codPtoEmi;
        public final int secuencialFE;
        public final String fechaEmisionFE;
        public final String tipoIdComp;
        public final String rutaPDF;
        public final String rutaXML;
        public final boolean firmaOk;
        public final BigDecimal sub;
        public final BigDecimal ivaCalc;
        public final BigDecimal descCalc;
        public final BigDecimal totCalc;
        public final List<FacturaDetalle> itemsDetalle;
        public final Cliente cliente;
        public final Empresa empresa;
        public final String formaPago;

        public ResultadoFactura(String claveAcceso, String numComprobante, String xmlFirmado,
                                String ambienteSri, String codEstab, String codPtoEmi,
                                int secuencialFE, String fechaEmisionFE, String tipoIdComp,
                                String rutaPDF, String rutaXML, boolean firmaOk,
                                BigDecimal sub, BigDecimal ivaCalc, BigDecimal descCalc, BigDecimal totCalc,
                                List<FacturaDetalle> itemsDetalle, Cliente cliente, Empresa empresa, String formaPago) {
            this.claveAcceso = claveAcceso;
            this.numComprobante = numComprobante;
            this.xmlFirmado = xmlFirmado;
            this.ambienteSri = ambienteSri;
            this.codEstab = codEstab;
            this.codPtoEmi = codPtoEmi;
            this.secuencialFE = secuencialFE;
            this.fechaEmisionFE = fechaEmisionFE;
            this.tipoIdComp = tipoIdComp;
            this.rutaPDF = rutaPDF;
            this.rutaXML = rutaXML;
            this.firmaOk = firmaOk;
            this.sub = sub;
            this.ivaCalc = ivaCalc;
            this.descCalc = descCalc;
            this.totCalc = totCalc;
            this.itemsDetalle = itemsDetalle;
            this.cliente = cliente;
            this.empresa = empresa;
            this.formaPago = formaPago;
        }
    }
}

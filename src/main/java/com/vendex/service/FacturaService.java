package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.config.DatabaseConnection;
import com.vendex.util.*;
import com.vendex.util.AppConstants;
import java.sql.Connection;
import java.sql.SQLException;

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
    private AlertaDAO alertaDAO;

    public FacturaService() {
        this.logDAO = new LogDAOPostgres();
        this.empresaDAO = new EmpresaDAOPostgres();
        this.clienteDAO = new ClienteDAOPostgres();
        this.inventarioDAO = new InventarioDAOPostgres();
        this.facturaRegistroDAO = new FacturaRegistroDAOPostgres();
        this.facturaDetalleDAO = new FacturaDetalleDAOPostgres();
        this.secuenciaDAO = new SecuenciaDocumentoDAOPostgres();
        this.comprobanteDAO = new ComprobanteDAOPostgres();
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOPostgres();
        this.historialProductoDAO = new HistorialProductoDAOPostgres();
    }
    public FacturaService(EmpresaDAO empresaDAO, ClienteDAO clienteDAO, InventarioDAO inventarioDAO, FacturaRegistroDAO facturaRegistroDAO, FacturaDetalleDAO facturaDetalleDAO, SecuenciaDocumentoDAO secuenciaDAO, ComprobanteDAO comprobanteDAO, CuentaPorCobrarDAO cuentaPorCobrarDAO, HistorialProductoDAO historialProductoDAO, LogDAO logDAO) {
        this.logDAO = logDAO;
        this.empresaDAO = empresaDAO;
        this.clienteDAO = clienteDAO;
        this.inventarioDAO = inventarioDAO;
        this.facturaRegistroDAO = facturaRegistroDAO;
        this.facturaDetalleDAO = facturaDetalleDAO;
        this.secuenciaDAO = secuenciaDAO;
        this.comprobanteDAO = comprobanteDAO;
        this.cuentaPorCobrarDAO = cuentaPorCobrarDAO;
        this.historialProductoDAO = historialProductoDAO;
    }

    /** Alerta inmediata de stock negativo al facturar (opcional; si es null se omite). */
    public void setAlertaDAO(AlertaDAO alertaDAO) {
        this.alertaDAO = alertaDAO;
    }


    public ResultadoFactura guardarFactura(Cliente cliente, Empresa empresa, String codigo,
                                           List<FacturaDetalle> itemsDetalle,
                                           String formaPago, Integer mesesPlazo, String interes,
                                           String ambienteSri, String rutaP12, String claveP12,
                                           File directorioEscritorio,
                                           BigDecimal descuentoValor) throws Exception {
        com.vendex.util.SesionActual.exigirPermiso("FACTURA_EMITIR");
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

        BigDecimal subBruto = itemsDetalle.stream()
                .map(FacturaDetalle::getPrecioTotal)
                .reduce(AppConstants.CERO, BigDecimal::add);
        BigDecimal totalBrutoCalc = subBruto.add(subBruto.multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP));
        // CONVENCION C (SRI): precios con IVA incluido; el descuento vive SOLO en
        // cabecera (totalDescuento + descuentoAdicional). Las lineas van en bruto
        // (descuento 0.00, neto = base) y el header es neto:
        //   subtotal = S bases sin IVA; base = S netos (= S bases); iva = 15% x base;
        //   total = base - descuento + iva.
        // Asi se cumplen las 3 reglas SRI a la vez:
        //   R1 sub == S netos; R2 desc == S desc lineas + adicional; T total == sub - adic + iva.
        // Descuento en VALOR FIJO ($), tope = suma de bases sin IVA.
        BigDecimal sumaBasesSinIva = sumarBasesSinIva(itemsDetalle);
        if (descuentoValor != null && descuentoValor.compareTo(BigDecimal.ZERO) > 0) {
            com.vendex.util.SesionActual.exigirDescuento(descuentoValor, totalBrutoCalc);
        }
        BigDecimal descCalc = calcularDescuento(sumaBasesSinIva, descuentoValor);
        final List<Object[]> filasDetalleSri = armarDetalles(itemsDetalle);
        BigDecimal baseNeta = filasDetalleSri.stream()
                .map(f -> new BigDecimal(f[5].toString().trim()))
                .reduce(AppConstants.CERO, BigDecimal::add);
        BigDecimal sub = baseNeta;
        // Regla SRI ImpuestoTotalizado: valor = tarifa * (base - descuentoAdicional).
        BigDecimal ivaCalc = baseNeta.subtract(descCalc).multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totCalc = sub.subtract(descCalc).add(ivaCalc).setScale(2, RoundingMode.HALF_UP);

        // Pre-validacion SRI ANTES de consumir secuencial/BD: espejo exacto del validador
        // (R1 sub==S netos, R2 desc==S lineas+adic, linea neta, iva==base*15%,
        // T importe==sub-adic+iva). Si algo no cuadra se aborta aqui con mensaje humano,
        // sin gastar numero, sin escribir BD y sin encolar reintentos fantasma.
        validarConsistenciaSri(filasDetalleSri, sub, descCalc, descCalc, ivaCalc, totCalc);
        validarNoNegativos(filasDetalleSri, totCalc);

        // Validacion SRI: consumidor final (9999999999999 / 9999999999 tipo 07) no puede exceder $50
        if (AppConstants.esConsumidorFinal(cliente.getIdentificacion()) && totCalc.compareTo(AppConstants.LIMITE_CONSUMIDOR_FINAL) > 0) {
            throw new IllegalArgumentException("Factura a Consumidor Final (9999999999999 / tipo 07) no puede exceder $50.00. Total: $" + totCalc + ". Use identificación válida (cédula/RUC/pasaporte).");
        }

        // Transacción atómica: secuencial + factura + detalles + stock + historial + cpc + comprobante
        // Usa una sola Connection del pool HikariCP con autoCommit=false
        int secuencialFE;
        String codEstab;
        String codPtoEmi;
        String claveAcceso;
        String fechaEmisionFE;
        String numComprobante;
        int facturaId;
        // Variables para firma/comprobante (fuera de la transacción inicial, pero comprobante insertado dentro)
        String xmlGenerado;
        String xmlFirmado;
        boolean firmaOk;

        // Pre-cálculo de tipoIdComp (no necesita BD)
        String tipoIdCompTmp;
        {
            String identTrimTmp = cliente.getIdentificacion() != null ? cliente.getIdentificacion().trim() : "";
            if (AppConstants.esConsumidorFinal(identTrimTmp)) tipoIdCompTmp = AppConstants.TIPO_ID_CONSUMIDOR_FINAL;
            else tipoIdCompTmp = identTrimTmp.length() == AppConstants.MAX_LONGITUD_IDENTIFICACION_JURIDICA ? "04" : "05";
        }

        // Construir XML antes de abrir transacción (no necesita BD)
        // Pero necesitamos secuencial/codEstab/codPtoEmi que vienen de la BD, así que primero abrimos tx para obtenerlos
        // y luego generamos XML/firma dentro de la misma tx antes del commit

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                secuencialFE = secuenciaDAO.marcarUsado(con, com.vendex.util.SesionActual.getPuntoEmisionId(), "FACTURA");
                if (secuencialFE == -1) throw new IllegalStateException("No se pudo obtener el secuencial de FACTURA (secuencia_documento).");
                SecuenciaDocumento secActual = secuenciaDAO.obtener(con, com.vendex.util.SesionActual.getPuntoEmisionId(), "FACTURA");
                codEstab = secActual.getEstablecimiento() != null ? secActual.getEstablecimiento() : AppConstants.ESTABLECIMIENTO_DEFAULT;
                codPtoEmi = secActual.getPuntoEmision() != null ? secActual.getPuntoEmision() : AppConstants.PUNTO_EMISION_DEFAULT;
                claveAcceso = ClaveAcceso.generar(AppConstants.TIPO_COMPROBANTE_FACTURA, empresa.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialFE);
                fechaEmisionFE = ahora.format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
                numComprobante = codEstab + "-" + codPtoEmi + "-" + String.format(AppConstants.PATRON_NUMERO_COMPROBANTE, secuencialFE);

                FacturaRegistro fr = new FacturaRegistro(empresaId, clienteId, ahora, codigo, formaPago, sub, ivaCalc, descCalc, totCalc, claveAcceso, numComprobante, ambienteSri);
                fr.setEstadoSri(AppConstants.ESTADO_PENDIENTE);
                fr.setSucursalId(com.vendex.util.SucursalActual.getId());
                facturaId = facturaRegistroDAO.insertar(con, fr);
                if (facturaId == -1) throw new IllegalStateException("Error al registrar la factura.");

                List<FacturaDetalle> detallesDb = new ArrayList<>();
                for (FacturaDetalle d : itemsDetalle) {
                    FacturaDetalle fd = new FacturaDetalle(d.getInventarioId(), d.getCodigo(), d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario());
                    fd.setFacturaRegistroId(facturaId);
                    detallesDb.add(fd);
                }
                facturaDetalleDAO.insertarDetalle(con, facturaId, detallesDb);
                for (FacturaDetalle d : itemsDetalle) {
                    inventarioDAO.descontarStock(con, d.getInventarioId(), d.getCantidad());
                }
                registrarAlertasStockNegativo(con, facturaId, numComprobante, itemsDetalle);
                String clienteNombre = cliente.getNombre();
                List<HistorialProducto> historial = new ArrayList<>();
                for (FacturaDetalle d : itemsDetalle) {
                    String provNombre = inventarioDAO.obtenerProveedorNombre(con, d.getInventarioId());
                    historial.add(new HistorialProducto(d.getInventarioId(), d.getCodigo(), d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario(), "FACTURA", codigo, clienteNombre, provNombre, ahora));
                }
                historialProductoDAO.insertar(con, historial);
                if (AppConstants.FORMA_PAGO_CREDITO.equals(formaPago) && mesesPlazo != null && interes != null) {
                    int dias = mesesPlazo;
                    BigDecimal tasaInteres = new BigDecimal(interes).divide(AppConstants.CIEN);
                    BigDecimal totalConInteres = totCalc.multiply(BigDecimal.ONE.add(tasaInteres)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal cuotaMensual = totalConInteres.divide(new BigDecimal(dias), 2, RoundingMode.HALF_UP);
                    CuentaPorCobrar cpc = new CuentaPorCobrar(0, clienteId, totCalc, dias, new BigDecimal(interes), cuotaMensual);
                    cpc.setNotaVentaId(null);
                    cpc.setFacturaRegistroId(facturaId);
                    cuentaPorCobrarDAO.insertar(con, cpc);
                }

                // Generar y firmar XML dentro de la transacción para poder hacer rollback si la firma falla
                if (rutaP12 == null || rutaP12.trim().isEmpty() || claveP12 == null || claveP12.trim().isEmpty()) {
                    throw new IllegalStateException("No se configuró la firma electrónica (.p12 y contraseña).");
                }
                String tipoIdComp = tipoIdCompTmp;
                xmlGenerado = XmlSriBuilder.construirFactura(ambienteSri, claveAcceso, empresa.getRuc(), empresa.getRazonSocial(), codEstab, codPtoEmi, secuencialFE, com.vendex.util.SucursalActual.direccionEstablecimiento(empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria()), empresa.getAgenteRetencion(), obligadoSri(empresa), tipoIdComp, cliente.getNombre(), cliente.getIdentificacion(), cliente.getDireccion(), sub.setScale(2, RoundingMode.HALF_UP).toString(), descCalc.setScale(2, RoundingMode.HALF_UP).toString(), totCalc.setScale(2, RoundingMode.HALF_UP).toString(), "0.00", formaPago, fechaEmisionFE, filasDetalleSri);
                try {
                    FirmaDigital firma = new FirmaDigital();
                    if (!firma.cargarCertificado(rutaP12, claveP12)) throw new IllegalStateException("No se pudo cargar el certificado. Verifique la ruta y la contraseña.");
                    xmlFirmado = firma.firmarXml(xmlGenerado);
                    firmaOk = true;
                } catch (Exception e) {
                    logDAO.guardar("FacturaService", "firmarXml", e.getMessage(), e);
                    throw new Exception("Error al firmar el XML: " + e.getMessage(), e);
                }
                comprobanteDAO.insertar(con, claveAcceso, null, numComprobante, ambienteSri, xmlFirmado);
                con.commit();
            } catch (Exception e) {
                try { con.rollback(); } catch (SQLException re) { /* ignore */ }
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (SQLException ignore) {}
            }
        }

        String tipoIdComp = tipoIdCompTmp;

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
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (AppConstants.ESTADO_AUTORIZADO.equals(estadoSri)) {
                    comprobanteDAO.actualizarEstado(con, resultado.claveAcceso, AppConstants.ESTADO_AUTORIZADO, sriResp.getMensaje(), resultado.xmlFirmado, numeroAutorizacion, fechaAutorizacion);
                    facturaRegistroDAO.actualizarEstado(con, resultado.claveAcceso, AppConstants.ESTADO_AUTORIZADO);
                } else if (AppConstants.ESTADO_RECHAZADA.equals(estadoSri) || AppConstants.ESTADO_DEVUELTA.equals(estadoSri)) {
                    comprobanteDAO.actualizarEstado(con, resultado.claveAcceso, estadoSri, sriResp.getMensaje(), resultado.xmlFirmado, numeroAutorizacion, null);
                    facturaRegistroDAO.actualizarEstado(con, resultado.claveAcceso, estadoSri);
                } else {
                    comprobanteDAO.actualizarEstado(con, resultado.claveAcceso, estadoSri, sriResp.getMensaje(), resultado.xmlFirmado, numeroAutorizacion, fechaAutorizacion);
                }
                comprobanteDAO.guardarEnvio(con, resultado.claveAcceso, resultado.numComprobante, resultado.ambienteSri, resultado.xmlFirmado, sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(), estadoSri, sriResp.getMensaje(), numeroAutorizacion, fechaAutorizacion);
                con.commit();
            } catch (Exception e) {
                try { con.rollback(); } catch (java.sql.SQLException re) {}
                throw new RuntimeException(e);
            } finally { try { con.setAutoCommit(true); } catch (java.sql.SQLException ignore) {} }
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator
                + AppConstants.PREFIJO_PDF_FACTURA + resultado.numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;

        PdfElectronico.generar(rutaPDF, resultado.claveAcceso, numeroAutorizacion, fechaAutorizacion, resultado.ambienteSri,
                resultado.empresa.getRuc(), resultado.empresa.getRazonSocial(),
                com.vendex.util.SucursalActual.direccionEstablecimiento(resultado.empresa.getDireccionCallePrincipal() + " y " + resultado.empresa.getDireccionCalleSecundaria()),
                resultado.empresa.getTelefono(), resultado.empresa.getCorreo(),
                resultado.empresa.getAgenteRetencion(), obligadoSri(resultado.empresa), resultado.empresa.getSucursal(),
                resultado.empresa.getAgenteRetencion(), resultado.empresa.getResolucion(),
                resultado.codEstab, resultado.codPtoEmi, resultado.secuencialFE,
                resultado.fechaEmisionFE, resultado.tipoIdComp,
                resultado.cliente.getNombre(), resultado.cliente.getIdentificacion(),
                resultado.cliente.getDireccion(), resultado.cliente.getCorreo(), resultado.cliente.getTelefono(),
                resultado.formaPago,
                armarDetalles(resultado.itemsDetalle),
                resultado.sub, resultado.descCalc, resultado.ivaCalc, resultado.totCalc);
    }

    /**
     * Regenera el RIDE (PDF) de una factura ya registrada cuando llega su
     * autorización de forma diferida, incorporando el número y fecha de
     * autorización. Devuelve la ruta del PDF regenerado.
     */
    public String regenerarRide(String claveAcceso, String numeroAutorizacion, String fechaAutorizacion,
                                File directorioEscritorio) {
        FacturaRegistro fr = facturaRegistroDAO.obtenerPorClaveAcceso(claveAcceso);
        if (fr == null) {
            logDAO.guardar("FacturaService", "regenerarRide", "Factura no encontrada por clave " + claveAcceso);
            return null;
        }
        Cliente cliente = clienteDAO.obtenerPorId(fr.getClienteId());
        if (cliente == null) {
            logDAO.guardar("FacturaService", "regenerarRide", "Cliente no encontrado para " + claveAcceso);
            return null;
        }
        List<Empresa> empresas = empresaDAO.listar();
        Empresa empresa = empresas.isEmpty() ? null : empresas.get(0);
        if (empresa == null) {
            logDAO.guardar("FacturaService", "regenerarRide", "Empresa no encontrada para " + claveAcceso);
            return null;
        }

        String[] partesNum = fr.getNumComprobante() == null ? new String[]{"001", "001", "000000000"}
                : fr.getNumComprobante().split("-");
        String codEstab = partesNum.length > 0 ? partesNum[0] : "001";
        String codPtoEmi = partesNum.length > 1 ? partesNum[1] : "001";
        int secuencial = partesNum.length > 2 ? Integer.parseInt(partesNum[2]) : 0;

        String tipoIdComp;
        String identRide = cliente.getIdentificacion() != null ? cliente.getIdentificacion().trim() : "";
        if (AppConstants.esConsumidorFinal(identRide)) tipoIdComp = AppConstants.TIPO_ID_CONSUMIDOR_FINAL;
        else tipoIdComp = identRide.length() == AppConstants.MAX_LONGITUD_IDENTIFICACION_JURIDICA ? "04" : "05";

        String fechaEmisionFE = fr.getFecha() != null
                ? fr.getFecha().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator
                + AppConstants.PREFIJO_PDF_FACTURA + fr.getNumComprobante().replace("-", "") + AppConstants.EXTENSION_PDF;

        List<FacturaDetalle> detallesDb = facturaDetalleDAO.listarPorFacturaRegistroId(fr.getId());

        PdfElectronico.generar(rutaPDF, fr.getClaveAcceso(), numeroAutorizacion, fechaAutorizacion, fr.getAmbienteSri(),
                empresa.getRuc(), empresa.getRazonSocial(),
                com.vendex.util.SucursalActual.direccionEstablecimiento(empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria()),
                empresa.getTelefono(), empresa.getCorreo(),
                empresa.getAgenteRetencion(), obligadoSri(empresa), empresa.getSucursal(),
                empresa.getAgenteRetencion(), empresa.getResolucion(),
                codEstab, codPtoEmi, secuencial,
                fechaEmisionFE, tipoIdComp,
                cliente.getNombre(), cliente.getIdentificacion(),
                cliente.getDireccion(), cliente.getCorreo(), cliente.getTelefono(),
                fr.getFormaPago(),
                armarDetalles(detallesDb),
                fr.getSubtotal() != null ? fr.getSubtotal() : AppConstants.CERO,
                fr.getDescuento() != null ? fr.getDescuento() : AppConstants.CERO,
                fr.getIva() != null ? fr.getIva() : AppConstants.CERO,
                fr.getTotal() != null ? fr.getTotal() : AppConstants.CERO);
        return rutaPDF;
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

    /**
     * Descuento en VALOR FIJO ($): total - descuento = total neto.
     * Tope real = suma de bases sin IVA (ahi se reparte); topar contra el bruto
     * con IVA dejaba netos de linea negativos que el SRI rechaza.
     */
    private BigDecimal calcularDescuento(BigDecimal sumaBasesSinIva, BigDecimal descuentoValorFijo) {
        if (descuentoValorFijo == null) return AppConstants.CERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fijo = descuentoValorFijo.setScale(2, RoundingMode.HALF_UP);
        if (fijo.compareTo(AppConstants.CERO) < 0) fijo = AppConstants.CERO;
        BigDecimal tope = sumaBasesSinIva != null ? sumaBasesSinIva.setScale(2, RoundingMode.HALF_UP) : AppConstants.CERO;
        if (fijo.compareTo(tope) > 0) fijo = tope;
        return fijo;
    }

    /** Suma de bases sin IVA de las lineas (misma formula que armarDetalles). */
    static BigDecimal sumarBasesSinIva(List<FacturaDetalle> items) {
        BigDecimal suma = AppConstants.CERO.setScale(2, RoundingMode.HALF_UP);
        if (items == null) return suma;
        for (FacturaDetalle d : items) {
            if (d == null || d.getPrecioUnitario() == null) continue;
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            suma = suma.add(precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP));
        }
        return suma.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Red de seguridad SRI: ninguna linea ni el total pueden quedar en negativo
     * (baseImponible tiene minInclusive 0.0). Se invoca antes de consumir secuencial/BD.
     * Visible para tests.
     */
    static void validarNoNegativos(List<Object[]> filasDet, BigDecimal total) {
        for (Object[] f : filasDet) {
            BigDecimal tl;
            try {
                tl = new BigDecimal(f[5].toString().trim());
            } catch (Exception e) {
                throw new IllegalStateException("Detalle con total no parseable, revisa cantidad/precio.");
            }
            if (tl.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El descuento deja la línea " + f[0] + " en " + tl
                        + ". El total no puede quedar en negativo: reduce el descuento o revisa precios.");
            }
        }
        if (total != null && total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El total de la factura no puede quedar en negativo (" + total
                    + "). Reduce el descuento.");
        }
    }

    /** Sin uso en convencion C (lineas siempre en bruto); se conserva por compatibilidad. */
    private static List<BigDecimal> distribuirDescuento(BigDecimal descuentoTotal, List<BigDecimal> bases) {
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

    /**
     * Alerta inmediata cuando la factura deja productos en negativo
     * (venta sobre stock, pendiente de ingreso por compra).
     * No interrumpe la facturación si falla.
     */
    private void registrarAlertasStockNegativo(Connection con, int facturaId, String numComprobante,
                                               List<FacturaDetalle> items) {
        if (alertaDAO == null) return;
        for (FacturaDetalle d : items) {
            try {
                Inventario inv = inventarioDAO.obtenerPorId(con, d.getInventarioId());
                if (inv != null && inv.getCantidad() < 0) {
                    String codigo = inv.getCodigo() != null && !inv.getCodigo().isEmpty()
                            ? inv.getCodigo() : inv.getTagCodigo();
                    String msg = String.format("Stock negativo: %s (Código: %s) — Cantidad actual: %d — por factura %s, pendiente de ingreso por compra",
                            inv.getDescripcion(), codigo, inv.getCantidad(), numComprobante);
                    alertaDAO.insertar(new Alerta("STOCK_NEGATIVO", msg, facturaId, "FACTURA"));
                }
            } catch (Exception e) {
                logDAO.guardar("FacturaService", "alertaStockNegativo", e.getMessage(), e);
            }
        }
    }

    /**
     * Obligado a llevar contabilidad desde la ficha de la empresa (vista "Mi Empresa").
     * Normaliza a "SI"/"NO" (SRI solo acepta esos valores).
     */
    static String obligadoSri(com.vendex.model.Empresa empresa) {
        if (empresa != null && "SI".equalsIgnoreCase(empresa.getObligadoContabilidad())) return "SI";
        return "NO";
    }

    /**
     * Espejo exacto del validador SRI (mensajes ERROR EN DIFERENCIAS observados):
     *  R1: totalSinImpuestos == suma de netos de lineas;
     *  R2: totalDescuento == suma desc lineas + descuentoAdicional;
     *  linea: cantidad*precio-descuento == neto;
     *  iva == base*15%; T: importe == sub - adicional + iva.
     * Visible para tests. Lanza IllegalStateException descriptivo si no cuadran.
     */
    static void validarConsistenciaSri(List<Object[]> filasDet, BigDecimal subtotal,
                                       BigDecimal descHeader, BigDecimal descAdicional,
                                       BigDecimal iva, BigDecimal total) {
        BigDecimal sumaDesc = AppConstants.CERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sumaNeto = AppConstants.CERO.setScale(2, RoundingMode.HALF_UP);
        for (Object[] f : filasDet) {
            BigDecimal cant, pu, dl, tl;
            try {
                cant = new BigDecimal(f[2].toString().trim());
                pu = new BigDecimal(f[3].toString().trim());
                dl = new BigDecimal(f[4].toString().trim());
                tl = new BigDecimal(f[5].toString().trim());
            } catch (Exception e) {
                throw new IllegalStateException("Detalle con numeros no parseables, revisa cantidad/precio.");
            }
            BigDecimal esperadoLinea = cant.multiply(pu).subtract(dl).setScale(2, RoundingMode.HALF_UP);
            if (esperadoLinea.compareTo(tl.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new IllegalStateException("Linea " + f[0] + ": cantidad*precio-descuento (" + esperadoLinea
                        + ") difiere del total " + tl + ". El SRI la rechazara (ERROR EN DIFERENCIAS).");
            }
            sumaDesc = sumaDesc.add(dl);
            sumaNeto = sumaNeto.add(tl);
        }
        if (subtotal.setScale(2, RoundingMode.HALF_UP).compareTo(sumaNeto.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalStateException("totalSinImpuestos del header (" + subtotal
                    + ") difiere de la suma de netos de lineas (" + sumaNeto + "). El SRI lo rechazara (R1).");
        }
        BigDecimal descCalculado = sumaDesc.add(descAdicional != null ? descAdicional : AppConstants.CERO)
                .setScale(2, RoundingMode.HALF_UP);
        if (descCalculado.compareTo(descHeader.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalStateException("totalDescuento del header (" + descHeader
                    + ") difiere de lineas+adicional (" + sumaDesc + " + "
                    + (descAdicional != null ? descAdicional : AppConstants.CERO) + "). El SRI lo rechazara (R2).");
        }
        BigDecimal adic = descAdicional != null ? descAdicional : AppConstants.CERO;
        BigDecimal ivaEsperado = sumaNeto.subtract(adic).multiply(AppConstants.IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        if (ivaEsperado.compareTo(iva.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalStateException("IVA " + iva + " no corresponde a (base " + sumaNeto
                    + " - adicional " + adic + ") * 15% (esperado " + ivaEsperado + "). Regla SRI ImpuestoTotalizado.");
        }
        BigDecimal totalEsperado = subtotal.subtract(adic).add(iva).setScale(2, RoundingMode.HALF_UP);
        if (totalEsperado.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalStateException("Importe " + total + " no cuadra: subtotal - descuentoAdicional + IVA = "
                    + totalEsperado + ". El SRI lo rechazara (T).");
        }
    }

    /**
     * Arma filas para XML/RIDE en CONVENCION C (SRI): lineas en BRUTO
     * (descuento 0.00, neto = base sin IVA); el descuento vive SOLO en cabecera
     * (totalDescuento + descuentoAdicional). Asi se cumplen R1/R2/T a la vez.
     */
    /** Visible para tests de reglas SRI. */
    static List<Object[]> armarDetalles(List<FacturaDetalle> items) {
        List<Object[]> resultado = new ArrayList<>();
        for (FacturaDetalle d : items) {
            BigDecimal precioSinIva = d.getPrecioUnitario().divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
            BigDecimal baseDet = precioSinIva.multiply(new BigDecimal(d.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            String desc = d.getDescripcion();
            if (desc.length() > AppConstants.MAX_DESCRIPCION_XML) desc = desc.substring(0, AppConstants.MAX_DESCRIPCION_XML);
            resultado.add(new Object[]{
                    d.getCodigo(), desc, String.valueOf(d.getCantidad()),
                    precioSinIva.setScale(2, RoundingMode.HALF_UP).toString(),
                    "0.00",
                    baseDet.toString()
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

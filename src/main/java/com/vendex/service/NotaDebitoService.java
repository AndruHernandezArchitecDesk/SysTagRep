package com.vendex.service;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.*;
import com.vendex.model.*;
import com.vendex.util.*;

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

public class NotaDebitoService {

    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final FacturaRegistroDAO facturaRegistroDAO;
    private final SecuenciaDocumentoDAO secuenciaDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final NotaDebitoRegistroDAO notaDebitoDAO;
    private final NotaDebitoMotivoDAO notaDebitoMotivoDAO;
    private final CajaSesionDAO cajaSesionDAO;
    private final CajaMovimientoDAO cajaMovimientoDAO;
    private final LogDAO logDAO = new LogDAOPostgres();

    public NotaDebitoService() {
        this.empresaDAO = new EmpresaDAOPostgres();
        this.clienteDAO = new ClienteDAOPostgres();
        this.facturaRegistroDAO = new FacturaRegistroDAOPostgres();
        this.secuenciaDAO = new SecuenciaDocumentoDAOPostgres();
        this.comprobanteDAO = new ComprobanteDAOPostgres();
        this.notaDebitoDAO = new NotaDebitoRegistroDAOPostgres();
        this.notaDebitoMotivoDAO = new NotaDebitoMotivoDAOPostgres();
        this.cajaSesionDAO = new CajaSesionDAOPostgres();
        this.cajaMovimientoDAO = new CajaMovimientoDAOPostgres();
    }
    public NotaDebitoService(EmpresaDAO empresaDAO, ClienteDAO clienteDAO, FacturaRegistroDAO facturaRegistroDAO, SecuenciaDocumentoDAO secuenciaDAO, ComprobanteDAO comprobanteDAO, NotaDebitoRegistroDAO notaDebitoDAO, NotaDebitoMotivoDAO notaDebitoMotivoDAO, CajaSesionDAO cajaSesionDAO, CajaMovimientoDAO cajaMovimientoDAO) {
        this.empresaDAO = empresaDAO;
        this.clienteDAO = clienteDAO;
        this.facturaRegistroDAO = facturaRegistroDAO;
        this.secuenciaDAO = secuenciaDAO;
        this.comprobanteDAO = comprobanteDAO;
        this.notaDebitoDAO = notaDebitoDAO;
        this.notaDebitoMotivoDAO = notaDebitoMotivoDAO;
        this.cajaSesionDAO = cajaSesionDAO;
        this.cajaMovimientoDAO = cajaMovimientoDAO;
    }


    public static class MotivoNDInput {
        public String razon;
        public BigDecimal valor;
        public boolean gravaIva;
        public MotivoNDInput(String razon, BigDecimal valor) {
            this.razon = razon;
            this.valor = valor;
            this.gravaIva = true;
        }
    }

    public static class ResultadoNotaDebito {
        public final String claveAcceso;
        public final String numComprobante;
        public final String xmlFirmado;
        public final String ambienteSri;
        public final String codEstab;
        public final String codPtoEmi;
        public final int secuencial;
        public final String fechaEmision;
        public final String fechaEmisionDocSustento;
        public final String numDocModificado;
        public final String tipoIdComp;
        public final String rutaPDF;
        public final String rutaXML;
        public final BigDecimal totalSinImpuestos;
        public final BigDecimal valorIva;
        public final BigDecimal valorTotal;
        public final String motivoStr;
        public final String formaPago;
        public final Cliente cliente;
        public final Empresa empresa;
        public final int notaDebitoId;
        public final List<Object[]> detallesParaXml;
        public final int facturaRegistroId;
        public ResultadoNotaDebito(String claveAcceso, String numComprobante, String xmlFirmado, String ambienteSri,
                                    String codEstab, String codPtoEmi, int secuencial, String fechaEmision,
                                    String fechaEmisionDocSustento, String numDocModificado, String tipoIdComp,
                                    String rutaPDF, String rutaXML, BigDecimal totalSinImpuestos, BigDecimal valorIva,
                                    BigDecimal valorTotal, String motivoStr, String formaPago, Cliente cliente,
                                    Empresa empresa, int notaDebitoId, List<Object[]> detallesParaXml, int facturaRegistroId) {
            this.claveAcceso=claveAcceso; this.numComprobante=numComprobante; this.xmlFirmado=xmlFirmado;
            this.ambienteSri=ambienteSri; this.codEstab=codEstab; this.codPtoEmi=codPtoEmi; this.secuencial=secuencial;
            this.fechaEmision=fechaEmision; this.fechaEmisionDocSustento=fechaEmisionDocSustento;
            this.numDocModificado=numDocModificado; this.tipoIdComp=tipoIdComp; this.rutaPDF=rutaPDF;
            this.rutaXML=rutaXML; this.totalSinImpuestos=totalSinImpuestos; this.valorIva=valorIva;
            this.valorTotal=valorTotal; this.motivoStr=motivoStr; this.formaPago=formaPago;
            this.cliente=cliente; this.empresa=empresa; this.notaDebitoId=notaDebitoId;
            this.detallesParaXml=detallesParaXml; this.facturaRegistroId=facturaRegistroId;
        }
    }

    public ResultadoNotaDebito emitirNotaDebito(int facturaRegistroId, List<MotivoNDInput> motivosInput,
                                                  String formaPago, String ambienteSri, String rutaP12, String claveP12,
                                                  File directorioEscritorio, int usuarioId) throws Exception {
        com.vendex.util.SesionActual.exigirPermiso("NOTA_DEBITO_EMITIR");
        if (motivosInput == null || motivosInput.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un motivo.");
        for (MotivoNDInput m : motivosInput) {
            if (m.valor == null || m.valor.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Cada motivo debe tener valor > 0.");
        }

        FacturaRegistro factura = facturaRegistroDAO.obtenerPorId(facturaRegistroId);
        if (factura == null) throw new IllegalArgumentException("Factura no encontrada id=" + facturaRegistroId);
        boolean esPendienteND = AppConstants.ESTADO_PENDIENTE.equals(factura.getEstadoSri()) || AppConstants.ESTADO_RECIBIDA.equals(factura.getEstadoSri()) || AppConstants.ESTADO_ERROR.equals(factura.getEstadoSri());
        if (!AppConstants.ESTADO_AUTORIZADO.equals(factura.getEstadoSri()) && !(esPendienteND && com.vendex.util.SRIContingenciaConfig.isModoContingencia()))
            throw new IllegalStateException("Solo se permite ND sobre factura AUTORIZADA. Estado actual: " + factura.getEstadoSri() + (esPendienteND ? " (habilitar modo contingencia para PENDIENTE)" : ""));

        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (empresa == null) throw new IllegalStateException("No se encontraron datos de la empresa.");
        Cliente cliente = clienteDAO.obtenerPorId(factura.getClienteId());
        if (cliente == null) throw new IllegalStateException("Cliente no encontrado para la factura.");

        int secuencialND = secuenciaDAO.marcarUsado("NOTA_DEBITO");
        if (secuencialND == -1) throw new IllegalStateException("No se pudo obtener secuencial NOTA_DEBITO.");
        SecuenciaDocumento sec = secuenciaDAO.obtener("NOTA_DEBITO");
        String codEstab = sec.getEstablecimiento() != null ? sec.getEstablecimiento() : AppConstants.ESTABLECIMIENTO_DEFAULT;
        String codPtoEmi = sec.getPuntoEmision() != null ? sec.getPuntoEmision() : AppConstants.PUNTO_EMISION_DEFAULT;
        String claveAcceso = ClaveAcceso.generar(AppConstants.TIPO_COMPROBANTE_NOTA_DEBITO, empresa.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialND);
        String secuencialStr = String.format("%09d", secuencialND);
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + secuencialStr;
        LocalDateTime ahora = LocalDateTime.now();
        String fechaEmision = ahora.format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
        String fechaEmisionDocSustento = factura.getFecha() != null ? factura.getFecha().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : fechaEmision;
        String numDocModificado = factura.getNumComprobante() != null ? factura.getNumComprobante() : factura.getCodigo();

        String tipoIdComp;
        String identTrim = cliente.getIdentificacion() != null ? cliente.getIdentificacion().trim() : "";
        tipoIdComp = AppConstants.esConsumidorFinal(identTrim) ? "05" : (identTrim.length() == AppConstants.MAX_LONGITUD_IDENTIFICACION_JURIDICA ? "04" : "05");

        BigDecimal subSinIva = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        StringBuilder motivoStr = new StringBuilder();
        List<NotaDebitoMotivo> motivosParaDao = new ArrayList<>();
        List<Object[]> detallesParaXml = new ArrayList<>();
        for (MotivoNDInput in : motivosInput) {
            BigDecimal val = in.valor.setScale(2, RoundingMode.HALF_UP);
            if (in.gravaIva) {
                BigDecimal precioSinIva = val.divide(new BigDecimal("1.15"), 6, RoundingMode.HALF_UP);
                BigDecimal iv = val.subtract(precioSinIva).setScale(2, RoundingMode.HALF_UP);
                subSinIva = subSinIva.add(precioSinIva);
                iva = iva.add(iv);
            } else {
                subSinIva = subSinIva.add(val);
            }
            if (motivoStr.length() > 0) motivoStr.append("; ");
            motivoStr.append(in.razon);
            NotaDebitoMotivo nd = new NotaDebitoMotivo();
            nd.setRazon(in.razon);
            nd.setValor(val);
            nd.setGravaIva(in.gravaIva);
            nd.setCodigoPorcentajeIva(in.gravaIva ? "4" : "4");
            motivosParaDao.add(nd);
            detallesParaXml.add(new Object[]{in.razon, val.toPlainString(), in.gravaIva ? "gravado" : "exento"});
        }
        if (subSinIva.compareTo(BigDecimal.ZERO) < 0) subSinIva = BigDecimal.ZERO;
        subSinIva = subSinIva.setScale(2, RoundingMode.HALF_UP);
        iva = iva.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorTotal = subSinIva.add(iva).setScale(2, RoundingMode.HALF_UP);

        String dirMatriz = empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria();
        String xmlGenerado = XmlNotaDebitoBuilder.construirNotaDebito(
                ambienteSri, claveAcceso, empresa.getRuc(), empresa.getRazonSocial(),
                codEstab, codPtoEmi, secuencialND, dirMatriz, dirMatriz,
                "", "NO", tipoIdComp, cliente.getNombre(), cliente.getIdentificacion(),
                AppConstants.COD_DOC_MODIFICADO_FACTURA, numDocModificado, fechaEmisionDocSustento,
                fechaEmision, subSinIva.toPlainString(), iva.toPlainString(), valorTotal.toPlainString(),
                motivoStr.length() > 300 ? motivoStr.substring(0,300) : motivoStr.toString(),
                formaPago, detallesParaXml
         );
         if (xmlGenerado == null) throw new IllegalStateException("No se pudo generar el XML de nota de debito.");

        String xmlFirmado = xmlGenerado;
        if (rutaP12 == null || rutaP12.trim().isEmpty() || claveP12 == null || claveP12.trim().isEmpty())
            throw new IllegalStateException("No se configuró la firma electrónica (.p12 y contraseña).");
        try {
            FirmaDigital firma = new FirmaDigital();
            if (!firma.cargarCertificado(rutaP12, claveP12)) throw new IllegalStateException("No se pudo cargar el certificado.");
            xmlFirmado = firma.firmarXml(xmlGenerado);
        } catch (Exception e) {
            logDAO.guardar("NotaDebitoService", "firmarXml", e.getMessage(), e);
            throw new Exception("Error al firmar XML: " + e.getMessage(), e);
        }

        NotaDebitoRegistro nd = new NotaDebitoRegistro();
        nd.setClaveAcceso(claveAcceso);
        nd.setFacturaRegistroId(facturaRegistroId);
        nd.setEstablecimiento(codEstab);
        nd.setPuntoEmision(codPtoEmi);
        nd.setSecuencial(secuencialStr);
        nd.setFechaEmision(ahora);
        nd.setClienteId(cliente.getId());
        nd.setFormaPago(formaPago);
        nd.setTotalSinImpuestos(subSinIva);
        nd.setValorIva(iva);
        nd.setValorTotal(valorTotal);
        nd.setEstadoSri(AppConstants.ESTADO_PENDIENTE);
        nd.setXmlFirmado(xmlFirmado);
        nd.setUsuarioId(usuarioId);
        int ndId;
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                ndId = notaDebitoDAO.insertar(con, nd);
                if (ndId == -1) throw new IllegalStateException("Error al registrar la nota de debito.");
                for (NotaDebitoMotivo m : motivosParaDao) m.setNotaDebitoId(ndId);
                notaDebitoMotivoDAO.insertarMotivos(con, ndId, motivosParaDao);
                comprobanteDAO.insertar(con, claveAcceso, ndId, numComprobante, ambienteSri, xmlFirmado, AppConstants.TIPO_COMPROBANTE_NOTA_DEBITO);
                con.commit();
            } catch (Exception e) { try { con.rollback(); } catch (SQLException re) {} throw e; } finally { try { con.setAutoCommit(true); } catch (SQLException ignore) {} }
        }

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_NOTA_DEBITO + numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
        String rutaXML = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_NOTA_DEBITO + numComprobante.replace("-", "") + AppConstants.EXTENSION_XML;
        try { Files.write(Paths.get(rutaXML), xmlFirmado.getBytes(StandardCharsets.UTF_8)); } catch (IOException e) { logDAO.guardar("NotaDebitoService","guardarXML", e.getMessage(), e); }

        return new ResultadoNotaDebito(claveAcceso, numComprobante, xmlFirmado, ambienteSri, codEstab, codPtoEmi, secuencialND, fechaEmision, fechaEmisionDocSustento, numDocModificado, tipoIdComp, rutaPDF, rutaXML, subSinIva, iva, valorTotal, motivoStr.toString(), formaPago, cliente, empresa, ndId, detallesParaXml, facturaRegistroId);
    }

    public SRIWebService.SRIResponse enviarYSolicitarAutorizacion(String ambienteSri, String xmlFirmado, String claveAcceso) {
        try {
            SRIWebService sriWs = new SRIWebService(ambienteSri);
            return sriWs.validarComprobante(xmlFirmado, claveAcceso);
        } catch (Exception e) {
            logDAO.guardar("NotaDebitoService", "validarSRI", e.getMessage(), e);
            SRIWebService.SRIResponse r = new SRIWebService.SRIResponse();
            r.setEstado(AppConstants.ESTADO_PENDIENTE);
            r.setMensaje("Error de conexion: " + e.getMessage());
            return r;
        }
    }

    public void finalizarEnvioSRI(SRIWebService.SRIResponse sriResp, ResultadoNotaDebito resultado, File directorioEscritorio) {
        String estado = sriResp.getEstado();
        String numAut = sriResp.getNumeroAutorizacion();
        String fechaAut = sriResp.getFechaAutorizacion();
        // Si SRI devuelve DEVUELTA/RECHAZADA/NO AUTORIZADO volcar XML y respuesta a vendex_errors para diagnóstico
        if ("DEVUELTA".equals(estado) || "RECHAZADA".equals(estado) || "NO AUTORIZADO".equals(estado) || (sriResp.getMensaje()!=null && sriResp.getMensaje().toLowerCase().contains("no cumple"))) {
            try {
                String home = System.getProperty("user.home");
                java.io.File dir = new java.io.File(home, "vendex_errors");
                if (!dir.exists()) dir.mkdirs();
                String base = "ND_" + resultado.numComprobante.replace("-", "") + "_" + resultado.claveAcceso;
                Files.write(Paths.get(new java.io.File(dir, base + "_enviado.xml").getAbsolutePath()), resultado.xmlFirmado.getBytes(StandardCharsets.UTF_8));
                if (sriResp.getRespuestaRecepcionXml()!=null) Files.write(Paths.get(new java.io.File(dir, base + "_recepcion.xml").getAbsolutePath()), sriResp.getRespuestaRecepcionXml().getBytes(StandardCharsets.UTF_8));
                if (sriResp.getRespuestaAutorizacionXml()!=null) Files.write(Paths.get(new java.io.File(dir, base + "_autorizacion.xml").getAbsolutePath()), sriResp.getRespuestaAutorizacionXml().getBytes(StandardCharsets.UTF_8));
                logDAO.guardar("NotaDebitoService","SRI-DEVUELTA","ND "+resultado.numComprobante+" clave="+resultado.claveAcceso+" estado="+estado+" msg="+sriResp.getMensaje());
            } catch (Exception e) { logDAO.guardar("NotaDebitoService","volcarXMLError", e.getMessage(), e); }
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                notaDebitoDAO.actualizarEstado(con, resultado.claveAcceso, estado, sriResp.getMensaje(), numAut, fechaAut);
                comprobanteDAO.actualizarEstado(con, resultado.claveAcceso, estado, sriResp.getMensaje(), resultado.xmlFirmado, numAut, fechaAut);
                comprobanteDAO.guardarEnvio(con, resultado.claveAcceso, resultado.numComprobante, resultado.ambienteSri, resultado.xmlFirmado, sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(), estado, sriResp.getMensaje(), numAut, fechaAut, AppConstants.TIPO_COMPROBANTE_NOTA_DEBITO);
                con.commit();
            } catch (Exception e) { try { con.rollback(); } catch (SQLException re) {} throw new RuntimeException(e); } finally { try { con.setAutoCommit(true); } catch (SQLException ignore) {} }
        } catch (SQLException e) { throw new RuntimeException(e); }
        actualizarEstadoFactura(resultado, estado);
        if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
            try {
                CajaSesion sesionAbierta = cajaSesionDAO.obtenerAbierta();
                if (sesionAbierta != null) {
                    CajaMovimiento mov = new CajaMovimiento(sesionAbierta.getId(), "INGRESO", resultado.valorTotal, "ND " + resultado.numComprobante + " agrega cargo a Factura " + resultado.numDocModificado + " - " + resultado.motivoStr, sesionAbierta.getUsuarioId());
                    mov.setReferenciaId(resultado.notaDebitoId);
                    mov.setReferenciaTipo("NOTA_DEBITO");
                    cajaMovimientoDAO.insertar(mov);
                }
            } catch (Exception e) { logDAO.guardar("NotaDebitoService","cajaMovimiento", e.getMessage(), e); }
        }
        try {
            String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_NOTA_DEBITO + resultado.numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
            PdfNotaDebito.generar(rutaPDF, resultado.claveAcceso, numAut, fechaAut, resultado.ambienteSri,
                    resultado.empresa.getRuc(), resultado.empresa.getRazonSocial(),
                    resultado.empresa.getDireccionCallePrincipal() + " y " + resultado.empresa.getDireccionCalleSecundaria(),
                    resultado.empresa.getTelefono(), resultado.empresa.getCorreo(),
                    resultado.codEstab, resultado.codPtoEmi, resultado.secuencial,
                    resultado.fechaEmision, resultado.fechaEmisionDocSustento, resultado.numDocModificado,
                    resultado.tipoIdComp, resultado.cliente.getNombre(), resultado.cliente.getIdentificacion(), resultado.cliente.getDireccion(), resultado.cliente.getCorreo(), resultado.cliente.getTelefono(),
                    resultado.motivoStr, resultado.detallesParaXml,
                    resultado.totalSinImpuestos, resultado.valorIva, resultado.valorTotal);
        } catch (Exception e) { logDAO.guardar("NotaDebitoService","generarPDF", e.getMessage(), e); }
    }

    private void actualizarEstadoFactura(ResultadoNotaDebito resultado, String estado) {
        if (!AppConstants.ESTADO_AUTORIZADO.equals(estado)) return;
        try (java.sql.Connection con = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(valor_total),0) FROM nota_debito_registro WHERE factura_registro_id=? AND estado_sri='AUTORIZADO'")) {
            ps.setInt(1, resultado.facturaRegistroId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal sum = rs.getBigDecimal(1);
                    FacturaRegistro fac = facturaRegistroDAO.obtenerPorId(resultado.facturaRegistroId);
                    String nuevoEstadoNd = "CON_CARGOS";
                    if (fac != null && fac.getTotal() != null && sum.compareTo(fac.getTotal()) >= 0) nuevoEstadoNd = "CON_CARGOS";
                    try (java.sql.PreparedStatement upd = con.prepareStatement("UPDATE factura_registro SET estado_nd=? WHERE id=?")) {
                        upd.setString(1, nuevoEstadoNd);
                        upd.setInt(2, resultado.facturaRegistroId);
                        upd.executeUpdate();
                    }
                }
            }
        } catch (Exception e) { logDAO.guardar("NotaDebitoService","actualizarEstadoFactura", e.getMessage(), e); }
    }

    public String regenerarRide(String claveAcceso, String numeroAutorizacion, String fechaAutorizacion, File directorioEscritorio) {
        NotaDebitoRegistro nd = notaDebitoDAO.obtenerPorClave(claveAcceso);
        if (nd == null) { logDAO.guardar("NotaDebitoService","regenerarRide","ND no encontrada "+claveAcceso); return null; }
        Cliente cliente = clienteDAO.obtenerPorId(nd.getClienteId());
        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (cliente == null || empresa == null) return null;
        FacturaRegistro factura = facturaRegistroDAO.obtenerPorId(nd.getFacturaRegistroId());
        String numDocMod = factura != null ? factura.getNumComprobante() : "";
        String fechaDocSust = factura != null && factura.getFecha()!=null ? factura.getFecha().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";
        String identTrim = cliente.getIdentificacion() != null ? cliente.getIdentificacion().trim() : "";
        String tipoIdComp = AppConstants.esConsumidorFinal(identTrim) ? "05" : (cliente.getIdentificacion()!=null&&cliente.getIdentificacion().trim().length()==13?"04":"05");
        String fechaEmision = nd.getFechaEmision()!=null? nd.getFechaEmision().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";
        String[] partes = nd.getSecuencial()!=null? new String[]{nd.getEstablecimiento(), nd.getPuntoEmision(), nd.getSecuencial()} : (nd.getNumComprobante()!=null? nd.getNumComprobante().split("-"): new String[]{"001","001","000000000"});
        String codEstab = partes.length>0?partes[0]:"001"; String codPtoEmi = partes.length>1?partes[1]:"001"; int sec = partes.length>2? Integer.parseInt(partes[2].replaceAll("\\D","")):0;
        List<NotaDebitoMotivo> motivos = notaDebitoMotivoDAO.listarPorNotaDebitoId(nd.getId());
        List<Object[]> detallesXml = new ArrayList<>();
        for (NotaDebitoMotivo m: motivos) detallesXml.add(new Object[]{m.getRazon(), m.getValor().toPlainString(), m.isGravaIva()?"gravado":"exento"});
        String rutaPDF = directorioEscritorio.getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_NOTA_DEBITO+nd.getNumComprobante().replace("-","")+AppConstants.EXTENSION_PDF;
        PdfNotaDebito.generar(rutaPDF, claveAcceso, numeroAutorizacion, fechaAutorizacion, nd.getEstadoSri()!=null? "PRUEBAS": "PRUEBAS",
                empresa.getRuc(), empresa.getRazonSocial(), empresa.getDireccionCallePrincipal()+" y "+empresa.getDireccionCalleSecundaria(),
                empresa.getTelefono(), empresa.getCorreo(),
                codEstab, codPtoEmi, sec, fechaEmision, fechaDocSust, numDocMod,
                tipoIdComp, cliente.getNombre(), cliente.getIdentificacion(), cliente.getDireccion(), cliente.getCorreo(), cliente.getTelefono(),
                nd.getMensajeSri()!=null?nd.getMensajeSri():"", detallesXml,
                nd.getTotalSinImpuestos()!=null?nd.getTotalSinImpuestos():BigDecimal.ZERO,
                nd.getValorIva()!=null?nd.getValorIva():BigDecimal.ZERO,
                nd.getValorTotal()!=null?nd.getValorTotal():BigDecimal.ZERO);
        return rutaPDF;
    }

    public boolean enviarCorreoAutorizacion(String destinatario, String nombreCliente, String codigo, String rutaPDF, String rutaXML) {
        if (destinatario==null||destinatario.trim().isEmpty()) return false;
        try {
            EmailService emailService = new EmailService();
            return emailService.enviarCorreoConArchivos(destinatario.trim(), nombreCliente, codigo, AppConstants.TIPO_DOCUMENTO_NOTA_DEBITO, new File(rutaPDF), new File(rutaXML));
        } catch (Exception e) { logDAO.guardar("NotaDebitoService","enviarCorreo","Error enviando correo a "+destinatario+": "+e.getMessage(), e); return false; }
    }
}

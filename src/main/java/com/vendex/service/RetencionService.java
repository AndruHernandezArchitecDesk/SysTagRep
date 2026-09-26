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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RetencionService {

    private final EmpresaDAO empresaDAO;
    private final ProveedorDAO proveedorDAO;
    private final SecuenciaDocumentoDAO secuenciaDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final RetencionRegistroDAO retencionDAO;
    private final RetencionDocumentoSustentoDAO docDAO;
    private final RetencionDetalleDAO detalleDAO;
    private final LogDAO logDAO = new LogDAO();

    public RetencionService() {
        this.empresaDAO = new EmpresaDAOPostgres();
        this.proveedorDAO = new ProveedorDAO();
        this.secuenciaDAO = new SecuenciaDocumentoDAOPostgres();
        this.comprobanteDAO = new ComprobanteDAOPostgres();
        this.retencionDAO = new RetencionRegistroDAOPostgres();
        this.docDAO = new RetencionDocumentoSustentoDAOPostgres();
        this.detalleDAO = new RetencionDetalleDAOPostgres();
    }
    public RetencionService(EmpresaDAO empresaDAO, ProveedorDAO proveedorDAO, SecuenciaDocumentoDAO secuenciaDAO, ComprobanteDAO comprobanteDAO, RetencionRegistroDAO retencionDAO, RetencionDocumentoSustentoDAO docDAO, RetencionDetalleDAO detalleDAO) {
        this.empresaDAO = empresaDAO;
        this.proveedorDAO = proveedorDAO;
        this.secuenciaDAO = secuenciaDAO;
        this.comprobanteDAO = comprobanteDAO;
        this.retencionDAO = retencionDAO;
        this.docDAO = docDAO;
        this.detalleDAO = detalleDAO;
    }


    public static class RetencionLineaInput {
        public String codigo; // 1 Renta, 2 IVA
        public String codigoRetencion;
        public BigDecimal baseImponible;
        public BigDecimal porcentajeRetener;
        public BigDecimal valorRetenido;
        public RetencionLineaInput(String codigo, String codigoRetencion, BigDecimal base, BigDecimal porc) {
            this.codigo = codigo; this.codigoRetencion = codigoRetencion; this.baseImponible = base; this.porcentajeRetener = porc;
            this.valorRetenido = base.multiply(porc).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    }

    public static class DocSustentoInput {
        public String codSustento = "01";
        public String codDocSustento = "01";
        public String numDocSustento;
        public LocalDate fechaEmisionDocSustento;
        public BigDecimal totalSinImpuestos;
        public List<RetencionLineaInput> retenciones = new ArrayList<>();
        public DocSustentoInput(String numDoc, LocalDate fecha, BigDecimal total) {
            this.numDocSustento = numDoc;
            this.fechaEmisionDocSustento = fecha;
            this.totalSinImpuestos = total;
        }
    }

    public static class ResultadoRetencion {
        public final String claveAcceso;
        public final String numComprobante;
        public final String xmlFirmado;
        public final String ambienteSri;
        public final String codEstab;
        public final String codPtoEmi;
        public final int secuencial;
        public final String fechaEmision;
        public final String periodoFiscal;
        public final String tipoIdSujeto;
        public final String razonSujeto;
        public final String identSujeto;
        public final String rutaPDF;
        public final String rutaXML;
        public final List<Object[]> docsParaXml;
        public final Empresa empresa;
        public final int retencionId;
        public ResultadoRetencion(String claveAcceso, String numComprobante, String xmlFirmado, String ambienteSri,
                                  String codEstab, String codPtoEmi, int secuencial, String fechaEmision, String periodoFiscal,
                                  String tipoIdSujeto, String razonSujeto, String identSujeto,
                                  String rutaPDF, String rutaXML, List<Object[]> docsParaXml, Empresa empresa, int retencionId) {
            this.claveAcceso=claveAcceso; this.numComprobante=numComprobante; this.xmlFirmado=xmlFirmado; this.ambienteSri=ambienteSri;
            this.codEstab=codEstab; this.codPtoEmi=codPtoEmi; this.secuencial=secuencial; this.fechaEmision=fechaEmision; this.periodoFiscal=periodoFiscal;
            this.tipoIdSujeto=tipoIdSujeto; this.razonSujeto=razonSujeto; this.identSujeto=identSujeto;
            this.rutaPDF=rutaPDF; this.rutaXML=rutaXML; this.docsParaXml=docsParaXml; this.empresa=empresa; this.retencionId=retencionId;
        }
    }

    public ResultadoRetencion emitirRetencion(List<DocSustentoInput> docsInput,
                                              String periodoFiscal,
                                              String tipoIdSujeto, String razonSujeto, String identificacionSujeto,
                                              Integer proveedorId,
                                              String ambienteSri, String rutaP12, String claveP12,
                                              File directorioEscritorio, int usuarioId) throws Exception {
        com.vendex.util.SesionActual.exigirPermiso("RETENCION_EMITIR");
        if (docsInput == null || docsInput.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un documento sustento.");
        if (periodoFiscal == null || !periodoFiscal.matches("\\d{2}/\\d{4}")) throw new IllegalArgumentException("Periodo fiscal debe ser MM/YYYY.");
        if (identificacionSujeto == null || identificacionSujeto.trim().isEmpty()) throw new IllegalArgumentException("Identificación sujeto retenido obligatoria.");
        if (razonSujeto == null || razonSujeto.trim().isEmpty()) throw new IllegalArgumentException("Razón social sujeto retenido obligatoria.");
        for (DocSustentoInput d : docsInput) {
            if (d.numDocSustento == null || d.numDocSustento.trim().isEmpty()) throw new IllegalArgumentException("Número documento sustento obligatorio.");
            if (d.fechaEmisionDocSustento == null) throw new IllegalArgumentException("Fecha emisión documento sustento obligatoria.");
            if (d.retenciones == null || d.retenciones.isEmpty()) throw new IllegalArgumentException("Cada documento debe tener al menos una retención.");
            for (RetencionLineaInput r : d.retenciones) {
                BigDecimal esperado = r.baseImponible.multiply(r.porcentajeRetener).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (esperado.compareTo(r.valorRetenido) != 0) r.valorRetenido = esperado;
                if (r.valorRetenido.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Valor retenido debe ser > 0.");
            }
        }

        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (empresa == null) throw new IllegalStateException("No se encontraron datos de la empresa.");

        int secuencialRet = secuenciaDAO.marcarUsado("RETENCION");
        if (secuencialRet == -1) throw new IllegalStateException("No se pudo obtener secuencial RETENCION.");
        SecuenciaDocumento sec = secuenciaDAO.obtener("RETENCION");
        String codEstab = sec.getEstablecimiento() != null ? sec.getEstablecimiento() : AppConstants.ESTABLECIMIENTO_DEFAULT;
        String codPtoEmi = sec.getPuntoEmision() != null ? sec.getPuntoEmision() : AppConstants.PUNTO_EMISION_DEFAULT;
        String claveAcceso = ClaveAcceso.generar(AppConstants.TIPO_COMPROBANTE_RETENCION, empresa.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialRet);
        String secuencialStr = String.format("%09d", secuencialRet);
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + secuencialStr;
        LocalDateTime ahora = LocalDateTime.now();
        String fechaEmision = ahora.format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
        String dirMatriz = empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria();
        String dirEstablecimiento = dirMatriz;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION);
        List<Object[]> docsParaXml = new ArrayList<>();
        List<RetencionDocumentoSustento> docEntities = new ArrayList<>();
        List<List<RetencionDetalle>> detallesPorDoc = new ArrayList<>();

        for (DocSustentoInput in : docsInput) {
            String fechaStr = in.fechaEmisionDocSustento.format(fmt);
            String totalStr = in.totalSinImpuestos.setScale(2, RoundingMode.HALF_UP).toPlainString();
            List<Object[]> retsXml = new ArrayList<>();
            List<RetencionDetalle> retsEnt = new ArrayList<>();
            for (RetencionLineaInput r : in.retenciones) {
                retsXml.add(new Object[]{r.codigo, r.codigoRetencion, r.baseImponible.toPlainString(), r.porcentajeRetener.toPlainString(), r.valorRetenido.toPlainString()});
                RetencionDetalle det = new RetencionDetalle();
                det.setCodigo(r.codigo); det.setCodigoRetencion(r.codigoRetencion);
                det.setBaseImponible(r.baseImponible); det.setPorcentajeRetener(r.porcentajeRetener); det.setValorRetenido(r.valorRetenido);
                retsEnt.add(det);
            }
            // docsSustento element: codSustento, codDocSustento, numDocSustento, fechaEmision, totalSinImpuestos, numAut (usamos mismo num), retsXml
            Object[] docXml = new Object[]{in.codSustento, in.codDocSustento, in.numDocSustento, fechaStr, totalStr, in.numDocSustento, retsXml};
            docsParaXml.add(docXml);

            RetencionDocumentoSustento docEnt = new RetencionDocumentoSustento();
            docEnt.setCodSustento(in.codSustento);
            docEnt.setCodDocSustento(in.codDocSustento);
            docEnt.setNumDocSustento(in.numDocSustento);
            docEnt.setFechaEmisionDocSustento(in.fechaEmisionDocSustento);
            docEnt.setTotalSinImpuestos(in.totalSinImpuestos);
            docEntities.add(docEnt);
            detallesPorDoc.add(retsEnt);
        }

        String xmlGenerado = XmlRetencionBuilder.construirRetencion(
                ambienteSri, claveAcceso, empresa.getRuc(), empresa.getRazonSocial(),
                codEstab, codPtoEmi, secuencialRet, dirMatriz, dirEstablecimiento,
                periodoFiscal, tipoIdSujeto, razonSujeto, identificacionSujeto, docsParaXml);
        if (xmlGenerado == null) throw new IllegalStateException("No se pudo generar XML de retención.");

        String xmlFirmado = xmlGenerado;
        if (rutaP12 == null || rutaP12.trim().isEmpty() || claveP12 == null || claveP12.trim().isEmpty())
            throw new IllegalStateException("No se configuró firma electrónica (.p12 y contraseña).");
        try {
            FirmaDigital firma = new FirmaDigital();
            if (!firma.cargarCertificado(rutaP12, claveP12)) throw new IllegalStateException("No se pudo cargar certificado.");
            xmlFirmado = firma.firmarXml(xmlGenerado);
        } catch (Exception e) {
            logDAO.guardar("RetencionService","firmarXml", e.getMessage(), e);
            throw new Exception("Error al firmar XML: " + e.getMessage(), e);
        }

        RetencionRegistro reg = new RetencionRegistro();
        reg.setClaveAcceso(claveAcceso);
        reg.setEstablecimiento(codEstab);
        reg.setPuntoEmision(codPtoEmi);
        reg.setSecuencial(secuencialStr);
        reg.setFechaEmision(ahora);
        reg.setPeriodoFiscal(periodoFiscal);
        reg.setProveedorId(proveedorId);
        reg.setTipoIdentificacionSujeto(tipoIdSujeto);
        reg.setRazonSocialSujeto(razonSujeto);
        reg.setIdentificacionSujeto(identificacionSujeto);
        reg.setEstadoSri(AppConstants.ESTADO_PENDIENTE);
        reg.setXmlFirmado(xmlFirmado);
        reg.setUsuarioId(usuarioId);
        int retId;
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                retId = retencionDAO.insertar(con, reg);
                if (retId == -1) throw new IllegalStateException("Error al registrar retención.");
                for (int i=0;i<docEntities.size();i++) docEntities.get(i).setRetencionId(retId);
                docDAO.insertarDocumentos(con, retId, docEntities);
                for (int i=0;i<docEntities.size();i++) {
                    int docId = docEntities.get(i).getId();
                    detalleDAO.insertarDetalles(con, docId, detallesPorDoc.get(i));
                }
                comprobanteDAO.insertar(con, claveAcceso, retId, numComprobante, ambienteSri, xmlFirmado, AppConstants.TIPO_COMPROBANTE_RETENCION);
                con.commit();
            } catch (Exception e) { try { con.rollback(); } catch (SQLException re) {} throw e; } finally { try { con.setAutoCommit(true); } catch (SQLException ignore) {} }
        }

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_RETENCION + numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
        String rutaXML = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_RETENCION + numComprobante.replace("-", "") + AppConstants.EXTENSION_XML;
        try { Files.write(Paths.get(rutaXML), xmlFirmado.getBytes(StandardCharsets.UTF_8)); } catch (IOException e) { logDAO.guardar("RetencionService","guardarXML", e.getMessage(), e); }

        return new ResultadoRetencion(claveAcceso, numComprobante, xmlFirmado, ambienteSri, codEstab, codPtoEmi, secuencialRet, fechaEmision, periodoFiscal, tipoIdSujeto, razonSujeto, identificacionSujeto, rutaPDF, rutaXML, docsParaXml, empresa, retId);
    }

    public SRIWebService.SRIResponse enviarYSolicitarAutorizacion(String ambienteSri, String xmlFirmado, String claveAcceso) {
        try {
            SRIWebService sriWs = new SRIWebService(ambienteSri);
            return sriWs.validarComprobante(xmlFirmado, claveAcceso);
        } catch (Exception e) {
            logDAO.guardar("RetencionService","validarSRI", e.getMessage(), e);
            SRIWebService.SRIResponse r = new SRIWebService.SRIResponse();
            r.setEstado(AppConstants.ESTADO_PENDIENTE);
            r.setMensaje("Error conexion: " + e.getMessage());
            return r;
        }
    }

    public void finalizarEnvioSRI(SRIWebService.SRIResponse sriResp, ResultadoRetencion resultado, File directorioEscritorio) {
        String estado = sriResp.getEstado();
        String numAut = sriResp.getNumeroAutorizacion();
        String fechaAut = sriResp.getFechaAutorizacion();
        if ("DEVUELTA".equals(estado) || "RECHAZADA".equals(estado) || "NO AUTORIZADO".equals(estado) || (sriResp.getMensaje()!=null && sriResp.getMensaje().toLowerCase().contains("no cumple"))) {
            try {
                String home = System.getProperty("user.home");
                java.io.File dir = new java.io.File(home, "vendex_errors");
                if (!dir.exists()) dir.mkdirs();
                String base = "RET_" + resultado.numComprobante.replace("-", "") + "_" + resultado.claveAcceso;
                Files.write(Paths.get(new java.io.File(dir, base + "_enviado.xml").getAbsolutePath()), resultado.xmlFirmado.getBytes(StandardCharsets.UTF_8));
                if (sriResp.getRespuestaRecepcionXml()!=null) Files.write(Paths.get(new java.io.File(dir, base + "_recepcion.xml").getAbsolutePath()), sriResp.getRespuestaRecepcionXml().getBytes(StandardCharsets.UTF_8));
                if (sriResp.getRespuestaAutorizacionXml()!=null) Files.write(Paths.get(new java.io.File(dir, base + "_autorizacion.xml").getAbsolutePath()), sriResp.getRespuestaAutorizacionXml().getBytes(StandardCharsets.UTF_8));
                logDAO.guardar("RetencionService","SRI-DEVUELTA","RET "+resultado.numComprobante+" clave="+resultado.claveAcceso+" estado="+estado+" msg="+sriResp.getMensaje());
            } catch (Exception e) { logDAO.guardar("RetencionService","volcarXMLError", e.getMessage(), e); }
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                retencionDAO.actualizarEstado(con, resultado.claveAcceso, estado, sriResp.getMensaje(), numAut, fechaAut);
                comprobanteDAO.actualizarEstado(con, resultado.claveAcceso, estado, sriResp.getMensaje(), resultado.xmlFirmado, numAut, fechaAut);
                comprobanteDAO.guardarEnvio(con, resultado.claveAcceso, resultado.numComprobante, resultado.ambienteSri, resultado.xmlFirmado, sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(), estado, sriResp.getMensaje(), numAut, fechaAut, AppConstants.TIPO_COMPROBANTE_RETENCION);
                con.commit();
            } catch (Exception e) { try { con.rollback(); } catch (SQLException re) {} throw new RuntimeException(e); } finally { try { con.setAutoCommit(true); } catch (SQLException ignore) {} }
        } catch (SQLException e) { throw new RuntimeException(e); }
        try {
            String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_RETENCION + resultado.numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
            PdfRetencion.generar(rutaPDF, resultado.claveAcceso, numAut, fechaAut, resultado.ambienteSri,
                    resultado.empresa.getRuc(), resultado.empresa.getRazonSocial(),
                    resultado.empresa.getDireccionCallePrincipal() + " y " + resultado.empresa.getDireccionCalleSecundaria(),
                    resultado.codEstab, resultado.codPtoEmi, resultado.secuencial, resultado.fechaEmision, resultado.periodoFiscal,
                    resultado.tipoIdSujeto, resultado.razonSujeto, resultado.identSujeto, resultado.docsParaXml);
        } catch (Exception e) { logDAO.guardar("RetencionService","generarPDF", e.getMessage(), e); }
    }

    public String regenerarRide(String claveAcceso, String numeroAutorizacion, String fechaAutorizacion, File directorioEscritorio) {
        RetencionRegistro reg = retencionDAO.obtenerPorClave(claveAcceso);
        if (reg == null) { logDAO.guardar("RetencionService","regenerarRide","Retencion no encontrada "+claveAcceso); return null; }
        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (empresa == null) return null;
        String fechaEmision = reg.getFechaEmision()!=null? reg.getFechaEmision().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";
        String[] partes = reg.getSecuencial()!=null? new String[]{reg.getEstablecimiento(), reg.getPuntoEmision(), reg.getSecuencial()} : (reg.getNumComprobante()!=null? reg.getNumComprobante().split("-"): new String[]{"001","001","000000000"});
        String codEstab = partes.length>0?partes[0]:"001"; String codPtoEmi = partes.length>1?partes[1]:"001"; int sec = partes.length>2? Integer.parseInt(partes[2].replaceAll("\\D","")):0;
        List<RetencionDocumentoSustento> docs = docDAO.listarPorRetencionId(reg.getId());
        List<Object[]> docsXml = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION);
        for (RetencionDocumentoSustento d : docs) {
            List<RetencionDetalle> dets = detalleDAO.listarPorDocumentoId(d.getId());
            List<Object[]> retsXml = new ArrayList<>();
            for (RetencionDetalle rd : dets) retsXml.add(new Object[]{rd.getCodigo(), rd.getCodigoRetencion(), rd.getBaseImponible().toPlainString(), rd.getPorcentajeRetener().toPlainString(), rd.getValorRetenido().toPlainString()});
            String fechaSust = d.getFechaEmisionDocSustento()!=null? d.getFechaEmisionDocSustento().format(fmt): "";
            docsXml.add(new Object[]{d.getCodSustento(), d.getCodDocSustento(), d.getNumDocSustento(), fechaSust, d.getTotalSinImpuestos().toPlainString(), d.getNumDocSustento(), retsXml});
        }
        String rutaPDF = directorioEscritorio.getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_RETENCION+reg.getNumComprobante().replace("-","")+AppConstants.EXTENSION_PDF;
        PdfRetencion.generar(rutaPDF, claveAcceso, numeroAutorizacion, fechaAutorizacion, reg.getEstadoSri()!=null? "PRUEBAS": "PRUEBAS",
                empresa.getRuc(), empresa.getRazonSocial(), empresa.getDireccionCallePrincipal()+" y "+empresa.getDireccionCalleSecundaria(),
                codEstab, codPtoEmi, sec, fechaEmision, reg.getPeriodoFiscal(), reg.getTipoIdentificacionSujeto(), reg.getRazonSocialSujeto(), reg.getIdentificacionSujeto(), docsXml);
        return rutaPDF;
    }

    public boolean enviarCorreoAutorizacion(String destinatario, String nombreCliente, String codigo, String rutaPDF, String rutaXML) {
        if (destinatario==null||destinatario.trim().isEmpty()) return false;
        try {
            EmailService emailService = new EmailService();
            return emailService.enviarCorreoConArchivos(destinatario.trim(), nombreCliente, codigo, AppConstants.TIPO_DOCUMENTO_RETENCION, new File(rutaPDF), new File(rutaXML));
        } catch (Exception e) { logDAO.guardar("RetencionService","enviarCorreo","Error enviando correo a "+destinatario+": "+e.getMessage(), e); return false; }
    }
}

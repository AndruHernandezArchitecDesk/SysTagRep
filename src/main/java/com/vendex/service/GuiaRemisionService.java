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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GuiaRemisionService {

    private final EmpresaDAO empresaDAO;
    private final ClienteDAO clienteDAO;
    private final FacturaRegistroDAO facturaRegistroDAO;
    private final SecuenciaDocumentoDAO secuenciaDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final GuiaRemisionRegistroDAO guiaDAO;
    private final GuiaRemisionDestinatarioDAO destDAO;
    private final GuiaRemisionDetalleDAO detalleDAO;
    private final LogDAO logDAO;

    public GuiaRemisionService() {
        this.empresaDAO = new EmpresaDAO();
        this.clienteDAO = new ClienteDAO();
        this.facturaRegistroDAO = new FacturaRegistroDAO();
        this.secuenciaDAO = new SecuenciaDocumentoDAO();
        this.comprobanteDAO = new ComprobanteDAO();
        this.guiaDAO = new GuiaRemisionRegistroDAO();
        this.destDAO = new GuiaRemisionDestinatarioDAO();
        this.detalleDAO = new GuiaRemisionDetalleDAO();
        this.logDAO = new LogDAO();
    }

    public static class DetalleGRInput {
        public String codigoInterno;
        public String descripcion;
        public BigDecimal cantidad;
        public Integer inventarioId;
        public DetalleGRInput(String codigoInterno, String descripcion, BigDecimal cantidad) {
            this.codigoInterno = codigoInterno;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
        }
        public DetalleGRInput(String codigoInterno, String descripcion, BigDecimal cantidad, Integer inventarioId) {
            this(codigoInterno, descripcion, cantidad);
            this.inventarioId = inventarioId;
        }
    }

    public static class DestinatarioGRInput {
        public String identificacionDestinatario;
        public String razonSocialDestinatario;
        public String direccionDestinatario;
        public String motivoTraslado;
        public Integer facturaRegistroId; // opcional
        public List<DetalleGRInput> detalles = new ArrayList<>();
        public DestinatarioGRInput(String identificacion, String razon, String direccion, String motivo) {
            this.identificacionDestinatario = identificacion;
            this.razonSocialDestinatario = razon;
            this.direccionDestinatario = direccion;
            this.motivoTraslado = motivo;
        }
    }

    public static class ResultadoGuiaRemision {
        public final String claveAcceso;
        public final String numComprobante;
        public final String xmlFirmado;
        public final String ambienteSri;
        public final String codEstab;
        public final String codPtoEmi;
        public final int secuencial;
        public final String fechaEmision;
        public final String dirPartida;
        public final String razonSocialTransportista;
        public final String tipoIdTransportista;
        public final String rucTransportista;
        public final String placa;
        public final String fechaIniTransporte;
        public final String fechaFinTransporte;
        public final String rutaPDF;
        public final String rutaXML;
        public final List<Object[]> destinatariosParaXml;
        public final Empresa empresa;
        public final int guiaId;
        public ResultadoGuiaRemision(String claveAcceso, String numComprobante, String xmlFirmado, String ambienteSri,
                                     String codEstab, String codPtoEmi, int secuencial, String fechaEmision,
                                     String dirPartida, String razonSocialTransportista, String tipoIdTransportista,
                                     String rucTransportista, String placa, String fechaIniTransporte, String fechaFinTransporte,
                                     String rutaPDF, String rutaXML, List<Object[]> destinatariosParaXml, Empresa empresa, int guiaId) {
            this.claveAcceso = claveAcceso; this.numComprobante = numComprobante; this.xmlFirmado = xmlFirmado;
            this.ambienteSri = ambienteSri; this.codEstab = codEstab; this.codPtoEmi = codPtoEmi; this.secuencial = secuencial;
            this.fechaEmision = fechaEmision; this.dirPartida = dirPartida; this.razonSocialTransportista = razonSocialTransportista;
            this.tipoIdTransportista = tipoIdTransportista; this.rucTransportista = rucTransportista; this.placa = placa;
            this.fechaIniTransporte = fechaIniTransporte; this.fechaFinTransporte = fechaFinTransporte;
            this.rutaPDF = rutaPDF; this.rutaXML = rutaXML; this.destinatariosParaXml = destinatariosParaXml; this.empresa = empresa; this.guiaId = guiaId;
        }
    }

    public ResultadoGuiaRemision emitirGuiaRemision(List<DestinatarioGRInput> destinatariosInput,
                                                    String dirPartida,
                                                    String razonSocialTransportista, String tipoIdTransportista, String rucTransportista, String placa,
                                                    LocalDate fechaIniTransporte, LocalDate fechaFinTransporte,
                                                    String ambienteSri, String rutaP12, String claveP12,
                                                    File directorioEscritorio, int usuarioId) throws Exception {
        com.vendex.util.SesionActual.exigirPermiso("GUIA_REMISION_EMITIR");
        if (destinatariosInput == null || destinatariosInput.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un destinatario.");
        if (dirPartida == null || dirPartida.trim().isEmpty()) throw new IllegalArgumentException("Dirección de partida es obligatoria.");
        if (razonSocialTransportista == null || razonSocialTransportista.trim().isEmpty()) throw new IllegalArgumentException("Razón social del transportista es obligatoria.");
        if (rucTransportista == null || rucTransportista.trim().isEmpty()) throw new IllegalArgumentException("RUC/identificación del transportista es obligatorio.");
        if (placa == null || placa.trim().isEmpty()) throw new IllegalArgumentException("Placa es obligatoria.");
        if (fechaIniTransporte == null || fechaFinTransporte == null) throw new IllegalArgumentException("Fechas de transporte son obligatorias.");
        if (fechaFinTransporte.isBefore(fechaIniTransporte)) throw new IllegalArgumentException("Fecha fin debe ser >= fecha inicio.");
        if (tipoIdTransportista == null || tipoIdTransportista.trim().isEmpty()) tipoIdTransportista = "04";

        for (DestinatarioGRInput d : destinatariosInput) {
            if (d.identificacionDestinatario == null || d.identificacionDestinatario.trim().isEmpty()) throw new IllegalArgumentException("Identificación destinatario obligatoria.");
            if (d.razonSocialDestinatario == null || d.razonSocialDestinatario.trim().isEmpty()) throw new IllegalArgumentException("Razón social destinatario obligatoria.");
            if (d.direccionDestinatario == null || d.direccionDestinatario.trim().isEmpty()) throw new IllegalArgumentException("Dirección destinatario obligatoria.");
            if (d.motivoTraslado == null || d.motivoTraslado.trim().isEmpty()) throw new IllegalArgumentException("Motivo traslado obligatorio.");
            if (d.detalles == null || d.detalles.isEmpty()) throw new IllegalArgumentException("Cada destinatario debe tener al menos un ítem.");
            for (DetalleGRInput det : d.detalles) {
                if (det.descripcion == null || det.descripcion.trim().isEmpty()) throw new IllegalArgumentException("Descripción del ítem obligatoria.");
                if (det.cantidad == null || det.cantidad.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Cantidad debe ser > 0.");
                if (det.codigoInterno == null || det.codigoInterno.trim().isEmpty()) det.codigoInterno = "001";
            }
            if (d.facturaRegistroId != null) {
                FacturaRegistro fac = facturaRegistroDAO.obtenerPorId(d.facturaRegistroId);
                if (fac == null) throw new IllegalArgumentException("Factura sustento no encontrada id=" + d.facturaRegistroId);
                boolean esPendienteGR = AppConstants.ESTADO_PENDIENTE.equals(fac.getEstadoSri()) || AppConstants.ESTADO_RECIBIDA.equals(fac.getEstadoSri()) || AppConstants.ESTADO_ERROR.equals(fac.getEstadoSri());
                if (!AppConstants.ESTADO_AUTORIZADO.equals(fac.getEstadoSri()) && !(esPendienteGR && com.vendex.util.SRIContingenciaConfig.isModoContingencia())) throw new IllegalStateException("Factura sustento debe estar AUTORIZADA. Estado: " + fac.getEstadoSri() + (esPendienteGR ? " (modo contingencia permite PENDIENTE)" : ""));
            }
        }

        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (empresa == null) throw new IllegalStateException("No se encontraron datos de la empresa.");

        int secuencialGR = secuenciaDAO.marcarUsado("GUIA_REMISION");
        if (secuencialGR == -1) throw new IllegalStateException("No se pudo obtener secuencial GUIA_REMISION.");
        SecuenciaDocumento sec = secuenciaDAO.obtener("GUIA_REMISION");
        String codEstab = sec.getEstablecimiento() != null ? sec.getEstablecimiento() : AppConstants.ESTABLECIMIENTO_DEFAULT;
        String codPtoEmi = sec.getPuntoEmision() != null ? sec.getPuntoEmision() : AppConstants.PUNTO_EMISION_DEFAULT;
        String claveAcceso = ClaveAcceso.generar(AppConstants.TIPO_COMPROBANTE_GUIA_REMISION, empresa.getRuc(), ambienteSri, codEstab, codPtoEmi, secuencialGR);
        String secuencialStr = String.format("%09d", secuencialGR);
        String numComprobante = codEstab + "-" + codPtoEmi + "-" + secuencialStr;
        LocalDateTime ahora = LocalDateTime.now();
        String fechaEmision = ahora.format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
        DateTimeFormatter fmtDate = DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION);
        String fechaIniStr = fechaIniTransporte.format(fmtDate);
        String fechaFinStr = fechaFinTransporte.format(fmtDate);
        String dirMatriz = empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria();
        String dirEstablecimiento = dirMatriz;

        // Preparar destinatarios para XML
        List<Object[]> destinatariosParaXml = new ArrayList<>();
        List<GuiaRemisionDestinatario> destEntities = new ArrayList<>();
        List<List<GuiaRemisionDetalle>> detallesPorDest = new ArrayList<>();

        for (DestinatarioGRInput in : destinatariosInput) {
            String codDocSust = null, numDocSust = null, numAut = null, fechaSust = null;
            if (in.facturaRegistroId != null) {
                FacturaRegistro fac = facturaRegistroDAO.obtenerPorId(in.facturaRegistroId);
                codDocSust = AppConstants.COD_DOC_MODIFICADO_FACTURA; // 01
                numDocSust = fac.getNumComprobante() != null ? fac.getNumComprobante() : fac.getCodigo();
                numAut = fac.getClaveAcceso();
                if (fac.getFecha() != null) fechaSust = fac.getFecha().toLocalDate().format(fmtDate);
            }
            // detalles para XML
            List<Object[]> detallesXml = new ArrayList<>();
            List<GuiaRemisionDetalle> detallesEnt = new ArrayList<>();
            for (DetalleGRInput det : in.detalles) {
                String cod = det.codigoInterno != null ? det.codigoInterno : "001";
                String desc = det.descripcion.length() > 300 ? det.descripcion.substring(0, 300) : det.descripcion;
                String cant = det.cantidad.toPlainString();
                detallesXml.add(new Object[]{cod, desc, cant});
                GuiaRemisionDetalle gd = new GuiaRemisionDetalle();
                gd.setCodigoInterno(cod);
                gd.setDescripcion(desc);
                gd.setCantidad(det.cantidad);
                gd.setInventarioId(det.inventarioId);
                detallesEnt.add(gd);
            }
            Object[] destXml = new Object[]{in.identificacionDestinatario, in.razonSocialDestinatario, in.direccionDestinatario, in.motivoTraslado, codDocSust, numDocSust, numAut, fechaSust, detallesXml};
            destinatariosParaXml.add(destXml);

            GuiaRemisionDestinatario destEnt = new GuiaRemisionDestinatario();
            destEnt.setIdentificacionDestinatario(in.identificacionDestinatario);
            destEnt.setRazonSocialDestinatario(in.razonSocialDestinatario);
            destEnt.setDireccionDestinatario(in.direccionDestinatario);
            destEnt.setMotivoTraslado(in.motivoTraslado);
            destEnt.setFacturaRegistroId(in.facturaRegistroId);
            destEnt.setCodDocSustento(codDocSust);
            destEnt.setNumDocSustento(numDocSust);
            destEnt.setNumAutDocSustento(numAut);
            if (fechaSust != null) destEnt.setFechaEmisionDocSustento(LocalDate.parse(fechaSust, fmtDate));
            destEntities.add(destEnt);
            detallesPorDest.add(detallesEnt);
        }

        String xmlGenerado = XmlGuiaRemisionBuilder.construirGuiaRemision(
                ambienteSri, claveAcceso, empresa.getRuc(), empresa.getRazonSocial(),
                codEstab, codPtoEmi, secuencialGR, dirMatriz, dirEstablecimiento,
                dirPartida, razonSocialTransportista, tipoIdTransportista, rucTransportista, placa,
                fechaIniStr, fechaFinStr, destinatariosParaXml);
        if (xmlGenerado == null) throw new IllegalStateException("No se pudo generar XML de guía de remisión.");

        String xmlFirmado = xmlGenerado;
        if (rutaP12 == null || rutaP12.trim().isEmpty() || claveP12 == null || claveP12.trim().isEmpty())
            throw new IllegalStateException("No se configuró firma electrónica (.p12 y contraseña).");
        try {
            FirmaDigital firma = new FirmaDigital();
            if (!firma.cargarCertificado(rutaP12, claveP12)) throw new IllegalStateException("No se pudo cargar certificado.");
            xmlFirmado = firma.firmarXml(xmlGenerado);
        } catch (Exception e) {
            logDAO.guardar("GuiaRemisionService", "firmarXml", e.getMessage(), e);
            throw new Exception("Error al firmar XML: " + e.getMessage(), e);
        }

        GuiaRemisionRegistro gr = new GuiaRemisionRegistro();
        gr.setClaveAcceso(claveAcceso);
        gr.setEstablecimiento(codEstab);
        gr.setPuntoEmision(codPtoEmi);
        gr.setSecuencial(secuencialStr);
        gr.setFechaEmision(ahora);
        gr.setDirPartida(dirPartida);
        gr.setRazonSocialTransportista(razonSocialTransportista);
        gr.setTipoIdentificacionTransportista(tipoIdTransportista);
        gr.setRucTransportista(rucTransportista);
        gr.setPlaca(placa);
        gr.setFechaIniTransporte(fechaIniTransporte);
        gr.setFechaFinTransporte(fechaFinTransporte);
        gr.setEstadoSri(AppConstants.ESTADO_PENDIENTE);
        gr.setXmlFirmado(xmlFirmado);
        gr.setUsuarioId(usuarioId);
        int guiaId;
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                guiaId = guiaDAO.insertar(con, gr);
                if (guiaId == -1) throw new IllegalStateException("Error al registrar guía de remisión.");
                for (int i = 0; i < destEntities.size(); i++) destEntities.get(i).setGuiaRemisionId(guiaId);
                destDAO.insertarDestinatarios(con, guiaId, destEntities);
                for (int i = 0; i < destEntities.size(); i++) {
                    int destId = destEntities.get(i).getId();
                    List<GuiaRemisionDetalle> detalles = detallesPorDest.get(i);
                    for (GuiaRemisionDetalle d : detalles) d.setGuiaRemisionDestinatarioId(destId);
                    detalleDAO.insertarDetalles(con, destId, detalles);
                }
                comprobanteDAO.insertar(con, claveAcceso, guiaId, numComprobante, ambienteSri, xmlFirmado, AppConstants.TIPO_COMPROBANTE_GUIA_REMISION);
                con.commit();
            } catch (Exception e) { try { con.rollback(); } catch (SQLException re) {} throw e; } finally { try { con.setAutoCommit(true); } catch (SQLException ignore) {} }
        }

        String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_GUIA_REMISION + numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
        String rutaXML = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_GUIA_REMISION + numComprobante.replace("-", "") + AppConstants.EXTENSION_XML;
        try { Files.write(Paths.get(rutaXML), xmlFirmado.getBytes(StandardCharsets.UTF_8)); } catch (IOException e) { logDAO.guardar("GuiaRemisionService","guardarXML", e.getMessage(), e); }

        return new ResultadoGuiaRemision(claveAcceso, numComprobante, xmlFirmado, ambienteSri, codEstab, codPtoEmi, secuencialGR, fechaEmision, dirPartida, razonSocialTransportista, tipoIdTransportista, rucTransportista, placa, fechaIniStr, fechaFinStr, rutaPDF, rutaXML, destinatariosParaXml, empresa, guiaId);
    }

    public SRIWebService.SRIResponse enviarYSolicitarAutorizacion(String ambienteSri, String xmlFirmado, String claveAcceso) {
        try {
            SRIWebService sriWs = new SRIWebService(ambienteSri);
            return sriWs.validarComprobante(xmlFirmado, claveAcceso);
        } catch (Exception e) {
            logDAO.guardar("GuiaRemisionService", "validarSRI", e.getMessage(), e);
            SRIWebService.SRIResponse r = new SRIWebService.SRIResponse();
            r.setEstado(AppConstants.ESTADO_PENDIENTE);
            r.setMensaje("Error conexion: " + e.getMessage());
            return r;
        }
    }

    public void finalizarEnvioSRI(SRIWebService.SRIResponse sriResp, ResultadoGuiaRemision resultado, File directorioEscritorio) {
        String estado = sriResp.getEstado();
        String numAut = sriResp.getNumeroAutorizacion();
        String fechaAut = sriResp.getFechaAutorizacion();
        if ("DEVUELTA".equals(estado) || "RECHAZADA".equals(estado) || "NO AUTORIZADO".equals(estado) || (sriResp.getMensaje()!=null && sriResp.getMensaje().toLowerCase().contains("no cumple"))) {
            try {
                String home = System.getProperty("user.home");
                java.io.File dir = new java.io.File(home, "vendex_errors");
                if (!dir.exists()) dir.mkdirs();
                String base = "GR_" + resultado.numComprobante.replace("-", "") + "_" + resultado.claveAcceso;
                Files.write(Paths.get(new java.io.File(dir, base + "_enviado.xml").getAbsolutePath()), resultado.xmlFirmado.getBytes(StandardCharsets.UTF_8));
                if (sriResp.getRespuestaRecepcionXml()!=null) Files.write(Paths.get(new java.io.File(dir, base + "_recepcion.xml").getAbsolutePath()), sriResp.getRespuestaRecepcionXml().getBytes(StandardCharsets.UTF_8));
                if (sriResp.getRespuestaAutorizacionXml()!=null) Files.write(Paths.get(new java.io.File(dir, base + "_autorizacion.xml").getAbsolutePath()), sriResp.getRespuestaAutorizacionXml().getBytes(StandardCharsets.UTF_8));
                logDAO.guardar("GuiaRemisionService","SRI-DEVUELTA","GR "+resultado.numComprobante+" clave="+resultado.claveAcceso+" estado="+estado+" msg="+sriResp.getMensaje());
            } catch (Exception e) { logDAO.guardar("GuiaRemisionService","volcarXMLError", e.getMessage(), e); }
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                guiaDAO.actualizarEstado(con, resultado.claveAcceso, estado, sriResp.getMensaje(), numAut, fechaAut);
                comprobanteDAO.actualizarEstado(con, resultado.claveAcceso, estado, sriResp.getMensaje(), resultado.xmlFirmado, numAut, fechaAut);
                comprobanteDAO.guardarEnvio(con, resultado.claveAcceso, resultado.numComprobante, resultado.ambienteSri, resultado.xmlFirmado, sriResp.getRespuestaRecepcionXml(), sriResp.getRespuestaAutorizacionXml(), estado, sriResp.getMensaje(), numAut, fechaAut, AppConstants.TIPO_COMPROBANTE_GUIA_REMISION);
                con.commit();
            } catch (Exception e) { try { con.rollback(); } catch (SQLException re) {} throw new RuntimeException(e); } finally { try { con.setAutoCommit(true); } catch (SQLException ignore) {} }
        } catch (SQLException e) { throw new RuntimeException(e); }
        try {
            String rutaPDF = directorioEscritorio.getAbsolutePath() + File.separator + AppConstants.PREFIJO_PDF_GUIA_REMISION + resultado.numComprobante.replace("-", "") + AppConstants.EXTENSION_PDF;
            PdfGuiaRemision.generar(rutaPDF, resultado.claveAcceso, numAut, fechaAut, resultado.ambienteSri,
                    resultado.empresa.getRuc(), resultado.empresa.getRazonSocial(),
                    resultado.empresa.getDireccionCallePrincipal() + " y " + resultado.empresa.getDireccionCalleSecundaria(),
                    resultado.empresa.getTelefono(), resultado.empresa.getCorreo(),
                    resultado.codEstab, resultado.codPtoEmi, resultado.secuencial, resultado.fechaEmision,
                    resultado.dirPartida, resultado.razonSocialTransportista, resultado.tipoIdTransportista, resultado.rucTransportista, resultado.placa,
                    resultado.fechaIniTransporte, resultado.fechaFinTransporte, resultado.destinatariosParaXml);
        } catch (Exception e) { logDAO.guardar("GuiaRemisionService","generarPDF", e.getMessage(), e); }
    }

    public String regenerarRide(String claveAcceso, String numeroAutorizacion, String fechaAutorizacion, File directorioEscritorio) {
        GuiaRemisionRegistro gr = guiaDAO.obtenerPorClave(claveAcceso);
        if (gr == null) { logDAO.guardar("GuiaRemisionService","regenerarRide","GR no encontrada "+claveAcceso); return null; }
        Empresa empresa = empresaDAO.listar().isEmpty() ? null : empresaDAO.listar().get(0);
        if (empresa == null) return null;
        String fechaEmision = gr.getFechaEmision()!=null? gr.getFechaEmision().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION)) : "";
        String[] partes = gr.getSecuencial()!=null? new String[]{gr.getEstablecimiento(), gr.getPuntoEmision(), gr.getSecuencial()} : (gr.getNumComprobante()!=null? gr.getNumComprobante().split("-"): new String[]{"001","001","000000000"});
        String codEstab = partes.length>0?partes[0]:"001"; String codPtoEmi = partes.length>1?partes[1]:"001"; int sec = partes.length>2? Integer.parseInt(partes[2].replaceAll("\\D","")):0;
        List<GuiaRemisionDestinatario> dests = destDAO.listarPorGuiaId(gr.getId());
        List<Object[]> destParaXml = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION);
        for (GuiaRemisionDestinatario d : dests) {
            List<GuiaRemisionDetalle> dets = detalleDAO.listarPorDestinatarioId(d.getId());
            List<Object[]> detsXml = new ArrayList<>();
            for (GuiaRemisionDetalle det : dets) detsXml.add(new Object[]{det.getCodigoInterno(), det.getDescripcion(), det.getCantidad().toPlainString()});
            String fechaSust = d.getFechaEmisionDocSustento()!=null? d.getFechaEmisionDocSustento().format(fmt): null;
            destParaXml.add(new Object[]{d.getIdentificacionDestinatario(), d.getRazonSocialDestinatario(), d.getDireccionDestinatario(), d.getMotivoTraslado(), d.getCodDocSustento(), d.getNumDocSustento(), d.getNumAutDocSustento(), fechaSust, detsXml});
        }
        String dirPartida = gr.getDirPartida();
        String fechaIni = gr.getFechaIniTransporte()!=null? gr.getFechaIniTransporte().format(fmt): "";
        String fechaFin = gr.getFechaFinTransporte()!=null? gr.getFechaFinTransporte().format(fmt): "";
        String rutaPDF = directorioEscritorio.getAbsolutePath()+File.separator+AppConstants.PREFIJO_PDF_GUIA_REMISION+gr.getNumComprobante().replace("-","")+AppConstants.EXTENSION_PDF;
        PdfGuiaRemision.generar(rutaPDF, claveAcceso, numeroAutorizacion, fechaAutorizacion, gr.getEstadoSri()!=null? "PRUEBAS": "PRUEBAS",
                empresa.getRuc(), empresa.getRazonSocial(), empresa.getDireccionCallePrincipal()+" y "+empresa.getDireccionCalleSecundaria(),
                empresa.getTelefono(), empresa.getCorreo(),
                codEstab, codPtoEmi, sec, fechaEmision, dirPartida, gr.getRazonSocialTransportista(), gr.getTipoIdentificacionTransportista(), gr.getRucTransportista(), gr.getPlaca(),
                fechaIni, fechaFin, destParaXml);
        return rutaPDF;
    }

    public boolean enviarCorreoAutorizacion(String destinatario, String nombreCliente, String codigo, String rutaPDF, String rutaXML) {
        if (destinatario==null||destinatario.trim().isEmpty()) return false;
        try {
            EmailService emailService = new EmailService();
            return emailService.enviarCorreoConArchivos(destinatario.trim(), nombreCliente, codigo, AppConstants.TIPO_DOCUMENTO_GUIA_REMISION, new File(rutaPDF), new File(rutaXML));
        } catch (Exception e) { logDAO.guardar("GuiaRemisionService","enviarCorreo","Error enviando correo a "+destinatario+": "+e.getMessage(), e); return false; }
    }
}

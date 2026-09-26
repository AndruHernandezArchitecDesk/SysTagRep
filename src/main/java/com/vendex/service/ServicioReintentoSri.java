package com.vendex.service;

import com.vendex.dao.ComprobanteDAO;
import com.vendex.dao.ComprobantePendienteSriDAO;
import com.vendex.dao.FacturaRegistroDAO;
import com.vendex.dao.GuiaRemisionRegistroDAO;
import com.vendex.dao.NotaCreditoRegistroDAO;
import com.vendex.dao.NotaDebitoRegistroDAO;
import com.vendex.dao.RetencionRegistroDAO;
import com.vendex.model.ComprobantePendienteSri;
import com.vendex.util.AppConstants;
import com.vendex.util.SRIContingenciaConfig;
import com.vendex.util.SRIWebService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.vendex.dao.FacturaRegistroDAOPostgres;
import com.vendex.dao.ComprobanteDAOPostgres;
import com.vendex.dao.NotaCreditoRegistroDAOPostgres;
import com.vendex.dao.NotaDebitoRegistroDAOPostgres;
import com.vendex.dao.GuiaRemisionRegistroDAOPostgres;
import com.vendex.dao.RetencionRegistroDAOPostgres;

/**
 * Job de fondo SRI contingencia: revisa comprobante_pendiente_sri cada 2 min,
 * reintenta solo los que esperan autorizacion (PENDIENTE/RECIBIDA/ERROR/timeout),
 * nunca RECHAZADA/DEVUELTA. Backoff creciente hasta AGOTADA a las 24h (~30 intentos).
 * Todo a cola: la emision ya no espera SRI, este servicio se encarga de todo.
 */
public class ServicioReintentoSri {

    private static final Logger LOG = Logger.getLogger(ServicioReintentoSri.class.getName());
    private static ServicioReintentoSri INSTANCE;
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private final ComprobantePendienteSriDAO pendienteDAO = new ComprobantePendienteSriDAO();
    private final ComprobanteDAO comprobanteDAO = new ComprobanteDAOPostgres();

    // DAOs para actualizar estado por tipo
    private final FacturaRegistroDAO facturaDAO = new FacturaRegistroDAOPostgres();
    private final NotaCreditoRegistroDAO ncDAO = new NotaCreditoRegistroDAOPostgres();
    private final NotaDebitoRegistroDAO ndDAO = new NotaDebitoRegistroDAOPostgres();
    private final GuiaRemisionRegistroDAO grDAO = new GuiaRemisionRegistroDAOPostgres();
    private final RetencionRegistroDAO retDAO = new RetencionRegistroDAOPostgres();

    public static synchronized ServicioReintentoSri getInstance() {
        if (INSTANCE == null) INSTANCE = new ServicioReintentoSri();
        return INSTANCE;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sri-reintento");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::ciclo, 0, 2, TimeUnit.MINUTES);
        LOG.info("ServicioReintentoSri iniciado (ciclo 2min, backoff hasta 24h AGOTADA)");
    }

    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    // visible para tests
    public void ciclo() {
        try {
            List<ComprobantePendienteSri> pendientes = pendienteDAO.listarParaReintentar(20);
            if (pendientes.isEmpty()) return;
            LOG.info("SRI reintento ciclo: " + pendientes.size() + " pendientes");
            for (ComprobantePendienteSri p : pendientes) {
                procesarUno(p);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error ciclo SRI", e);
        }
    }

    private void procesarUno(ComprobantePendienteSri p) {
        try {
            String ambiente = p.getAmbiente();
            if (ambiente == null || ambiente.isBlank()) ambiente = AppConstants.AMBIENTE_PRUEBAS;
            SRIWebService sri = new SRIWebService(ambiente);
            SRIWebService.SRIResponse resp = sri.consultarAutorizacion(p.getClaveAcceso());
            String estado = resp.getEstado();
            String mensaje = resp.getMensaje();
            LOG.info("SRI reintento " + p.getTipoComprobante() + " " + p.getClaveAcceso() + " -> " + estado + " intento#" + p.getIntentos());

            if (AppConstants.ESTADO_AUTORIZADO.equals(estado)) {
                // actualizar comprobantes_electronicos y registro especifico
                comprobanteDAO.actualizarEstado(p.getClaveAcceso(), AppConstants.ESTADO_AUTORIZADO, mensaje, null, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion());
                actualizarRegistroTipo(p, AppConstants.ESTADO_AUTORIZADO, mensaje, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion());
                comprobanteDAO.guardarEnvio(p.getClaveAcceso(), p.getNumeroComprobante(), ambiente, null, resp.getRespuestaRecepcionXml(), resp.getRespuestaAutorizacionXml(), AppConstants.ESTADO_AUTORIZADO, mensaje, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion(), mapTipo(p.getTipoComprobante()));
                // regenerar RIDE con autorizacion
                regenerarRide(p, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion());
                // email al cliente
                enviarCorreoCliente(p, resp.getNumeroAutorizacion(), resp.getFechaAutorizacion());
                pendienteDAO.marcarResultado(p.getId(), AppConstants.ESTADO_AUTORIZADO, mensaje, null, p.getIntentos() + 1);
                LOG.info("SRI autorizado " + p.getClaveAcceso() + " -> marcado AUTORIZADO, RIDE regenerado");

            } else if (AppConstants.ESTADO_RECHAZADA.equals(estado) || AppConstants.ESTADO_DEVUELTA.equals(estado) || "NO AUTORIZADO".equals(estado)) {
                // NO reintentar — requiere correccion manual
                comprobanteDAO.actualizarEstado(p.getClaveAcceso(), estado, mensaje, null, null, null);
                actualizarRegistroTipo(p, estado, mensaje, null, null);
                comprobanteDAO.guardarEnvio(p.getClaveAcceso(), p.getNumeroComprobante(), ambiente, null, resp.getRespuestaRecepcionXml(), resp.getRespuestaAutorizacionXml(), estado, mensaje, null, null, mapTipo(p.getTipoComprobante()));
                pendienteDAO.marcarResultado(p.getId(), AppConstants.ESTADO_RECHAZADA, mensaje, null, p.getIntentos() + 1);
                notificarRechazada(p, estado, mensaje);
                LOG.warning("SRI rechazada " + p.getClaveAcceso() + " estado=" + estado + " -> no reintentar, notificado");

            } else if (AppConstants.ESTADO_RECIBIDA.equals(estado) || AppConstants.ESTADO_PENDIENTE.equals(estado) || AppConstants.ESTADO_ERROR.equals(estado) || estado == null || estado.isBlank()) {
                // reintentar con backoff
                int nuevosIntentos = p.getIntentos() + 1;
                if (nuevosIntentos > 30) {
                    pendienteDAO.marcarResultado(p.getId(), AppConstants.ESTADO_AGOTADA, "Agotado tras 24h (~30 intentos): " + mensaje, null, nuevosIntentos);
                    comprobanteDAO.guardarEnvio(p.getClaveAcceso(), p.getNumeroComprobante(), ambiente, null, resp.getRespuestaRecepcionXml(), resp.getRespuestaAutorizacionXml(), AppConstants.ESTADO_AGOTADA, "AGOTADA: " + mensaje, null, null, mapTipo(p.getTipoComprobante()));
                    notificarAgotada(p, mensaje);
                    LOG.warning("SRI AGOTADA " + p.getClaveAcceso() + " tras " + nuevosIntentos + " intentos");
                } else {
                    LocalDateTime proximo = calcularProximoIntento(nuevosIntentos);
                    pendienteDAO.marcarReintento(p.getId(), nuevosIntentos, proximo, mensaje);
                    LOG.info("SRI reintento programado " + p.getClaveAcceso() + " intentos=" + nuevosIntentos + " proximo=" + proximo);
                }
            } else {
                // estado desconocido -> tratar como pendiente con backoff
                int nuevosIntentos = p.getIntentos() + 1;
                LocalDateTime proximo = calcularProximoIntento(nuevosIntentos);
                pendienteDAO.marcarReintento(p.getId(), nuevosIntentos, proximo, mensaje);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error procesando " + p.getClaveAcceso(), e);
            int nuevosIntentos = p.getIntentos() + 1;
            if (nuevosIntentos > 30) {
                pendienteDAO.marcarResultado(p.getId(), AppConstants.ESTADO_AGOTADA, "Excepcion: " + e.getMessage(), null, nuevosIntentos);
                notificarAgotada(p, e.getMessage());
            } else {
                LocalDateTime proximo = calcularProximoIntento(nuevosIntentos);
                pendienteDAO.marcarReintento(p.getId(), nuevosIntentos, proximo, "Excepcion: " + e.getMessage());
            }
        }
    }

    static LocalDateTime calcularProximoIntento(int intentos) {
        // intentos es el nuevo conteo (1..30)
        long minutos;
        if (intentos <= 5) minutos = 2;
        else if (intentos <= 15) minutos = 15;
        else minutos = 60;
        return LocalDateTime.now().plusMinutes(minutos);
    }

    private String mapTipo(String tipo) {
        if (tipo == null) return "01";
        return switch (tipo) {
            case "FACTURA" -> "01";
            case "NOTA_CREDITO" -> "04";
            case "NOTA_DEBITO" -> "05";
            case "GUIA_REMISION" -> "06";
            case "RETENCION" -> "07";
            default -> "01";
        };
    }

    private void actualizarRegistroTipo(ComprobantePendienteSri p, String estado, String mensaje, String numAut, String fechaAut) {
        try {
            String clave = p.getClaveAcceso();
            switch (p.getTipoComprobante()) {
                case "FACTURA" -> facturaDAO.actualizarEstado(clave, estado);
                case "NOTA_CREDITO" -> ncDAO.actualizarEstado(clave, estado, mensaje, numAut, fechaAut);
                case "NOTA_DEBITO" -> ndDAO.actualizarEstado(clave, estado, mensaje, numAut, fechaAut);
                case "GUIA_REMISION" -> grDAO.actualizarEstado(clave, estado, mensaje, numAut, fechaAut);
                case "RETENCION" -> retDAO.actualizarEstado(clave, estado, mensaje, numAut, fechaAut);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "actualizarRegistroTipo " + p.getTipoComprobante(), e);
        }
    }

    private void regenerarRide(ComprobantePendienteSri p, String numAut, String fechaAut) {
        try {
            File dir = obtenerDirectorioEscritorio();
            switch (p.getTipoComprobante()) {
                case "FACTURA" -> {
                    FacturaService fs = new FacturaService();
                    fs.regenerarRide(p.getClaveAcceso(), numAut, fechaAut, dir);
                }
                case "NOTA_CREDITO" -> {
                    NotaCreditoService s = new NotaCreditoService();
                    s.regenerarRide(p.getClaveAcceso(), numAut, fechaAut, dir);
                }
                case "NOTA_DEBITO" -> {
                    NotaDebitoService s = new NotaDebitoService();
                    s.regenerarRide(p.getClaveAcceso(), numAut, fechaAut, dir);
                }
                case "GUIA_REMISION" -> {
                    GuiaRemisionService s = new GuiaRemisionService();
                    s.regenerarRide(p.getClaveAcceso(), numAut, fechaAut, dir);
                }
                case "RETENCION" -> {
                    RetencionService s = new RetencionService();
                    s.regenerarRide(p.getClaveAcceso(), numAut, fechaAut, dir);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "regenerarRide " + p.getClaveAcceso(), e);
        }
    }

    private File obtenerDirectorioEscritorio() {
        File home = new File(System.getProperty("user.home"));
        for (String n : new String[]{AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT, AppConstants.DIRECTORIO_ESCRITORIO_ALT}) {
            File d = new File(home, n);
            if (d.exists() && d.isDirectory()) return d;
        }
        File d = new File(home, AppConstants.DIRECTORIO_ESCRITORIO_DEFAULT);
        d.mkdirs();
        return d;
    }

    private void enviarCorreoCliente(ComprobantePendienteSri p, String numAut, String fechaAut) {
        // solo si tiene correo cliente — recuperar via factura/registro
        // simplificado: no enviar automaticamente desde job si no tenemos destinatario; el regenerarRide ya deja PDF listo
        // Se puede extender para enviar si se guarda destinatario en xml_enviados
        try {
            // intentar enviar si el comprobante original tenia cliente con email — delegado a service especifico si expone
            // por ahora loguear
            LOG.info("Correo cliente para " + p.getClaveAcceso() + " no enviado automaticamente desde job (requiere destinatario). RIDE regenerado en escritorio.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "enviarCorreoCliente", e);
        }
    }

    private void notificarRechazada(ComprobantePendienteSri p, String estado, String mensaje) {
        String email = SRIContingenciaConfig.getEmailDestino();
        if (email.isBlank()) { LOG.warning("Sin email contingencia para RECHAZADA " + p.getClaveAcceso()); return; }
        String subject = "[SRI RECHAZADA] " + p.getTipoComprobante() + " " + p.getNumeroComprobante();
        String body = "Comprobante RECHAZADO por el SRI y no se reintentara (requiere correccion manual).\n\n" +
                      "Tipo: " + p.getTipoComprobante() + "\n" +
                      "Numero: " + p.getNumeroComprobante() + "\n" +
                      "Clave: " + p.getClaveAcceso() + "\n" +
                      "Estado SRI: " + estado + "\n" +
                      "Mensaje: " + mensaje + "\n\n" +
                      "Accion: corregir datos y volver a emitir.";
        enviarEmailSimple(email, subject, body);
    }

    private void notificarAgotada(ComprobantePendienteSri p, String mensaje) {
        String email = SRIContingenciaConfig.getEmailDestino();
        if (email.isBlank()) { LOG.warning("Sin email contingencia para AGOTADA " + p.getClaveAcceso()); return; }
        String subject = "[SRI AGOTADA 24h] " + p.getTipoComprobante() + " " + p.getNumeroComprobante();
        String body = "Comprobante lleva 24h (~30 intentos) sin autorizacion SRI.\n\n" +
                      "Tipo: " + p.getTipoComprobante() + "\n" +
                      "Numero: " + p.getNumeroComprobante() + "\n" +
                      "Clave: " + p.getClaveAcceso() + "\n" +
                      "Intentos: 30\n" +
                      "Ultimo mensaje: " + mensaje + "\n\n" +
                      "Accion manual requerida. Verificar SRI y reintentar desde Seguimiento SRI.";
        enviarEmailSimple(email, subject, body);
    }

    private void enviarEmailSimple(String destino, String subject, String body) {
        try {
            var dao = new com.vendex.dao.ConfiguracionEmailDAO();
            var opt = dao.obtenerActiva();
            if (opt.isEmpty()) { LOG.warning("Sin configuracion_email para notificar SRI"); return; }
            var cfg = opt.get();
            String pass = dao.obtenerPasswordPlano(cfg);
            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", cfg.getHostSmtp());
            props.put("mail.smtp.port", String.valueOf(cfg.getPuertoSmtp()));
            props.put("mail.smtp.ssl.trust", cfg.getHostSmtp());
            props.put("mail.smtp.connectiontimeout", "15000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "15000");
            if (cfg.getPuertoSmtp() == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
            } else {
                props.put("mail.smtp.starttls.enable", String.valueOf(cfg.isUsarTls()));
            }
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(cfg.getUsuarioSmtp(), pass);
                }
            });
            jakarta.mail.Message msg = new jakarta.mail.internet.MimeMessage(session);
            msg.setFrom(new jakarta.mail.internet.InternetAddress(cfg.getEmailRemitente(), cfg.getNombreRemitente() != null ? cfg.getNombreRemitente() : "Vendex"));
            msg.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(destino));
            msg.setSubject(subject);
            msg.setText(body);
            jakarta.mail.Transport.send(msg);
            LOG.info("Notificacion SRI enviada a " + destino + " subject=" + subject);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Fallo envio notificacion SRI a " + destino, e);
        }
    }

    public int contarPendientes() {
        return pendienteDAO.contarPendientes();
    }
}
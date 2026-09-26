package com.vendex.service;

import com.vendex.dao.CertificadoEstadoDAO;
import com.vendex.util.CertificadoDigitalInfo;
import com.vendex.util.ConfigFirma;
import com.vendex.util.EmailService;
import com.vendex.dao.EmpresaDAO;
import com.vendex.model.Empresa;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.vendex.dao.EmpresaDAOPostgres;
import com.vendex.dao.CertificadoEstadoDAOPostgres;

public class CertificadoAlertaService {

    private static final Logger LOGGER = Logger.getLogger(CertificadoAlertaService.class.getName());
    private final CertificadoEstadoDAO dao = new CertificadoEstadoDAOPostgres();

    public static class ResultadoAlerta {
        public final CertificadoDigitalInfo info;
        public final boolean debeMostrarBanner;
        public final boolean debeMostrarModal;
        public final boolean debeEnviarCorreo;
        public final String mensajeBanner;
        public final String mensajeModal;
        public final String mensajeCorreo;
        ResultadoAlerta(CertificadoDigitalInfo info, boolean banner, boolean modal, boolean correo, String msgBanner, String msgModal, String msgCorreo) {
            this.info = info; this.debeMostrarBanner = banner; this.debeMostrarModal = modal; this.debeEnviarCorreo = correo;
            this.mensajeBanner = msgBanner; this.mensajeModal = msgModal; this.mensajeCorreo = msgCorreo;
        }
    }

    public ResultadoAlerta verificarEstadoCertificado() {
        String[] cfg = ConfigFirma.cargar();
        String ruta = cfg[0];
        String clave = cfg[1];
        if (ruta == null || ruta.trim().isEmpty()) return null;
        try {
            CertificadoDigitalInfo info = CertificadoDigitalInfo.leer(ruta, clave);
            long dias = info.getDiasRestantes();
            String nivel = info.getNivel().name();
            dao.guardarEstado(ruta, info.getTitular(), info.getFechaEmision(), info.getFechaExpiracion(), (int) dias, nivel);

            boolean banner = false, modal = false, correo = false;
            String msgBanner = null, msgModal = null, msgCorreo = null;

            switch (info.getNivel()) {
                case VIGENTE: break;
                case AVISO:
                    banner = true;
                    msgBanner = "Tu certificado de firma vence en " + dias + " días (" + info.getFechaExpiracion() + "). Titular: " + extraerCN(info.getTitular());
                    break;
                case ADVERTENCIA:
                    banner = true;
                    msgBanner = "⚠ Certificado vence en " + dias + " días (" + info.getFechaExpiracion() + ") — Titular: " + extraerCN(info.getTitular());
                    msgCorreo = "Vendex Alerta ADVERTENCIA: certificado " + extraerCN(info.getTitular()) + " vence en " + dias + " días (" + info.getFechaExpiracion() + "). Renueva con tiempo para evitar cortes de facturación.";
                    Timestamp ultimo = dao.obtenerUltimoEmailEnviado(ruta);
                    if (dao.debeEnviarCorreo(ruta, nivel, ultimo)) correo = true;
                    break;
                case CRITICO:
                    banner = true; modal = true;
                    msgBanner = "🔴 CRÍTICO: certificado vence en " + dias + " días (" + info.getFechaExpiracion() + ").";
                    msgModal = "Tu certificado de firma electrónica vence en " + dias + " días.\nTitular: " + extraerCN(info.getTitular()) + "\nExpira: " + info.getFechaExpiracion() + "\n\nRenueva hoy para no interrumpir la facturación.";
                    msgCorreo = "Vendex CRÍTICO: certificado vence en " + dias + " días (" + info.getFechaExpiracion() + "). Acción inmediata requerida.";
                    Timestamp u2 = dao.obtenerUltimoEmailEnviado(ruta);
                    if (dao.debeEnviarCorreo(ruta, nivel, u2)) correo = true;
                    break;
                case EXPIRADO:
                    banner = true; modal = true;
                    msgBanner = "⛔ CERTIFICADO EXPIRADO desde " + info.getFechaExpiracion() + " (" + Math.abs(dias) + " días). La facturación electrónica NO funcionará.";
                    msgModal = "⛔ CERTIFICADO EXPIRADO\nTitular: " + extraerCN(info.getTitular()) + "\nExpiró: " + info.getFechaExpiracion() + " (" + Math.abs(dias) + " días atrás)\n\nLa facturación electrónica está BLOQUEADA hasta que renueves el .p12.\nEl SRI rechazará toda firma con error de certificado expirado.";
                    msgCorreo = "Vendex EXPIRADO: certificado expiró el " + info.getFechaExpiracion() + " (" + Math.abs(dias) + " días). Facturación bloqueada.";
                    Timestamp u3 = dao.obtenerUltimoEmailEnviado(ruta);
                    if (dao.debeEnviarCorreo(ruta, nivel, u3)) correo = true;
                    break;
            }

            if (correo) {
                try {
                    String adminEmail = obtenerEmailAdmin();
                    if (adminEmail != null && !adminEmail.isEmpty()) {
                        boolean ok = enviarCorreoCertificado(adminEmail, msgCorreo, info);
                        if (ok) dao.actualizarUltimoEmailEnviado(ruta);
                    }
                } catch (Exception e) { LOGGER.log(Level.WARNING, "correo alerta cert", e); }
            }

            return new ResultadoAlerta(info, banner, modal, correo, msgBanner, msgModal, msgCorreo);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("contraseña del .p12 incorrecta") || msg.contains("keystore password was incorrect") || msg.contains("badpaddingexception")) {
                LOGGER.log(Level.INFO, "verificar certificado: contraseña .p12 incorrecta (reconfigure en Firma Electrónica) - ruta: " + ruta);
                return null;
            }
            LOGGER.log(Level.WARNING, "verificar certificado", e);
            // persistir como error para que quede rastro
            try { dao.guardarEstado(ruta, e.getMessage(), null, LocalDate.now().plusDays(999), 999, "VIGENTE"); } catch (Exception ignore) {}
            return null;
        }
    }

    private boolean enviarCorreoCertificado(String destinatario, String cuerpo, CertificadoDigitalInfo info) {
        try {
            EmailService es = new EmailService();
            // reutiliza plantilla CERTIFICADO_POR_EXPIRAR via tipoDocumento
            java.io.File pdf = null; java.io.File xml = null;
            return es.enviarCorreoConArchivos(destinatario, extraerCN(info.getTitular()), "Certificado vence " + info.getFechaExpiracion() + " (" + info.getDiasRestantes() + " días)", "CERTIFICADO_POR_EXPIRAR", pdf, xml);
        } catch (Exception e) { return false; }
    }

    private String obtenerEmailAdmin() {
        try {
            Empresa emp = new EmpresaDAOPostgres().listar().stream().findFirst().orElse(null);
            if (emp != null && emp.getCorreo() != null && !emp.getCorreo().trim().isEmpty()) return emp.getCorreo().trim();
        } catch (Exception ignore) {}
        return "tagrepuestosvick@gmail.com";
    }

    private String extraerCN(String dn) {
        if (dn == null) return "";
        // CN=Juan Perez, O=... -> Juan Perez
        for (String part : dn.split(",")) {
            String t = part.trim();
            if (t.startsWith("CN=")) return t.substring(3);
        }
        return dn.length() > 80 ? dn.substring(0,80) : dn;
    }
}
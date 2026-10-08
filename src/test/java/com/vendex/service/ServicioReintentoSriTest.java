package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.ComprobantePendienteSri;
import com.vendex.util.AppConstants;
import com.vendex.util.SRIWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El reintento debe TRANSMITIR (recepcion) antes de consultar autorizacion:
 * sin XML recibido por el SRI, la autorizacion queda PENDIENTE eternamente.
 */
class ServicioReintentoSriTest {

    private ComprobantePendienteSriDAO pendienteDAO;
    private ComprobanteDAO comprobanteDAO;
    private FacturaRegistroDAO facturaDAO;

    private static class SriFalso extends SRIWebService {
        String estadoRecepcion = "RECIBIDA";
        String mensajeRecepcion = "OK";
        String estadoAutorizacion = "AUTORIZADO";
        final AtomicInteger llamadasRecepcion = new AtomicInteger();
        final AtomicInteger llamadasAutorizacion = new AtomicInteger();

        SriFalso() { super("PRUEBAS"); }

        @Override
        public SRIResponse enviarComprobante(String xmlFirmado) {
            llamadasRecepcion.incrementAndGet();
            SRIResponse r = new SRIResponse();
            r.setEstado(estadoRecepcion);
            r.setMensaje(mensajeRecepcion);
            r.setRespuestaRecepcionXml("<recepcion>ok</recepcion>");
            return r;
        }

        @Override
        public SRIResponse consultarAutorizacion(String claveAcceso) {
            llamadasAutorizacion.incrementAndGet();
            SRIResponse r = new SRIResponse();
            r.setEstado(estadoAutorizacion);
            r.setMensaje("aut");
            r.setNumeroAutorizacion("123");
            r.setFechaAutorizacion("2026-10-07T12:00:00-05:00");
            return r;
        }
    }

    private SriFalso sri;
    private ServicioReintentoSri servicio;

    @BeforeEach
    void setUp() {
        pendienteDAO = mock(ComprobantePendienteSriDAO.class);
        comprobanteDAO = mock(ComprobanteDAO.class);
        facturaDAO = mock(FacturaRegistroDAO.class);
        sri = new SriFalso();
        servicio = new ServicioReintentoSri(pendienteDAO, comprobanteDAO,
                facturaDAO, mock(NotaCreditoRegistroDAO.class), mock(NotaDebitoRegistroDAO.class),
                mock(GuiaRemisionRegistroDAO.class), mock(RetencionRegistroDAO.class)) {
            @Override
            protected SRIWebService crearSriWebService(String ambiente) {
                return sri;
            }
        };
    }

    private ComprobantePendienteSri pendiente(String xml) {
        ComprobantePendienteSri p = new ComprobantePendienteSri();
        p.setId(1);
        p.setTipoComprobante("FACTURA");
        p.setClaveAcceso("CLAVE123");
        p.setNumeroComprobante("001-001-000000001");
        p.setAmbiente("PRUEBAS");
        p.setIntentos(5);
        p.setEstado(AppConstants.ESTADO_PENDIENTE);
        when(comprobanteDAO.obtenerXmlFirmado("CLAVE123")).thenReturn(xml);
        when(pendienteDAO.listarParaReintentar(20)).thenReturn(List.of(p));
        return p;
    }

    @Test
    void reintento_transmiteAntesDeConsultar_yAutoriza() {
        pendiente("<firmado/>");

        servicio.ciclo();

        assertEquals(1, sri.llamadasRecepcion.get(), "Debe transmitir (recepcion) antes de consultar");
        assertEquals(1, sri.llamadasAutorizacion.get(), "Debe consultar autorizacion tras RECIBIDA");
        verify(pendienteDAO).marcarResultado(eq(1), eq(AppConstants.ESTADO_AUTORIZADO), any(), argThat(java.util.Objects::nonNull), eq(6));
        verify(comprobanteDAO).actualizarEstado(eq("CLAVE123"), eq(AppConstants.ESTADO_AUTORIZADO), any(), isNull(), eq("123"), any());
    }

    @Test
    void recepcionDevueltaReal_noConsultaYMarcaRechazada() {
        sri.estadoRecepcion = "DEVUELTA";
        sri.mensajeRecepcion = "RUC del emisor no corresponde al certificado";
        pendiente("<firmado/>");

        servicio.ciclo();

        assertEquals(1, sri.llamadasRecepcion.get());
        assertEquals(0, sri.llamadasAutorizacion.get(), "Con DEVUELTA real no debe consultar");
        verify(pendienteDAO).marcarResultado(eq(1), eq(AppConstants.ESTADO_RECHAZADA), contains("DEVUELTA"), argThat(java.util.Objects::nonNull), eq(6));
    }

    @Test
    void recepcionYaRegistrada_sigueAConsultar() {
        sri.estadoRecepcion = "DEVUELTA";
        sri.mensajeRecepcion = "CLAVE DE ACCESO REGISTRADA, ya fue recibida";
        pendiente("<firmado/>");

        servicio.ciclo();

        assertEquals(1, sri.llamadasRecepcion.get());
        assertEquals(1, sri.llamadasAutorizacion.get(), "Clave ya registrada debe seguir a consultar");
        verify(pendienteDAO).marcarResultado(eq(1), eq(AppConstants.ESTADO_AUTORIZADO), any(), argThat(java.util.Objects::nonNull), eq(6));
    }

    @Test
    void sinXmlFirmado_soloConsultaPorCompatibilidad() {
        pendiente(null);

        servicio.ciclo();

        assertEquals(0, sri.llamadasRecepcion.get(), "Sin XML no hay nada que transmitir");
        assertEquals(1, sri.llamadasAutorizacion.get(), "Igual consulta por compatibilidad");
    }

    @Test
    void recepcionSecuencialRegistradoMasculino_sigueAConsultar() {
        sri.estadoRecepcion = "DEVUELTA";
        sri.mensajeRecepcion = "[ERROR] ERROR SECUENCIAL REGISTRADO";
        pendiente("<firmado/>");

        servicio.ciclo();

        assertEquals(1, sri.llamadasRecepcion.get());
        assertEquals(1, sri.llamadasAutorizacion.get(),
                "SECUENCIAL REGISTRADO significa ya recibido: debe consultar, no rechazar");
        verify(pendienteDAO).marcarResultado(eq(1), eq(AppConstants.ESTADO_AUTORIZADO), any(), any(), eq(6));
    }

    @Test
    void marcadoTerminal_usaProximoNoNulo() {
        sri.estadoRecepcion = "DEVUELTA";
        sri.mensajeRecepcion = "RUC no corresponde";
        pendiente("<firmado/>");

        servicio.ciclo();

        verify(pendienteDAO).marcarResultado(eq(1), eq(AppConstants.ESTADO_RECHAZADA), any(),
                argThat(java.util.Objects::nonNull), eq(6));
    }

    @Test
    void truncarMensaje_cortaSinRomperNulos() {
        assertNull(ServicioReintentoSri.truncarMensaje(null));
        assertEquals("abc", ServicioReintentoSri.truncarMensaje("abc"));
        String largo = "x".repeat(2000);
        String t = ServicioReintentoSri.truncarMensaje(largo);
        assertTrue(t.length() <= 470, "Truncado debe caber en VARCHAR(500)");
        assertTrue(t.endsWith("...[truncado]"));
    }
}

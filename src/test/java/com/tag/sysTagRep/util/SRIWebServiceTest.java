package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SRIWebServiceTest {

    private static class TestableSRIWebService extends SRIWebService {
        private String recepcionResponse;
        private String autorizacionResponse;

        TestableSRIWebService(String recepcionResponse, String autorizacionResponse) {
            this.recepcionResponse = recepcionResponse;
            this.autorizacionResponse = autorizacionResponse;
        }

        @Override
        protected String postSoap(String url, String soapBody) {
            if (url.contains("RecepcionComprobantesOffline")) {
                return recepcionResponse;
            }
            if (url.contains("AutorizacionComprobantesOffline")) {
                return autorizacionResponse;
            }
            return null;
        }
    }

    @Test
    void validarComprobante_recepcionRecibidaYAutorizacionAutorizado_retornaAutorizado() {
        String recepcion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><rece:validarComprobanteResponse xmlns:rece=\"http://ec.gob.sri.ws.recepcion\"><respuesta><estado>RECIBIDA</estado><comprobantes><comprobante><mensajes><mensaje><tipo>INFORMACION</tipo><mensaje>Comprobante recibido</mensaje></mensaje></mensajes></comprobante></comprobantes></respuesta></rece:validarComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        String autorizacion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><aut:autorizacionComprobanteResponse xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\"><respuestaAutorizacionComprobante><estado>AUTORIZADO</estado><numeroAutorizacion>1234567890123456789012345678901234567890123456789</numeroAutorizacion><fechaAutorizacion>2026-08-08 12:00:00</fechaAutorizacion><comprobantes><comprobante><mensajes><mensaje><tipo>INFORMACION</tipo><mensaje>Autorizado</mensaje></mensaje></mensajes></comprobante></comprobantes></respuestaAutorizacionComprobante></aut:autorizacionComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        TestableSRIWebService service = new TestableSRIWebService(recepcion, autorizacion);
        SRIWebService.SRIResponse response = service.validarComprobante("<xml>test</xml>", "1234567890123456789012345678901234567890123456789");
        assertEquals("AUTORIZADO", response.getEstado());
    }

    @Test
    void enviarComprobante_recepcionRecibida_retornaRecibida() {
        String recepcion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><rece:validarComprobanteResponse xmlns:rece=\"http://ec.gob.sri.ws.recepcion\"><respuesta><estado>RECIBIDA</estado><comprobantes><comprobante><mensajes><mensaje><tipo>INFORMACION</tipo><mensaje>Comprobante recibido</mensaje></mensaje></mensajes></comprobante></comprobantes></respuesta></rece:validarComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        TestableSRIWebService service = new TestableSRIWebService(recepcion, null);
        SRIWebService.SRIResponse response = service.enviarComprobante("<xml>test</xml>");
        assertEquals("RECIBIDA", response.getEstado());
    }

    @Test
    void consultarAutorizacion_rechazada_retornaRechazada() {
        String autorizacion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><aut:autorizacionComprobanteResponse xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\"><respuestaAutorizacionComprobante><estado>RECHAZADA</estado><comprobantes><comprobante><mensajes><mensaje><tipo>ERROR</tipo><mensaje>Rechazado</mensaje></mensaje></mensajes></comprobante></comprobantes></respuestaAutorizacionComprobante></aut:autorizacionComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        TestableSRIWebService service = new TestableSRIWebService(null, autorizacion);
        SRIWebService.SRIResponse response = service.consultarAutorizacion("1234567890123456789012345678901234567890123456789");
        assertEquals("RECHAZADA", response.getEstado());
    }

    @Test
    void validarComprobante_autorizacionAunNoDisponible_retornaPendiente() {
        String recepcion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><rece:validarComprobanteResponse xmlns:rece=\"http://ec.gob.sri.ws.recepcion\"><respuesta><estado>RECIBIDA</estado><comprobantes><comprobante><mensajes><mensaje><tipo>INFORMACION</tipo><mensaje>Comprobante recibido</mensaje></mensaje></mensajes></comprobante></comprobantes></respuesta></rece:validarComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        String autorizacion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><aut:autorizacionComprobanteResponse xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\"><respuestaAutorizacionComprobante><estado>PENDIENTE</estado></respuestaAutorizacionComprobante></aut:autorizacionComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        TestableSRIWebService service = new TestableSRIWebService(recepcion, autorizacion);
        SRIWebService.SRIResponse response = service.validarComprobante("<xml>test</xml>", "1234567890123456789012345678901234567890123456789");
        assertEquals("PENDIENTE", response.getEstado());
        assertEquals("Comprobante recibido por el SRI; autorización aún pendiente.", response.getMensaje());
    }

    @Test
    void consultarAutorizacion_devuelta_retornaDevuelta() {
        String autorizacion = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><aut:autorizacionComprobanteResponse xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\"><respuestaAutorizacionComprobante><estado>DEVUELTA</estado><comprobantes><comprobante><mensajes><mensaje><tipo>ERROR</tipo><mensaje>Devuelto por error</mensaje></mensaje></mensajes></comprobante></comprobantes></respuestaAutorizacionComprobante></aut:autorizacionComprobanteResponse></soapenv:Body></soapenv:Envelope>";
        TestableSRIWebService service = new TestableSRIWebService(null, autorizacion);
        SRIWebService.SRIResponse response = service.consultarAutorizacion("1234567890123456789012345678901234567890123456789");
        assertEquals("DEVUELTA", response.getEstado());
    }
}

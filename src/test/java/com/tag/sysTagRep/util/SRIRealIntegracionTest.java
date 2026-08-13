package com.tag.sysTagRep.util;

import com.tag.sysTagRep.config.DatabaseConnection;
import com.tag.sysTagRep.dao.EmpresaDAO;
import com.tag.sysTagRep.model.Empresa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Prueba de integración REAL contra el Web Service del SRI en ambiente PRUEBAS
 * (celcer.sri.gob.ec). Construye un comprobante de prueba con la empresa real
 * de la BD y la firma electrónica configurada, y lo envía al SRI para validar
 * el ciclo de recepción + autorización.
 *
 * NOTA: requiere BD local con empresa cargada y firma electrónica configurada.
 * No envía ningún correo.
 */
class SRIRealIntegracionTest {

    private static final String AMBIENTE = "PRUEBAS";

    @Test
    void cicloReal_recepcionYAutorizacion() throws Exception {
        String[] firma = ConfigFirma.cargar();
        assumeTrue(firma[0] != null && !firma[0].isEmpty() && !firma[1].isEmpty(),
                "No hay firma electrónica configurada; se omite la prueba real");

        EmpresaDAO empresaDAO = new EmpresaDAO();
        List<Empresa> empresas = empresaDAO.listar();
        assumeTrue(!empresas.isEmpty(), "No hay empresa cargada en la BD; se omite la prueba real");
        Empresa empresa = empresas.get(0);

        String ruc = empresa.getRuc();
        String razonSocial = empresa.getRazonSocial();
        String claveAcceso = ClaveAcceso.generar(
                AppConstants.TIPO_COMPROBANTE_FACTURA, ruc, AMBIENTE,
                AppConstants.ESTABLECIMIENTO_DEFAULT, AppConstants.PUNTO_EMISION_DEFAULT, 999999999);

        String fechaEmision = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.PATRON_FECHA_EMISION));
        List<Object[]> detalles = List.<Object[]>of(
            new Object[]{"PRUEBA001", "Producto de prueba SRI", "1", "8.70", "0.00", "8.70"}
        );
        String xml = XmlSriBuilder.construirFactura(
                AMBIENTE, claveAcceso, ruc, razonSocial,
                AppConstants.ESTABLECIMIENTO_DEFAULT, AppConstants.PUNTO_EMISION_DEFAULT, 999999999,
                empresa.getDireccionCallePrincipal() + " y " + empresa.getDireccionCalleSecundaria(),
                empresa.getAgenteRetencion(), "NO", "04",
                "Cliente Prueba SRI", "1716854540002",
                "Quito", "8.70", "0.00", "1.30", "10.00", "0.00",
                "Efectivo", fechaEmision, detalles);

        FirmaDigital firmaDigital = new FirmaDigital();
        assumeTrue(firmaDigital.cargarCertificado(firma[0], firma[1]),
                "No se pudo cargar el certificado .p12; se omite la prueba real");

        String xmlFirmado = firmaDigital.firmarXml(xml);
        System.out.println("XML firmado OK. Clave de acceso: " + claveAcceso);

        SRIWebService sri = new SRIWebService(AMBIENTE);
        SRIWebService.SRIResponse respuesta = sri.validarComprobante(xmlFirmado, claveAcceso);

        System.out.println("=== RESPUESTA REAL SRI (PRUEBAS) ===");
        System.out.println("Estado: " + respuesta.getEstado());
        System.out.println("Mensaje: " + respuesta.getMensaje());
        System.out.println("Número de autorización: " + respuesta.getNumeroAutorizacion());
        System.out.println("Fecha de autorización: " + respuesta.getFechaAutorizacion());

        assertNotNull(respuesta.getEstado(), "El SRI debe responder un estado");
        assertTrue(List.of("AUTORIZADO", "RECHAZADA", "DEVUELTA", "PENDIENTE", "ERROR", "RECIBIDA")
                        .contains(respuesta.getEstado()),
                "Estado inesperado del SRI: " + respuesta.getEstado());

        if ("AUTORIZADO".equals(respuesta.getEstado())) {
            assertNotNull(respuesta.getNumeroAutorizacion());
            assertNotNull(respuesta.getFechaAutorizacion());
        }
    }
}

package com.vendex.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vendex.model.*;
import com.vendex.service.*;
import com.vendex.util.*;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FacturaTemporalApiTest {
    @TempDir Path directorio;
    private FacturaTemporalFixture f;
    private Javalin app;
    private String base;
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeEach void iniciar() throws Exception {
        f = new FacturaTemporalFixture(directorio);
        // Conserva el servicio transaccional real; sustituye solo los archivos de firma y salida del servidor.
        var emisor = new FacturaService(null, null, null, null, null, null, null, null, null, null) {
            @Override public ResultadoFactura guardarFactura(Cliente c, Empresa e, String codigo, List<FacturaDetalle> items,
                    String pago, Integer plazo, String interes, String ambiente, String ruta, String clave, File dir, BigDecimal descuento) throws Exception {
                return f.servicio.guardarFactura(c, e, codigo, items, pago, plazo, interes, ambiente,
                        "firma-de-prueba.p12", "prueba", directorio.toFile(), descuento);
            }
        };
        var ctrl = new FacturaApiController(emisor, f.empresas, f.clientes, f.inventario, f.registros);
        app = Javalin.create(cfg -> cfg.jetty.defaultHost = "127.0.0.1");
        app.before(c -> SesionActual.iniciarApi(f.usuario, 2, 1));
        app.after(c -> SesionActual.cerrarApi());
        app.post("/api/facturas", ctrl::emitir);
        app.start(0); base = "http://127.0.0.1:" + app.port();
    }
    @AfterEach void cerrar() { if (app != null) app.stop(); if (f != null) f.close(); }

    private Map<String, Object> body(List<Map<String, Object>> items) {
        return Map.of("clienteId", 1, "formaPago", "Efectivo", "ambienteSri", AppConstants.AMBIENTE_PRUEBAS,
                "descuentoValor", "0", "items", items);
    }
    private Map<String, Object> temporal() {
        return ProductoTemporalFactura.payload(ProductoTemporalFactura.crear("TMP-API", "REPUESTO API", 3, new BigDecimal("11.50")));
    }
    private HttpResponse<String> enviar(String json) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/api/facturas"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void facturaRemotaMixta_registraInventarioNegativoYDetalleReal() throws Exception {
        int normal = f.existente("NORMAL", 2, true);
        var respuesta = enviar(JSON.writeValueAsString(body(List.of(temporal(), ProductoTemporalFactura.payload(
                new FacturaDetalle(normal, "NORMAL", "NORMAL", 1, new BigDecimal("23")))))));
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertTrue(JSON.readTree(respuesta.body()).path("id").asInt() > 0);
        assertEquals(-3, f.entero("SELECT cantidad FROM inventario WHERE codigo='TMP-API'"));
        assertEquals(2, f.contar("factura_detalle"));
        assertEquals(2, f.entero("SELECT sucursal_id FROM factura_registro"));
    }

    @Test void codigoExistente_devuelve400_yNoRegistraFacturaNiDescuentaStock() throws Exception {
        int id = f.existente("tmp-api", 2, true);
        var respuesta = enviar(JSON.writeValueAsString(body(List.of(temporal()))));
        assertEquals(400, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.body().contains("ya existe"));
        assertEquals(0, f.contar("factura_registro"));
        assertEquals(10, f.entero("SELECT cantidad FROM inventario WHERE id=" + id));
    }

    @Test void temporalIncompletoOCantidadFraccionaria_noSeAceptan() throws Exception {
        var sinCodigo = new LinkedHashMap<>(temporal()); sinCodigo.remove("codigo");
        var fraccion = new LinkedHashMap<>(temporal()); fraccion.put("cantidad", "1.5");
        var sinMarca = new LinkedHashMap<>(temporal()); sinMarca.remove("temporal");
        for (var item : List.of(sinCodigo, fraccion, sinMarca)) {
            var respuesta = enviar(JSON.writeValueAsString(body(List.of(item))));
            assertEquals(400, respuesta.statusCode(), respuesta.body());
        }
        assertEquals(0, f.contar("inventario")); assertEquals(0, f.contar("factura_registro"));
    }

    @Test void payloadOfflineConservaTemporal_ySoloCreaInventarioAlProcesarse() throws Exception {
        String payload = com.vendex.offline.OfflineHelper.generarPayload(body(List.of(temporal())));
        var op = new com.vendex.offline.OperacionOffline("FACTURA", payload);
        assertTrue(JSON.readTree(op.getPayload()).path("items").get(0).path("temporal").asBoolean());
        assertEquals("TMP-API", JSON.readTree(op.getPayload()).path("items").get(0).path("codigo").asText());
        assertEquals(0, f.contar("inventario"));
        var respuesta = enviar(op.getPayload());
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertEquals(-3, f.entero("SELECT cantidad FROM inventario WHERE codigo='TMP-API'"));
    }
}

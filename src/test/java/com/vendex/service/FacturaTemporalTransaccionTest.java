package com.vendex.service;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.FacturaDetalle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaTemporalTransaccionTest {
    @TempDir Path dir;
    private FacturaTemporalFixture f;
    @BeforeEach void iniciar() throws Exception { f = new FacturaTemporalFixture(dir); }
    @AfterEach void cerrar() { f.close(); }
    private FacturaDetalle temporal() { return ProductoTemporalFactura.crear(" tmp-01 ", " repuesto nuevo ", 3, new BigDecimal("11.50")); }

    @Test void emisionMixta_creaReferenciaConMenosTres_yVinculaDetalleHistorialYXml() throws Exception {
        int normal = f.existente("NORMAL", 2, true);
        var temporal = temporal();
        var resultado = f.guardar(List.of(temporal, new FacturaDetalle(normal, "NORMAL", "NORMAL", 1, new BigDecimal("23.00"))));
        int nuevoId = resultado.itemsDetalle.get(0).getInventarioId();
        assertTrue(nuevoId > 0); assertNotEquals(normal, nuevoId);
        assertEquals(0, temporal.getInventarioId(), "El borrador no recibe IDs transitorios");
        assertEquals(-3, f.entero("SELECT cantidad FROM inventario WHERE id=" + nuevoId));
        assertEquals(9, f.entero("SELECT cantidad FROM inventario WHERE id=" + normal));
        assertEquals(2, f.entero("SELECT sucursal_id FROM inventario WHERE id=" + nuevoId));
        assertEquals(2, f.entero("SELECT sucursal_id FROM factura_registro"));
        assertEquals(1, f.entero("SELECT COUNT(*) FROM inventario WHERE id=" + nuevoId + " AND costo_sin_iva IS NULL AND estado=true"));
        assertEquals(1, f.entero("SELECT COUNT(*) FROM factura_detalle WHERE inventario_id=" + nuevoId + " AND codigo='TMP-01'"));
        assertEquals(1, f.entero("SELECT COUNT(*) FROM historial_producto WHERE producto_id=" + nuevoId));
        assertTrue(resultado.itemsDetalle.get(0).getFacturaRegistroId() > 0);
        assertTrue(f.xml.contains("TMP-01")); assertFalse(f.xml.contains("[TEMPORAL]"));
        assertEquals(1, f.alertas.size());
        assertEquals(1, f.contar("comprobantes_prueba"));
    }

    @Test void dosTemporalesConIdCero_quedanComoReferenciasIndependientes() throws Exception {
        var segundo = ProductoTemporalFactura.crear("TMP-02", "SEGUNDO REPUESTO", 2, new BigDecimal("5.75"));
        var resultado = f.guardar(List.of(temporal(), segundo));
        assertNotEquals(resultado.itemsDetalle.get(0).getInventarioId(), resultado.itemsDetalle.get(1).getInventarioId());
        assertEquals(-3, f.entero("SELECT cantidad FROM inventario WHERE codigo='TMP-01'"));
        assertEquals(-2, f.entero("SELECT cantidad FROM inventario WHERE codigo='TMP-02'"));
        assertEquals(2, f.contar("historial_producto"));
    }

    @Test void firmaFallida_revierteTodo_yMismoBorradorPuedeReintentarse() throws Exception {
        var borrador = temporal(); f.fallarFirma = true;
        var error = assertThrows(Exception.class, () -> f.guardar(List.of(borrador)));
        assertTrue(error.getMessage().contains("Firma fallida"), error.getMessage());
        comprobarRollback(); assertEquals(0, borrador.getInventarioId());
        f.fallarFirma = false;
        var resultado = f.guardar(List.of(borrador));
        assertEquals(-3, f.entero("SELECT cantidad FROM inventario WHERE id=" + resultado.itemsDetalle.get(0).getInventarioId()));
        assertEquals(1, f.contar("inventario"));
    }

    @Test void falloDespuesDeFirmar_revierteAltaStockDetallesHistorialYSecuencia() throws Exception {
        f.fallarComprobante = true;
        var error = assertThrows(Exception.class, () -> f.guardar(List.of(temporal())));
        assertTrue(error.getMessage().contains("Fallo de comprobante"), error.getMessage());
        comprobarRollback();
    }

    private void comprobarRollback() throws Exception {
        for (String tabla : List.of("inventario", "factura_registro", "factura_detalle", "historial_producto", "comprobantes_prueba"))
            assertEquals(0, f.contar(tabla), tabla);
        assertEquals(1, f.entero("SELECT numero FROM secuencia_prueba WHERE id=1"));
        assertTrue(f.alertas.isEmpty());
    }

    @Test void codigoExistenteInclusoInactivo_caseInsensitive_seRechazaSinAfectarSuStock() throws Exception {
        int existente = f.existente(" tmp-01 ", 2, false);
        var error = assertThrows(IllegalArgumentException.class, () -> f.guardar(List.of(temporal())));
        assertTrue(error.getMessage().contains("ya existe"));
        assertEquals(1, f.contar("inventario")); assertEquals(10, f.entero("SELECT cantidad FROM inventario WHERE id=" + existente));
        assertEquals(0, f.contar("factura_registro"));
    }

    @Test void mismoCodigoEnOtraSucursal_noImpideCrearElTemporal() throws Exception {
        int otra = f.existente("TMP-01", 1, true);
        f.guardar(List.of(temporal()));
        assertEquals(2, f.contar("inventario"));
        assertEquals(10, f.entero("SELECT cantidad FROM inventario WHERE id=" + otra));
        assertEquals(-3, f.entero("SELECT cantidad FROM inventario WHERE sucursal_id=2"));
    }

    @Test void codigoTemporalRepetidoEnBorrador_seRechazaAntesDeConsumirNumero() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> f.guardar(List.of(temporal(), temporal())));
        comprobarRollback();
    }

    @Test void validacionesYAltaFueraDeTransaccion_noCreanProductos() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> ProductoTemporalFactura.crear("", "DESC", 1, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> ProductoTemporalFactura.crear("COD", "", 1, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> ProductoTemporalFactura.crear("COD", "DESC", 0, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> ProductoTemporalFactura.crear("COD", "DESC", 1, new BigDecimal("1.001")));
        try (var con = DatabaseConnection.getConnection()) {
            assertThrows(IllegalStateException.class, () -> f.inventario.crearTemporalFactura(con, "COD", "DESC", BigDecimal.TEN, 2));
        }
        comprobarRollback();
    }
}

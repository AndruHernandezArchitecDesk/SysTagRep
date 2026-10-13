package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.Cliente;
import com.vendex.model.Empresa;
import com.vendex.model.FacturaDetalle;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FacturaServiceTest {

    private FacturaService service;

    @BeforeEach
    void setUp() {
        EmpresaDAO empresaDAO = mock(EmpresaDAO.class);
        ClienteDAO clienteDAO = mock(ClienteDAO.class);
        InventarioDAO inventarioDAO = mock(InventarioDAO.class);
        FacturaRegistroDAO facturaDAO = mock(FacturaRegistroDAO.class);
        FacturaDetalleDAO detalleDAO = mock(FacturaDetalleDAO.class);
        SecuenciaDocumentoDAO secDAO = mock(SecuenciaDocumentoDAO.class);
        ComprobanteDAO comprobanteDAO = mock(ComprobanteDAO.class);
        CuentaPorCobrarDAO cpcDAO = mock(CuentaPorCobrarDAO.class);
        HistorialProductoDAO histDAO = mock(HistorialProductoDAO.class);

        service = new FacturaService(empresaDAO, clienteDAO, inventarioDAO, facturaDAO, detalleDAO, secDAO, comprobanteDAO, cpcDAO, histDAO, mock(com.vendex.dao.LogDAO.class));

        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("VENDEDOR");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("FACTURA_EMITIR", "DESCUENTO_APLICAR"));
        // Limite descuento 10% para test
        SesionActual.setPermisosForTest(Set.of("FACTURA_EMITIR", "DESCUENTO_APLICAR"));
        // Usamos setUsuarioForTest con rol VENDEDOR que tiene limite 5% por defecto, pero para este test permitimos
        u.setLimiteDescuentoPct(new BigDecimal("10"));
        SesionActual.setUsuarioForTest(u);
    }

    @AfterEach
    void tearDown() { SesionActual.cerrar(); }

    @Test
    void guardarFactura_rechazaClienteNulo() {
        Empresa emp = new Empresa();
        emp.setId(1);
        assertThrows(IllegalArgumentException.class, () ->
                service.guardarFactura(null, emp, "001", List.of(new FacturaDetalle(1, "COD", "Desc", 1, new BigDecimal("10"))),
                        "Efectivo", null, null, "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), BigDecimal.ZERO)
        );
    }

    @Test
    void guardarFactura_rechazaConsumidorFinalExcede50() {
        Empresa emp = new Empresa();
        emp.setId(1);
        emp.setRuc("0990000000001");
        Cliente cli = new Cliente();
        cli.setId(1);
        cli.setIdentificacion("9999999999999"); // consumidor final
        cli.setNombre("Consumidor Final");
        // Total 60 >50 debe rechazar
        FacturaDetalle det = new FacturaDetalle(1, "COD", "Desc", 1, new BigDecimal("60"));
        det.setPrecioTotal(new BigDecimal("60"));
        assertThrows(IllegalArgumentException.class, () ->
                service.guardarFactura(cli, emp, "001", List.of(det),
                        "Efectivo", null, null, "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), BigDecimal.ZERO)
        );
    }

    private BigDecimal topeDescuento(BigDecimal sumaBases, BigDecimal fijo) throws Exception {
        var m = FacturaService.class.getDeclaredMethod("calcularDescuento", BigDecimal.class, BigDecimal.class);
        m.setAccessible(true);
        return (BigDecimal) m.invoke(service, sumaBases, fijo);
    }

    @Test
    void descuentoValor_esMontoFijo_noPorcentaje() throws Exception {
        // Ejemplo usuario: total 350 - descuento 200 = 150 (bases sin IVA ~= 304.35)
        assertEquals(0, topeDescuento(new BigDecimal("304.35"), new BigDecimal("200.00"))
                .compareTo(new BigDecimal("200.00")), "descuento fijo se respeta integro");
        assertEquals(0, topeDescuento(new BigDecimal("63.00"), new BigDecimal("40.00"))
                .compareTo(new BigDecimal("40.00")));
        assertEquals(0, topeDescuento(new BigDecimal("63.00"), null)
                .compareTo(new BigDecimal("0.00")), "null => 0");
    }

    @Test
    void descuentoValor_seTopaABaseSinIva_noABruto() throws Exception {
        // Fijo 70 < bruto 72.45 pero > base 63.00: antes rompia la linea (-7.00, SRI minInclusive 0.0)
        assertEquals(0, topeDescuento(new BigDecimal("63.00"), new BigDecimal("70.00"))
                .compareTo(new BigDecimal("63.00")), "tope real = base sin IVA");
    }

    @Test
    void sumarBasesSinIva_precioConIvaEntre115() {
        FacturaDetalle d = new FacturaDetalle(1, "4PK1210", "TEST12", 1, new BigDecimal("72.45"));
        assertEquals(0, FacturaService.sumarBasesSinIva(List.of(d)).compareTo(new BigDecimal("63.00")));
    }

    @Test
    void validarNoNegativos_rechazaLineaNegativaSRI() {
        // Caso real: baseImponible '-12.00' rechazado por minInclusive 0.0
        Object[] mala = new Object[]{"COD", "Desc", "1", "63.00", "75.00", "-12.00"};
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                FacturaService.validarNoNegativos(java.util.Collections.singletonList(mala), new BigDecimal("10.00")));
        assertTrue(ex.getMessage().contains("negativo"), "mensaje accionable: " + ex.getMessage());

        Object[] buena = new Object[]{"COD", "Desc", "1", "63.00", "40.00", "23.00"};
        assertDoesNotThrow(() ->
                FacturaService.validarNoNegativos(java.util.Collections.singletonList(buena), new BigDecimal("35.90")));
    }
}

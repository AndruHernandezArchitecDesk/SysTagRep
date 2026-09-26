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

        service = new FacturaService(empresaDAO, clienteDAO, inventarioDAO, facturaDAO, detalleDAO, secDAO, comprobanteDAO, cpcDAO, histDAO);

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
}

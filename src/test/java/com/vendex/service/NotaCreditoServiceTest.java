package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.FacturaRegistro;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotaCreditoServiceTest {

    private NotaCreditoRegistroDAO notaCreditoDAO;
    private FacturaRegistroDAO facturaDAO;
    private EmpresaDAO empresaDAO;
    private ClienteDAO clienteDAO;
    private SecuenciaDocumentoDAO secuenciaDAO;
    private ComprobanteDAO comprobanteDAO;
    private NotaCreditoDetalleDAO detalleDAO;
    private InventarioDAO inventarioDAO;
    private HistorialProductoDAO historialDAO;
    private CajaSesionDAO cajaSesionDAO;
    private CajaMovimientoDAO cajaMovimientoDAO;

    private NotaCreditoService service;

    @BeforeEach
    void setUp() {
        notaCreditoDAO = mock(NotaCreditoRegistroDAO.class);
        facturaDAO = mock(FacturaRegistroDAO.class);
        empresaDAO = mock(EmpresaDAO.class);
        clienteDAO = mock(ClienteDAO.class);
        secuenciaDAO = mock(SecuenciaDocumentoDAO.class);
        comprobanteDAO = mock(ComprobanteDAO.class);
        detalleDAO = mock(NotaCreditoDetalleDAO.class);
        inventarioDAO = mock(InventarioDAO.class);
        historialDAO = mock(HistorialProductoDAO.class);
        cajaSesionDAO = mock(CajaSesionDAO.class);
        cajaMovimientoDAO = mock(CajaMovimientoDAO.class);

        service = new NotaCreditoService(
                empresaDAO, clienteDAO, inventarioDAO, facturaDAO, mock(FacturaDetalleDAO.class),
                secuenciaDAO, comprobanteDAO, notaCreditoDAO, detalleDAO,
                historialDAO, cajaSesionDAO, cajaMovimientoDAO
        );

        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("VENDEDOR");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("NOTA_CREDITO_EMITIR", "FACTURA_ANULAR"));
    }

    @AfterEach
    void tearDown() {
        SesionActual.cerrar();
    }

    @Test
    void emitirNotaCredito_rechazaSiExcedeSaldo() {
        FacturaRegistro factura = new FacturaRegistro();
        factura.setId(10);
        factura.setTotal(new BigDecimal("100.00"));
        factura.setEstadoSri("AUTORIZADO");
        factura.setFecha(LocalDateTime.now());
        factura.setCodigo("001-001-000000001");
        factura.setNumComprobante("001-001-000000001");
        factura.setClienteId(1);

        when(facturaDAO.obtenerPorId(10)).thenReturn(factura);
        when(notaCreditoDAO.sumarValorModificacionPorFactura(10, "AUTORIZADO")).thenReturn(new BigDecimal("90.00"));

        // Mock empresa/cliente para que no falle antes (aunque la validación de saldo ocurre después)
        com.vendex.model.Empresa empresaMock = new com.vendex.model.Empresa();
        empresaMock.setRuc("0990000000001");
        empresaMock.setRazonSocial("Test SA");
        empresaMock.setDireccionCallePrincipal("Dir1");
        empresaMock.setDireccionCalleSecundaria("Dir2");
        when(empresaDAO.listar()).thenReturn(List.of(empresaMock));
        com.vendex.model.Cliente clienteMock = new com.vendex.model.Cliente();
        clienteMock.setId(1);
        clienteMock.setNombre("Cliente Test");
        clienteMock.setIdentificacion("0990000001");
        when(clienteDAO.obtenerPorId(anyInt())).thenReturn(clienteMock);

        // Detalle que genera valorModificacion 20 (90+20 >100)
        NotaCreditoService.DetalleNCInput det = new NotaCreditoService.DetalleNCInput(1, 1, "COD1", "Desc", new BigDecimal("5"), new BigDecimal("4.60"));
        // precioSinIva = 4.60/1.15=4.00, totalSinIva=20, iva=3, valorMod=23 -> excede
        // Simplificamos: usar cantidad 10 * 4.00 =40 +iva 6 =46 -> 90+46>100
        det.cantidad = new BigDecimal("10");
        det.precioUnitario = new BigDecimal("4.60");

        // Necesitamos también mock para empresa/cliente dentro del servicio antes de la validación de saldo?
        // La validación de saldo ocurre antes de empresa/cliente, así que no necesita, pero el servicio primero obtiene factura y luego calcula subSinIva
        // Por lo tanto el test debe llegar hasta la comprobación de saldo
        // Mock factura total y suma previas para forzar excepción
        // El servicio calcula valorModificacion a partir de detalles, no de sumaPrevias directamente, así que necesitamos que detalles generen >10
        // Ya con cantidad 10, valorModificacion ~46, 90+46=136 >100 -> debe lanzar

        // Forzar que factura tenga total 100 y suma previas 90 ya mockeado
        // Ejecutar
        assertThrows(IllegalStateException.class, () ->
                service.emitirNotaCredito(10, List.of(det), "motivo", "DEVOLUCION", false, "PRUEBAS", "ruta.p12", "clave", new java.io.File("/tmp"), 1)
        );

        // Verificar que no se intentó insertar
        verify(notaCreditoDAO, never()).insertar(any());
    }

    @Test
    void emitirNotaCredito_requiereMotivo() {
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirNotaCredito(10, List.of(), "", "DEVOLUCION", false, "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }
}

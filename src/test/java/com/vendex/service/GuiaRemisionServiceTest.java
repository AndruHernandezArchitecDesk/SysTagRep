package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GuiaRemisionServiceTest {

    private GuiaRemisionService service;

    @BeforeEach
    void setUp() {
        EmpresaDAO empresaDAO = mock(EmpresaDAO.class);
        ClienteDAO clienteDAO = mock(ClienteDAO.class);
        FacturaRegistroDAO facturaDAO = mock(FacturaRegistroDAO.class);
        SecuenciaDocumentoDAO secDAO = mock(SecuenciaDocumentoDAO.class);
        ComprobanteDAO comprobanteDAO = mock(ComprobanteDAO.class);
        GuiaRemisionRegistroDAO guiaDAO = mock(GuiaRemisionRegistroDAO.class);
        GuiaRemisionDestinatarioDAO destDAO = mock(GuiaRemisionDestinatarioDAO.class);
        GuiaRemisionDetalleDAO detalleDAO = mock(GuiaRemisionDetalleDAO.class);

        service = new GuiaRemisionService(empresaDAO, clienteDAO, facturaDAO, secDAO, comprobanteDAO, guiaDAO, destDAO, detalleDAO);

        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("BODEGUERO");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("GUIA_REMISION_EMITIR"));
    }

    @AfterEach
    void tearDown() { SesionActual.cerrar(); }

    @Test
    void emitirGuiaRemision_rechazaSinDestinatario() {
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirGuiaRemision(List.of(), "dirPartida", "Transp", "04", "0990000000001", "ABC123",
                        LocalDate.now(), LocalDate.now().plusDays(1), "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }

    @Test
    void emitirGuiaRemision_rechazaPlacaVacia() {
        GuiaRemisionService.DestinatarioGRInput dest = new GuiaRemisionService.DestinatarioGRInput("0990000001", "Dest SA", "Dir", "Venta");
        dest.detalles.add(new GuiaRemisionService.DetalleGRInput("COD1", "Desc", new java.math.BigDecimal("1")));
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirGuiaRemision(List.of(dest), "dirPartida", "Transp", "04", "0990000000001", "",
                        LocalDate.now(), LocalDate.now().plusDays(1), "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }
}

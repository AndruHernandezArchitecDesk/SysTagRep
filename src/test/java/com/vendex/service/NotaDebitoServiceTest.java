package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotaDebitoServiceTest {

    private NotaDebitoService service;

    @BeforeEach
    void setUp() {
        EmpresaDAO empresaDAO = mock(EmpresaDAO.class);
        ClienteDAO clienteDAO = mock(ClienteDAO.class);
        FacturaRegistroDAO facturaDAO = mock(FacturaRegistroDAO.class);
        SecuenciaDocumentoDAO secDAO = mock(SecuenciaDocumentoDAO.class);
        ComprobanteDAO comprobanteDAO = mock(ComprobanteDAO.class);
        NotaDebitoRegistroDAO ndDAO = mock(NotaDebitoRegistroDAO.class);
        NotaDebitoMotivoDAO motivoDAO = mock(NotaDebitoMotivoDAO.class);
        CajaSesionDAO cajaSesionDAO = mock(CajaSesionDAO.class);
        CajaMovimientoDAO cajaMovimientoDAO = mock(CajaMovimientoDAO.class);

        service = new NotaDebitoService(empresaDAO, clienteDAO, facturaDAO, secDAO, comprobanteDAO, ndDAO, motivoDAO, cajaSesionDAO, cajaMovimientoDAO);

        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("VENDEDOR");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("NOTA_DEBITO_EMITIR"));
    }

    @AfterEach
    void tearDown() {
        SesionActual.cerrar();
    }

    @Test
    void emitirNotaDebito_rechazaMotivosVacios() {
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirNotaDebito(1, List.of(), "01", "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }

    @Test
    void emitirNotaDebito_rechazaValorCero() {
        NotaDebitoService.MotivoNDInput m = new NotaDebitoService.MotivoNDInput("Interes", new java.math.BigDecimal("0"));
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirNotaDebito(1, List.of(m), "01", "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }
}

package com.vendex.service;

import com.vendex.dao.*;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RetencionServiceTest {

    private RetencionService service;

    @BeforeEach
    void setUp() {
        EmpresaDAO empresaDAO = mock(EmpresaDAO.class);
        ProveedorDAO proveedorDAO = mock(ProveedorDAO.class);
        SecuenciaDocumentoDAO secDAO = mock(SecuenciaDocumentoDAO.class);
        ComprobanteDAO comprobanteDAO = mock(ComprobanteDAO.class);
        RetencionRegistroDAO retDAO = mock(RetencionRegistroDAO.class);
        RetencionDocumentoSustentoDAO docDAO = mock(RetencionDocumentoSustentoDAO.class);
        RetencionDetalleDAO detalleDAO = mock(RetencionDetalleDAO.class);

        service = new RetencionService(empresaDAO, proveedorDAO, secDAO, comprobanteDAO, retDAO, docDAO, detalleDAO);

        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("CAJERO");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("RETENCION_EMITIR"));
    }

    @AfterEach
    void tearDown() { SesionActual.cerrar(); }

    @Test
    void emitirRetencion_rechazaSinDocumentoSustento() {
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirRetencion(List.of(), "03/2026", "04", "Razon", "0990000000001", 1, "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }

    @Test
    void emitirRetencion_rechazaPeriodoInvalido() {
        RetencionService.DocSustentoInput doc = new RetencionService.DocSustentoInput("001-001-000000001", LocalDate.now(), new BigDecimal("100"));
        doc.retenciones.add(new RetencionService.RetencionLineaInput("1", "303", new BigDecimal("100"), new BigDecimal("10")));
        assertThrows(IllegalArgumentException.class, () ->
                service.emitirRetencion(List.of(doc), "2026-03", "04", "Razon", "0990000000001", 1, "PRUEBAS", "ruta", "clave", new java.io.File("/tmp"), 1)
        );
    }
}

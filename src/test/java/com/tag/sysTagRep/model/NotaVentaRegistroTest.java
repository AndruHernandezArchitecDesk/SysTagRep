package com.tag.sysTagRep.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class NotaVentaRegistroTest {

    @Test
    void registroProforma_camposSeAsignanCorrectamente() {
        LocalDateTime ahora = LocalDateTime.now();
        NotaVentaRegistro reg = new NotaVentaRegistro(1, 2, ahora, "PRO-001-00000001", "Efectivo", ahora);

        assertEquals(1, reg.getEmpresaId());
        assertEquals(2, reg.getClienteId());
        assertEquals(ahora, reg.getFecha());
        assertEquals("PRO-001-00000001", reg.getCodigo());
        assertEquals("Efectivo", reg.getFormaPago());
        assertEquals(ahora, reg.getFechaRegistro());
    }

    @Test
    void registroProforma_formaPagoTAGCredito_sePersiste() {
        LocalDateTime ahora = LocalDateTime.now();
        NotaVentaRegistro reg = new NotaVentaRegistro(1, 2, ahora, "PRO-001-00000002", "TAG Crédito", ahora);
        assertEquals("TAG Crédito", reg.getFormaPago());
    }

    @Test
    void registroProforma_codigoVacio_seAcepta() {
        LocalDateTime ahora = LocalDateTime.now();
        NotaVentaRegistro reg = new NotaVentaRegistro(1, 2, ahora, "", "Efectivo", ahora);
        assertEquals("", reg.getCodigo());
    }
}

package com.tag.sysTagRep.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {

    @Test
    void getUltimoError_inicialmente_esNull() {
        EmailService service = new EmailService();
        assertNull(service.getUltimoError());
    }

    @Test
    void enviarCorreoConPDF_retornaFalseCuandoDestinatarioVacio() {
        EmailService service = new EmailService();
        File pdfFicticio = new File("/tmp/proforma_ficticia_test.pdf");
        boolean resultado = service.enviarCorreoConPDF("", "Cliente", "PRO-001", "PROFORMA", pdfFicticio);
        assertFalse(resultado);
    }
}

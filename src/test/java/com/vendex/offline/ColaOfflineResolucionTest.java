package com.vendex.offline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 7: resolución de conflictos de la cola offline (reencolar/descartar). */
class ColaOfflineResolucionTest {

    private LocalOperationQueue cola;

    @BeforeEach
    void setUp() throws Exception {
        LocalOperationQueue.setDirectorioPrueba(Files.createTempDirectory("cola_resolucion"));
        cola = new LocalOperationQueue();
    }

    @Test
    void reencolar_conflicto_vuelveAPendienteConIntentosEnCero() {
        OperacionOffline op = new OperacionOffline("FACTURA", "{}");
        cola.encolar(op);
        cola.marcarConflicto(op.getId(), "HTTP 409: duplicado");
        assertEquals(0, cola.listarPendientes().size());

        assertTrue(cola.reencolar(op.getId()));
        List<OperacionOffline> pend = cola.listarPendientes();
        assertEquals(1, pend.size());
        assertEquals(0, pend.get(0).getIntentos());
        assertNull(pend.get(0).getError());
    }

    @Test
    void reencolar_fallida_vuelveAPendiente() {
        OperacionOffline op = new OperacionOffline("NOTA_DEBITO", "{}");
        cola.encolar(op);
        cola.marcarFallida(op.getId(), "Maximo de intentos");
        assertTrue(cola.reencolar(op.getId()));
        assertEquals(1, cola.listarPendientes().size());
    }

    @Test
    void reencolar_enviada_noHaceNada() {
        OperacionOffline op = new OperacionOffline("CAJA_ABRIR", "{}");
        cola.encolar(op);
        cola.marcarEnviada(op.getId());
        assertFalse(cola.reencolar(op.getId()));
        assertEquals(0, cola.listarPendientes().size());
    }

    @Test
    void descartar_eliminaLaOperacion() {
        OperacionOffline op = new OperacionOffline("INVENTARIO", "{}");
        cola.encolar(op);
        cola.marcarConflicto(op.getId(), "error");
        assertTrue(cola.descartar(op.getId()));
        assertEquals(0, cola.listarTodas().size());
        assertFalse(cola.descartar(op.getId())); // ya no existe
    }

    @Test
    void listarTodas_incluyeTodosLosEstados() {
        OperacionOffline a = new OperacionOffline("FACTURA", "{}");
        OperacionOffline b = new OperacionOffline("RETENCION", "{}");
        OperacionOffline c = new OperacionOffline("GUIA_REMISION", "{}");
        cola.encolar(a);
        cola.encolar(b);
        cola.encolar(c);
        cola.marcarEnviada(a.getId());
        cola.marcarConflicto(b.getId(), "conflicto");
        List<OperacionOffline> todas = cola.listarTodas();
        assertEquals(3, todas.size());
        assertEquals(1, cola.listarPendientes().size());
    }
}

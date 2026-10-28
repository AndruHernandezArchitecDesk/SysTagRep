package com.vendex.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class NavegadorTest {

    @AfterEach void restaurar() { Navegador.paraPruebas(null); }

    @Test void soloAceptaUrlsWebHttpS() {
        assertTrue(Navegador.esWeb("https://ai.dev/rate-limit"));
        assertTrue(Navegador.esWeb("http://localhost:7070"));
        assertFalse(Navegador.esWeb("file:///etc/passwd"));
        assertFalse(Navegador.esWeb("javascript:alert(1)"));
        assertFalse(Navegador.esWeb(null));
        assertFalse(Navegador.esWeb("  "));
    }

    @Test void urlInvalidaNoLanzaNiIniciaProceso() {
        var mensaje = new AtomicReference<String>();
        assertFalse(Navegador.abrir("javascript:alert(1)", mensaje::set));
        assertEquals("El enlace no es válido: javascript:alert(1)", mensaje.get());
    }

    /** Reproduce un navegador que no responde: la llamada no debe esperarlo. */
    @Test void navegadorColgado_noRetieneAlLlamador() throws Exception {
        CountDownLatch entrado = new CountDownLatch(1);
        Navegador.paraPruebas(url -> {
            entrado.countDown();
            Thread.sleep(60_000); // navegador bloqueado
            return true;
        });
        var mensaje = new AtomicReference<String>();
        CountDownLatch resuelto = new CountDownLatch(1);
        long inicio = System.nanoTime();
        boolean iniciado = Navegador.abrir("https://ai.dev/rate-limit", m -> {
            mensaje.set(m); resuelto.countDown();
        });
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
        assertTrue(iniciado);
        assertTrue(ms < 1000, "abrir debe volver de inmediato, tardó " + ms + "ms");
        assertTrue(entrado.await(5, TimeUnit.SECONDS));
        // El hilo de navegador sigue en pausa, pero nunca estuvo en el hilo del llamador.
        Thread.sleep(200);
        assertEquals(1, resuelto.getCount(), "un navegador colgado no debe responder antes de tiempo");
        assertTrue(mensaje.get() == null || mensaje.get().startsWith("Enlace abierto"));
    }

    @Test void soloSePermiteUnaAperturaALaVez() {
        Navegador.paraPruebas(url -> { Thread.sleep(1500); return true; });
        assertTrue(Navegador.abrir("https://ai.dev/rate-limit", m -> {}));
        assertFalse(Navegador.abrir("https://ai.dev/rate-limit", m -> {}),
                "la segunda apertura se ignora mientras la primera sigue en curso");
    }

    @Test void falloDelNavegador_entregaMensajeConElEnlace() throws Exception {
        Navegador.paraPruebas(url -> false);
        var mensaje = new AtomicReference<String>();
        CountDownLatch fin = new CountDownLatch(1);
        Navegador.abrir("https://ai.dev/rate-limit", m -> { mensaje.set(m); fin.countDown(); });
        assertTrue(fin.await(5, TimeUnit.SECONDS));
        assertTrue(mensaje.get().startsWith("No se pudo abrir el navegador"));
        assertTrue(mensaje.get().contains("https://ai.dev/rate-limit"));
    }
}

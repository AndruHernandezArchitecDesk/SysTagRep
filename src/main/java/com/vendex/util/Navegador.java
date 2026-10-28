package com.vendex.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Apertura de enlaces fuera del hilo JavaFX. Desktop.browse puede bloquearse en
 * algunos escritorios y congelar la interfaz si se invoca desde el hilo de UI.
 */
public final class Navegador {
    private static final Logger LOGGER = Logger.getLogger(Navegador.class.getName());
    private static final long ESPERA_MS = 8000;
    private static final Object CANDADO = new Object();
    private static volatile boolean abriendo;

    /** Integración real con el sistema; sustituible en pruebas para no abrir ventanas. */
    @FunctionalInterface
    public interface Apertura { boolean abrir(String url) throws Exception; }
    private static volatile Apertura apertura = Navegador::aperturaDelSistema;

    /** Solo para pruebas: instala un navegador falso. null restaura el comportamiento real. */
    public static void paraPruebas(Apertura sustituta) {
        apertura = sustituta != null ? sustituta : Navegador::aperturaDelSistema;
        synchronized (CANDADO) { abriendo = false; }
    }

    private Navegador() {}

    /** true si la URL parece un recurso web seguro de abrir. */
    public static boolean esWeb(String url) {
        if (url == null) return false;
        try {
            var u = java.net.URI.create(url);
            return ("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme()))
                    && u.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Abre la URL en un hilo propio y entrega un mensaje a {@code resultado} en el
     * mismo hilo llamador cuando termina. Devuelve false si ya hay una apertura en curso.
     */
    public static boolean abrir(String url, Consumer<String> resultado) {
        if (!esWeb(url)) {
            if (resultado != null) resultado.accept("El enlace no es válido: " + url);
            return false;
        }
        synchronized (CANDADO) {
            if (abriendo) return false;
            abriendo = true;
        }
        Thread hilo = new Thread(() -> {
            String mensaje;
            try {
                boolean ok = apertura.abrir(url);
                mensaje = ok ? "Enlace abierto en el navegador."
                        : "No se pudo abrir el navegador. Copia el enlace: " + url;
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "No se pudo abrir " + url, ex);
                mensaje = "No se pudo abrir el navegador. Copia el enlace: " + url;
            } finally {
                synchronized (CANDADO) { abriendo = false; }
            }
            if (resultado != null) resultado.accept(mensaje);
        }, "abrir-enlace");
        hilo.setDaemon(true);
        hilo.start();
        return true;
    }

    /** Integración por defecto: Desktop.browse con espera acotada y comando externo de respaldo. */
    private static boolean aperturaDelSistema(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                CountDownLatch fin = new CountDownLatch(1);
                AtomicReference<Exception> fallo = new AtomicReference<>();
                Thread lanzador = new Thread(() -> {
                    try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(url)); }
                    catch (Exception ex) { fallo.set(ex); }
                    finally { fin.countDown(); }
                }, "desktop-browse");
                lanzador.setDaemon(true);
                lanzador.start();
                if (fin.await(ESPERA_MS, TimeUnit.MILLISECONDS) && fallo.get() == null) return true;
                if (fallo.get() != null) lanzador.join(200);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Desktop.browse no disponible", ex);
        }
        return externo(url);
    }

    /** Intentos por SO con espera acotada; xdg-open puede quedarse esperando indefinidamente. */
    private static boolean externo(String url) {
        String so = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        String[] cmd = so.contains("win") ? new String[]{"cmd", "/c", "start", "", url}
                : so.contains("mac") ? new String[]{"open", url}
                : new String[]{"xdg-open", url};
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(false).start();
            if (p.waitFor(5, TimeUnit.SECONDS)) return p.exitValue() == 0;
            p.destroyForcibly();
        } catch (Exception ignored) {}
        return false;
    }
}

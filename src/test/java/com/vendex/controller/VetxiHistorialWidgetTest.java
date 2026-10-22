package com.vendex.controller;

import com.vendex.chatbot.GeminiChatbotService;
import com.vendex.config.AppContext;
import com.vendex.config.DatabaseConnection;
import com.vendex.dao.VetxiHistorialDAO;
import com.vendex.dao.VetxiHistorialDAOPostgres;
import com.vendex.model.VetxiHistorial.*;
import com.vendex.service.VendexiConsumoService;
import com.vendex.service.VendexiSugerenciaService;
import com.vendex.service.VetxiHistorialService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class VetxiHistorialWidgetTest {
    private final Ambito ana = new Ambito(10, 1), juan = new Ambito(20, 1);
    private final AtomicReference<Ambito> identidad = new AtomicReference<>(ana);
    private final VetxiHistorialDAO dao = new VetxiHistorialDAOPostgres();
    private final List<ChatWidgetController> widgets = new ArrayList<>();
    private final AtomicInteger consultasIA = new AtomicInteger();
    private final VendexiConsumoService medicion = new VendexiConsumoService(null) {
        @Override public long iniciarSesion(String modulo, String tipo) { return -1; }
        @Override public void finalizarSesion(long id) {}
    };

    @BeforeAll static void iniciarFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }
    @BeforeEach void prepararSql() throws Exception {
        // AppContext incluye un DAO legacy cuyo constructor crea una tabla con SERIAL.
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:vetxi_widget_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        DatabaseConnection.ensureVetxiHistorialSchema();
        try (var con = DatabaseConnection.getConnection(); var st = con.createStatement()) {
            st.execute("CREATE TABLE vehiculo (marca VARCHAR(100))");
            st.execute("CREATE TABLE nota_venta_registro (id INTEGER PRIMARY KEY)");
        }
    }
    @AfterEach void limpiar() throws Exception {
        identidad.set(null);
        for (var widget : widgets) {
            fx(() -> {
                var liberar = ChatWidgetController.class.getDeclaredMethod("liberar"); liberar.setAccessible(true); liberar.invoke(widget);
            });
            var executor = (java.util.concurrent.ExecutorService) campo(widget, "hiloHistorial");
            if (executor != null) assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        DatabaseConnection.resetToDefault();
    }

    @FunctionalInterface private interface Accion { void ejecutar() throws Exception; }
    private void fx(Accion accion) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { accion.ejecutar(); } catch (Throwable t) { error.set(t); } finally { fin.countDown(); }
        });
        assertTrue(fin.await(15, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("Historial del widget inválido", error.get());
    }
    private Object campo(ChatWidgetController c, String nombre) throws Exception {
        var f = ChatWidgetController.class.getDeclaredField(nombre); f.setAccessible(true); return f.get(c);
    }
    private void esperar(ChatWidgetController c, BooleanSupplier condicion) throws Exception {
        long hasta = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < hasta) {
            boolean[] listo = {false}; fx(() -> listo[0] = condicion.getAsBoolean());
            if (listo[0]) return;
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        fail("Timeout esperando historial: " + campo(c, "conversacionChat"));
    }
    private boolean listo(ChatWidgetController c) {
        try { return (boolean) campo(c, "historialCargado") && !(boolean) campo(c, "cargandoHistorial")
                && !(boolean) campo(c, "consultaEnCurso"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private GeminiChatbotService chatbot() {
        return new GeminiChatbotService((d, m, mo, a) -> GeminiChatbotService.ResultadoBusqueda.noEncontrado()) {
            @Override public RespuestaChat preguntarConEstado(String t, String m, String mo, Integer a) {
                consultasIA.incrementAndGet();
                return new RespuestaChat("Respuesta real del test", EstadoIA.IA_ACTIVA, false, "VENTAS", List.of());
            }
        };
    }
    private ChatWidgetController abrir(VetxiHistorialService servicio, GeminiChatbotService chatbot) throws Exception {
        AtomicReference<ChatWidgetController> creado = new AtomicReference<>();
        fx(() -> {
            var loader = new FXMLLoader(getClass().getResource("/view/ChatWidget.fxml"));
            loader.setControllerFactory(tipo -> new ChatWidgetController(AppContext.getInstance(),
                    new VendexiSugerenciaService(), servicio, chatbot, medicion));
            Parent raiz = loader.load();
            Scene scene = new Scene(raiz, 900, 650);
            com.vendex.util.ThemeManager.aplicarTemaGuardado(scene);
            ((Region) raiz).resize(900, 650);
            var c = (ChatWidgetController) loader.getController(); widgets.add(c); creado.set(c); c.abrir();
        });
        return creado.get();
    }
    private void enviar(ChatWidgetController c, String texto) throws Exception {
        fx(() -> { ((TextField) campo(c, "inputField")).setText(texto); c.onEnviar(); });
    }
    private Mensaje mensaje(String id, String titulo, String texto, List<Fuente> fuentes) {
        return new Mensaje(UUID.randomUUID().toString(), id, titulo, false, texto, false,
                "INVENTARIO", List.of(), fuentes, null);
    }
    private String textos(Node nodo) {
        String texto = nodo instanceof Label l ? l.getText() : "";
        if (nodo instanceof Parent p) for (var hijo : p.getChildrenUnmodifiable()) texto += "\n" + textos(hijo);
        return texto;
    }

    @Test void guardaPreguntaYRespuesta_yReabreSinDuplicarNiVolverALlamarIa() throws Exception {
        var servicio = new VetxiHistorialService(dao, identidad::get);
        var c = abrir(servicio, chatbot()); esperar(c, () -> listo(c));
        enviar(c, "hola repuestos");
        esperar(c, () -> listo(c) && ((Label) obtener(c, "lblHistorial")).getText().startsWith("Historial guardado"));
        // Esperar la segunda escritura, no solo la de la pregunta.
        var executor = (java.util.concurrent.ExecutorService) campo(c, "hiloHistorial");
        executor.submit(() -> {}).get(10, TimeUnit.SECONDS);
        String id = (String) campo(c, "conversacionChat");
        assertEquals(2, dao.mensajes(ana, id, 100).size());
        var reabierto = abrir(servicio, chatbot()); esperar(reabierto, () -> listo(reabierto));
        fx(() -> {
            var box = (VBox) campo(reabierto, "mensajesBox");
            assertEquals(2, box.getChildren().size());
            assertTrue(textos(box).contains("hola repuestos"));
            assertTrue(textos(box).contains("Rol IA: VENTAS"));
            reabierto.onToggleChat(); reabierto.onToggleChat();
            assertEquals(2, box.getChildren().size());
        });
        assertEquals(1, consultasIA.get(), "Recuperar no vuelve a preguntar al proveedor");
    }

    private Object obtener(ChatWidgetController c, String campo) {
        try { return campo(c, campo); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test void nuevaConversacionConservaLasAnteriores_ySelectorRecuperaFuentes() throws Exception {
        String primera = UUID.randomUUID().toString(), ultima = UUID.randomUUID().toString();
        dao.guardar(ana, mensaje(primera, "Catálogo de filtro", "Información con fuentes", List.of(
                new Fuente("Catálogo", "https://example.com/filtro"), new Fuente("No web", "file:///tmp/no-abrir"))));
        dao.guardar(ana, mensaje(ultima, "Última consulta", "última", List.of()));
        var c = abrir(new VetxiHistorialService(dao, identidad::get), chatbot()); esperar(c, () -> listo(c));
        assertEquals(ultima, campo(c, "conversacionChat"));
        fx(c::nuevaConversacion);
        assertNotEquals(ultima, campo(c, "conversacionChat"));
        assertEquals(2, dao.conversaciones(ana, 20).size(), "Nueva no crea conversaciones vacías");
        fx(() -> {
            @SuppressWarnings("unchecked") var combo = (ComboBox<Conversacion>) campo(c, "cmbConversaciones");
            combo.setValue(combo.getItems().stream().filter(v -> v.id().equals(primera)).findFirst().orElseThrow());
        });
        esperar(c, () -> listo(c));
        fx(() -> {
            var box = (VBox) campo(c, "mensajesBox");
            assertTrue(textos(box).contains("Información con fuentes"), textos(box));
            assertTrue(textos(box).contains("Rol IA: INVENTARIO"), "Conservar rol y fuentes en la misma respuesta");
            var links = box.lookupAll(".hyperlink");
            assertEquals(2, links.size());
            assertEquals(1, links.stream().filter(Node::isDisabled).count(), "Solo enlaces web válidos son activos");
        });
        assertEquals(0, consultasIA.get());
    }

    @Test void respuestaQueTerminaDespuesDelLogout_noSeMuestraNiSeGuardaParaOtroUsuario() throws Exception {
        var inicio = new CountDownLatch(1); var continuar = new Semaphore(0);
        var hiloConsulta = new AtomicReference<Thread>();
        var lento = new GeminiChatbotService((d, m, mo, a) -> GeminiChatbotService.ResultadoBusqueda.noEncontrado()) {
            @Override public RespuestaChat preguntarConEstado(String t, String m, String mo, Integer a) {
                hiloConsulta.set(Thread.currentThread()); inicio.countDown(); continuar.acquireUninterruptibly();
                return new RespuestaChat("Respuesta privada de Ana", EstadoIA.IA_ACTIVA, false);
            }
        };
        var servicio = new VetxiHistorialService(dao, identidad::get);
        var anterior = abrir(servicio, lento); esperar(anterior, () -> listo(anterior));
        try {
            enviar(anterior, "hola repuestos"); assertTrue(inicio.await(10, TimeUnit.SECONDS));
            fx(() -> {
                var liberar = ChatWidgetController.class.getDeclaredMethod("liberar"); liberar.setAccessible(true); liberar.invoke(anterior);
                identidad.set(juan);
            });
            var nuevo = abrir(servicio, chatbot()); esperar(nuevo, () -> listo(nuevo));
            continuar.release(); hiloConsulta.get().join(10000);
            fx(() -> {
                assertFalse(textos((VBox) campo(nuevo, "mensajesBox")).contains("Respuesta privada de Ana"));
                assertTrue(((VBox) campo(anterior, "mensajesBox")).getChildren().isEmpty());
            });
            assertTrue(dao.conversaciones(juan, 20).isEmpty());
        } finally { continuar.release(); }
    }

    @Test void cargaLentaDeUnUsuario_noContaminaElHistorialDelSiguiente() throws Exception {
        String id = UUID.randomUUID().toString(); dao.guardar(ana, mensaje(id, "Privado Ana", "Secreto Ana", List.of()));
        var inicio = new CountDownLatch(1); var continuar = new Semaphore(0);
        var delegado = new VetxiHistorialDAOPostgres();
        var lento = new VetxiHistorialDAOPostgres() {
            @Override public List<Conversacion> conversaciones(Ambito a, int limite) throws Exception {
                var lista = delegado.conversaciones(a, limite);
                if (a.equals(ana)) { inicio.countDown(); continuar.acquireUninterruptibly(); }
                return lista;
            }
        };
        var anterior = abrir(new VetxiHistorialService(lento, identidad::get), chatbot());
        try {
            assertTrue(inicio.await(10, TimeUnit.SECONDS));
            identidad.set(juan);
            var nuevo = abrir(new VetxiHistorialService(dao, identidad::get), chatbot()); esperar(nuevo, () -> listo(nuevo));
            continuar.release();
            ((java.util.concurrent.ExecutorService) campo(anterior, "hiloHistorial")).submit(() -> {}).get(10, TimeUnit.SECONDS);
            fx(() -> {
                assertFalse(textos((VBox) campo(nuevo, "mensajesBox")).contains("Secreto Ana"));
                assertFalse(textos((VBox) campo(anterior, "mensajesBox")).contains("Secreto Ana"));
            });
        } finally { continuar.release(); }
    }

    @Test void baseNoDisponible_noBloqueaLaConversacion_yAdvierteQueNoSeGuardo() throws Exception {
        var sinDb = new VetxiHistorialDAOPostgres() {
            @Override public List<Conversacion> conversaciones(Ambito a, int l) { return List.of(); }
            @Override public void guardar(Ambito a, Mensaje m) throws Exception { throw new java.sql.SQLException("BD no disponible en prueba"); }
        };
        var c = abrir(new VetxiHistorialService(sinDb, identidad::get), chatbot()); esperar(c, () -> listo(c));
        enviar(c, "hola repuestos"); esperar(c, () -> listo(c));
        ((java.util.concurrent.ExecutorService) campo(c, "hiloHistorial")).submit(() -> {}).get(10, TimeUnit.SECONDS);
        fx(() -> {
            assertTrue(textos((VBox) campo(c, "mensajesBox")).contains("Respuesta real del test"));
            assertTrue(((Label) campo(c, "lblHistorial")).getText().contains("No se pudo guardar"));
            assertFalse(((TextField) campo(c, "inputField")).isDisabled());
        });
    }
}

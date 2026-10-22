package com.vendex.controller;

import com.vendex.config.AppContext;
import com.vendex.model.Inventario;
import com.vendex.service.VendexiSugerenciaService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class VendexiMascotaTest {
    @BeforeAll
    static void iniciarFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @FunctionalInterface private interface AccionFx { void ejecutar() throws Exception; }
    private void fx(AccionFx accion) throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch fin = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { accion.ejecutar(); } catch (Throwable t) { error.set(t); } finally { fin.countDown(); }
        });
        assertTrue(fin.await(30, TimeUnit.SECONDS), "Timeout en hilo FX");
        if (error.get() != null) throw new AssertionError("Fallo de interacción con la mascota", error.get());
    }

    private static class Documento implements DocumentoVendexi {
        ChatWidgetController asistente;
        final List<VendexiSugerenciaService.Linea> lineas = new ArrayList<>();
        int agregados;
        @Override public VendexiSugerenciaService.Contexto contextoVendexi() {
            return new VendexiSugerenciaService.Contexto("factura", false, List.copyOf(lineas));
        }
        @Override public void conectarVendexi(ChatWidgetController asistente) { this.asistente = asistente; }
        @Override public Consumer<Inventario> accionAgregarVendexi() {
            return i -> {
                agregados++;
                lineas.add(new VendexiSugerenciaService.Linea(i.getId(), i.getCodigo(), i.getDescripcion(),
                        1, i.getPrecioVenta(), i.getCantidad()));
                asistente.documentoModificado(this);
            };
        }
    }

    private ChatWidgetController cargar(VendexiSugerenciaService servicio) throws Exception {
        return cargar(servicio, null);
    }

    private ChatWidgetController cargar(VendexiSugerenciaService servicio, CountDownLatch recibido) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ChatWidget.fxml"));
        loader.setControllerFactory(tipo -> new ChatWidgetController(AppContext.getInstance(), servicio) {
            @Override void recibirSugerencias(DocumentoVendexi origen, long revision, List<VendexiSugerenciaService.Sugerencia> sugerencias) {
                super.recibirSugerencias(origen, revision, sugerencias);
                if (recibido != null) recibido.countDown();
            }
        });
        Parent raiz = loader.load();
        Scene scene = new Scene(raiz, 900, 650);
        com.vendex.util.ThemeManager.aplicarTemaGuardado(scene);
        raiz.applyCss();
        ((Region) raiz).resize(900, 650);
        raiz.layout();
        return loader.getController();
    }

    private VendexiSugerenciaService sinIa() {
        return new VendexiSugerenciaService() {
            @Override public boolean esIaDisponible() { return false; }
            @Override public List<Sugerencia> sugerenciasComplementarias(Inventario a, List<Inventario> i, Set<Integer> ids, String v) {
                fail("Las verificaciones locales no llaman a la IA"); return List.of();
            }
        };
    }

    private Object campo(ChatWidgetController c, String nombre) throws Exception {
        var f = ChatWidgetController.class.getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(c);
    }

    private long revision(ChatWidgetController c) throws Exception { return (long) campo(c, "revisionDocumento"); }

    private VendexiSugerenciaService.Sugerencia sugerencia(int id, int stock) {
        Inventario i = new Inventario();
        i.setId(id); i.setCodigo("COMP-" + id); i.setDescripcion("COMPLEMENTO " + id);
        i.setCantidad(stock); i.setPrecioVenta(new BigDecimal("12.50"));
        return new VendexiSugerenciaService.Sugerencia(VendexiSugerenciaService.Tipo.COMPLEMENTARIO,
                i.getDescripcion(), "Se utiliza como complemento. Verificar compatibilidad.", i);
    }

    @Test
    void verificarSinIa_muestraGloboSinRobarFoco_yPermiteRecuperarlo() throws Exception {
        fx(() -> {
            var c = cargar(sinIa());
            var doc = new Documento();
            c.setDocumentoActivo(doc);
            assertSame(c, doc.asistente);
            var raiz = (Parent) campo(c, "rootStack");
            var focoAnterior = raiz.getScene().getFocusOwner();
            c.verificarDocumento();
            assertSame(focoAnterior, raiz.getScene().getFocusOwner());
            assertTrue(((Region) campo(c, "sugerenciaBubble")).isVisible());
            assertEquals("2", ((Label) campo(c, "contadorSugerencias")).getText());
            assertFalse(((Button) campo(c, "btnAgregarSugerencia")).isVisible());
            c.ocultarSugerencias();
            assertTrue(((Label) campo(c, "contadorSugerencias")).isVisible());
            c.mostrarSugerencias();
            c.descartarSugerencia();
            assertEquals("1", ((Label) campo(c, "contadorSugerencias")).getText());
            c.setDocumentoActivo(null);
            assertNull(doc.asistente);
        });
    }

    @Test
    void resultadoDeOtraPantallaORevision_noAparece() throws Exception {
        fx(() -> {
            var c = cargar(sinIa());
            var anterior = new Documento(); var actual = new Documento();
            c.setDocumentoActivo(anterior);
            long version = revision(c);
            c.setDocumentoActivo(actual);
            c.recibirSugerencias(anterior, version, List.of(sugerencia(1, 3)));
            assertFalse(((Label) campo(c, "contadorSugerencias")).isVisible());
            long versionActual = revision(c);
            c.documentoModificado(actual);
            c.recibirSugerencias(actual, versionActual, List.of(sugerencia(1, 3)));
            assertFalse(((Region) campo(c, "sugerenciaBubble")).isVisible());
            assertNull(anterior.asistente);
            c.setDocumentoActivo(null);
        });
    }

    @Test
    void agregarComplemento_modificaSoloDocumentoActivo_yUnaSolaVez() throws Exception {
        fx(() -> {
            var c = cargar(sinIa()); var doc = new Documento();
            c.setDocumentoActivo(doc);
            c.recibirSugerencias(doc, revision(c), List.of(sugerencia(7, 3)));
            c.agregarSugerencia();
            c.agregarSugerencia();
            assertEquals(1, doc.agregados);
            assertEquals(7, doc.lineas.get(0).inventarioId);
            assertFalse(((Label) campo(c, "contadorSugerencias")).isVisible());
            c.setDocumentoActivo(null);
        });
    }

    @Test
    void excluyeDuplicadosSinStockYProductosYaAgregados() throws Exception {
        fx(() -> {
            var c = cargar(sinIa()); var doc = new Documento();
            doc.lineas.add(new VendexiSugerenciaService.Linea(1, "BASE", "BASE", 1, BigDecimal.TEN, 10));
            c.setDocumentoActivo(doc);
            c.recibirSugerencias(doc, revision(c), List.of(sugerencia(1, 3), sugerencia(2, 0), sugerencia(3, 4), sugerencia(3, 4)));
            assertEquals("1", ((Label) campo(c, "contadorSugerencias")).getText());
            assertEquals("COMPLEMENTO 3", ((Label) campo(c, "lblSugerenciaTitulo")).getText());
            c.setDocumentoActivo(null);
        });
    }

    @Test
    void notaCredito_soloVerificaciones_ySinControlDeStock() throws Exception {
        fx(() -> {
            var c = cargar(sinIa());
            var nc = new DocumentoVendexi() {
                @Override public void conectarVendexi(ChatWidgetController a) {}
                @Override public VendexiSugerenciaService.Contexto contextoVendexi() {
                    return new VendexiSugerenciaService.Contexto("nota_credito", true, List.of(
                            new VendexiSugerenciaService.Linea(1, "NC", "DEVOLUCION", 10, BigDecimal.TEN, 0)));
                }
            };
            c.setDocumentoActivo(nc);
            c.recibirSugerencias(nc, revision(c), List.of(sugerencia(2, 3)));
            assertFalse(((Label) campo(c, "contadorSugerencias")).isVisible());
            c.verificarDocumento();
            assertEquals("Todo listo por aquí", ((Label) campo(c, "lblSugerenciaTitulo")).getText());
            assertFalse(((Button) campo(c, "btnAgregarSugerencia")).isVisible());
            c.setDocumentoActivo(null);
        });
    }

    @Test
    void mascotaYGlobos_seMantienenDentroAlMoverYRedimensionar() throws Exception {
        fx(() -> {
            var c = cargar(sinIa()); var doc = new Documento();
            c.setDocumentoActivo(doc);
            c.recibirSugerencias(doc, revision(c), List.of(sugerencia(2, 4)));
            Region raiz = (Region) campo(c, "rootStack");
            for (double[] tam : new double[][]{{900,650}, {600,400}}) {
                raiz.resize(tam[0], tam[1]);
                for (double[] pos : new double[][]{{-100,-100}, {2000,-100}, {-100,2000}, {2000,2000}}) {
                    c.moverMascota(pos[0], pos[1]);
                    for (String nombre : List.of("mascota", "sugerenciaBubble", "chatPanel")) {
                        Region nodo = (Region) campo(c, nombre);
                        assertTrue(nodo.getLayoutX() >= 0 && nodo.getLayoutY() >= 0, nombre);
                        assertTrue(nodo.getLayoutX() + nodo.getWidth() <= tam[0] + 0.1, nombre + " fuera a la derecha");
                        assertTrue(nodo.getLayoutY() + nodo.getHeight() <= tam[1] + 0.1, nombre + " fuera abajo");
                    }
                }
            }
            c.ocultarSugerencias();
            assertFalse(raiz.isPickOnBounds(), "La capa no captura clicks por sus límites");
            assertTrue(raiz.getBackground() == null || raiz.getBackground().getFills().isEmpty(), "La capa no pinta un fondo sobre el formulario");
            c.setDocumentoActivo(null);
        });
    }

    @Test
    void main_conectaElMismoAsistente_yDesconectaAlCambiarDeFormulario() throws Exception {
        fx(() -> {
            var usuario = new com.vendex.model.Usuario(); usuario.setId(1); usuario.setRol("ADMINISTRADOR");
            com.vendex.util.SesionActual.setUsuarioForTest(usuario);
            com.vendex.util.SesionActual.setPermisosForTest(Set.of("REPORTE_VER", "FACTURA_EMITIR"));
            try {
                var loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
                loader.load();
                MainController main = loader.getController();
                var campoAsistente = MainController.class.getDeclaredField("asistenteController");
                campoAsistente.setAccessible(true);
                var c = (ChatWidgetController) campoAsistente.get(main);
                var navegar = MainController.class.getDeclaredMethod("cargarVista", String.class);
                navegar.setAccessible(true);
                navegar.invoke(main, "/view/FacturaView.fxml");
                var factura = (DocumentoVendexi) campo(c, "documentoActivo");
                assertTrue(factura instanceof FacturaController);
                assertTrue(((Parent) campo(c, "capibara")).lookup("#lentes").isVisible());
                factura.accionAgregarVendexi().accept(sugerencia(9, 5).producto);
                assertEquals(9, factura.contextoVendexi().lineas.get(0).inventarioId);
                c.recibirSugerencias(factura, revision(c), List.of(sugerencia(10, 4)));
                navegar.invoke(main, "/view/NotaVentaView.fxml");
                assertTrue(campo(c, "documentoActivo") instanceof NotaVentaController);
                assertTrue(((Parent) campo(c, "capibara")).lookup("#libreta").isVisible());
                var campoConexion = FacturaController.class.getDeclaredField("vendexi");
                campoConexion.setAccessible(true);
                assertNull(campoConexion.get(factura), "La factura anterior ya no recibe eventos");
                assertFalse(((Label) campo(c, "contadorSugerencias")).isVisible());
                navegar.invoke(main, "/view/DashboardView.fxml");
                assertNull(campo(c, "documentoActivo"));
                assertFalse(((Parent) campo(c, "capibara")).lookup("#lentes").isVisible());
                assertTrue(((Button) campo(c, "btnVerificar")).isDisabled());
            } finally {
                com.vendex.util.SesionActual.cerrar();
            }
        });
    }

    @Test
    void actividadEnLaEscenaInterrumpeAnimacion_yLaIdentidadVisibleEsVetxi() throws Exception {
        fx(() -> {
            var c = cargar(sinIa());
            Parent raiz = (Parent) campo(c, "rootStack");
            assertEquals("Vetxi", ((Label) raiz.lookup(".capibara-nombre")).getText());
            var motor = (VetxiAnimaciones) campo(c, "animaciones");
            try {
                motor.setActivo(true);
                motor.rascar();
                assertEquals(VetxiAnimaciones.Estado.RASCANDO, motor.estado());
                javafx.event.Event.fireEvent(raiz, new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,
                        "a", "a", javafx.scene.input.KeyCode.A, false, false, false, false));
                assertEquals(VetxiAnimaciones.Estado.REPOSO, motor.estado());
                motor.rascar();
                enviarMouse((Button) campo(c, "bubbleButton"), javafx.scene.input.MouseEvent.MOUSE_MOVED, 30, 30, false);
                assertEquals(VetxiAnimaciones.Estado.REPOSO, motor.estado());
            } finally { motor.setActivo(false); }
        });
    }

    @Test
    void arrastrarMascota_noDisparaClickNiAbreVentanas() throws Exception {
        fx(() -> {
            var c = cargar(sinIa()); var doc = new Documento();
            c.setDocumentoActivo(doc);
            c.recibirSugerencias(doc, revision(c), List.of(sugerencia(3, 4)));
            c.ocultarSugerencias();
            Button boton = (Button) campo(c, "bubbleButton");
            Region mascota = (Region) campo(c, "mascota");
            double xAnterior = mascota.getLayoutX();
            int[] acciones = {0};
            boton.addEventHandler(javafx.event.ActionEvent.ACTION, e -> acciones[0]++);
            var prefs = (java.util.prefs.Preferences) campo(c, "preferencias");
            String x = prefs.get("x", null), y = prefs.get("y", null);
            try {
                enviarMouse(boton, javafx.scene.input.MouseEvent.MOUSE_PRESSED, 50, 40, true);
                enviarMouse(boton, javafx.scene.input.MouseEvent.MOUSE_DRAGGED, -250, -150, true);
                enviarMouse(boton, javafx.scene.input.MouseEvent.MOUSE_RELEASED, -250, -150, false);
                assertNotEquals(xAnterior, mascota.getLayoutX());
                assertEquals(0, acciones[0], "Arrastrar no equivale a pulsar");
                assertFalse(((Region) campo(c, "chatPanel")).isVisible());
                assertFalse(((Region) campo(c, "sugerenciaBubble")).isVisible());
            } finally {
                if (x == null) prefs.remove("x"); else prefs.put("x", x);
                if (y == null) prefs.remove("y"); else prefs.put("y", y);
                c.setDocumentoActivo(null);
            }
        });
    }

    private void enviarMouse(Button boton, javafx.event.EventType<javafx.scene.input.MouseEvent> tipo,
                             double x, double y, boolean presionado) {
        var evento = new javafx.scene.input.MouseEvent(tipo, x, y, x, y, javafx.scene.input.MouseButton.PRIMARY,
                1, false, false, false, false, presionado, false, false, false, false, false,
                new javafx.scene.input.PickResult(boton, x, y));
        javafx.event.Event.fireEvent(boton, evento);
    }

    @Test
    void respuestaAsincronaTardia_trasNavegar_seDescartaAunqueLaIAIgnoreCancelacion() throws Exception {
        CountDownLatch inicio = new CountDownLatch(1), recibido = new CountDownLatch(1);
        Semaphore continuar = new Semaphore(0);
        var sugerida = sugerencia(8, 3);
        var servicio = new VendexiSugerenciaService() {
            @Override public boolean esIaDisponible() { return true; }
            @Override public List<Sugerencia> sugerenciasComplementarias(Inventario a, List<Inventario> i, Set<Integer> ids, String v) {
                inicio.countDown(); continuar.acquireUninterruptibly(); return List.of(sugerida);
            }
        };
        AtomicReference<ChatWidgetController> controlador = new AtomicReference<>();
        try {
            fx(() -> {
                var c = cargar(servicio, recibido); controlador.set(c);
                var doc = new Documento(); c.setDocumentoActivo(doc);
                c.consultarComplementarios(doc, sugerida.producto, List.of(sugerida.producto), null);
            });
            assertTrue(inicio.await(10, TimeUnit.SECONDS));
            fx(() -> controlador.get().setDocumentoActivo(new Documento()));
            continuar.release();
            assertTrue(recibido.await(10, TimeUnit.SECONDS));
            fx(() -> {
                assertFalse(((Label) campo(controlador.get(), "contadorSugerencias")).isVisible());
                controlador.get().setDocumentoActivo(null);
            });
        } finally { continuar.release(); }
    }
}

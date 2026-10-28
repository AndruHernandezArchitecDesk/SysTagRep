package com.vendex.controller;

import com.vendex.config.DatabaseConnection;
import com.vendex.dao.CuotaIADAOPostgres;
import com.vendex.dao.VendexiConsumoDAOPostgres;
import com.vendex.model.CuotaIA.Estado;
import com.vendex.service.VendexiConsumoService;
import com.vendex.util.Navegador;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import org.junit.jupiter.api.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class CuotasIaPanelTest {

    @BeforeAll static void iniciarFx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
        Platform.setImplicitExit(false);
    }

    @AfterEach void cerrar() {
        Navegador.paraPruebas(null);
        DatabaseConnection.resetToDefault();
    }

    private <T> T fx(Callable<T> tarea) throws Exception {
        FutureTask<T> f = new FutureTask<>(tarea);
        Platform.runLater(f);
        return f.get(10, TimeUnit.SECONDS);
    }

    private void preparar() throws Exception {
        DatabaseConnection.setConnectionParams(
                "jdbc:hsqldb:mem:cuotas_ui_" + UUID.randomUUID() + ";sql.syntax_pgs=true", "sa", "");
        CuotaIADAOPostgres.prepararEsquema();
        DatabaseConnection.ensureVendexiSchema();
    }

    private Parent cargar(int ancho, int alto) throws Exception {
        return fx(() -> {
            var loader = new FXMLLoader(getClass().getResource("/view/ConsumoIaView.fxml"));
            loader.setControllerFactory(t -> new ConsumoIaController(
                    new VendexiConsumoService(new VendexiConsumoDAOPostgres(), () -> 10000)));
            Parent p = loader.load();
            new Scene(p, ancho, alto);
            p.applyCss();
            p.layout();
            return p;
        });
    }

    private boolean esperar(Parent root, BooleanSupplier condicion) throws Exception {
        long hasta = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < hasta) {
            if (fx(condicion::getAsBoolean)) return true;
            Thread.sleep(25);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static TableView<Estado> tablaModelos(Parent root) {
        return (TableView<Estado>) root.lookupAll(".table-view").stream()
                .filter(n -> n instanceof TableView<?> t && t.getItems().size() == 4)
                .findFirst().orElse(null);
    }

    /** Celdas realmente dibujadas dentro del viewport; no basta con tener filas en memoria. */
    private static int filasDibujadas(TableView<?> tabla) {
        tabla.applyCss();
        tabla.layout();
        VirtualFlow<?> flujo = (VirtualFlow<?>) tabla.lookup(".virtual-flow");
        if (flujo == null) return 0;
        int visibles = 0;
        for (int i = 0; i < tabla.getItems().size(); i++) {
            IndexedCell<?> celda = flujo.getCell(i);
            if (celda != null && celda.isVisible() && celda.getBoundsInParent().getHeight() > 1) visibles++;
        }
        return visibles;
    }

    @Test void filasDelModeloSonVisibles_noSoloElEncabezado() throws Exception {
        preparar();
        Parent root = cargar(1400, 900);
        assertTrue(esperar(root, () -> tablaModelos(root) != null
                        && !((Button) root.lookup("#btnActualizar")).isDisabled()),
                "El panel debe recuperar los cuatro modelos sin bloquear JavaFX");
        int[] dibujadas = {0};
        fx(() -> {
            root.applyCss();
            root.layout();
            TableView<?> tabla = tablaModelos(root);
            assertEquals(4, tabla.getItems().size());
            dibujadas[0] = filasDibujadas(tabla);
            return null;
        });
        assertTrue(dibujadas[0] >= 3,
                "Deben dibujarse al menos 3 filas de modelo; se dibujaron " + dibujadas[0]);
    }

    @Test void ventanaBajaConservaFilasMedianteDesplazamiento() throws Exception {
        preparar();
        Parent root = cargar(1400, 480);
        assertTrue(esperar(root, () -> tablaModelos(root) != null));
        int[] dibujadas = {0};
        double[] minHeight = {0};
        fx(() -> {
            root.applyCss(); root.layout();
            TableView<?> tabla = tablaModelos(root);
            dibujadas[0] = filasDibujadas(tabla);
            minHeight[0] = tabla.getMinHeight();
            return null;
        });
        assertTrue(minHeight[0] >= 120,
                "La tabla debe reservar altura mínima; tenía " + minHeight[0]);
        assertTrue(dibujadas[0] >= 1,
                "Con ventana baja debe seguir mostrándose al menos una fila; se dibujaron " + dibujadas[0]);
        assertNotNull(root.lookup(".scroll-pane"), "El panel debe incluir un desplazador");
    }

    @Test void abrirCuotasOficiales_noCongelaElHiloJavaFX() throws Exception {
        preparar();
        Parent root = cargar(1400, 900);
        assertTrue(esperar(root, () -> tablaModelos(root) != null));
        CountDownLatch colgado = new CountDownLatch(1);
        Navegador.paraPruebas(url -> {
            colgado.countDown();
            Thread.sleep(60_000); // navegador que no responde
            return true;
        });
        Hyperlink enlace = (Hyperlink) fx(() -> root.lookupAll(".hyperlink").stream()
                .filter(n -> n instanceof Hyperlink h && h.getText().contains("cuotas oficiales"))
                .findFirst().orElseThrow());
        long[] ms = {0};
        fx(() -> {
            long inicio = System.nanoTime();
            enlace.fire();
            ms[0] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
            return null;
        });
        assertTrue(ms[0] < 1000, "El clic no debe esperar al navegador; tardó " + ms[0] + "ms");
        assertTrue(colgado.await(5, TimeUnit.SECONDS), "La apertura debe delegarse a un hilo propio");
        fx(() -> {
            // JavaFX sigue respondiendo pese al navegador bloqueado.
            assertTrue(enlace.isDisable());
            assertFalse(root.lookup(".scroll-pane") == null);
            return null;
        });
    }

    @Test void errorDeCargaMuestraLaCausaVisible() throws Exception {
        preparar();
        Parent root = cargar(1400, 900);
        assertTrue(esperar(root, () -> tablaModelos(root) != null));
        // Rompe el esquema: provoca el fallo real del control central sin tocar la conexión.
        try (var con = DatabaseConnection.getConnection(); var st = con.createStatement()) {
            st.execute("DROP TABLE ia_solicitud");
            st.execute("DROP TABLE ia_cuota");
        }
        fx(() -> {
            ((Button) root.lookup("#btnActualizar")).fire();
            return null;
        });
        assertTrue(esperar(root, () -> {
            Label lbl = etiquetaEstado(root);
            return lbl != null && lbl.getText() != null && lbl.getText().contains("Control central no disponible");
        }), "Debe mostrarse la causa real, no solo un mensaje genérico");
        Label lbl = fx(() -> etiquetaEstado(root));
        assertTrue(lbl.getText().contains("V41") || lbl.getText().length() > 30,
                "El aviso debe incluir detalle: " + lbl.getText());
    }

    private static Label etiquetaEstado(Parent root) {
        return (Label) root.lookupAll(".label").stream()
                .filter(n -> n instanceof Label l && l.getText() != null
                        && (l.getText().contains("Control central")
                            || l.getText().contains("Actualizado")
                            || l.getText().contains("Consultando")))
                .findFirst().orElse(null);
    }
}

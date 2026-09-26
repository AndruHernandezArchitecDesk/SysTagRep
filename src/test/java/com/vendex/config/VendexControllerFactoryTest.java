package com.vendex.config;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que el composition root JavaFX resuelve TODOS los controllers sin excepción
 * (lineamiento §3 y §5: "forma barata de detectar un cableado roto antes de que falle
 * en tiempo de ejecución real").
 */
class VendexControllerFactoryTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // toolkit ya inicializado
        }
        Platform.setImplicitExit(false);
    }

    @Test
    void todosLosControllersSeResuelvenEnLaFactory() throws Exception {
        // ubicación de las clases MAIN (no test-classes)
        File clasesRoot = new File(com.vendex.MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File dir = new File(clasesRoot, "com/vendex/controller");
        assertTrue(dir.isDirectory(), "no existe directorio de clases: " + dir);

        List<Class<?>> controllers = new ArrayList<>();
        File[] clases = dir.listFiles((d, n) -> n.endsWith(".class") && !n.contains("$"));
        assertNotNull(clases);
        for (File f : clases) {
            String base = f.getName().replace(".class", "");
            if (!base.endsWith("Controller")) continue;
            controllers.add(Class.forName("com.vendex.controller." + base));
        }
        assertTrue(controllers.size() >= 40, "pocos controllers encontrados: " + controllers.size());

        AppContext ctx = AppContext.getInstance();
        VendexControllerFactory factory = new VendexControllerFactory(ctx);
        List<String> fallidos = new ArrayList<>();
        for (Class<?> c : controllers) {
            try {
                Object instance = factory.call(c);
                if (instance == null || !c.isInstance(instance)) fallidos.add(c.getSimpleName() + " -> instancia inválida");
            } catch (Exception e) {
                fallidos.add(c.getSimpleName() + " -> " + e.getMessage());
            }
        }
        assertTrue(fallidos.isEmpty(), "controllers sin resolver en factory: " + fallidos);
    }

    @Test
    void controllersRegistradosTienenConstructorAppContext() throws Exception {
        // Los 40 registrados en la factory deben exponer XController(AppContext) para inyección
        String[] registrados = {
                "AlertaController", "BackupController", "CajaController", "ChatWidgetController",
                "ClienteController", "CodigoController", "ComprobanteVentaReporteController",
                "ConfiguracionEmailController", "Dashboard2Controller", "DashboardController",
                "EmpresaController", "FacturaController", "FacturasIngresadasController",
                "FirmaController", "GestionRolesController", "GestionStockController",
                "GrupoController", "GuiaRemisionController", "HistorialCompraController",
                "HistorialProductoController", "HistorialVentaController",
                "IngresoMercaderiaController", "IngresoProductoController", "InventarioController",
                "LoginController", "MainController", "MarcaController", "NotaCreditoController",
                "NotaDebitoController", "NotaVentaController", "NumeracionController",
                "PorCobrarController", "PorPagarController", "ProveedorController",
                "RetencionController", "SeguimientoSriController", "UbicacionController",
                "UbicacionPercheroController", "UsuarioController", "VendedorController"
        };
        for (String n : registrados) {
            Class<?> c = Class.forName("com.vendex.controller." + n);
            assertNotNull(c.getConstructor(AppContext.class), n + " sin constructor (AppContext)");
        }
    }
}

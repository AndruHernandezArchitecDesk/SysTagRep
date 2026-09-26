package com.vendex.config;

import com.vendex.controller.*;
import javafx.fxml.FXMLLoader;
import javafx.util.Callback;

/**
 * Factory para FXMLLoader — resuelve controllers con dependencias inyectadas desde AppContext.
 * Composition root JavaFX: un único lugar donde se decide qué implementación recibe cada controller
 * (lineamiento §3). Los controllers expuestos aquí tienen constructor {@code XController(AppContext)}.
 * Los que aún no (sin dependencias) caen al fallback de constructor vacío.
 */
public class VendexControllerFactory implements Callback<Class<?>, Object> {

    private final AppContext ctx;

    public VendexControllerFactory(AppContext ctx) {
        this.ctx = ctx;
    }

    /** Aplica la factory a un loader cualquiera (navegación, modales, etc.). */
    public static void aplicar(FXMLLoader loader) {
        loader.setControllerFactory(new VendexControllerFactory(AppContext.getInstance()));
    }

    @Override
    public Object call(Class<?> type) {
        try {
            if (type == AlertaController.class) return new AlertaController(ctx);
            if (type == BackupController.class) return new BackupController(ctx);
            if (type == CajaController.class) return new CajaController(ctx);
            if (type == ChatWidgetController.class) return new ChatWidgetController(ctx);
            if (type == ClienteController.class) return new ClienteController(ctx);
            if (type == CodigoController.class) return new CodigoController(ctx);
            if (type == ComprobanteVentaReporteController.class) return new ComprobanteVentaReporteController(ctx);
            if (type == ConfiguracionEmailController.class) return new ConfiguracionEmailController(ctx);
            if (type == Dashboard2Controller.class) return new Dashboard2Controller(ctx);
            if (type == DashboardController.class) return new DashboardController(ctx);
            if (type == EmpresaController.class) return new EmpresaController(ctx);
            if (type == FacturaController.class) return new FacturaController(ctx);
            if (type == FacturasIngresadasController.class) return new FacturasIngresadasController(ctx);
            if (type == FirmaController.class) return new FirmaController(ctx);
            if (type == GestionRolesController.class) return new GestionRolesController(ctx);
            if (type == GestionStockController.class) return new GestionStockController(ctx);
            if (type == GrupoController.class) return new GrupoController(ctx);
            if (type == GuiaRemisionController.class) return new GuiaRemisionController(ctx);
            if (type == HistorialCompraController.class) return new HistorialCompraController(ctx);
            if (type == HistorialProductoController.class) return new HistorialProductoController(ctx);
            if (type == HistorialVentaController.class) return new HistorialVentaController(ctx);
            if (type == IngresoMercaderiaController.class) return new IngresoMercaderiaController(ctx);
            if (type == IngresoProductoController.class) return new IngresoProductoController(ctx);
            if (type == InventarioController.class) return new InventarioController(ctx);
            if (type == LoginController.class) return new LoginController(ctx);
            if (type == MainController.class) return new MainController(ctx);
            if (type == MarcaController.class) return new MarcaController(ctx);
            if (type == NotaCreditoController.class) return new NotaCreditoController(ctx);
            if (type == NotaDebitoController.class) return new NotaDebitoController(ctx);
            if (type == NotaVentaController.class) return new NotaVentaController(ctx);
            if (type == NumeracionController.class) return new NumeracionController(ctx);
            if (type == PorCobrarController.class) return new PorCobrarController(ctx);
            if (type == PorPagarController.class) return new PorPagarController(ctx);
            if (type == ProveedorController.class) return new ProveedorController(ctx);
            if (type == RetencionController.class) return new RetencionController(ctx);
            if (type == SeguimientoSriController.class) return new SeguimientoSriController(ctx);
            if (type == UbicacionController.class) return new UbicacionController(ctx);
            if (type == UbicacionPercheroController.class) return new UbicacionPercheroController(ctx);
            if (type == UsuarioController.class) return new UsuarioController(ctx);
            if (type == VendedorController.class) return new VendedorController(ctx);
            // Controllers sin dependencias (sin inyección): ctor vacío explícito
            if (type == DbSetupWizardController.class) return new DbSetupWizardController();
            if (type == LicenseActivatorController.class) return new LicenseActivatorController();
            if (type == TerminosController.class) return new TerminosController();
            // Si tiene constructor (AppContext) es que alguien olvidó registrarlo arriba
            type.getConstructor(AppContext.class);
            throw new RuntimeException("Controller con dependencias NO registrado en la factory: " + type.getName());
        } catch (NoSuchMethodException e) {
            // Controller sin dependencias: usa su constructor vacío
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Controller sin constructor válido: " + type.getName(), ex);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Controller no registrado en factory y sin constructor válido: " + type.getName(), e);
        }
    }
}

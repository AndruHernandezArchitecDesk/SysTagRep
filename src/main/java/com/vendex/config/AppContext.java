package com.vendex.config;

import com.vendex.dao.*;
import com.vendex.service.*;
import com.vendex.remote.*;

/**
 * Composition root — cableado manual de DAOs y servicios.
 * Centraliza la creación de implementaciones Postgres y su inyección en servicios.
 * Transición dual: código legacy que hace `new FacturaRegistroDAO()` debe migrar a `new FacturaRegistroDAOPostgres()`
 * o mejor a `AppContext.getInstance().facturaRegistroDAO`.
 */
public class AppContext {

    private static volatile AppContext instancia;
    private final boolean modoRemoto = esRemoto();
    private final RestClient restClient = new RestClient(com.vendex.remote.ApiConfig.host());

    private static boolean esRemoto() { return com.vendex.remote.ApiConfig.isModoRemoto(); }

    private <T> T dao(java.util.function.Supplier<T> local, java.util.function.Supplier<T> remote) {
        return modoRemoto ? remote.get() : local.get();
    }

    // DAOs
    public final FacturaRegistroDAO facturaRegistroDAO;
    public final FacturaDetalleDAO facturaDetalleDAO;
    public final SecuenciaDocumentoDAO secuenciaDAO;
    public final ComprobanteDAO comprobanteDAO;
    public final NotaCreditoRegistroDAO notaCreditoRegistroDAO;
    public final NotaCreditoDetalleDAO notaCreditoDetalleDAO;
    public final NotaDebitoRegistroDAO notaDebitoRegistroDAO;
    public final NotaDebitoMotivoDAO notaDebitoMotivoDAO;
    public final GuiaRemisionRegistroDAO guiaRemisionRegistroDAO;
    public final GuiaRemisionDestinatarioDAO guiaRemisionDestinatarioDAO;
    public final GuiaRemisionDetalleDAO guiaRemisionDetalleDAO;
    public final RetencionRegistroDAO retencionRegistroDAO;
    public final RetencionDocumentoSustentoDAO retencionDocumentoSustentoDAO;
    public final RetencionDetalleDAO retencionDetalleDAO;
    public final InventarioDAO inventarioDAO;
    public final HistorialProductoDAO historialProductoDAO;
    public final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    public final ClienteDAO clienteDAO;
    public final EmpresaDAO empresaDAO;
    public final CajaSesionDAO cajaSesionDAO;
    public final CajaMovimientoDAO cajaMovimientoDAO;
    public final ProveedorDAO proveedorDAO;
    public final CuentaPorPagarDAO cuentaPorPagarDAO;
    public final FacturaProveedorDAO facturaProveedorDAO;
    public final VendedorDAO vendedorDAO;
    public final UsuarioDAO usuarioDAO;
    public final RolDAO rolDAO;
    public final PermisoDAO permisoDAO;
    public final AuditoriaAccionDAO auditoriaAccionDAO;
    public final VentaResumenDAO ventaResumenDAO;
    public final CertificadoEstadoDAO certificadoEstadoDAO;
    public final ComprobantePendienteSriDAO comprobantePendienteSriDAO;
    public final ComprobanteTempDAO comprobanteTempDAO;
    public final MarcaDAO marcaDAO;
    public final NotaVentaRegistroDAO notaVentaRegistroDAO;
    public final UbicacionDetalleDAO ubicacionDetalleDAO;
    public final LogDAO logDAO;
    public final GrupoDAO grupoDAO;
    public final TablaRetencionDAO tablaRetencionDAO;
    public final ConfiguracionEmailDAO configuracionEmailDAO;
    public final PercheroDAO percheroDAO;
    public final NotaVentaDetalleDAO notaVentaDetalleDAO;
    public final UbicacionPerchaDAO ubicacionPerchaDAO;
    public final CodigoDAO codigoDAO;
    public final RepuestoChatbotDAO repuestoChatbotDAO;
    public final AlertaDAO alertaDAO;
    public final LoginIntentoLogDAO loginIntentoLogDAO;
    public final DashboardDAO dashboardDAO;
    public final SucursalDAO sucursalDAO;
    public final PuntoEmisionDAO puntoEmisionDAO;
    public final TransferenciaInventarioDAO transferenciaInventarioDAO;
    public final VehiculoDAO vehiculoDAO;
    public final InventarioVehiculoDAO inventarioVehiculoDAO;
    public final SecuenciaBloqueDAO secuenciaBloqueDAO;

    // Servicios
    public final FacturaService facturaService;
    public final NotaCreditoService notaCreditoService;
    public final NotaDebitoService notaDebitoService;
    public final GuiaRemisionService guiaRemisionService;
    public final RetencionService retencionService;
    public final TransferenciaInventarioService transferenciaInventarioService;
    public final AlertaService alertaService;
    public final CajaService cajaService;
    public final CertificadoAlertaService certificadoAlertaService;
    public final ServicioReintentoSri servicioReintentoSri;

    private AppContext() {
        // DAOs (Postgres locales o Rest segun modoRemoto)
        this.facturaRegistroDAO = dao(() -> new FacturaRegistroDAOPostgres(), () -> new FacturaRegistroDAORest(restClient));
        this.facturaDetalleDAO = dao(() -> new FacturaDetalleDAOPostgres(), () -> new FacturaDetalleDAORest(restClient));
        this.secuenciaDAO = dao(() -> new SecuenciaDocumentoDAOPostgres(), () -> new SecuenciaDocumentoDAORest(restClient));
        this.comprobanteDAO = dao(() -> new ComprobanteDAOPostgres(), () -> new ComprobanteDAORest(restClient));
        this.notaCreditoRegistroDAO = dao(() -> new NotaCreditoRegistroDAOPostgres(), () -> new NotaCreditoRegistroDAORest(restClient));
        this.notaCreditoDetalleDAO = dao(() -> new NotaCreditoDetalleDAOPostgres(), () -> new NotaCreditoDetalleDAORest(restClient));
        this.notaDebitoRegistroDAO = dao(() -> new NotaDebitoRegistroDAOPostgres(), () -> new NotaDebitoRegistroDAORest(restClient));
        this.notaDebitoMotivoDAO = dao(() -> new NotaDebitoMotivoDAOPostgres(), () -> new NotaDebitoMotivoDAORest(restClient));
        this.guiaRemisionRegistroDAO = dao(() -> new GuiaRemisionRegistroDAOPostgres(), () -> new GuiaRemisionRegistroDAORest(restClient));
        this.guiaRemisionDestinatarioDAO = dao(() -> new GuiaRemisionDestinatarioDAOPostgres(), () -> new GuiaRemisionDestinatarioDAORest(restClient));
        this.guiaRemisionDetalleDAO = dao(() -> new GuiaRemisionDetalleDAOPostgres(), () -> new GuiaRemisionDetalleDAORest(restClient));
        this.retencionRegistroDAO = dao(() -> new RetencionRegistroDAOPostgres(), () -> new RetencionRegistroDAORest(restClient));
        this.retencionDocumentoSustentoDAO = dao(() -> new RetencionDocumentoSustentoDAOPostgres(), () -> new RetencionDocumentoSustentoDAORest(restClient));
        this.retencionDetalleDAO = dao(() -> new RetencionDetalleDAOPostgres(), () -> new RetencionDetalleDAORest(restClient));
        this.inventarioDAO = dao(() -> new InventarioDAOPostgres(), () -> new InventarioDAORest(restClient));
        this.historialProductoDAO = dao(() -> new HistorialProductoDAOPostgres(), () -> new HistorialProductoDAORest(restClient));
        this.cuentaPorCobrarDAO = dao(() -> new CuentaPorCobrarDAOPostgres(), () -> new CuentaPorCobrarDAORest(restClient));
        this.clienteDAO = dao(() -> new ClienteDAOPostgres(), () -> new ClienteDAORest(restClient));
        this.empresaDAO = dao(() -> new EmpresaDAOPostgres(), () -> new EmpresaDAORest(restClient));
        this.cajaSesionDAO = dao(() -> new CajaSesionDAOPostgres(), () -> new CajaSesionDAORest(restClient));
        this.cajaMovimientoDAO = dao(() -> new CajaMovimientoDAOPostgres(), () -> new CajaMovimientoDAORest(restClient));
        this.proveedorDAO = dao(() -> new ProveedorDAOPostgres(), () -> new ProveedorDAORest(restClient));
        this.cuentaPorPagarDAO = dao(() -> new CuentaPorPagarDAOPostgres(), () -> new CuentaPorPagarDAORest(restClient));
        this.facturaProveedorDAO = dao(() -> new FacturaProveedorDAOPostgres(), () -> new FacturaProveedorDAORest(restClient));
        this.vendedorDAO = dao(() -> new VendedorDAOPostgres(), () -> new VendedorDAORest(restClient));
        this.usuarioDAO = dao(() -> new UsuarioDAOPostgres(), () -> new UsuarioDAORest(restClient));
        this.rolDAO = dao(() -> new RolDAOPostgres(), () -> new RolDAORest(restClient));
        this.permisoDAO = dao(() -> new PermisoDAOPostgres(), () -> new PermisoDAORest(restClient));
        this.auditoriaAccionDAO = dao(() -> new AuditoriaAccionDAOPostgres(), () -> new AuditoriaAccionDAORest(restClient));
        this.ventaResumenDAO = dao(() -> new VentaResumenDAOPostgres(), () -> new VentaResumenDAORest(restClient));
        this.certificadoEstadoDAO = dao(() -> new CertificadoEstadoDAOPostgres(), () -> new CertificadoEstadoDAORest(restClient));
        this.comprobantePendienteSriDAO = dao(() -> new ComprobantePendienteSriDAOPostgres(), () -> new ComprobantePendienteSriDAORest(restClient));
        this.comprobanteTempDAO = dao(() -> new ComprobanteTempDAOPostgres(), () -> new ComprobanteTempDAORest(restClient));
        this.marcaDAO = dao(() -> new MarcaDAOPostgres(), () -> new MarcaDAORest(restClient));
        this.notaVentaRegistroDAO = dao(() -> new NotaVentaRegistroDAOPostgres(), () -> new NotaVentaRegistroDAORest(restClient));
        this.ubicacionDetalleDAO = dao(() -> new UbicacionDetalleDAOPostgres(), () -> new UbicacionDetalleDAORest(restClient));
        this.logDAO = dao(() -> new LogDAOPostgres(), () -> new LogDAORest(restClient));
        this.grupoDAO = dao(() -> new GrupoDAOPostgres(), () -> new GrupoDAORest(restClient));
        this.tablaRetencionDAO = dao(() -> new TablaRetencionDAOPostgres(), () -> new TablaRetencionDAORest(restClient));
        this.configuracionEmailDAO = dao(() -> new ConfiguracionEmailDAOPostgres(), () -> new ConfiguracionEmailDAORest(restClient));
        this.percheroDAO = dao(() -> new PercheroDAOPostgres(), () -> new PercheroDAORest(restClient));
        this.notaVentaDetalleDAO = dao(() -> new NotaVentaDetalleDAOPostgres(), () -> new NotaVentaDetalleDAORest(restClient));
        this.ubicacionPerchaDAO = dao(() -> new UbicacionPerchaDAOPostgres(), () -> new UbicacionPerchaDAORest(restClient));
        this.codigoDAO = dao(() -> new CodigoDAOPostgres(), () -> new CodigoDAORest(restClient));
        this.repuestoChatbotDAO = dao(() -> new RepuestoChatbotDAOPostgres(), () -> new RepuestoChatbotDAORest(restClient));
        this.alertaDAO = dao(() -> new AlertaDAOPostgres(), () -> new AlertaDAORest(restClient));
        this.secuenciaBloqueDAO = dao(() -> new SecuenciaBloqueDAOPostgres(), () -> new SecuenciaBloqueDAORest(restClient));
        this.loginIntentoLogDAO = dao(() -> new LoginIntentoLogDAOPostgres(), () -> new LoginIntentoLogDAORest(restClient));
        this.dashboardDAO = dao(() -> new DashboardDAOPostgres(), () -> new DashboardDAORest(restClient));
        this.sucursalDAO = dao(() -> new SucursalDAOPostgres(), () -> new SucursalDAORest(restClient));
        this.puntoEmisionDAO = dao(() -> new PuntoEmisionDAOPostgres(), () -> new PuntoEmisionDAORest(restClient));
        this.transferenciaInventarioDAO = dao(() -> new TransferenciaInventarioDAOPostgres(), () -> new TransferenciaInventarioDAORest(restClient));
        this.vehiculoDAO = dao(() -> new VehiculoDAOPostgres(), () -> new VehiculoDAORest(restClient));
        this.inventarioVehiculoDAO = dao(() -> new InventarioVehiculoDAOPostgres(), () -> new InventarioVehiculoDAORest(restClient));

        // Servicios con inyección por constructor
        this.facturaService = new FacturaService(
                empresaDAO, clienteDAO, inventarioDAO, facturaRegistroDAO, facturaDetalleDAO,
                secuenciaDAO, comprobanteDAO, cuentaPorCobrarDAO, historialProductoDAO, logDAO
        );
        this.notaCreditoService = new NotaCreditoService(
                empresaDAO, clienteDAO, inventarioDAO, facturaRegistroDAO, facturaDetalleDAO,
                secuenciaDAO, comprobanteDAO, notaCreditoRegistroDAO, notaCreditoDetalleDAO,
                historialProductoDAO, cajaSesionDAO, cajaMovimientoDAO, logDAO
        );
        this.notaDebitoService = new NotaDebitoService(
                empresaDAO, clienteDAO, facturaRegistroDAO, secuenciaDAO, comprobanteDAO,
                notaDebitoRegistroDAO, notaDebitoMotivoDAO, cajaSesionDAO, cajaMovimientoDAO, logDAO
        );
        this.guiaRemisionService = new GuiaRemisionService(
                empresaDAO, clienteDAO, facturaRegistroDAO, secuenciaDAO, comprobanteDAO,
                guiaRemisionRegistroDAO, guiaRemisionDestinatarioDAO, guiaRemisionDetalleDAO, logDAO
        );
        this.retencionService = new RetencionService(
                empresaDAO, proveedorDAO, secuenciaDAO, comprobanteDAO,
                retencionRegistroDAO, retencionDocumentoSustentoDAO, retencionDetalleDAO, logDAO
        );
        this.transferenciaInventarioService = new TransferenciaInventarioService(
                inventarioDAO, sucursalDAO, transferenciaInventarioDAO, logDAO
        );
        this.alertaService = new AlertaService(alertaDAO, inventarioDAO, cuentaPorCobrarDAO);
        this.cajaService = new CajaService(cajaSesionDAO, cajaMovimientoDAO);
        this.certificadoAlertaService = new CertificadoAlertaService(certificadoEstadoDAO, empresaDAO);
        this.servicioReintentoSri = new ServicioReintentoSri(
                comprobantePendienteSriDAO, comprobanteDAO, facturaRegistroDAO,
                notaCreditoRegistroDAO, notaDebitoRegistroDAO, guiaRemisionRegistroDAO, retencionRegistroDAO
        );
    }

    public static AppContext getInstance() {
        if (instancia == null) {
            synchronized (AppContext.class) {
                if (instancia == null) instancia = new AppContext();
            }
        }
        return instancia;
    }

    /** Para tests: permite reemplazar la instancia singleton con mocks */
    public static void setInstance(AppContext ctx) {
        instancia = ctx;
    }

    public static void reset() {
        instancia = null;
    }
}

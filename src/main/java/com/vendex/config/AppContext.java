package com.vendex.config;

import com.vendex.dao.*;
import com.vendex.service.*;

/**
 * Composition root — cableado manual de DAOs y servicios.
 * Centraliza la creación de implementaciones Postgres y su inyección en servicios.
 * Transición dual: código legacy que hace `new FacturaRegistroDAO()` debe migrar a `new FacturaRegistroDAOPostgres()`
 * o mejor a `AppContext.getInstance().facturaRegistroDAO`.
 */
public class AppContext {

    private static volatile AppContext instancia;

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
    public final TransferenciaInventarioDAO transferenciaInventarioDAO;

    // Servicios
    public final FacturaService facturaService;
    public final NotaCreditoService notaCreditoService;
    public final NotaDebitoService notaDebitoService;
    public final GuiaRemisionService guiaRemisionService;
    public final RetencionService retencionService;
    public final TransferenciaInventarioService transferenciaInventarioService;

    private AppContext() {
        // DAOs Postgres
        this.facturaRegistroDAO = new FacturaRegistroDAOPostgres();
        this.facturaDetalleDAO = new FacturaDetalleDAOPostgres();
        this.secuenciaDAO = new SecuenciaDocumentoDAOPostgres();
        this.comprobanteDAO = new ComprobanteDAOPostgres();
        this.notaCreditoRegistroDAO = new NotaCreditoRegistroDAOPostgres();
        this.notaCreditoDetalleDAO = new NotaCreditoDetalleDAOPostgres();
        this.notaDebitoRegistroDAO = new NotaDebitoRegistroDAOPostgres();
        this.notaDebitoMotivoDAO = new NotaDebitoMotivoDAOPostgres();
        this.guiaRemisionRegistroDAO = new GuiaRemisionRegistroDAOPostgres();
        this.guiaRemisionDestinatarioDAO = new GuiaRemisionDestinatarioDAOPostgres();
        this.guiaRemisionDetalleDAO = new GuiaRemisionDetalleDAOPostgres();
        this.retencionRegistroDAO = new RetencionRegistroDAOPostgres();
        this.retencionDocumentoSustentoDAO = new RetencionDocumentoSustentoDAOPostgres();
        this.retencionDetalleDAO = new RetencionDetalleDAOPostgres();
        this.inventarioDAO = new InventarioDAOPostgres();
        this.historialProductoDAO = new HistorialProductoDAOPostgres();
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOPostgres();
        this.clienteDAO = new ClienteDAOPostgres();
        this.empresaDAO = new EmpresaDAOPostgres();
        this.cajaSesionDAO = new CajaSesionDAOPostgres();
        this.cajaMovimientoDAO = new CajaMovimientoDAOPostgres();
        this.proveedorDAO = new ProveedorDAOPostgres();
        this.cuentaPorPagarDAO = new CuentaPorPagarDAOPostgres();
        this.facturaProveedorDAO = new FacturaProveedorDAOPostgres();
        this.vendedorDAO = new VendedorDAOPostgres();
        this.usuarioDAO = new UsuarioDAOPostgres();
        this.rolDAO = new RolDAOPostgres();
        this.permisoDAO = new PermisoDAOPostgres();
        this.auditoriaAccionDAO = new AuditoriaAccionDAOPostgres();
        this.ventaResumenDAO = new VentaResumenDAOPostgres();
        this.certificadoEstadoDAO = new CertificadoEstadoDAOPostgres();
        this.comprobantePendienteSriDAO = new ComprobantePendienteSriDAOPostgres();
        this.comprobanteTempDAO = new ComprobanteTempDAOPostgres();
        this.marcaDAO = new MarcaDAOPostgres();
        this.notaVentaRegistroDAO = new NotaVentaRegistroDAOPostgres();
        this.ubicacionDetalleDAO = new UbicacionDetalleDAOPostgres();
        this.logDAO = new LogDAOPostgres();
        this.grupoDAO = new GrupoDAOPostgres();
        this.tablaRetencionDAO = new TablaRetencionDAOPostgres();
        this.configuracionEmailDAO = new ConfiguracionEmailDAOPostgres();
        this.percheroDAO = new PercheroDAOPostgres();
        this.notaVentaDetalleDAO = new NotaVentaDetalleDAOPostgres();
        this.ubicacionPerchaDAO = new UbicacionPerchaDAOPostgres();
        this.codigoDAO = new CodigoDAOPostgres();
        this.repuestoChatbotDAO = new RepuestoChatbotDAOPostgres();
        this.alertaDAO = new AlertaDAOPostgres();
        this.loginIntentoLogDAO = new LoginIntentoLogDAOPostgres();
        this.dashboardDAO = new DashboardDAOPostgres();
        this.sucursalDAO = new SucursalDAOPostgres();
        this.transferenciaInventarioDAO = new TransferenciaInventarioDAOPostgres();

        // Servicios con inyección por constructor
        this.facturaService = new FacturaService(
                empresaDAO, clienteDAO, inventarioDAO, facturaRegistroDAO, facturaDetalleDAO,
                secuenciaDAO, comprobanteDAO, cuentaPorCobrarDAO, historialProductoDAO
        );
        this.notaCreditoService = new NotaCreditoService(
                empresaDAO, clienteDAO, inventarioDAO, facturaRegistroDAO, facturaDetalleDAO,
                secuenciaDAO, comprobanteDAO, notaCreditoRegistroDAO, notaCreditoDetalleDAO,
                historialProductoDAO, cajaSesionDAO, cajaMovimientoDAO
        );
        this.notaDebitoService = new NotaDebitoService(
                empresaDAO, clienteDAO, facturaRegistroDAO, secuenciaDAO, comprobanteDAO,
                notaDebitoRegistroDAO, notaDebitoMotivoDAO, cajaSesionDAO, cajaMovimientoDAO
        );
        this.guiaRemisionService = new GuiaRemisionService(
                empresaDAO, clienteDAO, facturaRegistroDAO, secuenciaDAO, comprobanteDAO,
                guiaRemisionRegistroDAO, guiaRemisionDestinatarioDAO, guiaRemisionDetalleDAO
        );
        this.retencionService = new RetencionService(
                empresaDAO, proveedorDAO, secuenciaDAO, comprobanteDAO,
                retencionRegistroDAO, retencionDocumentoSustentoDAO, retencionDetalleDAO
        );
        this.transferenciaInventarioService = new TransferenciaInventarioService(
                inventarioDAO, sucursalDAO, transferenciaInventarioDAO, logDAO
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

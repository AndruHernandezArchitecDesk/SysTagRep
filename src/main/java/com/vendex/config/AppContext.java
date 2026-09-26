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

    // Servicios
    public final FacturaService facturaService;
    public final NotaCreditoService notaCreditoService;
    public final NotaDebitoService notaDebitoService;
    public final GuiaRemisionService guiaRemisionService;
    public final RetencionService retencionService;

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

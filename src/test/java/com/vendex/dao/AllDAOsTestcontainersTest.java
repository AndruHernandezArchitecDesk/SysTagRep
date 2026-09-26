package com.vendex.dao;

import com.vendex.config.AppContext;
import com.vendex.config.DatabaseConnection;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class AllDAOsTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        String url = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String pass = postgres.getPassword();

        // Flyway migrate para crear todo el esquema (V1..V23)
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, pass)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load();
        flyway.migrate();

        DatabaseConnection.setConnectionParams(url, user, pass);

        // Asegurar que el pool se reinicie con la nueva URL
        DatabaseConnection.closePool();

        // Verificar que tablas existen
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("SELECT 1 FROM empresa LIMIT 1");
            st.execute("SELECT 1 FROM cliente LIMIT 1");
        }
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
        AppContext.reset();
    }

    @Test
    void todosLosDAOs_RespondenSinErrorTrasFlyway() throws Exception {
        AppContext ctx = AppContext.getInstance();

        // Verificar que cada DAO puede ejecutar su método principal de lectura sin excepción tras Flyway
        // Se usan los nombres reales de cada interface (evita falsos negativos por método inexistente)
        assertDoesNotThrow(() -> ctx.clienteDAO.listar(), "ClienteDAO.listar");
        assertDoesNotThrow(() -> ctx.proveedorDAO.listar(), "ProveedorDAO.listar");
        assertDoesNotThrow(() -> ctx.vendedorDAO.listar(), "VendedorDAO.listar");
        assertDoesNotThrow(() -> ctx.empresaDAO.listar(), "EmpresaDAO.listar");
        assertDoesNotThrow(() -> ctx.inventarioDAO.listar(), "InventarioDAO.listar (STRING_AGG)");
        assertDoesNotThrow(() -> ctx.marcaDAO.listar(), "MarcaDAO.listar");
        assertDoesNotThrow(() -> ctx.grupoDAO.listar(), "GrupoDAO.listar");
        assertDoesNotThrow(() -> ctx.ubicacionPerchaDAO.listar(), "UbicacionPerchaDAO.listar");
        assertDoesNotThrow(() -> ctx.percheroDAO.listar(), "PercheroDAO.listar");
        assertDoesNotThrow(() -> ctx.facturaRegistroDAO.listarPaginado(1, 10, null), "FacturaRegistroDAO.listarPaginado");
        assertDoesNotThrow(() -> ctx.facturaDetalleDAO.listarPorFacturaRegistroId(1), "FacturaDetalleDAO.listarPorFacturaRegistroId");
        assertDoesNotThrow(() -> ctx.secuenciaDAO.obtener("FACTURA"), "SecuenciaDocumentoDAO.obtener");
        assertDoesNotThrow(() -> ctx.comprobanteDAO.consultarSecuencial("FACTURA"), "ComprobanteDAO.consultarSecuencial");
        assertDoesNotThrow(() -> ctx.notaCreditoRegistroDAO.listarPendientesSri(), "NotaCreditoRegistroDAO.listarPendientesSri");
        assertDoesNotThrow(() -> ctx.notaCreditoDetalleDAO.listarPorNotaCreditoId(1), "NotaCreditoDetalleDAO.listarPorNotaCreditoId");
        assertDoesNotThrow(() -> ctx.notaDebitoRegistroDAO.listarPendientesSri(), "NotaDebitoRegistroDAO.listarPendientesSri");
        assertDoesNotThrow(() -> ctx.notaDebitoMotivoDAO.listarPorNotaDebitoId(1), "NotaDebitoMotivoDAO.listarPorNotaDebitoId");
        assertDoesNotThrow(() -> ctx.guiaRemisionRegistroDAO.listarPendientesSri(), "GuiaRemisionRegistroDAO.listarPendientesSri");
        assertDoesNotThrow(() -> ctx.guiaRemisionDestinatarioDAO.listarPorGuiaId(1), "GuiaRemisionDestinatarioDAO.listarPorGuiaId");
        assertDoesNotThrow(() -> ctx.guiaRemisionDetalleDAO.listarPorDestinatarioId(1), "GuiaRemisionDetalleDAO.listarPorDestinatarioId");
        assertDoesNotThrow(() -> ctx.retencionRegistroDAO.listarPendientesSri(), "RetencionRegistroDAO.listarPendientesSri");
        assertDoesNotThrow(() -> ctx.retencionDocumentoSustentoDAO.listarPorRetencionId(1), "RetencionDocumentoSustentoDAO.listarPorRetencionId");
        assertDoesNotThrow(() -> ctx.retencionDetalleDAO.listarPorDocumentoId(1), "RetencionDetalleDAO.listarPorDocumentoId");
        assertDoesNotThrow(() -> ctx.tablaRetencionDAO.listarTodas(), "TablaRetencionDAO.listarTodas");
        assertDoesNotThrow(() -> ctx.cajaSesionDAO.obtenerAbierta(), "CajaSesionDAO.obtenerAbierta");
        assertDoesNotThrow(() -> ctx.cajaMovimientoDAO.listarPorSesion(1), "CajaMovimientoDAO.listarPorSesion");
        assertDoesNotThrow(() -> ctx.usuarioDAO.listar(), "UsuarioDAO.listar");
        assertDoesNotThrow(() -> ctx.rolDAO.listar(), "RolDAO.listar");
        assertDoesNotThrow(() -> ctx.permisoDAO.listarTodos(), "PermisoDAO.listarTodos");
        assertDoesNotThrow(() -> ctx.alertaDAO.listarTodas(), "AlertaDAO.listarTodas");
        assertDoesNotThrow(() -> ctx.dashboardDAO.ventasPorDia(7), "DashboardDAO.ventasPorDia");
        assertDoesNotThrow(() -> ctx.ventaResumenDAO.listar(), "VentaResumenDAO.listar");
        assertDoesNotThrow(() -> ctx.historialProductoDAO.listar(), "HistorialProductoDAO.listar");
        assertDoesNotThrow(() -> ctx.comprobantePendienteSriDAO.listarTodos(10), "ComprobantePendienteSriDAO.listarTodos");
        assertDoesNotThrow(() -> ctx.logDAO.guardar("Test", "test", "msg"), "LogDAO.guardar");
        assertDoesNotThrow(() -> ctx.configuracionEmailDAO.obtenerActiva(), "ConfiguracionEmailDAO.obtenerActiva");
        assertDoesNotThrow(() -> ctx.certificadoEstadoDAO.obtenerUltimoEmailEnviado("test"), "CertificadoEstadoDAO.obtenerUltimoEmailEnviado");
        assertDoesNotThrow(() -> ctx.loginIntentoLogDAO.contarFallosRecientes("test", 15), "LoginIntentoLogDAO.contarFallosRecientes");
        assertDoesNotThrow(() -> ctx.notaVentaRegistroDAO.obtenerNumNotaVenta(), "NotaVentaRegistroDAO.obtenerNumNotaVenta");
        assertDoesNotThrow(() -> ctx.facturaProveedorDAO.listarFacturas(java.time.LocalDate.now(), java.time.LocalDate.now()), "FacturaProveedorDAO.listarFacturas");
        assertDoesNotThrow(() -> ctx.codigoDAO.listar(), "CodigoDAO.listar");
        assertDoesNotThrow(() -> ctx.repuestoChatbotDAO.buscar("tapa radiador", null, null, null), "RepuestoChatbotDAO.buscar");
        assertDoesNotThrow(() -> ctx.ubicacionDetalleDAO.listarOcupados(), "UbicacionDetalleDAO.listarOcupados");
    }

    @Test
    void appContext_todosLosDAOsNoNulos() {
        AppContext ctx = AppContext.getInstance();
        assertNotNull(ctx.clienteDAO);
        assertNotNull(ctx.proveedorDAO);
        assertNotNull(ctx.vendedorDAO);
        assertNotNull(ctx.empresaDAO);
        assertNotNull(ctx.inventarioDAO);
        assertNotNull(ctx.facturaRegistroDAO);
        assertNotNull(ctx.facturaDetalleDAO);
        assertNotNull(ctx.secuenciaDAO);
        assertNotNull(ctx.comprobanteDAO);
        assertNotNull(ctx.notaCreditoRegistroDAO);
        assertNotNull(ctx.notaDebitoRegistroDAO);
        assertNotNull(ctx.guiaRemisionRegistroDAO);
        assertNotNull(ctx.retencionRegistroDAO);
        assertNotNull(ctx.usuarioDAO);
        assertNotNull(ctx.rolDAO);
        assertNotNull(ctx.dashboardDAO);
        assertNotNull(ctx.logDAO);
        assertNotNull(ctx.repuestoChatbotDAO);
        assertNotNull(ctx.facturaProveedorDAO);
        assertNotNull(ctx.cuentaPorPagarDAO);
        assertNotNull(ctx.cuentaPorCobrarDAO);
    }
}

package com.vendex.service;

import com.vendex.config.AppContext;
import com.vendex.config.DatabaseConnection;
import com.vendex.dao.InventarioDAO;
import com.vendex.dao.InventarioDAOPostgres;
import com.vendex.dao.SucursalDAO;
import com.vendex.dao.SucursalDAOPostgres;
import com.vendex.dao.TransferenciaInventarioDAO;
import com.vendex.dao.TransferenciaInventarioDAOPostgres;
import com.vendex.model.Inventario;
import com.vendex.model.Sucursal;
import com.vendex.model.Usuario;
import com.vendex.util.SesionActual;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class TransferenciaInventarioServiceTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres");

    private TransferenciaInventarioService service;
    private InventarioDAO inventarioDAO;
    private SucursalDAO sucursalDAO;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        String url = postgres.getJdbcUrl();
        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load();
        flyway.migrate();
        DatabaseConnection.setConnectionParams(url, postgres.getUsername(), postgres.getPassword());
        DatabaseConnection.closePool();

        inventarioDAO = new InventarioDAOPostgres();
        sucursalDAO = new SucursalDAOPostgres();
        TransferenciaInventarioDAO transDAO = new TransferenciaInventarioDAOPostgres();
        service = new TransferenciaInventarioService(inventarioDAO, sucursalDAO, transDAO, new com.vendex.dao.LogDAOPostgres());

        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("BODEGUERO");
        SesionActual.setUsuarioForTest(u);
        SesionActual.setPermisosForTest(Set.of("INVENTARIO_AJUSTAR"));

        // Crear segunda sucursal si no existe
        if (sucursalDAO.obtenerPorCodigo("002").isEmpty()) {
            Sucursal s2 = new Sucursal("002", "Sucursal Sur", "Av Sur", false);
            sucursalDAO.guardar(s2);
        }
        // Limpiar inventario y crear uno en sucursal 1
        try (var con = DatabaseConnection.getConnection(); var st = con.createStatement()) {
            st.execute("DELETE FROM transferencia_inventario");
            st.execute("DELETE FROM inventario");
        }
    }

    @AfterEach
    void tearDown() {
        SesionActual.cerrar();
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
        AppContext.reset();
    }

    @Test
    void transferir_descuentaOrigenIncrementaDestino_transaccional() throws Exception {
        // Crear inventario en sucursal 1
        Inventario inv = new Inventario();
        inv.setDescripcion("TORNILLO M6");
        inv.setCodigo("TORN001");
        inv.setCantidad(10);
        inv.setCostoSinIVA(new BigDecimal("1.00"));
        inv.setPrecioVenta(new BigDecimal("2.00"));
        inv.setFecha_ingreso(LocalDateTime.now());
        inv.setEstado(true);
        inv.setProveedorId(0);
        inv.setSucursalId(1);
        inv.setGrupoId(0);
        inv.setMarcaId(0);
        // Necesita grupo/marca/perchero existentes o null
        int idOrigen = inventarioDAO.guardar(inv);
        assertTrue(idOrigen > 0);

        var trans = service.transferir(idOrigen, 2, 4, "Reposición sucursal sur");
        assertNotNull(trans);
        assertEquals(1, trans.getOrigenSucursalId());
        assertEquals(2, trans.getDestinoSucursalId());
        assertEquals(4, trans.getCantidad());

        Inventario origenPost = inventarioDAO.obtenerPorId(idOrigen);
        assertEquals(6, origenPost.getCantidad(), "Origen debe quedar 6");

        Inventario destino = inventarioDAO.obtenerPorCodigoYSucursal("TORN001", 2);
        assertNotNull(destino, "Destino debe haberse creado");
        assertEquals(4, destino.getCantidad());
    }

    @Test
    void transferir_fallaSiStockInsuficiente_yNoAlteraStock() throws Exception {
        Inventario inv = new Inventario();
        inv.setDescripcion("TUERCA M8");
        inv.setCodigo("TUER001");
        inv.setCantidad(2);
        inv.setCostoSinIVA(new BigDecimal("0.50"));
        inv.setPrecioVenta(new BigDecimal("1.00"));
        inv.setFecha_ingreso(LocalDateTime.now());
        inv.setEstado(true);
        inv.setSucursalId(1);
        inv.setGrupoId(0);
        inv.setMarcaId(0);
        inv.setProveedorId(0);
        int id = inventarioDAO.guardar(inv);

        assertThrows(IllegalStateException.class, () -> service.transferir(id, 2, 5, "Intento excede stock"));

        Inventario post = inventarioDAO.obtenerPorId(id);
        assertEquals(2, post.getCantidad(), "Stock no debe cambiar tras fallo");
        assertNull(inventarioDAO.obtenerPorCodigoYSucursal("TUER001", 2), "No debe crearse destino si falla");
    }

    @Test
    void transferir_requierePermiso() {
        SesionActual.setPermisosForTest(Set.of()); // sin permiso
        assertThrows(com.vendex.exception.SinPermisoException.class, () -> service.transferir(1, 2, 1, "test"));
    }
}

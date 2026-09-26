package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Sucursal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class SucursalDAOTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres");

    private SucursalDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        String url = postgres.getJdbcUrl();
        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load();
        flyway.migrate();
        DatabaseConnection.setConnectionParams(url, postgres.getUsername(), postgres.getPassword());
        DatabaseConnection.closePool();
        dao = new SucursalDAOPostgres();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
    }

    @Test
    void listar_contieneMatrizPorDefecto() {
        var lista = dao.listar();
        assertFalse(lista.isEmpty(), "Debe existir al menos la sucursal Matriz 001");
        assertTrue(lista.stream().anyMatch(s -> "001".equals(s.getCodigo()) && s.isEsCentral()));
    }

    @Test
    void guardar_y_obtenerPorCodigo() {
        Sucursal s = new Sucursal("002", "Sucursal Sur", "Av. Sur 123", false);
        s.setTelefono("0999999999");
        int id = dao.guardar(s);
        assertTrue(id > 0);
        var opt = dao.obtenerPorCodigo("002");
        assertTrue(opt.isPresent());
        assertEquals("Sucursal Sur", opt.get().getNombre());
        assertFalse(opt.get().isEsCentral());
    }

    @Test
    void inventario_sucursalId_default1_trasFlyway() throws Exception {
        try (var con = DatabaseConnection.getConnection();
             var ps = con.prepareStatement("SELECT sucursal_id FROM inventario LIMIT 1")) {
            // Si no hay filas, la columna debe existir (no lanza does not exist)
            try { ps.executeQuery(); } catch (Exception e) {
                fail("Columna sucursal_id debe existir tras V24: " + e.getMessage());
            }
        }
        // Verificar índice
        try (var con = DatabaseConnection.getConnection();
             var rs = con.createStatement().executeQuery("SELECT indexname FROM pg_indexes WHERE tablename='inventario' AND indexname='idx_inventario_sucursal'")) {
            assertTrue(rs.next(), "Índice idx_inventario_sucursal debe existir");
        }
    }
}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
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
public class SecuenciaDocumentoDAOTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private SecuenciaDocumentoDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available - skipping Testcontainers test");
        assertTrue(postgres.isRunning(), "Postgres container should be running - requires Docker");
        String url = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String pass = postgres.getPassword();
        DatabaseConnection.setConnectionParams(url, user, pass);

        // Crear tabla secuencia_documento (DDL Postgres)
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS secuencia_documento (" +
                    "tipo VARCHAR(50) PRIMARY KEY, " +
                    "prefijo VARCHAR(10), " +
                    "establecimiento VARCHAR(3), " +
                    "punto_emision VARCHAR(3), " +
                    "siguiente_numero INT NOT NULL DEFAULT 1)");
            st.execute("DELETE FROM secuencia_documento");
            st.execute("INSERT INTO secuencia_documento(tipo, prefijo, establecimiento, punto_emision, siguiente_numero) VALUES ('FACTURA','001','001','001',1)");
        }
        dao = new SecuenciaDocumentoDAOPostgres();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
    }

    @Test
    void marcarUsado_incrementaSecuencialAtomico() {
        int primero = dao.marcarUsado("FACTURA");
        int segundo = dao.marcarUsado("FACTURA");
        assertEquals(1, primero, "Primer uso debe ser 1");
        assertEquals(2, segundo, "Segundo uso debe ser 2");
    }

    @Test
    void obtener_devuelveEstablecimientoYPunto() {
        var sec = dao.obtener("FACTURA");
        assertNotNull(sec);
        assertEquals("FACTURA", sec.getTipo());
        assertEquals("001", sec.getEstablecimiento());
        assertEquals("001", sec.getPuntoEmision());
        assertEquals(1, sec.getSiguienteNumero());
    }

    @Test
    void marcarUsado_conConnection_transaccional() throws Exception {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            int usado = dao.marcarUsado(con, "FACTURA");
            assertEquals(1, usado);
            con.rollback();
            // Después de rollback, el siguiente debe seguir siendo 1 (no se consumió)
            int despues = dao.marcarUsado("FACTURA");
            assertEquals(1, despues, "Rollback debe liberar el secuencial");
        }
    }
}

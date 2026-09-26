package com.vendex.api;

import com.vendex.config.AppContext;
import com.vendex.config.ApiServer;
import com.vendex.config.DatabaseConnection;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class ApiIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("postgres")
            .withPassword("postgres");

    private static int port = 18080;
    private static HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        String url = postgres.getJdbcUrl();
        Flyway flyway = Flyway.configure().dataSource(url, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load();
        flyway.migrate();
        DatabaseConnection.setConnectionParams(url, postgres.getUsername(), postgres.getPassword());
        DatabaseConnection.closePool();
        AppContext.reset();
        // Forzar AppContext a usar el container
        AppContext ctx = AppContext.getInstance();
        // Iniciar API en puerto aleatorio
        port = 18080 + (int) (Math.random() * 1000);
        System.setProperty("api.port", String.valueOf(port));
        ApiServer.start(port);
        Thread.sleep(500);
    }

    @AfterAll
    static void tearDown() {
        ApiServer.stop();
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
        AppContext.reset();
        System.clearProperty("api.port");
    }

    @Test
    void health_respondeUP() throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/health")).GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("UP"));
    }

    @Test
    void sucursales_listar_contieneMatriz() throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/sucursales")).GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("001") && res.body().contains("Matriz"));
    }

    @Test
    void inventario_listarPorSucursal() throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/inventario?sucursalId=1")).GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().startsWith("["));
    }
}

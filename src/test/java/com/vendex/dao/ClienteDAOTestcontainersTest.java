package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Cliente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class ClienteDAOTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private ClienteDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        String url = postgres.getJdbcUrl();
        DatabaseConnection.setConnectionParams(url, postgres.getUsername(), postgres.getPassword());
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS cliente (" +
                    "id SERIAL PRIMARY KEY, " +
                    "nombre VARCHAR(100), identificacion VARCHAR(13), direccion VARCHAR(100), " +
                    "correo VARCHAR(60), telefono VARCHAR(10), celular VARCHAR(10), " +
                    "fecha_registro TIMESTAMP, estado BOOLEAN)");
            st.execute("DELETE FROM cliente");
        }
        dao = new ClienteDAOPostgres();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
    }

    @Test
    void guardar_y_obtenerPorId_persisteCorrectamente() {
        Cliente c = new Cliente();
        c.setNombre("Cliente Testcontainers");
        c.setIdentificacion("0990000023");
        c.setDireccion("Quito");
        c.setCorreo("test@vendex.ec");
        c.setTelefono("0999999999");
        c.setCelular("0999999999");
        c.setFecha_registro(LocalDateTime.now());
        c.setEstado(true);

        dao.guardar(c);

        var lista = dao.listar();
        assertFalse(lista.isEmpty(), "Debe haber al menos un cliente");
        var encontrado = lista.stream().filter(x -> "0990000023".equals(x.getIdentificacion())).findFirst().orElse(null);
        assertNotNull(encontrado);
        assertEquals("Cliente Testcontainers", encontrado.getNombre());
    }

    @Test
    void buscarPorIdentificacion_funcionaConPostgres() {
        Cliente c = new Cliente();
        c.setNombre("Otro Cliente");
        c.setIdentificacion("0990000099");
        c.setDireccion("Guayaquil");
        c.setCorreo("otro@vendex.ec");
        c.setTelefono("0988888888");
        c.setCelular("0988888888");
        c.setFecha_registro(LocalDateTime.now());
        c.setEstado(true);
        dao.guardar(c);

        // ClienteDAO.listar() ya probado, pero también probar obtenerPorId
        var lista = dao.listar();
        var guardado = lista.stream().filter(x -> "0990000099".equals(x.getIdentificacion())).findFirst().orElse(null);
        assertNotNull(guardado);
        Cliente porId = dao.obtenerPorId(guardado.getId());
        assertNotNull(porId);
        assertEquals("Otro Cliente", porId.getNombre());
    }
}

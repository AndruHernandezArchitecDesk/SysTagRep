package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.PuntoEmision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class PuntoEmisionDAOTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private PuntoEmisionDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        String url = postgres.getJdbcUrl();
        DatabaseConnection.setConnectionParams(url, postgres.getUsername(), postgres.getPassword());
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS sucursal (" +
                    "id SERIAL PRIMARY KEY, codigo VARCHAR(3) UNIQUE NOT NULL, nombre VARCHAR(100) NOT NULL, " +
                    "direccion VARCHAR(300), telefono VARCHAR(20), es_central BOOLEAN NOT NULL DEFAULT false, " +
                    "activo BOOLEAN NOT NULL DEFAULT true, creado_en TIMESTAMP NOT NULL DEFAULT now())");
            st.execute("INSERT INTO sucursal(codigo, nombre, direccion, es_central, activo) " +
                    "VALUES ('001','Sucursal Test','Direccion test',false,true) " +
                    "ON CONFLICT (codigo) DO NOTHING");
            st.execute("CREATE TABLE IF NOT EXISTS punto_emision (" +
                    "id SERIAL PRIMARY KEY, sucursal_id INTEGER NOT NULL REFERENCES sucursal(id), " +
                    "codigo VARCHAR(3) NOT NULL, descripcion VARCHAR(100), activo BOOLEAN NOT NULL DEFAULT true, " +
                    "creado_en TIMESTAMP NOT NULL DEFAULT now(), UNIQUE (sucursal_id, codigo))");
            st.execute("DELETE FROM punto_emision");
        }
        dao = new PuntoEmisionDAOPostgres();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
    }

    @Test
    void listarPorSucursal_devuelvePuntosDeLaSucursal() throws Exception {
        int sucursalId = 1;
        PuntoEmision p = new PuntoEmision(sucursalId, "001", "Punto principal");
        dao.guardar(p);

        List<PuntoEmision> lista = dao.listarPorSucursal(sucursalId);
        assertFalse(lista.isEmpty(), "Debe haber al menos un punto");
        assertEquals("001", lista.get(0).getCodigo());
    }

    @Test
    void listarPorSucursal_sinPuntos_retornaListaVacia() throws Exception {
        List<PuntoEmision> lista = dao.listarPorSucursal(1);
        assertTrue(lista.isEmpty());
    }

    @Test
    void obtenerPorId_persisteCorrectamente() throws Exception {
        PuntoEmision p = new PuntoEmision(1, "001", "Punto principal");
        dao.guardar(p);
        var encontrada = dao.listarPorSucursal(1).stream().findFirst().orElse(null);
        assertNotNull(encontrada);
        Optional<PuntoEmision> porId = dao.obtenerPorId(encontrada.getId());
        assertTrue(porId.isPresent());
        assertEquals("001", porId.get().getCodigo());
    }
}

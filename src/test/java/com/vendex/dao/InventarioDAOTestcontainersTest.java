package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Inventario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class InventarioDAOTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private InventarioDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        DatabaseConnection.setConnectionParams(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS proveedor (id SERIAL PRIMARY KEY, nombre VARCHAR(100), ruc VARCHAR(13))");
            st.execute("CREATE TABLE IF NOT EXISTS grupo (id SERIAL PRIMARY KEY, nombre VARCHAR(100))");
            st.execute("CREATE TABLE IF NOT EXISTS marca (id SERIAL PRIMARY KEY, nombre VARCHAR(100))");
            st.execute("CREATE TABLE IF NOT EXISTS ubicacion_percha (id SERIAL PRIMARY KEY, nombre VARCHAR(100))");
            st.execute("CREATE TABLE IF NOT EXISTS ubicacion (id SERIAL PRIMARY KEY, id_producto INT, codigo_ubicacion VARCHAR(20))");
            st.execute("CREATE TABLE IF NOT EXISTS inventario (" +
                    "id SERIAL PRIMARY KEY, descripcion VARCHAR(200), grupo_id INT, marca_id INT, " +
                    "costo_sin_iva NUMERIC(12,2), cantidad INT, ubicacion_percha_id INT, precio_venta NUMERIC(12,2), " +
                    "fecha_ingreso TIMESTAMP, estado BOOLEAN, tag_codigo VARCHAR(50), codigo VARCHAR(50), " +
                    "proveedor_id INT, forma_pago VARCHAR(20), meses_plazo INT, interes NUMERIC(5,2), numero_factura VARCHAR(50))");
            st.execute("DELETE FROM ubicacion");
            st.execute("DELETE FROM inventario");
            st.execute("DELETE FROM proveedor");
            st.execute("INSERT INTO proveedor(id, nombre) VALUES (1, 'Proveedor Test')");
            st.execute("INSERT INTO grupo(id, nombre) VALUES (1, 'Grupo Test')");
            st.execute("INSERT INTO marca(id, nombre) VALUES (1, 'Marca Test')");
            st.execute("INSERT INTO ubicacion_percha(id, nombre) VALUES (1, 'Percha A')");
        }
        dao = new InventarioDAOPostgres();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
    }

    @Test
    void guardar_y_listar_conStringAgg_funcionaEnPostgres() {
        Inventario inv = new Inventario();
        inv.setDescripcion("TAPA RADIADOR CHEV AVEO");
        inv.setGrupoId(1);
        inv.setMarcaId(1);
        inv.setCostoSinIVA(new BigDecimal("10.00"));
        inv.setCantidad(5);
        inv.setUbicacionPerchaId(1);
        inv.setPrecioVenta(new BigDecimal("15.00"));
        inv.setFecha_ingreso(LocalDateTime.now());
        inv.setEstado(true);
        inv.setCodigo("TAPA001");
        inv.setTagCodigo("TAG001");
        inv.setProveedorId(1);
        inv.setNumeroFactura("001-001-000000001");

        int id = dao.guardar(inv);
        assertTrue(id > 0, "ID debe generarse");

        // Insertar ubicaciones para probar STRING_AGG (caso que fallaba en HSQLDB)
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("INSERT INTO ubicacion(id_producto, codigo_ubicacion) VALUES (" + id + ", 'A1-01'), (" + id + ", 'A1-02')");
        } catch (Exception e) { fail("No debe fallar insert ubicacion: " + e.getMessage()); }

        List<Inventario> lista = dao.listar();
        assertFalse(lista.isEmpty());
        Inventario encontrado = lista.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
        assertNotNull(encontrado);
        // STRING_AGG debe haber agregado ubicaciones
        assertNotNull(encontrado.getUbicacionPercha());
        assertTrue(encontrado.getUbicacionPercha().contains("A1-01") || encontrado.getUbicacionPercha().contains("A1-02"));
    }

    @Test
    void descontarStock_transaccional_conHikari() throws Exception {
        Inventario inv = new Inventario();
        inv.setDescripcion("FILTRO ACEITE");
        inv.setGrupoId(1);
        inv.setMarcaId(1);
        inv.setCostoSinIVA(new BigDecimal("5.00"));
        inv.setCantidad(10);
        inv.setUbicacionPerchaId(1);
        inv.setPrecioVenta(new BigDecimal("8.00"));
        inv.setFecha_ingreso(LocalDateTime.now());
        inv.setEstado(true);
        inv.setCodigo("FILTRO001");
        inv.setProveedorId(1);
        int id = dao.guardar(inv);

        dao.descontarStock(id, 3);
        Inventario actualizado = dao.obtenerPorId(id);
        assertEquals(7, actualizado.getCantidad());

        // Probar rollback: descontar dentro de tx y hacer rollback no debe cambiar stock
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            dao.descontarStock(con, id, 2);
            con.rollback();
        }
        Inventario despuesRollback = dao.obtenerPorId(id);
        assertEquals(7, despuesRollback.getCantidad(), "Rollback debe mantener stock");
    }
}

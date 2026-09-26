package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.FacturaRegistro;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers(disabledWithoutDocker = true)
public class FacturaRegistroDAOTestcontainersTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private FacturaRegistroDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(org.testcontainers.DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        DatabaseConnection.setConnectionParams(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS empresa (id SERIAL PRIMARY KEY, ruc VARCHAR(13), razon_social VARCHAR(150), sucursal VARCHAR(150), direccion_calle_principal VARCHAR(150), direccion_calle_secundaria VARCHAR(150), telefono VARCHAR(20), celular VARCHAR(20), correo VARCHAR(100), logo_url VARCHAR(255), agente_retencion VARCHAR(20), resolucion VARCHAR(50), estado BOOLEAN)");
            st.execute("CREATE TABLE IF NOT EXISTS cliente (id SERIAL PRIMARY KEY, nombre VARCHAR(100), identificacion VARCHAR(13), direccion VARCHAR(100), correo VARCHAR(60), telefono VARCHAR(10), celular VARCHAR(10), fecha_registro TIMESTAMP, estado BOOLEAN)");
            st.execute("CREATE TABLE IF NOT EXISTS comprobantes_electronicos (id SERIAL PRIMARY KEY, documento_relacionado_id INT, tipo_comprobante VARCHAR(2), clave_acceso VARCHAR(49), numero_comprobante VARCHAR(20), ambiente VARCHAR(10), estado_sri VARCHAR(20), mensaje_sri VARCHAR(500), xml_generado TEXT, xml_autorizado TEXT, numero_autorizacion VARCHAR(49), fecha_autorizacion TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS factura_registro (id SERIAL PRIMARY KEY, empresa_id INT, cliente_id INT, fecha TIMESTAMP, codigo VARCHAR(30), forma_pago VARCHAR(30), subtotal NUMERIC(12,2), iva NUMERIC(12,2), descuento NUMERIC(12,2), total NUMERIC(12,2), clave_acceso VARCHAR(49), num_comprobante VARCHAR(20), ambiente_sri VARCHAR(10), estado_sri VARCHAR(20), fecha_registro TIMESTAMP)");
            st.execute("DELETE FROM factura_registro");
            st.execute("DELETE FROM comprobantes_electronicos");
            st.execute("DELETE FROM cliente");
            st.execute("DELETE FROM empresa");
            st.execute("INSERT INTO empresa(id, ruc, razon_social, sucursal, direccion_calle_principal, direccion_calle_secundaria, telefono, celular, correo, estado) VALUES (1, '1790000000001', 'TAG REPUESTOS', 'Matriz', 'Av. Principal', 'Secundaria', '0999999999', '0999999999', 'tag@example.com', TRUE)");
            st.execute("INSERT INTO cliente(id, nombre, identificacion, direccion, correo, telefono, celular, fecha_registro, estado) VALUES (1, 'Cliente Test', '1790000000001', 'Quito', 'cliente@test.ec', '0999999999', '0999999999', CURRENT_TIMESTAMP, TRUE)");
        }
        dao = new FacturaRegistroDAOPostgres();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.resetToDefault();
        DatabaseConnection.closePool();
    }

    @Test
    void insertar_y_obtenerPorClaveAcceso_persisteConReturningId() {
        FacturaRegistro fr = new FacturaRegistro();
        fr.setEmpresaId(1);
        fr.setClienteId(1);
        fr.setFecha(LocalDateTime.now());
        fr.setCodigo("F001");
        fr.setFormaPago("Efectivo");
        fr.setSubtotal(new BigDecimal("100.00"));
        fr.setIva(new BigDecimal("15.00"));
        fr.setDescuento(BigDecimal.ZERO);
        fr.setTotal(new BigDecimal("115.00"));
        fr.setClaveAcceso("1234567890123456789012345678901234567890123456789");
        fr.setNumComprobante("001-001-000000001");
        fr.setAmbienteSri("PRUEBAS");
        fr.setEstadoSri("PENDIENTE");
        fr.setFechaRegistro(LocalDateTime.now());

        int id = dao.insertar(fr);
        assertTrue(id > 0, "ID debe generarse via RETURNING");

        FacturaRegistro recuperado = dao.obtenerPorClaveAcceso("1234567890123456789012345678901234567890123456789");
        assertNotNull(recuperado);
        assertEquals("F001", recuperado.getCodigo());
        assertEquals(new BigDecimal("115.00").setScale(2), recuperado.getTotal().setScale(2));
        assertEquals("PENDIENTE", recuperado.getEstadoSri());
    }

    @Test
    void actualizarEstado_cambiaEstadoSri() {
        FacturaRegistro fr = new FacturaRegistro();
        fr.setEmpresaId(1);
        fr.setClienteId(1);
        fr.setFecha(LocalDateTime.now());
        fr.setCodigo("F002");
        fr.setFormaPago("Efectivo");
        fr.setSubtotal(new BigDecimal("50.00"));
        fr.setIva(new BigDecimal("7.50"));
        fr.setDescuento(BigDecimal.ZERO);
        fr.setTotal(new BigDecimal("57.50"));
        fr.setClaveAcceso("2234567890123456789012345678901234567890123456789");
        fr.setNumComprobante("001-001-000000002");
        fr.setAmbienteSri("PRUEBAS");
        fr.setEstadoSri("PENDIENTE");
        fr.setFechaRegistro(LocalDateTime.now());
        int id = dao.insertar(fr);
        assertTrue(id > 0);

        dao.actualizarEstado("2234567890123456789012345678901234567890123456789", "AUTORIZADO");
        FacturaRegistro actualizado = dao.obtenerPorClaveAcceso("2234567890123456789012345678901234567890123456789");
        assertEquals("AUTORIZADO", actualizado.getEstadoSri());
    }

    @Test
    void insertar_conConnection_transaccional_rollbackNoPersiste() throws Exception {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            FacturaRegistro fr = new FacturaRegistro();
            fr.setEmpresaId(1);
            fr.setClienteId(1);
            fr.setFecha(LocalDateTime.now());
            fr.setCodigo("F003");
            fr.setFormaPago("Efectivo");
            fr.setSubtotal(new BigDecimal("10.00"));
            fr.setIva(new BigDecimal("1.50"));
            fr.setDescuento(BigDecimal.ZERO);
            fr.setTotal(new BigDecimal("11.50"));
            fr.setClaveAcceso("3234567890123456789012345678901234567890123456789");
            fr.setNumComprobante("001-001-000000003");
            fr.setAmbienteSri("PRUEBAS");
            fr.setEstadoSri("PENDIENTE");
            fr.setFechaRegistro(LocalDateTime.now());
            int id = dao.insertar(con, fr);
            assertTrue(id > 0);
            con.rollback();

            // Fuera de la transacción no debe existir
            FacturaRegistro noDebeExistir = dao.obtenerPorClaveAcceso("3234567890123456789012345678901234567890123456789");
            assertNull(noDebeExistir, "Rollback debe descartar el insert");
        }
    }
}

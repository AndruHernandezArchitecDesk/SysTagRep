package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HerramientasAgenteVehiculoTest {
    @BeforeEach void crearDatosDePrueba() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:agente_vehiculo_" + UUID.randomUUID(), "sa", "");
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE proveedor (id INT PRIMARY KEY, nombre VARCHAR(100))");
            st.execute("CREATE TABLE inventario (id INT PRIMARY KEY, codigo VARCHAR(30), descripcion VARCHAR(200), "
                    + "cantidad INT, precio_venta DECIMAL(12,2), costo_sin_iva DECIMAL(12,2), proveedor_id INT, "
                    + "estado BOOLEAN, sucursal_id INT)");
            st.execute("CREATE TABLE vehiculo (id INT PRIMARY KEY, marca VARCHAR(40), modelo VARCHAR(40), "
                    + "anio_desde INT, anio_hasta INT, motor VARCHAR(40))");
            st.execute("CREATE TABLE inventario_vehiculo (inventario_id INT, vehiculo_id INT)");
            st.execute("INSERT INTO proveedor VALUES (1,'Proveedor Uno'),(2,'Proveedor Dos')");
            st.execute("INSERT INTO inventario VALUES "
                    + "(1,'BUJ-18','BUJIAS MOTOR COROLLA 1.8',5,8.00,4.00,1,true,1),"
                    + "(2,'BUJ-X','BUJIAS COROLLA 1.8 EXTRA',7,9.00,5.00,2,true,2),"
                    + "(3,'BUJ-TXT','BUJIAS COROLLA 1.8 TEXTO SIN ASOCIACION',9,10.00,6.00,1,true,1)");
            st.execute("INSERT INTO vehiculo VALUES (10,'TOYOTA','COROLLA',2014,2016,'1.8'),"
                    + "(11,'TOYOTA','COROLLA',2014,2016,'2.0')");
            st.execute("INSERT INTO inventario_vehiculo VALUES (1,10),(2,10),(2,11)");
        }
    }

    @AfterEach void restaurarConexion() { DatabaseConnection.resetToDefault(); }

    @Test void consultaPrecioDevuelveCostoMargenYProveedorSoloDeSucursalActiva() {
        var encontrados = new HerramientasAgenteDAOPostgres(1).productos("bujias", 10);
        assertEquals(2, encontrados.size());
        assertFalse(encontrados.stream().anyMatch(p -> "BUJ-X".equals(p.codigo())));
        assertTrue(encontrados.stream().allMatch(p -> "Proveedor Uno".equals(p.proveedor())));
        var p = encontrados.stream().filter(i -> "BUJ-18".equals(i.codigo())).findFirst().orElseThrow();
        assertEquals(50.0, p.margenPct());
    }

    @Test void compatibilidadRequiereRelacionRegistradaVehiculoAnioMotorYParte() {
        var dao = new HerramientasAgenteDAOPostgres(1);
        var encontrados = dao.compatibles("bujías", "Toyota", "Corolla", 2015, "1.8", 5);
        assertEquals(1, encontrados.size());
        assertEquals("BUJ-18", encontrados.get(0).codigo());
        assertTrue(dao.compatibles("bujías", "Toyota", "Corolla", 2015, "2.0", 5).isEmpty());
        assertTrue(dao.compatibles("frenos", "Toyota", "Corolla", 2015, "1.8", 5).isEmpty());
    }
}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HerramientasAgenteComplementariosTest {

    private final HerramientasAgenteDAOPostgres dao = new HerramientasAgenteDAOPostgres();

    @BeforeEach
    void prepararInventario() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:complementos_" + UUID.randomUUID(), "sa", "");
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("CREATE TABLE grupo (id INT PRIMARY KEY, nombre VARCHAR(100))");
            st.execute("CREATE TABLE marca (id INT PRIMARY KEY, nombre VARCHAR(100))");
            st.execute("CREATE TABLE inventario (id INT PRIMARY KEY, codigo VARCHAR(50), descripcion VARCHAR(200), " +
                    "cantidad INT, precio_venta DECIMAL(12,2), grupo_id INT, marca_id INT, estado BOOLEAN)");
            st.execute("INSERT INTO grupo VALUES (1,'REFRIGERACION'), (2,'ELECTRICO')");
            st.execute("INSERT INTO marca VALUES (1,'CHEV'), (2,'BOSCH')");
            st.execute("INSERT INTO inventario VALUES " +
                    "(1,'TAPA-01','TAPA RADIADOR CHEV AVEO',0,25.90,1,1,true)," +
                    "(2,'MANG-02','MANGUERA RADIADOR CHEV AVEO',3,14.50,1,1,true)," +
                    "(3,'LIQ-03','LIQUIDO REFRIGERANTE',5,8.00,1,2,true)," +
                    "(4,'SIN-04','RADIADOR CHEV AVEO',0,40.00,1,1,true)," +
                    "(5,'INACT-05','RADIADOR CHEV AVEO',5,40.00,1,1,false)," +
                    "(6,'BAT-06','BATERIA BOSCH',4,60.00,2,2,true)," +
                    "(7,'TOKEN-07','ABRAZADERA RADIADOR',3,2.00,null,null,true)");
        }
    }

    @AfterEach
    void restaurarConexion() {
        DatabaseConnection.resetToDefault();
    }

    @Test
    void complementos_realesConStock_priorizaGrupoMarcaYTokens() {
        var resultado = dao.complementarios("tapa-01", 2);
        assertEquals(List.of("MANG-02", "LIQ-03"), resultado.stream().map(i -> i.getCodigo()).toList());
        assertEquals(new BigDecimal("14.50"), resultado.get(0).getPrecioVenta());
        assertEquals(3, resultado.get(0).getCantidad());
        assertEquals(1, dao.complementarios("TAPA-01", 1).size());
    }

    @Test
    void complementos_sinBaseOLimite_noDevuelveReferenciasAjenas() {
        assertTrue(dao.complementarios("INEXISTENTE", 2).isEmpty());
        assertTrue(dao.complementarios(null, 2).isEmpty());
        assertTrue(dao.complementarios("TAPA-01", 0).isEmpty());
    }

    @Test
    void complementos_codigoAmbiguo_noAsociaOtraReferencia() throws Exception {
        try (Connection con = DatabaseConnection.getConnection(); Statement st = con.createStatement()) {
            st.execute("INSERT INTO inventario VALUES (8,'TAPA-01','OTRA TAPA',1,20.00,2,2,true)");
        }
        assertTrue(dao.complementarios("TAPA-01", 2).isEmpty());
    }
}

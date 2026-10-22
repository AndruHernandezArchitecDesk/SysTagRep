package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventarioStockNegativoTest {
    private final InventarioDAOPostgres dao=new InventarioDAOPostgres();
    @BeforeEach void crear() throws Exception {
        DatabaseConnection.setConnectionParams("jdbc:hsqldb:mem:stock_negative_"+UUID.randomUUID()+";sql.syntax_pgs=true","sa","");
        try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement()) {
            s.execute("CREATE TABLE inventario(id INTEGER PRIMARY KEY,codigo VARCHAR(50),descripcion VARCHAR(300),cantidad INTEGER,fecha_ingreso TIMESTAMP,estado BOOLEAN,sucursal_id INTEGER)");
            s.execute("INSERT INTO inventario VALUES " +
                    "(1,'OLD','Viejo',-2,TIMESTAMP '2025-01-01 10:00:00',true,7)," +
                    "(2,'NEW','Nuevo',-1,TIMESTAMP '2026-06-01 10:00:00',true,7)," +
                    "(3,'NULL','Sin fecha',-5,null,true,7)," +
                    "(4,'POS','Positivo',3,TIMESTAMP '2026-12-01 10:00:00',true,7)," +
                    "(5,'OTHER','Otra sucursal',-8,TIMESTAMP '2026-10-01 00:00:00',true,8)," +
                    "(6,'INACTIVE','Inactivo',-3,TIMESTAMP '2026-11-01 00:00:00',false,7)");
        }
    }
    @AfterEach void cerrar(){DatabaseConnection.resetToDefault();}
    @Test void negativosFiltraActivosPorSucursal_yOrdenaAltaDescConNulosAlFinal() throws Exception {
        List<com.vendex.model.StockNegativoItem> lista=dao.listarStockNegativoPorFecha(7);
        assertEquals(List.of("NEW","OLD","NULL"),lista.stream().map(com.vendex.model.StockNegativoItem::codigo).toList());
        assertEquals(-5,lista.get(2).cantidad());
    }
    @Test void sucursalInvalida_seRechaza(){assertThrows(IllegalArgumentException.class,()->dao.listarStockNegativoPorFecha(0));}
}

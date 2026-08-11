package com.tag.sysTagRep.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class InventarioStockTest {

    @Test
    void inventarioCreacion_gettersYSetters_funcionan() {
        Inventario inv = new Inventario();
        inv.setId(1);
        inv.setDescripcion("Aceite 5W30");
        inv.setCantidad(10);
        inv.setPrecioVenta(new BigDecimal("15.50"));
        inv.setGrupo("Lubricantes");
        inv.setMarca("Bosch");

        assertEquals(1, inv.getId());
        assertEquals("Aceite 5W30", inv.getDescripcion());
        assertEquals(10, inv.getCantidad());
        assertEquals(new BigDecimal("15.50"), inv.getPrecioVenta());
        assertEquals("Lubricantes", inv.getGrupo());
        assertEquals("Bosch", inv.getMarca());
    }

    @Test
    void inventarioStockDisponible_conCantidadPositiva() {
        Inventario inv = new Inventario();
        inv.setCantidad(5);
        assertTrue(inv.getCantidad() > 0);
    }

    @Test
    void inventarioStockDisponible_conCantidadCero() {
        Inventario inv = new Inventario();
        inv.setCantidad(0);
        assertEquals(0, inv.getCantidad());
    }

    @Test
    void inventarioPrecioVenta_formatoDosDecimales() {
        Inventario inv = new Inventario();
        inv.setPrecioVenta(new BigDecimal("10.00"));
        assertEquals(0, new BigDecimal("10.00").compareTo(inv.getPrecioVenta()));
    }

    @Test
    void inventarioCamposOpcionales_aceptaNulos() {
        Inventario inv = new Inventario();
        inv.setDescripcion("Producto");
        inv.setGrupo(null);
        inv.setMarca(null);
        assertNull(inv.getGrupo());
        assertNull(inv.getMarca());
    }
}

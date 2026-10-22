package com.vendex.chatbot;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fase agente IA — herramientas de LECTURA del ERP para el chatbot.
 * El chatbot consulta datos reales del negocio (nunca los inventa).
 * Implementación local: HerramientasAgenteDAOPostgres. En modo remoto es null
 * y el agente degrada a la búsqueda de repuestos de siempre.
 */
public interface HerramientasAgente {

    /** Productos por código exacto o descripción (ILIKE). */
    List<Producto> productos(String criterio, int limite);

    /** Complementos locales con stock; vacío si esta implementación no los soporta. */
    default List<com.vendex.model.Inventario> complementarios(String codigo, int limite) {
        return List.of();
    }

    /** Clientes por nombre (LIKE) o identificación exacta. */
    List<Cliente> clientes(String criterio, int limite);

    /** Resumen de facturas de un cliente. */
    Ventas ventas(int clienteId);

    /** Saldo total pendiente (cuentas por cobrar) de un cliente; null si no calculable. */
    BigDecimal saldo(int clienteId);

    /** Top productos por monto facturado (factura_detalle). */
    List<Top> masVendidos(int limite);

    /** Top productos por margen (precio venta vs costo sin IVA, criterio de Análisis IA). */
    List<Top> mejorMargen(int limite);

    record Producto(String codigo, String descripcion, int stock,
                    BigDecimal precioVenta, BigDecimal costoSinIva, double margenPct) {}

    record Cliente(int id, String nombre, String identificacion) {}

    record Ventas(int facturas, BigDecimal total, String ultimaFecha) {}

    record Top(String codigo, String descripcion, int cantidad, BigDecimal monto, Double margenPct) {}
}

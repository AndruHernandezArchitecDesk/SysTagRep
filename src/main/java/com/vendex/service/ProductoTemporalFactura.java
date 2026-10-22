package com.vendex.service;

import com.vendex.model.FacturaDetalle;
import java.math.BigDecimal;
import java.util.*;

/** Temporal de factura: id de inventario 0 hasta que la transacción de emisión lo registre. */
public final class ProductoTemporalFactura {
    private ProductoTemporalFactura() {}

    public static String codigo(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    public static FacturaDetalle crear(String codigo, String descripcion, int cantidad, BigDecimal precio) {
        String cod = codigo(codigo);
        String desc = descripcion == null ? "" : descripcion.trim().toUpperCase(Locale.ROOT);
        if (cod.isEmpty() || cod.length() > 50) throw new IllegalArgumentException("Código obligatorio, máximo 50 caracteres.");
        if (desc.isEmpty() || desc.length() > 300) throw new IllegalArgumentException("Descripción obligatoria, máximo 300 caracteres.");
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser un entero mayor a cero.");
        if (!precioValido(precio))
            throw new IllegalArgumentException("Precio unitario mayor a cero, con hasta 2 decimales y máximo 10 cifras enteras.");
        return new FacturaDetalle(0, cod, desc, cantidad, precio.setScale(2));
    }

    /** Misma regla que el guardado temporal y el XML: > 0, hasta 2 decimales, máximo 10 cifras enteras. */
    public static boolean precioValido(BigDecimal precio) {
        return precio != null && precio.signum() > 0 && precio.compareTo(new BigDecimal("10000000000")) < 0
                && precio.stripTrailingZeros().scale() <= 2;
    }

    /** Copia el borrador sin asignarle IDs que podrían quedar inválidos tras un rollback. */
    public static List<FacturaDetalle> copiarYValidar(List<FacturaDetalle> originales) {
        List<FacturaDetalle> copia = new ArrayList<>();
        Map<String, Integer> repeticiones = new HashMap<>();
        for (FacturaDetalle d : originales) {
            if (d == null || d.getInventarioId() < 0) throw new IllegalArgumentException("Detalle de factura inválido.");
            repeticiones.merge(codigo(d.getCodigo()), 1, Integer::sum);
            FacturaDetalle nuevo = d.getInventarioId() == 0
                    ? crear(d.getCodigo(), d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario())
                    : new FacturaDetalle(d.getInventarioId(), d.getCodigo(), d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario());
            if (d.getInventarioId() > 0) nuevo.setPrecioTotal(d.getPrecioTotal());
            copia.add(nuevo);
        }
        for (FacturaDetalle d : copia) {
            if (d.getInventarioId() == 0 && repeticiones.get(d.getCodigo()) > 1)
                throw new IllegalArgumentException("Código temporal repetido en la factura: " + d.getCodigo());
        }
        return copia;
    }

    /** Mismo contrato para envío inmediato y cola offline; incluye los datos del temporal. */
    public static Map<String, Object> payload(FacturaDetalle detalle) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("inventarioId", detalle.getInventarioId());
        item.put("cantidad", detalle.getCantidad());
        item.put("precioUnitario", detalle.getPrecioUnitario().toPlainString());
        if (detalle.getInventarioId() == 0) {
            item.put("temporal", true);
            item.put("codigo", detalle.getCodigo());
            item.put("descripcion", detalle.getDescripcion());
        }
        return item;
    }
}

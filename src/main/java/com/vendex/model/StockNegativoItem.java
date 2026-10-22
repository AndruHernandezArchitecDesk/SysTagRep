package com.vendex.model;

import java.time.LocalDateTime;

/** Proyección ligera del panel; la fecha ISO permite transportar los datos por REST. */
public record StockNegativoItem(int id, String codigo, String descripcion, int cantidad, String fechaAlta) {
    public LocalDateTime fecha() {
        return fechaAlta == null || fechaAlta.isBlank() ? null : LocalDateTime.parse(fechaAlta);
    }
}

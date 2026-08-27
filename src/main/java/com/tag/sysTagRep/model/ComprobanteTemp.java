package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ComprobanteTemp {
    private int id;
    private int proformaId;
    private String codigo;
    private String descripcion;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
    private LocalDateTime fechaCreacion;

    public ComprobanteTemp() {}

    public ComprobanteTemp(String codigo, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.precioTotal = precioUnitario.multiply(new BigDecimal(cantidad));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProformaId() { return proformaId; }
    public void setProformaId(int proformaId) { this.proformaId = proformaId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        recalcularTotal();
    }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        recalcularTotal();
    }

    public BigDecimal getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(BigDecimal precioTotal) { this.precioTotal = precioTotal; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    private void recalcularTotal() {
        if (precioUnitario != null) {
            this.precioTotal = precioUnitario.multiply(new BigDecimal(cantidad));
        }
    }
}

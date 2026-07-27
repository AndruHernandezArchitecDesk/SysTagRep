package com.tag.sysTagRep.model;

import java.math.BigDecimal;

public class FacturaDetalle {
    private int id;
    private int facturaRegistroId;
    private int inventarioId;
    private String codigo;
    private String descripcion;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal descuento;
    private BigDecimal total;

    public FacturaDetalle() {}

    public FacturaDetalle(int inventarioId, String codigo, String descripcion, int cantidad,
                          BigDecimal precioUnitario) {
        this.inventarioId = inventarioId;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.precioTotal = precioUnitario.multiply(new BigDecimal(cantidad));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFacturaRegistroId() { return facturaRegistroId; }
    public void setFacturaRegistroId(int facturaRegistroId) { this.facturaRegistroId = facturaRegistroId; }

    public int getInventarioId() { return inventarioId; }
    public void setInventarioId(int inventarioId) { this.inventarioId = inventarioId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        calcularPrecioTotal();
    }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        calcularPrecioTotal();
    }

    public BigDecimal getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(BigDecimal precioTotal) { this.precioTotal = precioTotal; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    private void calcularPrecioTotal() {
        if (precioUnitario != null) {
            this.precioTotal = precioUnitario.multiply(new BigDecimal(cantidad));
        }
    }
}

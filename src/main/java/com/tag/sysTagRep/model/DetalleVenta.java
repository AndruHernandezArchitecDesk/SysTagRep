package com.tag.sysTagRep.model;

import java.math.BigDecimal;

public class DetalleVenta {
    private int productoId;
    private String codigo;
    private String descripcion;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;

    public DetalleVenta() {}

    public DetalleVenta(int productoId, String codigo, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this.productoId = productoId;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.precioTotal = precioUnitario.multiply(new BigDecimal(cantidad));
    }

    // Getters y Setters
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }

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

    private void calcularPrecioTotal() {
        if (precioUnitario != null) {
            this.precioTotal = precioUnitario.multiply(new BigDecimal(cantidad));
        }
    }
}

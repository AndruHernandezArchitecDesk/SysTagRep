package com.vendex.model;

import java.math.BigDecimal;

public class NotaCreditoDetalle {
    private int id;
    private int notaCreditoId;
    private Integer facturaDetalleId;
    private Integer inventarioId;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;
    private String codigoPorcentajeIva;
    private BigDecimal precioTotalSinImpuesto;

    public NotaCreditoDetalle() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getNotaCreditoId() { return notaCreditoId; }
    public void setNotaCreditoId(int notaCreditoId) { this.notaCreditoId = notaCreditoId; }
    public Integer getFacturaDetalleId() { return facturaDetalleId; }
    public void setFacturaDetalleId(Integer facturaDetalleId) { this.facturaDetalleId = facturaDetalleId; }
    public Integer getInventarioId() { return inventarioId; }
    public void setInventarioId(Integer inventarioId) { this.inventarioId = inventarioId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    public String getCodigoPorcentajeIva() { return codigoPorcentajeIva; }
    public void setCodigoPorcentajeIva(String codigoPorcentajeIva) { this.codigoPorcentajeIva = codigoPorcentajeIva; }
    public BigDecimal getPrecioTotalSinImpuesto() { return precioTotalSinImpuesto; }
    public void setPrecioTotalSinImpuesto(BigDecimal precioTotalSinImpuesto) { this.precioTotalSinImpuesto = precioTotalSinImpuesto; }
}

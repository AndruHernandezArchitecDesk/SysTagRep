package com.tag.sysTagRep.model;

import java.math.BigDecimal;

public class DetalleVentaReporte {
    private String tipo;
    private String codigoComprobante;
    private String cliente;
    private String codigoProducto;
    private String descripcion;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal iva;
    private BigDecimal total;

    public DetalleVentaReporte() {}

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCodigoComprobante() { return codigoComprobante; }
    public void setCodigoComprobante(String codigoComprobante) { this.codigoComprobante = codigoComprobante; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}

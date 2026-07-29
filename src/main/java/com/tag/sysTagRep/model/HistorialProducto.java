package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HistorialProducto {
    private int id;
    private int productoId;
    private String productoCodigo;
    private String productoDescripcion;
    private int cantidad;
    private BigDecimal precioUnitario;
    private String tipoComprobante;
    private String codigoComprobante;
    private String clienteNombre;
    private String proveedorNombre;
    private LocalDateTime fechaVenta;
    private LocalDateTime fechaRegistro;

    public HistorialProducto() {}

    public HistorialProducto(int productoId, String productoCodigo, String productoDescripcion,
                             int cantidad, BigDecimal precioUnitario, String tipoComprobante,
                             String codigoComprobante, String clienteNombre,
                             String proveedorNombre, LocalDateTime fechaVenta) {
        this.productoId = productoId;
        this.productoCodigo = productoCodigo;
        this.productoDescripcion = productoDescripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.tipoComprobante = tipoComprobante;
        this.codigoComprobante = codigoComprobante;
        this.clienteNombre = clienteNombre;
        this.proveedorNombre = proveedorNombre;
        this.fechaVenta = fechaVenta;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }

    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }

    public String getProductoDescripcion() { return productoDescripcion; }
    public void setProductoDescripcion(String productoDescripcion) { this.productoDescripcion = productoDescripcion; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getCodigoComprobante() { return codigoComprobante; }
    public void setCodigoComprobante(String codigoComprobante) { this.codigoComprobante = codigoComprobante; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getProveedorNombre() { return proveedorNombre; }
    public void setProveedorNombre(String proveedorNombre) { this.proveedorNombre = proveedorNombre; }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

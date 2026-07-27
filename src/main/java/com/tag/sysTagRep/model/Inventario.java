package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Inventario {
    private int id;
    private String descripcion;
    private String grupo;
    private String marca;
    private BigDecimal costoSinIVA;
    private int cantidad;
    private String ubicacionPercha;
    private BigDecimal precioVenta;
    private LocalDateTime fecha_ingreso;
    private Boolean estado;
    private String codigo;
    private String proveedor;
    private int proveedorId;
    private int grupoId;
    private int marcaId;
    private int ubicacionPerchaId;
    private String formaPago;
    private int mesesPlazo;
    private BigDecimal interes;

    public Inventario() {}

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public BigDecimal getCostoSinIVA() { return costoSinIVA; }
    public void setCostoSinIVA(BigDecimal costoSinIVA) { this.costoSinIVA = costoSinIVA; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getUbicacionPercha() { return ubicacionPercha; }
    public void setUbicacionPercha(String ubicacionPercha) { this.ubicacionPercha = ubicacionPercha; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }

    public LocalDateTime getFecha_ingreso() { return fecha_ingreso; }
    public void setFecha_ingreso(LocalDateTime fecha_ingreso) { this.fecha_ingreso = fecha_ingreso; }

    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }

    public int getGrupoId() { return grupoId; }
    public void setGrupoId(int grupoId) { this.grupoId = grupoId; }

    public int getMarcaId() { return marcaId; }
    public void setMarcaId(int marcaId) { this.marcaId = marcaId; }

    public int getUbicacionPerchaId() { return ubicacionPerchaId; }
    public void setUbicacionPerchaId(int ubicacionPerchaId) { this.ubicacionPerchaId = ubicacionPerchaId; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public int getMesesPlazo() { return mesesPlazo; }
    public void setMesesPlazo(int mesesPlazo) { this.mesesPlazo = mesesPlazo; }

    public BigDecimal getInteres() { return interes; }
    public void setInteres(BigDecimal interes) { this.interes = interes; }
}

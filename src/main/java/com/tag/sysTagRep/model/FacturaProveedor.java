package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FacturaProveedor {
    private int id;
    private String numeroFactura;
    private int proveedorId;
    private String proveedor;
    private String codigo;
    private String codigoManual;
    private String descripcion;
    private int grupoId;
    private int marcaId;
    private BigDecimal costoSinIVA;
    private BigDecimal iva;
    private int cantidad;
    private BigDecimal totalLinea;
    private LocalDateTime fecha;

    public FacturaProveedor() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCodigoManual() { return codigoManual; }
    public void setCodigoManual(String codigoManual) { this.codigoManual = codigoManual; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getGrupoId() { return grupoId; }
    public void setGrupoId(int grupoId) { this.grupoId = grupoId; }

    public int getMarcaId() { return marcaId; }
    public void setMarcaId(int marcaId) { this.marcaId = marcaId; }

    public BigDecimal getCostoSinIVA() { return costoSinIVA; }
    public void setCostoSinIVA(BigDecimal costoSinIVA) { this.costoSinIVA = costoSinIVA; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getTotalLinea() { return totalLinea; }
    public void setTotalLinea(BigDecimal totalLinea) { this.totalLinea = totalLinea; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}

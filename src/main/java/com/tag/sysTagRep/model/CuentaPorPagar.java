package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CuentaPorPagar {
    private int id;
    private int inventarioId;
    private int proveedorId;
    private BigDecimal total;
    private int mesesPlazo;
    private BigDecimal interes;
    private BigDecimal cuotaMensual;
    private BigDecimal adelanto;
    private String estado;
    private LocalDateTime fechaRegistro;

    public CuentaPorPagar() {}

    public CuentaPorPagar(int inventarioId, int proveedorId, BigDecimal total, int mesesPlazo, BigDecimal interes, BigDecimal cuotaMensual) {
        this.inventarioId = inventarioId;
        this.proveedorId = proveedorId;
        this.total = total;
        this.mesesPlazo = mesesPlazo;
        this.interes = interes;
        this.cuotaMensual = cuotaMensual;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInventarioId() { return inventarioId; }
    public void setInventarioId(int inventarioId) { this.inventarioId = inventarioId; }

    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public int getMesesPlazo() { return mesesPlazo; }
    public void setMesesPlazo(int mesesPlazo) { this.mesesPlazo = mesesPlazo; }

    public BigDecimal getInteres() { return interes; }
    public void setInteres(BigDecimal interes) { this.interes = interes; }

    public BigDecimal getCuotaMensual() { return cuotaMensual; }
    public void setCuotaMensual(BigDecimal cuotaMensual) { this.cuotaMensual = cuotaMensual; }

    public BigDecimal getAdelanto() { return adelanto; }
    public void setAdelanto(BigDecimal adelanto) { this.adelanto = adelanto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

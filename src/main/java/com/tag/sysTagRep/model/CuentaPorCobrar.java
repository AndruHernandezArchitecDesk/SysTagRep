package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CuentaPorCobrar {
    private int id;
    private int notaVentaId;
    private int clienteId;
    private BigDecimal total;
    private int mesesPlazo;
    private BigDecimal interes;
    private BigDecimal cuotaMensual;
    private String estado;
    private LocalDateTime fechaRegistro;

    public CuentaPorCobrar() {}

    public CuentaPorCobrar(int notaVentaId, int clienteId, BigDecimal total, int mesesPlazo, BigDecimal interes, BigDecimal cuotaMensual) {
        this.notaVentaId = notaVentaId;
        this.clienteId = clienteId;
        this.total = total;
        this.mesesPlazo = mesesPlazo;
        this.interes = interes;
        this.cuotaMensual = cuotaMensual;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNotaVentaId() { return notaVentaId; }
    public void setNotaVentaId(int notaVentaId) { this.notaVentaId = notaVentaId; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public int getMesesPlazo() { return mesesPlazo; }
    public void setMesesPlazo(int mesesPlazo) { this.mesesPlazo = mesesPlazo; }

    public BigDecimal getInteres() { return interes; }
    public void setInteres(BigDecimal interes) { this.interes = interes; }

    public BigDecimal getCuotaMensual() { return cuotaMensual; }
    public void setCuotaMensual(BigDecimal cuotaMensual) { this.cuotaMensual = cuotaMensual; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

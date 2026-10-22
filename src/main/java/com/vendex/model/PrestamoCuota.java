package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrestamoCuota {
    private int id;
    private int prestamoId;
    private int numero;
    private LocalDate fechaVencimiento;
    private BigDecimal monto;
    private BigDecimal capital;
    private BigDecimal interes;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoRestante;
    private BigDecimal montoPagado;
    private BigDecimal interesPagado;
    private String estado;
    private LocalDate fechaPago;

    public PrestamoCuota() {}

    public PrestamoCuota(int prestamoId, int numero, LocalDate fechaVencimiento, BigDecimal monto,
                         BigDecimal capital, BigDecimal interes, BigDecimal saldoAnterior, BigDecimal saldoRestante) {
        this.prestamoId = prestamoId;
        this.numero = numero;
        this.fechaVencimiento = fechaVencimiento;
        this.monto = monto;
        this.capital = capital;
        this.interes = interes;
        this.saldoAnterior = saldoAnterior;
        this.saldoRestante = saldoRestante;
        this.montoPagado = BigDecimal.ZERO;
        this.interesPagado = BigDecimal.ZERO;
        this.estado = "PENDIENTE";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPrestamoId() { return prestamoId; }
    public void setPrestamoId(int prestamoId) { this.prestamoId = prestamoId; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getCapital() { return capital; }
    public void setCapital(BigDecimal capital) { this.capital = capital; }

    public BigDecimal getInteres() { return interes; }
    public void setInteres(BigDecimal interes) { this.interes = interes; }

    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(BigDecimal saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public BigDecimal getSaldoRestante() { return saldoRestante; }
    public void setSaldoRestante(BigDecimal saldoRestante) { this.saldoRestante = saldoRestante; }

    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }

    public BigDecimal getInteresPagado() { return interesPagado; }
    public void setInteresPagado(BigDecimal interesPagado) { this.interesPagado = interesPagado; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public BigDecimal getFaltante() {
        BigDecimal mp = montoPagado != null ? montoPagado : BigDecimal.ZERO;
        return monto.subtract(mp).max(BigDecimal.ZERO);
    }

    public boolean estaPagada() {
        return "PAGADA".equalsIgnoreCase(estado);
    }
}

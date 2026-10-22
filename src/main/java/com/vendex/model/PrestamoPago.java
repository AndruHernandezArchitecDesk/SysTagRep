package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PrestamoPago {
    private int id;
    private int prestamoId;
    private Integer cuotaId;
    private BigDecimal monto;
    private BigDecimal interesParte;
    private BigDecimal capitalParte;
    private String fuente;
    private Integer referenciaId;
    private Integer usuarioId;
    private String notas;
    private LocalDateTime fecha;

    public PrestamoPago() {}

    public PrestamoPago(int prestamoId, BigDecimal monto, String fuente) {
        this.prestamoId = prestamoId;
        this.monto = monto;
        this.fuente = fuente;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPrestamoId() { return prestamoId; }
    public void setPrestamoId(int prestamoId) { this.prestamoId = prestamoId; }

    public Integer getCuotaId() { return cuotaId; }
    public void setCuotaId(Integer cuotaId) { this.cuotaId = cuotaId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public BigDecimal getInteresParte() { return interesParte; }
    public void setInteresParte(BigDecimal interesParte) { this.interesParte = interesParte; }

    public BigDecimal getCapitalParte() { return capitalParte; }
    public void setCapitalParte(BigDecimal capitalParte) { this.capitalParte = capitalParte; }

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}

package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CajaMovimiento {
    private int id;
    private int sesionId;
    private String tipo;
    private BigDecimal monto;
    private String descripcion;
    private Integer referenciaId;
    private String referenciaTipo;
    private LocalDateTime fecha;
    private int usuarioId;

    public CajaMovimiento() {}

    public CajaMovimiento(int sesionId, String tipo, BigDecimal monto, String descripcion, int usuarioId) {
        this.sesionId = sesionId;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.usuarioId = usuarioId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSesionId() { return sesionId; }
    public void setSesionId(int sesionId) { this.sesionId = sesionId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }

    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String referenciaTipo) { this.referenciaTipo = referenciaTipo; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
}

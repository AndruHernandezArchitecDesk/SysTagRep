package com.vendex.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fase 6b: registro de una licencia emitida por la instancia central. */
public class LicenciaEmitida {
    private long id;
    private String machineCode;
    private String claveHash;
    private String modalidad;
    private boolean incluyeIA;
    private String roles;
    private long capacidad;
    private LocalDate expiracion;
    private Integer emitidaPor;
    private LocalDateTime fecha;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public String getClaveHash() { return claveHash; }
    public void setClaveHash(String claveHash) { this.claveHash = claveHash; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public boolean isIncluyeIA() { return incluyeIA; }
    public void setIncluyeIA(boolean incluyeIA) { this.incluyeIA = incluyeIA; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public long getCapacidad() { return capacidad; }
    public void setCapacidad(long capacidad) { this.capacidad = capacidad; }
    public LocalDate getExpiracion() { return expiracion; }
    public void setExpiracion(LocalDate expiracion) { this.expiracion = expiracion; }
    public Integer getEmitidaPor() { return emitidaPor; }
    public void setEmitidaPor(Integer emitidaPor) { this.emitidaPor = emitidaPor; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}

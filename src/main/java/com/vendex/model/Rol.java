package com.vendex.model;

import java.math.BigDecimal;

public class Rol {
    private int id;
    private String nombre;
    private String descripcion;
    private BigDecimal limiteDescuentoPct;

    public Rol() {}
    public Rol(int id, String nombre, String descripcion, BigDecimal limite) {
        this.id=id; this.nombre=nombre; this.descripcion=descripcion; this.limiteDescuentoPct=limite;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id=id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre=nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion=descripcion; }
    public BigDecimal getLimiteDescuentoPct() { return limiteDescuentoPct; }
    public void setLimiteDescuentoPct(BigDecimal limiteDescuentoPct) { this.limiteDescuentoPct = limiteDescuentoPct; }
    @Override public String toString() { return nombre; }
}

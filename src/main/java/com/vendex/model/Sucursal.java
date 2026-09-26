package com.vendex.model;

import java.time.LocalDateTime;

public class Sucursal {
    private int id;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private boolean esCentral;
    private boolean activo;
    private LocalDateTime creadoEn;

    public Sucursal() {}

    public Sucursal(String codigo, String nombre, String direccion, boolean esCentral) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.esCentral = esCentral;
        this.activo = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isEsCentral() { return esCentral; }
    public void setEsCentral(boolean esCentral) { this.esCentral = esCentral; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    @Override
    public String toString() { return codigo + " - " + nombre; }
}

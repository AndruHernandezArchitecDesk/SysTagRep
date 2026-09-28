package com.vendex.model;

public class PuntoEmision {
    private int id;
    private int sucursalId;
    private String codigo;
    private String descripcion;

    public PuntoEmision() {}

    public PuntoEmision(int sucursalId, String codigo, String descripcion) {
        this.sucursalId = sucursalId;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSucursalId() { return sucursalId; }
    public void setSucursalId(int sucursalId) { this.sucursalId = sucursalId; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() { return codigo + " - " + descripcion; }
}

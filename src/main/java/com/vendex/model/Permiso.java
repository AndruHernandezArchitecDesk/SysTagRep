package com.vendex.model;

public class Permiso {
    private String codigo;
    private String descripcion;
    private String categoria;

    public Permiso() {}
    public Permiso(String codigo, String descripcion, String categoria) {
        this.codigo=codigo; this.descripcion=descripcion; this.categoria=categoria;
    }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo=codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion=descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria=categoria; }
}

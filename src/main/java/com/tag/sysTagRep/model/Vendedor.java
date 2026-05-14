package com.tag.sysTagRep.model;

public class Vendedor {

    private int id;
    private String nombre;
    private String identificacion;
    private String correo;
    private boolean estado;

    public Vendedor() {}

    public Vendedor(String nombre, String identificacion, String correo, boolean estado) {
        this.nombre = nombre;
        this.identificacion = identificacion;
        this.correo = correo;
        this.estado = estado;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
}

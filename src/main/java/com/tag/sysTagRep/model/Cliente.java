package com.tag.sysTagRep.model;

import java.time.LocalDateTime;

public class Cliente {
    private int id;
    private String nombre;
    private String identificacion;
    private String direccion;
    private String correo;
    private String telefono;
    private String celular;
    private LocalDateTime fecha_registro;
    private boolean estado;

    public Cliente(){}

    public Cliente(String nombre, String identificacion, String direccion, String correo, String telefono, String celular){
        this.nombre = nombre;
        this.identificacion = identificacion;
        this.direccion = direccion;
        this.correo = correo;
        this.telefono = telefono;
        this.celular = celular;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public LocalDateTime getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(LocalDateTime fecha_registro) {
        this.fecha_registro = fecha_registro;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}

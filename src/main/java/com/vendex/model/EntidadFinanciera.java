package com.vendex.model;

import java.time.LocalDateTime;

public class EntidadFinanciera {
    private int id;
    private String nombre;
    private String tipo;
    private String identificacion;
    private String telefono;
    private String contacto;
    private String notas;
    private boolean estado;
    private LocalDateTime fechaRegistro;

    public EntidadFinanciera() {}

    public EntidadFinanciera(String nombre, String tipo, String identificacion, String telefono, String contacto) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.identificacion = identificacion;
        this.telefono = telefono;
        this.contacto = contacto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    @Override
    public String toString() { return nombre != null ? nombre : ""; }
}

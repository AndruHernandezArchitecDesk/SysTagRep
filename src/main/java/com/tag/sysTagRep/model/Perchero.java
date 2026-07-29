package com.tag.sysTagRep.model;

public class Perchero {
    private int id;
    private String nombrePerchero;
    private String seccion;
    private int cantidadLugares;
    private boolean estado;

    public Perchero() {}

    public Perchero(int id, String nombrePerchero, String seccion, int cantidadLugares, boolean estado) {
        this.id = id;
        this.nombrePerchero = nombrePerchero;
        this.seccion = seccion;
        this.cantidadLugares = cantidadLugares;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombrePerchero() { return nombrePerchero; }
    public void setNombrePerchero(String nombrePerchero) { this.nombrePerchero = nombrePerchero; }

    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }

    public int getCantidadLugares() { return cantidadLugares; }
    public void setCantidadLugares(int cantidadLugares) { this.cantidadLugares = cantidadLugares; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    @Override
    public String toString() { return nombrePerchero + " - " + seccion; }
}

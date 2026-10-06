package com.vendex.model;

import java.time.LocalDateTime;

public class AccesoDirecto {
    private int id;
    private int usuarioId;
    private String vistaRuta;
    private String titulo;
    private String iconoLiteral;
    private int posicion;
    private LocalDateTime creadoEn;

    public AccesoDirecto() {}

    public AccesoDirecto(int usuarioId, String vistaRuta, String titulo, String iconoLiteral, int posicion) {
        this.usuarioId = usuarioId;
        this.vistaRuta = vistaRuta;
        this.titulo = titulo;
        this.iconoLiteral = iconoLiteral;
        this.posicion = posicion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getVistaRuta() { return vistaRuta; }
    public void setVistaRuta(String vistaRuta) { this.vistaRuta = vistaRuta; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getIconoLiteral() { return iconoLiteral; }
    public void setIconoLiteral(String iconoLiteral) { this.iconoLiteral = iconoLiteral; }

    public int getPosicion() { return posicion; }
    public void setPosicion(int posicion) { this.posicion = posicion; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}

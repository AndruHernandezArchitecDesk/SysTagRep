package com.vendex.model;

import java.math.BigDecimal;

public class GuiaRemisionDetalle {
    private int id;
    private int guiaRemisionDestinatarioId;
    private Integer inventarioId; // opcional
    private String codigoInterno;
    private String descripcion;
    private BigDecimal cantidad;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGuiaRemisionDestinatarioId() { return guiaRemisionDestinatarioId; }
    public void setGuiaRemisionDestinatarioId(int v) { this.guiaRemisionDestinatarioId = v; }
    public Integer getInventarioId() { return inventarioId; }
    public void setInventarioId(Integer v) { this.inventarioId = v; }
    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String v) { this.codigoInterno = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal v) { this.cantidad = v; }
}

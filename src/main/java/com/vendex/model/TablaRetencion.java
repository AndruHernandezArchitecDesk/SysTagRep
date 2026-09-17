package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TablaRetencion {
    private int id;
    private String codigoRetencion;
    private String descripcion;
    private String tipo;
    private BigDecimal porcentaje;
    private LocalDate vigenteDesde;
    private LocalDate vigenteHasta;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCodigoRetencion() { return codigoRetencion; }
    public void setCodigoRetencion(String v) { this.codigoRetencion = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal v) { this.porcentaje = v; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public void setVigenteDesde(LocalDate v) { this.vigenteDesde = v; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public void setVigenteHasta(LocalDate v) { this.vigenteHasta = v; }
}

package com.vendex.model;

import java.time.LocalDate;

public class GuiaRemisionDestinatario {
    private int id;
    private int guiaRemisionId;
    private String identificacionDestinatario;
    private String razonSocialDestinatario;
    private String direccionDestinatario;
    private String motivoTraslado;
    private Integer facturaRegistroId; // opcional
    private String codDocSustento; // 01 para factura
    private String numDocSustento; // 001-001-000000001
    private String numAutDocSustento;
    private LocalDate fechaEmisionDocSustento;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGuiaRemisionId() { return guiaRemisionId; }
    public void setGuiaRemisionId(int v) { this.guiaRemisionId = v; }
    public String getIdentificacionDestinatario() { return identificacionDestinatario; }
    public void setIdentificacionDestinatario(String v) { this.identificacionDestinatario = v; }
    public String getRazonSocialDestinatario() { return razonSocialDestinatario; }
    public void setRazonSocialDestinatario(String v) { this.razonSocialDestinatario = v; }
    public String getDireccionDestinatario() { return direccionDestinatario; }
    public void setDireccionDestinatario(String v) { this.direccionDestinatario = v; }
    public String getMotivoTraslado() { return motivoTraslado; }
    public void setMotivoTraslado(String v) { this.motivoTraslado = v; }
    public Integer getFacturaRegistroId() { return facturaRegistroId; }
    public void setFacturaRegistroId(Integer v) { this.facturaRegistroId = v; }
    public String getCodDocSustento() { return codDocSustento; }
    public void setCodDocSustento(String v) { this.codDocSustento = v; }
    public String getNumDocSustento() { return numDocSustento; }
    public void setNumDocSustento(String v) { this.numDocSustento = v; }
    public String getNumAutDocSustento() { return numAutDocSustento; }
    public void setNumAutDocSustento(String v) { this.numAutDocSustento = v; }
    public LocalDate getFechaEmisionDocSustento() { return fechaEmisionDocSustento; }
    public void setFechaEmisionDocSustento(LocalDate v) { this.fechaEmisionDocSustento = v; }
}

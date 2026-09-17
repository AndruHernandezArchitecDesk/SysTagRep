package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RetencionDocumentoSustento {
    private int id;
    private int retencionId;
    private String codSustento = "01";
    private String codDocSustento = "01";
    private String numDocSustento;
    private LocalDate fechaEmisionDocSustento;
    private BigDecimal totalSinImpuestos;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRetencionId() { return retencionId; }
    public void setRetencionId(int v) { this.retencionId = v; }
    public String getCodSustento() { return codSustento; }
    public void setCodSustento(String v) { this.codSustento = v; }
    public String getCodDocSustento() { return codDocSustento; }
    public void setCodDocSustento(String v) { this.codDocSustento = v; }
    public String getNumDocSustento() { return numDocSustento; }
    public void setNumDocSustento(String v) { this.numDocSustento = v; }
    public LocalDate getFechaEmisionDocSustento() { return fechaEmisionDocSustento; }
    public void setFechaEmisionDocSustento(LocalDate v) { this.fechaEmisionDocSustento = v; }
    public BigDecimal getTotalSinImpuestos() { return totalSinImpuestos; }
    public void setTotalSinImpuestos(BigDecimal v) { this.totalSinImpuestos = v; }
}

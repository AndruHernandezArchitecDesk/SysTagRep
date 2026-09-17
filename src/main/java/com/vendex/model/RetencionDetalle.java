package com.vendex.model;

import java.math.BigDecimal;

public class RetencionDetalle {
    private int id;
    private int docSustentoId;
    private String codigo; // 1 Renta, 2 IVA
    private String codigoRetencion;
    private BigDecimal baseImponible;
    private BigDecimal porcentajeRetener;
    private BigDecimal valorRetenido;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getDocSustentoId() { return docSustentoId; }
    public void setDocSustentoId(int v) { this.docSustentoId = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { this.codigo = v; }
    public String getCodigoRetencion() { return codigoRetencion; }
    public void setCodigoRetencion(String v) { this.codigoRetencion = v; }
    public BigDecimal getBaseImponible() { return baseImponible; }
    public void setBaseImponible(BigDecimal v) { this.baseImponible = v; }
    public BigDecimal getPorcentajeRetener() { return porcentajeRetener; }
    public void setPorcentajeRetener(BigDecimal v) { this.porcentajeRetener = v; }
    public BigDecimal getValorRetenido() { return valorRetenido; }
    public void setValorRetenido(BigDecimal v) { this.valorRetenido = v; }
}

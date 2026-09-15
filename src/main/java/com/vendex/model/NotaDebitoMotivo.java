package com.vendex.model;

import java.math.BigDecimal;

public class NotaDebitoMotivo {
    private int id;
    private Integer notaDebitoId;
    private String razon;
    private BigDecimal valor;
    private boolean gravaIva;
    private String codigoPorcentajeIva;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getNotaDebitoId() { return notaDebitoId; }
    public void setNotaDebitoId(Integer notaDebitoId) { this.notaDebitoId = notaDebitoId; }
    public String getRazon() { return razon; }
    public void setRazon(String razon) { this.razon = razon; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public boolean isGravaIva() { return gravaIva; }
    public void setGravaIva(boolean gravaIva) { this.gravaIva = gravaIva; }
    public String getCodigoPorcentajeIva() { return codigoPorcentajeIva; }
    public void setCodigoPorcentajeIva(String codigoPorcentajeIva) { this.codigoPorcentajeIva = codigoPorcentajeIva; }
}

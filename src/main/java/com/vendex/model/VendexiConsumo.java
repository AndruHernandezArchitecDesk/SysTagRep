package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro de consumo de IA (Fase 3 Vendexi). */
public class VendexiConsumo {
    private long id;
    private LocalDateTime fecha;
    private String modulo;
    private String operacion;
    private String modelo;
    private long tokensEntrada;
    private long tokensSalida;
    private BigDecimal costoEstimado;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public long getTokensEntrada() { return tokensEntrada; }
    public void setTokensEntrada(long tokensEntrada) { this.tokensEntrada = tokensEntrada; }
    public long getTokensSalida() { return tokensSalida; }
    public void setTokensSalida(long tokensSalida) { this.tokensSalida = tokensSalida; }
    public BigDecimal getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(BigDecimal costoEstimado) { this.costoEstimado = costoEstimado; }

    public long getTotalTokens() { return tokensEntrada + tokensSalida; }
}

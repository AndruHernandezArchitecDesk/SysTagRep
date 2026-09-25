package com.vendex.model;

import java.time.LocalDateTime;

public class ComprobantePendienteSri {
    private int id;
    private Integer comprobanteId;
    private String tipoComprobante; // FACTURA, NOTA_CREDITO, NOTA_DEBITO, GUIA_REMISION, RETENCION
    private String claveAcceso;
    private int intentos;
    private LocalDateTime ultimoIntento;
    private LocalDateTime proximoIntento;
    private String estado; // PENDIENTE, RECHAZADA, AUTORIZADA, AGOTADA
    private String ultimoMensajeSri;
    private String numeroComprobante;
    private String ambiente;

    public ComprobantePendienteSri() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getComprobanteId() { return comprobanteId; }
    public void setComprobanteId(Integer comprobanteId) { this.comprobanteId = comprobanteId; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public LocalDateTime getUltimoIntento() { return ultimoIntento; }
    public void setUltimoIntento(LocalDateTime ultimoIntento) { this.ultimoIntento = ultimoIntento; }
    public LocalDateTime getProximoIntento() { return proximoIntento; }
    public void setProximoIntento(LocalDateTime proximoIntento) { this.proximoIntento = proximoIntento; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getUltimoMensajeSri() { return ultimoMensajeSri; }
    public void setUltimoMensajeSri(String ultimoMensajeSri) { this.ultimoMensajeSri = ultimoMensajeSri; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
}

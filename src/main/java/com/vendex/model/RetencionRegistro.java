package com.vendex.model;

import java.time.LocalDateTime;

public class RetencionRegistro {
    private int id;
    private String claveAcceso;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private String numComprobante;
    private LocalDateTime fechaEmision;
    private String periodoFiscal; // MM/YYYY
    private Integer proveedorId;
    private String tipoIdentificacionSujeto;
    private String razonSocialSujeto;
    private String identificacionSujeto;
    private String estadoSri;
    private String mensajeSri;
    private String numeroAutorizacion;
    private LocalDateTime fechaAutorizacion;
    private String xmlFirmado;
    private int usuarioId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String v) { this.claveAcceso = v; }
    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String v) { this.establecimiento = v; }
    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String v) { this.puntoEmision = v; }
    public String getSecuencial() { return secuencial; }
    public void setSecuencial(String v) { this.secuencial = v; }
    public String getNumComprobante() { return numComprobante != null ? numComprobante : (establecimiento + "-" + puntoEmision + "-" + secuencial); }
    public void setNumComprobante(String v) { this.numComprobante = v; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime v) { this.fechaEmision = v; }
    public String getPeriodoFiscal() { return periodoFiscal; }
    public void setPeriodoFiscal(String v) { this.periodoFiscal = v; }
    public Integer getProveedorId() { return proveedorId; }
    public void setProveedorId(Integer v) { this.proveedorId = v; }
    public String getTipoIdentificacionSujeto() { return tipoIdentificacionSujeto; }
    public void setTipoIdentificacionSujeto(String v) { this.tipoIdentificacionSujeto = v; }
    public String getRazonSocialSujeto() { return razonSocialSujeto; }
    public void setRazonSocialSujeto(String v) { this.razonSocialSujeto = v; }
    public String getIdentificacionSujeto() { return identificacionSujeto; }
    public void setIdentificacionSujeto(String v) { this.identificacionSujeto = v; }
    public String getEstadoSri() { return estadoSri; }
    public void setEstadoSri(String v) { this.estadoSri = v; }
    public String getMensajeSri() { return mensajeSri; }
    public void setMensajeSri(String v) { this.mensajeSri = v; }
    public String getNumeroAutorizacion() { return numeroAutorizacion; }
    public void setNumeroAutorizacion(String v) { this.numeroAutorizacion = v; }
    public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }
    public void setFechaAutorizacion(LocalDateTime v) { this.fechaAutorizacion = v; }
    public String getXmlFirmado() { return xmlFirmado; }
    public void setXmlFirmado(String v) { this.xmlFirmado = v; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int v) { this.usuarioId = v; }
}

package com.vendex.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuiaRemisionRegistro {
    private int id;
    private String claveAcceso;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private String numComprobante; // EEE-PPP-NNNNNNNNN
    private LocalDateTime fechaEmision;
    private String dirPartida;
    private String razonSocialTransportista;
    private String tipoIdentificacionTransportista; // 04 RUC, 05 cedula, 06 pasaporte
    private String rucTransportista;
    private String placa;
    private LocalDate fechaIniTransporte;
    private LocalDate fechaFinTransporte;
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
    public String getDirPartida() { return dirPartida; }
    public void setDirPartida(String v) { this.dirPartida = v; }
    public String getRazonSocialTransportista() { return razonSocialTransportista; }
    public void setRazonSocialTransportista(String v) { this.razonSocialTransportista = v; }
    public String getTipoIdentificacionTransportista() { return tipoIdentificacionTransportista; }
    public void setTipoIdentificacionTransportista(String v) { this.tipoIdentificacionTransportista = v; }
    public String getRucTransportista() { return rucTransportista; }
    public void setRucTransportista(String v) { this.rucTransportista = v; }
    public String getPlaca() { return placa; }
    public void setPlaca(String v) { this.placa = v; }
    public LocalDate getFechaIniTransporte() { return fechaIniTransporte; }
    public void setFechaIniTransporte(LocalDate v) { this.fechaIniTransporte = v; }
    public LocalDate getFechaFinTransporte() { return fechaFinTransporte; }
    public void setFechaFinTransporte(LocalDate v) { this.fechaFinTransporte = v; }
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

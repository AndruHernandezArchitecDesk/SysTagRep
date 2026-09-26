package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NotaDebitoRegistro {
    private int id;
    private String claveAcceso;
    private int facturaRegistroId;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private LocalDateTime fechaEmision;
    private int clienteId;
    private String formaPago;
    private BigDecimal totalSinImpuestos;
    private BigDecimal valorIva;
    private BigDecimal valorTotal;
    private String estadoSri;
    private String mensajeSri;
    private String numeroAutorizacion;
    private LocalDateTime fechaAutorizacion;
    private String xmlFirmado;
    private int usuarioId;
    private LocalDateTime creadoEn;
    private String nombreCliente;
    private String numComprobante;

    private int sucursalId = 1;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }
    public int getFacturaRegistroId() { return facturaRegistroId; }
    public void setFacturaRegistroId(int facturaRegistroId) { this.facturaRegistroId = facturaRegistroId; }
    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String establecimiento) { this.establecimiento = establecimiento; }
    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }
    public String getSecuencial() { return secuencial; }
    public void setSecuencial(String secuencial) { this.secuencial = secuencial; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public BigDecimal getTotalSinImpuestos() { return totalSinImpuestos; }
    public void setTotalSinImpuestos(BigDecimal totalSinImpuestos) { this.totalSinImpuestos = totalSinImpuestos; }
    public BigDecimal getValorIva() { return valorIva; }
    public void setValorIva(BigDecimal valorIva) { this.valorIva = valorIva; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getEstadoSri() { return estadoSri; }
    public void setEstadoSri(String estadoSri) { this.estadoSri = estadoSri; }
    public String getMensajeSri() { return mensajeSri; }
    public void setMensajeSri(String mensajeSri) { this.mensajeSri = mensajeSri; }
    public String getNumeroAutorizacion() { return numeroAutorizacion; }
    public void setNumeroAutorizacion(String numeroAutorizacion) { this.numeroAutorizacion = numeroAutorizacion; }
    public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }
    public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) { this.fechaAutorizacion = fechaAutorizacion; }
    public String getXmlFirmado() { return xmlFirmado; }
    public void setXmlFirmado(String xmlFirmado) { this.xmlFirmado = xmlFirmado; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getNumComprobante() { return numComprobante; }
    public void setNumComprobante(String numComprobante) { this.numComprobante = numComprobante; }

    public int getSucursalId() { return sucursalId; }
    public void setSucursalId(int sucursalId) { this.sucursalId = sucursalId; }
}

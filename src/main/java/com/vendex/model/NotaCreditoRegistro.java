package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NotaCreditoRegistro {
    private int id;
    private String claveAcceso;
    private int facturaRegistroId;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private LocalDateTime fechaEmision;
    private int clienteId;
    private String motivo;
    private String tipoMotivo; // DEVOLUCION | DESCUENTO | ANULACION
    private BigDecimal totalSinImpuestos;
    private BigDecimal valorIva;
    private BigDecimal valorModificacion;
    private boolean reingresaStock;
    private String estadoSri;
    private String mensajeSri;
    private String numeroAutorizacion;
    private LocalDateTime fechaAutorizacion;
    private String xmlFirmado;
    private int usuarioId;
    private LocalDateTime creadoEn;
    private String nombreCliente;
    private String numComprobante;

    public NotaCreditoRegistro() {}

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
    public String getNumComprobante() {
        if (numComprobante != null) return numComprobante;
        if (establecimiento != null && puntoEmision != null && secuencial != null)
            return establecimiento + "-" + puntoEmision + "-" + secuencial;
        return null;
    }
    public void setNumComprobante(String numComprobante) { this.numComprobante = numComprobante; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getTipoMotivo() { return tipoMotivo; }
    public void setTipoMotivo(String tipoMotivo) { this.tipoMotivo = tipoMotivo; }
    public BigDecimal getTotalSinImpuestos() { return totalSinImpuestos; }
    public void setTotalSinImpuestos(BigDecimal totalSinImpuestos) { this.totalSinImpuestos = totalSinImpuestos; }
    public BigDecimal getValorIva() { return valorIva; }
    public void setValorIva(BigDecimal valorIva) { this.valorIva = valorIva; }
    public BigDecimal getValorModificacion() { return valorModificacion; }
    public void setValorModificacion(BigDecimal valorModificacion) { this.valorModificacion = valorModificacion; }
    public boolean isReingresaStock() { return reingresaStock; }
    public void setReingresaStock(boolean reingresaStock) { this.reingresaStock = reingresaStock; }
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
}

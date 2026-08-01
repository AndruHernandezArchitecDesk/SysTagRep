package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FacturaRegistro {
    private int id;
    private int empresaId;
    private int clienteId;
    private LocalDateTime fecha;
    private String codigo;
    private String formaPago;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal descuento;
    private BigDecimal total;
    private String claveAcceso;
    private String numComprobante;
    private String ambienteSri;
    private String estadoSri;
    private String mensajeSri;
    private LocalDateTime fechaRegistro;
    private String nombreCliente;

    public FacturaRegistro() {}

    public FacturaRegistro(int empresaId, int clienteId, LocalDateTime fecha, String codigo,
                           String formaPago, BigDecimal subtotal, BigDecimal iva, BigDecimal descuento,
                           BigDecimal total,
                           String claveAcceso, String numComprobante, String ambienteSri) {
        this.empresaId = empresaId;
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.codigo = codigo;
        this.formaPago = formaPago;
        this.subtotal = subtotal;
        this.iva = iva;
        this.descuento = descuento;
        this.total = total;
        this.claveAcceso = claveAcceso;
        this.numComprobante = numComprobante;
        this.ambienteSri = ambienteSri;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEmpresaId() { return empresaId; }
    public void setEmpresaId(int empresaId) { this.empresaId = empresaId; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }

    public String getNumComprobante() { return numComprobante; }
    public void setNumComprobante(String numComprobante) { this.numComprobante = numComprobante; }

    public String getAmbienteSri() { return ambienteSri; }
    public void setAmbienteSri(String ambienteSri) { this.ambienteSri = ambienteSri; }

    public String getEstadoSri() { return estadoSri; }
    public void setEstadoSri(String estadoSri) { this.estadoSri = estadoSri; }

    public String getMensajeSri() { return mensajeSri; }
    public void setMensajeSri(String mensajeSri) { this.mensajeSri = mensajeSri; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
}

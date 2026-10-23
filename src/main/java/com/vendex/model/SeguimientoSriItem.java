package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila unificada del Seguimiento SRI (pestañas por tipo).
 * Propiedades compatibles con PropertyValueFactory del TableView.
 */
public class SeguimientoSriItem {

    private String tipo; // FACTURA, NC, ND, GUIA, RETENCION
    private int registroId;
    private String codigo;
    private String numComprobante;
    private LocalDateTime fecha;
    private String nombreCliente;
    private String formaPago;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
    private String estadoSri;
    private String mensajeSri;
    private String claveAcceso;
    private int refId; // cliente_id o proveedor_id (0 si no aplica)

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    /** Etiqueta corta para la columna Tipo. */
    public String getTipoLabel() {
        if (tipo == null) return "";
        return switch (tipo) {
            case "FACTURA" -> "FACTURA";
            case "NC" -> "NC";
            case "ND" -> "ND";
            case "GUIA" -> "GUÍA";
            case "RETENCION" -> "RET";
            default -> tipo;
        };
    }

    public int getRegistroId() { return registroId; }
    public void setRegistroId(int registroId) { this.registroId = registroId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNumComprobante() { return numComprobante; }
    public void setNumComprobante(String numComprobante) { this.numComprobante = numComprobante; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal iva) { this.iva = iva; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getEstadoSri() { return estadoSri; }
    public void setEstadoSri(String estadoSri) { this.estadoSri = estadoSri; }

    public String getMensajeSri() { return mensajeSri; }
    public void setMensajeSri(String mensajeSri) { this.mensajeSri = mensajeSri; }

    /** Explicación apta para usuario; el mensaje SRI original permanece disponible. */
    public String getExplicacionSri() {
        return com.vendex.util.MensajeSriUsuario.explicar(estadoSri, mensajeSri);
    }

    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }

    public int getRefId() { return refId; }
    public void setRefId(int refId) { this.refId = refId; }
}

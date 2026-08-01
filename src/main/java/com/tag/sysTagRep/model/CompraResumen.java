package com.tag.sysTagRep.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CompraResumen {
    private String numeroFactura;
    private String proveedor;
    private LocalDateTime fecha;
    private int items;
    private BigDecimal total;

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public int getItems() { return items; }
    public void setItems(int items) { this.items = items; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}

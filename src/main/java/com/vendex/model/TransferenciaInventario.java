package com.vendex.model;

import java.time.LocalDateTime;

public class TransferenciaInventario {
    private int id;
    private int inventarioId;
    private int origenSucursalId;
    private int destinoSucursalId;
    private int cantidad;
    private String estado;
    private Integer usuarioId;
    private String motivo;
    private LocalDateTime creadoEn;

    public TransferenciaInventario() {}

    public TransferenciaInventario(int inventarioId, int origenSucursalId, int destinoSucursalId, int cantidad, Integer usuarioId, String motivo) {
        this.inventarioId = inventarioId;
        this.origenSucursalId = origenSucursalId;
        this.destinoSucursalId = destinoSucursalId;
        this.cantidad = cantidad;
        this.usuarioId = usuarioId;
        this.motivo = motivo;
        this.estado = "COMPLETADA";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getInventarioId() { return inventarioId; }
    public void setInventarioId(int inventarioId) { this.inventarioId = inventarioId; }
    public int getOrigenSucursalId() { return origenSucursalId; }
    public void setOrigenSucursalId(int origenSucursalId) { this.origenSucursalId = origenSucursalId; }
    public int getDestinoSucursalId() { return destinoSucursalId; }
    public void setDestinoSucursalId(int destinoSucursalId) { this.destinoSucursalId = destinoSucursalId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}

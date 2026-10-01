package com.vendex.offline;

import java.time.LocalDateTime;

public class OperacionOffline {

    public enum Estado { PENDIENTE, ENVIADA, CONFLICTO, FALLIDA }

    private int id;
    private String tipo;
    private String payload;
    private LocalDateTime creadoEn;
    private int intentos;
    private LocalDateTime ultimoIntento;
    private Estado estado;
    private String error;

    public OperacionOffline() {}

    public OperacionOffline(String tipo, String payload) {
        this.tipo = tipo;
        this.payload = payload;
        this.creadoEn = LocalDateTime.now();
        this.estado = Estado.PENDIENTE;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public LocalDateTime getUltimoIntento() { return ultimoIntento; }
    public void setUltimoIntento(LocalDateTime ultimoIntento) { this.ultimoIntento = ultimoIntento; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}

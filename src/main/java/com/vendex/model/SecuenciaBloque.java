package com.vendex.model;

import java.time.LocalDateTime;

public class SecuenciaBloque {

    private int id;
    private int puntoEmisionId;
    private String tipo;
    private int numeroInicio;
    private int numeroFin;
    private int usadoHasta;
    private LocalDateTime reservadoEn;

    public SecuenciaBloque() {}

    public SecuenciaBloque(int puntoEmisionId, String tipo, int numeroInicio, int numeroFin, int usadoHasta) {
        this.puntoEmisionId = puntoEmisionId;
        this.tipo = tipo;
        this.numeroInicio = numeroInicio;
        this.numeroFin = numeroFin;
        this.usadoHasta = usadoHasta;
        this.reservadoEn = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPuntoEmisionId() { return puntoEmisionId; }
    public void setPuntoEmisionId(int puntoEmisionId) { this.puntoEmisionId = puntoEmisionId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getNumeroInicio() { return numeroInicio; }
    public void setNumeroInicio(int numeroInicio) { this.numeroInicio = numeroInicio; }
    public int getNumeroFin() { return numeroFin; }
    public void setNumeroFin(int numeroFin) { this.numeroFin = numeroFin; }
    public int getUsadoHasta() { return usadoHasta; }
    public void setUsadoHasta(int usadoHasta) { this.usadoHasta = usadoHasta; }
    public LocalDateTime getReservadoEn() { return reservadoEn; }
    public void setReservadoEn(LocalDateTime reservadoEn) { this.reservadoEn = reservadoEn; }

    public boolean estaAgotado() {
        return usadoHasta > numeroFin;
    }
}

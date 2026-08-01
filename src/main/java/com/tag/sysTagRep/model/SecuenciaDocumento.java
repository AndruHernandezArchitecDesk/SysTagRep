package com.tag.sysTagRep.model;

public class SecuenciaDocumento {
    private String tipo;
    private String establecimiento;
    private String puntoEmision;
    private int siguienteNumero;

    public SecuenciaDocumento() {}

    public SecuenciaDocumento(String tipo, String establecimiento, String puntoEmision, int siguienteNumero) {
        this.tipo = tipo;
        this.establecimiento = establecimiento;
        this.puntoEmision = puntoEmision;
        this.siguienteNumero = siguienteNumero;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String establecimiento) { this.establecimiento = establecimiento; }

    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }

    public int getSiguienteNumero() { return siguienteNumero; }
    public void setSiguienteNumero(int siguienteNumero) { this.siguienteNumero = siguienteNumero; }

    public String getProximoCodigo() {
        return establecimiento + "-" + puntoEmision + "-" + String.format("%09d", siguienteNumero);
    }
}

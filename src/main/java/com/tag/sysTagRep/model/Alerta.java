package com.tag.sysTagRep.model;

import java.time.LocalDateTime;

public class Alerta {
    private int id;
    private String tipo;
    private String mensaje;
    private Integer referenciaId;
    private String referenciaTipo;
    private boolean leida;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLectura;

    public Alerta() {}

    public Alerta(String tipo, String mensaje, Integer referenciaId, String referenciaTipo) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.referenciaId = referenciaId;
        this.referenciaTipo = referenciaTipo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }

    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String referenciaTipo) { this.referenciaTipo = referenciaTipo; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaLectura() { return fechaLectura; }
    public void setFechaLectura(LocalDateTime fechaLectura) { this.fechaLectura = fechaLectura; }
}

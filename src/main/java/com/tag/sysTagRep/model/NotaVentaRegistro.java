package com.tag.sysTagRep.model;

import java.time.LocalDateTime;

public class NotaVentaRegistro {
    private int id;
    private int empresaId;
    private int clienteId;
    private LocalDateTime fecha;
    private int codigo;
    private LocalDateTime fechaRegistro;

    public NotaVentaRegistro(){}

    public NotaVentaRegistro(int empresa_id, int cliente_id, LocalDateTime fecha, int codigo, LocalDateTime fecha_registro){
        this.empresaId = empresa_id;
        this.clienteId = cliente_id;
        this.fecha = fecha;
        this.codigo = codigo;
        this.fechaRegistro = fecha_registro;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(int empresaId) {
        this.empresaId = empresaId;
    }

    public int getClienteId() {
        return clienteId;
    }

    public void setClienteId(int clienteId) {
        this.clienteId = clienteId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public int getCodigo() {
        return codigo;
    }

    public void setCodigo(int codigo) {
        this.codigo = codigo;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}

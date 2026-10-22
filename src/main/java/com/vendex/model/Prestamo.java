package com.vendex.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Prestamo {
    private int id;
    private int entidadId;
    private String entidadNombre;
    private BigDecimal montoPrincipal;
    private BigDecimal tasaPeriodoPct;
    private String periodicidad;
    private int numeroCuotas;
    private BigDecimal cuotaMonto;
    private BigDecimal totalAPagar;
    private BigDecimal totalInteres;
    private LocalDate fechaInicio;
    private String estado;
    private String metaPeriodicidad;
    private BigDecimal metaMonto;
    private String notas;
    private LocalDateTime fechaRegistro;

    public Prestamo() {}

    public Prestamo(int entidadId, BigDecimal montoPrincipal, BigDecimal tasaPeriodoPct,
                    String periodicidad, int numeroCuotas, LocalDate fechaInicio) {
        this.entidadId = entidadId;
        this.montoPrincipal = montoPrincipal;
        this.tasaPeriodoPct = tasaPeriodoPct;
        this.periodicidad = periodicidad;
        this.numeroCuotas = numeroCuotas;
        this.fechaInicio = fechaInicio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEntidadId() { return entidadId; }
    public void setEntidadId(int entidadId) { this.entidadId = entidadId; }

    public String getEntidadNombre() { return entidadNombre; }
    public void setEntidadNombre(String entidadNombre) { this.entidadNombre = entidadNombre; }

    public BigDecimal getMontoPrincipal() { return montoPrincipal; }
    public void setMontoPrincipal(BigDecimal montoPrincipal) { this.montoPrincipal = montoPrincipal; }

    public BigDecimal getTasaPeriodoPct() { return tasaPeriodoPct; }
    public void setTasaPeriodoPct(BigDecimal tasaPeriodoPct) { this.tasaPeriodoPct = tasaPeriodoPct; }

    public String getPeriodicidad() { return periodicidad; }
    public void setPeriodicidad(String periodicidad) { this.periodicidad = periodicidad; }

    public int getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(int numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public BigDecimal getCuotaMonto() { return cuotaMonto; }
    public void setCuotaMonto(BigDecimal cuotaMonto) { this.cuotaMonto = cuotaMonto; }

    public BigDecimal getTotalAPagar() { return totalAPagar; }
    public void setTotalAPagar(BigDecimal totalAPagar) { this.totalAPagar = totalAPagar; }

    public BigDecimal getTotalInteres() { return totalInteres; }
    public void setTotalInteres(BigDecimal totalInteres) { this.totalInteres = totalInteres; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMetaPeriodicidad() { return metaPeriodicidad; }
    public void setMetaPeriodicidad(String metaPeriodicidad) { this.metaPeriodicidad = metaPeriodicidad; }

    public BigDecimal getMetaMonto() { return metaMonto; }
    public void setMetaMonto(BigDecimal metaMonto) { this.metaMonto = metaMonto; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isSemanal() { return "S".equalsIgnoreCase(periodicidad); }
}

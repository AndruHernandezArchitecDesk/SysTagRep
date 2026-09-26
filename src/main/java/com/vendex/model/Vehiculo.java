package com.vendex.model;

import java.time.LocalDateTime;

public class Vehiculo {
    private int id;
    private String marca;
    private String modelo;
    private Integer anioDesde;
    private Integer anioHasta;
    private String motor;
    private String combustible;
    private String vinPrefijo;
    private String paisOrigen;
    private String tipoVehiculo;
    private LocalDateTime creadoEn;

    public Vehiculo() {}

    public Vehiculo(String marca, String modelo, Integer anioDesde, Integer anioHasta, String motor) {
        this.marca = marca;
        this.modelo = modelo;
        this.anioDesde = anioDesde;
        this.anioHasta = anioHasta;
        this.motor = motor;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Integer getAnioDesde() { return anioDesde; }
    public void setAnioDesde(Integer anioDesde) { this.anioDesde = anioDesde; }
    public Integer getAnioHasta() { return anioHasta; }
    public void setAnioHasta(Integer anioHasta) { this.anioHasta = anioHasta; }
    public String getMotor() { return motor; }
    public void setMotor(String motor) { this.motor = motor; }
    public String getCombustible() { return combustible; }
    public void setCombustible(String combustible) { this.combustible = combustible; }
    public String getVinPrefijo() { return vinPrefijo; }
    public void setVinPrefijo(String vinPrefijo) { this.vinPrefijo = vinPrefijo; }
    public String getPaisOrigen() { return paisOrigen; }
    public void setPaisOrigen(String paisOrigen) { this.paisOrigen = paisOrigen; }
    public String getTipoVehiculo() { return tipoVehiculo; }
    public void setTipoVehiculo(String tipoVehiculo) { this.tipoVehiculo = tipoVehiculo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    @Override
    public String toString() {
        String anio = (anioDesde != null && anioHasta != null && !anioDesde.equals(anioHasta)) ? anioDesde + "-" + anioHasta : (anioDesde != null ? String.valueOf(anioDesde) : "");
        return marca + " " + modelo + (anio.isEmpty() ? "" : " " + anio) + (motor != null ? " " + motor : "");
    }

    public String getDisplay() { return toString(); }
}

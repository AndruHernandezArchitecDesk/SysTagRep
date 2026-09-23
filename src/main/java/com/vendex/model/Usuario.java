package com.vendex.model;

import java.time.LocalDateTime;

public class Usuario {
    private int id;
    private String nombre;
    private String apellido;
    private String correo;
    private String username;
    private String password;
    private String rol;
    private LocalDateTime fecha_creacion;
    private LocalDateTime ultimo_login;
    private boolean estado;
    private String permisos;
    // anti fuerza bruta (Fase 4)
    private int intentosFallidos;
    private LocalDateTime ultimoIntentoFallido;
    private LocalDateTime bloqueadoHasta;

    public Usuario() {}

    public Usuario(String nombre, String apellido, String correo, String rol, LocalDateTime fecha_creacion, LocalDateTime ultimo_login, boolean estado) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.rol = rol;
        this.fecha_creacion = fecha_creacion;
        this.ultimo_login = ultimo_login;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(LocalDateTime fecha_creacion) { this.fecha_creacion = fecha_creacion; }

    public LocalDateTime getUltimo_login() { return ultimo_login; }
    public void setUltimo_login(LocalDateTime ultimo_login) { this.ultimo_login = ultimo_login; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public String getPermisos() { return permisos; }
    public void setPermisos(String permisos) { this.permisos = permisos; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public LocalDateTime getUltimoIntentoFallido() { return ultimoIntentoFallido; }
    public void setUltimoIntentoFallido(LocalDateTime ultimoIntentoFallido) { this.ultimoIntentoFallido = ultimoIntentoFallido; }

    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    // permisos granulares
    private int rolId;
    private java.math.BigDecimal limiteDescuentoPct;
    public int getRolId() { return rolId; }
    public void setRolId(int rolId) { this.rolId = rolId; }
    public java.math.BigDecimal getLimiteDescuentoPct() { return limiteDescuentoPct; }
    public void setLimiteDescuentoPct(java.math.BigDecimal limiteDescuentoPct) { this.limiteDescuentoPct = limiteDescuentoPct; }
}

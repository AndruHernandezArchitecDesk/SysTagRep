package com.vendex.controller;

/** Ciclo de vida común de las mascotas; todas las llamadas ocurren en el hilo FX. */
interface MascotaAnimaciones {
    void setActivo(boolean activo);
    void setTrabajando(boolean trabajando);
    void registrarActividad();
    void avisar();
}

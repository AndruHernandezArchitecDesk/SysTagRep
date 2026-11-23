package com.vendex.controller;

/** Ciclo de vida común de las mascotas; todas las llamadas ocurren en el hilo FX. */
interface MascotaAnimaciones {
    void setActivo(boolean activo);
    void setTrabajando(boolean trabajando);
    /** Pose de sueño: congela accesorios y muestra Zzz aunque haya documento activo. */
    void setDurmiendo(boolean dormido);
    void registrarActividad();
    void avisar();
}

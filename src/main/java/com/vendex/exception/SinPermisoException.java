package com.vendex.exception;

public class SinPermisoException extends RuntimeException {
    private final String permiso;

    public SinPermisoException(String permiso) {
        super("No tienes permiso para esta acción: " + permiso);
        this.permiso = permiso;
    }

    public SinPermisoException(String permiso, String detalle) {
        super("No tienes permiso para esta acción: " + permiso + (detalle != null ? " - " + detalle : ""));
        this.permiso = permiso;
    }

    public String getPermiso() { return permiso; }
}

package com.vendex.exception;

public class SecretoNoEncontradoException extends RuntimeException {
    public SecretoNoEncontradoException(String nombre) {
        super("Secreto no encontrado: " + nombre + ". ¿Se generó/importó la clave maestra de instalación?");
    }
}

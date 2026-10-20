package com.vendex.api;

/** Respuesta controlada de la API (401/403) — mapeada en ApiServer. */
public class ApiAuthException extends RuntimeException {
    public final int status;
    public ApiAuthException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }
}

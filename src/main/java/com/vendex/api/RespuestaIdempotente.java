package com.vendex.api;

/** Fase 7: respuesta a una Idempotency-Key ya procesada (replay sin re-ejecutar). */
public class RespuestaIdempotente extends RuntimeException {
    private final int status;
    private final String body;

    public RespuestaIdempotente(int status, String body) {
        super("Replay idempotente");
        this.status = status;
        this.body = body;
    }

    public int getStatus() { return status; }
    public String getBody() { return body; }
}

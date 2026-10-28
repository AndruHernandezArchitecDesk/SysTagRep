package com.vendex.remote;

import com.vendex.dao.CuotaIADAO;
import com.vendex.model.CuotaIA.*;
import java.time.Duration;

public class CuotaIADAORest implements CuotaIADAO {
    private final RestClient cliente;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    public CuotaIADAORest(RestClient cliente) { this.cliente = cliente; }
    public Permiso reservar(Reserva r) throws Exception {
        return cliente.post("/api/ia/cuotas/reservar", r, Permiso.class, TIMEOUT);
    }
    public void finalizar(Fin f) throws Exception {
        cliente.post("/api/ia/cuotas/finalizar", f, Object.class, TIMEOUT);
    }
    public Panel panel() throws Exception {
        return cliente.get("/api/ia/cuotas/panel", new com.fasterxml.jackson.core.type.TypeReference<Panel>() {}, TIMEOUT);
    }
    public void guardar(Config cfg) throws Exception {
        cliente.post("/api/ia/cuotas/config", cfg, Object.class, TIMEOUT);
    }
}

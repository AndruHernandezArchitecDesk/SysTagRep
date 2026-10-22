package com.vendex.service;

import com.vendex.dao.VetxiHistorialDAO;
import com.vendex.model.VetxiHistorial.*;
import com.vendex.util.SesionActual;

import java.util.List;
import java.util.function.Supplier;

public class VetxiHistorialService {
    private final VetxiHistorialDAO dao;
    private final Supplier<Ambito> identidad;

    public VetxiHistorialService(VetxiHistorialDAO dao) { this(dao, VetxiHistorialService::sesionActual); }
    public VetxiHistorialService(VetxiHistorialDAO dao, Supplier<Ambito> identidad) {
        this.dao = dao; this.identidad = identidad;
    }

    private static Ambito sesionActual() {
        var usuario = SesionActual.getUsuario();
        return usuario == null || usuario.getId() <= 0 ? null : new Ambito(usuario.getId(), SesionActual.getSucursalId());
    }

    public Ambito ambitoActual() { return identidad.get(); }
    public void exigirAmbito(Ambito capturado) {
        if (capturado == null || !capturado.equals(ambitoActual()))
            throw new SecurityException("La sesión o sucursal del historial cambió");
    }

    public void guardar(Ambito a, Mensaje m) throws Exception { exigirAmbito(a); dao.guardar(a, m); }
    public List<Conversacion> conversaciones(Ambito a) throws Exception { return conversaciones(a, 20); }
    public List<Mensaje> mensajes(Ambito a, String id) throws Exception { return mensajes(a, id, 100); }
    public List<Conversacion> conversaciones(Ambito a, int limite) throws Exception {
        exigirAmbito(a); return dao.conversaciones(a, Math.max(1, Math.min(50, limite)));
    }
    public List<Mensaje> mensajes(Ambito a, String id, int limite) throws Exception {
        exigirAmbito(a); return dao.mensajes(a, id, Math.max(1, Math.min(200, limite)));
    }
}

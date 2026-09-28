package com.vendex.remote;

import com.vendex.dao.AuditoriaAccionDAO;
import java.lang.String;
import java.io.IOException;

public class AuditoriaAccionDAORest implements AuditoriaAccionDAO {
    private final RestClient rest;
    public AuditoriaAccionDAORest(RestClient rest) { this.rest = rest; }
    public void registrar(int usuarioId, String permisoCodigo, String resultado, String detalle) { try { try { rest.post("/auditoria-accion/registrar", java.util.Map.of(), Void.class); } catch (IOException e) { throw new RuntimeException(e); } } catch(Exception e) { throw new RuntimeException(e); } }
}
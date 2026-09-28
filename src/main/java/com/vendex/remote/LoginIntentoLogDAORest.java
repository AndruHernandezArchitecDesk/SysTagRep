package com.vendex.remote;

import com.vendex.dao.LoginIntentoLogDAO;
import java.lang.String;

public class LoginIntentoLogDAORest implements LoginIntentoLogDAO {
    private final RestClient rest;
    public LoginIntentoLogDAORest(RestClient rest) { this.rest = rest; }
    public void registrar(String usuarioInput, boolean exitoso, String equipo) { try { rest.post("/login-intento-log/registrar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public int contarFallosRecientes(String usuarioInput, int ventanaMinutos) { throw new UnsupportedOperationException("LoginIntentoLog.contarFallosRecientes"); }
}
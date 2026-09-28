package com.vendex.remote;

import com.vendex.dao.LogDAO;
import java.lang.Exception;
import java.lang.String;

public class LogDAORest implements LogDAO {
    private final RestClient rest;
    public LogDAORest(RestClient rest) { this.rest = rest; }
    public void guardar(String controlador, String metodo, String mensaje, Exception ex) { try { rest.post("/logs/guardar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
    public void guardar(String controlador, String metodo, String mensaje) { try { rest.post("/logs/guardar", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
}
package com.vendex.remote;

import com.vendex.dao.AlertaDAO;
import com.vendex.model.Alerta;
import java.util.List;

public class AlertaDAORest extends RestDao<Alerta> implements AlertaDAO {
    public AlertaDAORest(RestClient rest) { super(rest, "/alertas", Alerta.class); }
    public int insertar(Alerta a) { throw new UnsupportedOperationException("Alerta.insertar rest"); }
    public List<Alerta> listarTodas() { throw new UnsupportedOperationException("Alerta.listarTodas rest"); }
    public List<Alerta> listarNoLeidas() { throw new UnsupportedOperationException("Alerta.listarNoLeidas rest"); }
    public int contarNoLeidas() { throw new UnsupportedOperationException("Alerta.contarNoLeidas rest"); }
    public void marcarComoLeida(int id) { throw new UnsupportedOperationException("Alerta.marcarComoLeida rest"); }
    public void marcarTodasComoLeidas() { throw new UnsupportedOperationException("Alerta.marcarTodasComoLeidas rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Alerta.eliminar rest"); }
    public void eliminarLeidas() { throw new UnsupportedOperationException("Alerta.eliminarLeidas rest"); }
    public void limpiar() { throw new UnsupportedOperationException("Alerta.limpiar rest"); }
}
package com.vendex.remote;

import com.vendex.dao.PercheroDAO;
import com.vendex.model.Perchero;
import java.util.List;
import java.lang.String;

public class PercheroDAORest extends RestDao<Perchero> implements PercheroDAO {
    public PercheroDAORest(RestClient rest) { super(rest, "/percheros", Perchero.class); }
    public void guardar(Perchero p) { throw new UnsupportedOperationException("Perchero.guardar rest"); }
    public void actualizar(Perchero p) { throw new UnsupportedOperationException("Perchero.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Perchero.eliminar rest"); }
    public void eliminarPorNombre(String nombrePerchero) { throw new UnsupportedOperationException("Perchero.eliminarPorNombre rest"); }
    public List<Perchero> listar() { throw new UnsupportedOperationException("Perchero.listar rest"); }
    public List<String> listarNombres() { throw new UnsupportedOperationException("Perchero.listarNombres rest"); }
    public List<Perchero> listarPorNombre(String nombrePerchero) { throw new UnsupportedOperationException("Perchero.listarPorNombre rest"); }
    public Perchero obtenerPorId(int id) { throw new UnsupportedOperationException("Perchero.obtenerPorId rest"); }
}
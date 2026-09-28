package com.vendex.remote;

import com.vendex.dao.MarcaDAO;
import com.vendex.model.Marca;
import java.util.List;
import java.lang.String;

public class MarcaDAORest extends RestDao<Marca> implements MarcaDAO {
    public MarcaDAORest(RestClient rest) { super(rest, "/marcas", Marca.class); }
    public void guardar(Marca m) { throw new UnsupportedOperationException("Marca.guardar rest"); }
    public void actualizar(Marca m) { throw new UnsupportedOperationException("Marca.actualizar rest"); }
    public boolean existe(String nombre) { throw new UnsupportedOperationException("Marca.existe rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Marca.eliminar rest"); }
    public List<Marca> listar() { throw new UnsupportedOperationException("Marca.listar rest"); }
}
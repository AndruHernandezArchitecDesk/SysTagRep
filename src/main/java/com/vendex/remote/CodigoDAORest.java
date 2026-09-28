package com.vendex.remote;

import com.vendex.dao.CodigoDAO;
import com.vendex.model.Codigo;
import java.util.List;
import java.lang.String;

public class CodigoDAORest extends RestDao<Codigo> implements CodigoDAO {
    public CodigoDAORest(RestClient rest) { super(rest, "/codigos", Codigo.class); }
    public void guardar(Codigo c) { throw new UnsupportedOperationException("Codigo.guardar rest"); }
    public boolean existe(String nombre) { throw new UnsupportedOperationException("Codigo.existe rest"); }
    public void actualizar(Codigo c) { throw new UnsupportedOperationException("Codigo.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Codigo.eliminar rest"); }
    public List<Codigo> listar() { throw new UnsupportedOperationException("Codigo.listar rest"); }
}
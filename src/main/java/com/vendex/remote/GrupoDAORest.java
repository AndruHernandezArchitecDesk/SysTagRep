package com.vendex.remote;

import com.vendex.dao.GrupoDAO;
import com.vendex.model.Grupo;
import java.util.List;
import java.lang.String;

public class GrupoDAORest extends RestDao<Grupo> implements GrupoDAO {
    public GrupoDAORest(RestClient rest) { super(rest, "/grupos", Grupo.class); }
    public void guardar(Grupo g) { throw new UnsupportedOperationException("Grupo.guardar rest"); }
    public void actualizar(Grupo g) { throw new UnsupportedOperationException("Grupo.actualizar rest"); }
    public boolean existe(String nombre) { throw new UnsupportedOperationException("Grupo.existe rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Grupo.eliminar rest"); }
    public List<Grupo> listar() { throw new UnsupportedOperationException("Grupo.listar rest"); }
}
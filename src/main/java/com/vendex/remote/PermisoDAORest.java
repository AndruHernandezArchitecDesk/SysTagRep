package com.vendex.remote;

import com.vendex.dao.PermisoDAO;
import com.vendex.model.Permiso;
import java.util.List;
import java.util.Set;
import java.lang.String;

public class PermisoDAORest extends RestDao<Permiso> implements PermisoDAO {
    public PermisoDAORest(RestClient rest) { super(rest, "/permisos", Permiso.class); }
    public List<Permiso> listarTodos() { throw new UnsupportedOperationException("Permiso.listarTodos rest"); }
    public Set<String> listarPorRol(int rolId) { throw new UnsupportedOperationException("Permiso.listarPorRol rest"); }
    public void actualizarPermisosDeRol(int rolId, List codigos) { throw new UnsupportedOperationException("Permiso.actualizarPermisosDeRol rest"); }
}
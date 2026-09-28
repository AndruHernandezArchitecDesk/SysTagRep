package com.vendex.remote;

import com.vendex.dao.RolDAO;
import com.vendex.model.Rol;
import java.util.List;
import java.lang.String;

public class RolDAORest extends RestDao<Rol> implements RolDAO {
    public RolDAORest(RestClient rest) { super(rest, "/roles", Rol.class); }
    public List<Rol> listar() { throw new UnsupportedOperationException("Rol.listar rest"); }
    public Rol obtenerPorId(int id) { throw new UnsupportedOperationException("Rol.obtenerPorId rest"); }
    public Rol obtenerPorNombre(String nombre) { throw new UnsupportedOperationException("Rol.obtenerPorNombre rest"); }
    public void actualizarLimiteDescuento(int rolId, java.math.BigDecimal limite) { throw new UnsupportedOperationException("Rol.actualizarLimiteDescuento rest"); }
}
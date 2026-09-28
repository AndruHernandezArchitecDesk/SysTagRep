package com.vendex.remote;

import com.vendex.dao.SucursalDAO;
import com.vendex.model.Sucursal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.lang.String;

public class SucursalDAORest extends RestDao<Sucursal> implements SucursalDAO {
    public SucursalDAORest(RestClient rest) { super(rest, "/sucursales", Sucursal.class); }
    public List<Sucursal> listar() { throw new UnsupportedOperationException("Sucursal.listar rest"); }
    public List<Sucursal> listarActivas() { throw new UnsupportedOperationException("Sucursal.listarActivas rest"); }
    public Optional<Sucursal> obtenerPorId(int id) { throw new UnsupportedOperationException("Sucursal.obtenerPorId rest"); }
    public Optional<Sucursal> obtenerPorCodigo(String codigo) { throw new UnsupportedOperationException("Sucursal.obtenerPorCodigo rest"); }
    public Optional<Sucursal> obtenerCentral() { throw new UnsupportedOperationException("Sucursal.obtenerCentral rest"); }
    public int guardar(Sucursal s) { throw new UnsupportedOperationException("Sucursal.guardar rest"); }
    public void actualizar(Sucursal s) { throw new UnsupportedOperationException("Sucursal.actualizar rest"); }
    public void desactivar(int id) { throw new UnsupportedOperationException("Sucursal.desactivar rest"); }
    public int guardar(Connection con, Sucursal s) { throw new UnsupportedOperationException("Sucursal.guardar rest"); }
    public void actualizar(Connection con, Sucursal s) { throw new UnsupportedOperationException("Sucursal.actualizar rest"); }
}
package com.vendex.remote;

import com.vendex.dao.ProveedorDAO;
import com.vendex.model.Proveedor;
import java.util.List;

public class ProveedorDAORest extends RestDao<Proveedor> implements ProveedorDAO {
    public ProveedorDAORest(RestClient rest) { super(rest, "/proveedores", Proveedor.class); }
    public List<Proveedor> listar() { throw new UnsupportedOperationException("Proveedor.listar rest"); }
    public void guardar(Proveedor proveedor) { throw new UnsupportedOperationException("Proveedor.guardar rest"); }
    public void actualizar(Proveedor proveedor) { throw new UnsupportedOperationException("Proveedor.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Proveedor.eliminar rest"); }
}
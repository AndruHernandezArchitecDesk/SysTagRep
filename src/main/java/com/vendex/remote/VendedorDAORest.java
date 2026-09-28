package com.vendex.remote;

import com.vendex.dao.VendedorDAO;
import com.vendex.model.Vendedor;
import java.util.List;

public class VendedorDAORest extends RestDao<Vendedor> implements VendedorDAO {
    public VendedorDAORest(RestClient rest) { super(rest, "/vendedores", Vendedor.class); }
    public List<Vendedor> listar() { throw new UnsupportedOperationException("Vendedor.listar rest"); }
    public void guardar(Vendedor vendedor) { throw new UnsupportedOperationException("Vendedor.guardar rest"); }
    public void actualizar(Vendedor vendedor) { throw new UnsupportedOperationException("Vendedor.actualizar rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Vendedor.eliminar rest"); }
}
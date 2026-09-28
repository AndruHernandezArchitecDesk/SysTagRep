package com.vendex.remote;

import com.vendex.dao.ClienteDAO;
import com.vendex.model.Cliente;
import java.util.List;

public class ClienteDAORest extends RestDao<Cliente> implements ClienteDAO {
    public ClienteDAORest(RestClient rest) { super(rest, "/clientes", Cliente.class); }
    public List<Cliente> obtenerListaClientes() { throw new UnsupportedOperationException("Cliente.obtenerListaClientes rest"); }
    public List<Cliente> listar() { throw new UnsupportedOperationException("Cliente.listar rest"); }
    public void guardar(Cliente cliente) { throw new UnsupportedOperationException("Cliente.guardar rest"); }
    public void actualizar(Cliente cliente) { throw new UnsupportedOperationException("Cliente.actualizar rest"); }
    public Cliente obtenerPorId(int id) { throw new UnsupportedOperationException("Cliente.obtenerPorId rest"); }
    public void eliminar(int id) { throw new UnsupportedOperationException("Cliente.eliminar rest"); }
}
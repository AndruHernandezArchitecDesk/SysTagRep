package com.vendex.remote;

import com.vendex.dao.EmpresaDAO;
import com.vendex.model.Empresa;
import java.util.List;

public class EmpresaDAORest extends RestDao<Empresa> implements EmpresaDAO {
    public EmpresaDAORest(RestClient rest) { super(rest, "/empresa", Empresa.class); }
    public Empresa obtenerPorId(int id) { throw new UnsupportedOperationException("Empresa.obtenerPorId rest"); }
    public List<Empresa> listar() { throw new UnsupportedOperationException("Empresa.listar rest"); }
    public boolean actualizar(Empresa empresa) { throw new UnsupportedOperationException("Empresa.actualizar rest"); }
}
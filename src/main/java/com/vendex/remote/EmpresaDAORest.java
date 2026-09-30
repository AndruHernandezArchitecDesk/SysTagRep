package com.vendex.remote;

import com.vendex.dao.EmpresaDAO;
import com.vendex.model.Empresa;

import java.util.List;
import java.util.Map;

public class EmpresaDAORest extends RestDao<Empresa> implements EmpresaDAO {

    public EmpresaDAORest(RestClient rest) {
        super(rest, "/empresa", Empresa.class);
    }

    @Override
    public Empresa obtenerPorId(int id) {
        try {
            return rest.get("/api/empresa/" + id, Empresa.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Empresa> listar() {
        try {
            return rest.get("/api/empresa", new com.fasterxml.jackson.core.type.TypeReference<List<Empresa>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean actualizar(Empresa empresa) {
        try {
            rest.put("/api/empresa/" + empresa.getId(), empresa, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

package com.vendex.remote;

import com.vendex.dao.ClienteDAO;
import com.vendex.model.Cliente;

import java.util.List;
import java.util.Map;

public class ClienteDAORest extends RestDao<Cliente> implements ClienteDAO {

    public ClienteDAORest(RestClient rest) {
        super(rest, "/clientes", Cliente.class);
    }

    @Override
    public List<Cliente> obtenerListaClientes() {
        return listar();
    }

    @Override
    public List<Cliente> listar() {
        try {
            return rest.get("/api/clientes", new com.fasterxml.jackson.core.type.TypeReference<List<Cliente>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void guardar(Cliente cliente) {
        try {
            rest.post("/api/clientes", cliente, Cliente.class);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando cliente vía REST", e);
        }
    }

    @Override
    public void actualizar(Cliente cliente) {
        try {
            rest.put("/api/clientes/" + cliente.getId(), cliente, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando cliente vía REST", e);
        }
    }

    @Override
    public Cliente obtenerPorId(int id) {
        try {
            return rest.get("/api/clientes/" + id, Cliente.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void eliminar(int id) {
        try {
            rest.delete("/api/clientes/" + id);
        } catch (Exception e) {
            throw new RuntimeException("Error eliminando cliente vía REST", e);
        }
    }
}

package com.vendex.dao;

import com.vendex.model.AccesoDirecto;
import java.util.List;

public interface AccesoDirectoDAO {

    List<AccesoDirecto> listarPorUsuario(int usuarioId);

    int contarPorUsuario(int usuarioId);

    boolean existe(int usuarioId, String vistaRuta);

    int agregar(AccesoDirecto a);

    void eliminar(int id);

    void eliminarPorVista(int usuarioId, String vistaRuta);

    void reordenar(int usuarioId, List<Integer> idsEnOrden);
}

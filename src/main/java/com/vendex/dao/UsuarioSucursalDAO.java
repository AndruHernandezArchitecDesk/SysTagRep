package com.vendex.dao;

import java.util.List;

/**
 * Fase 7: sucursales a las que un usuario puede operar.
 * Si el usuario no tiene filas, opera en su sucursal principal (usuarios.sucursal_id).
 */
public interface UsuarioSucursalDAO {
    List<Integer> listarSucursales(int usuarioId);
    void reemplazar(int usuarioId, List<Integer> sucursalIds);
    /**
     * Devuelve la sucursal válida para operar: la preferida si el usuario puede usarla,
     * si no la primera permitida, y si no hay filas la preferida (compatibilidad).
     */
    int sucursalValidaPara(int usuarioId, Integer sucursalPreferida);
}

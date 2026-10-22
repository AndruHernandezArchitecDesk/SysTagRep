package com.vendex.dao;

import java.util.List;
import java.util.Map;

/**
 * Fase 7: permisos granulares por usuario.
 * Complementan la matriz rol×permiso: otorgar=true añade un permiso que el rol no da;
 * otorgar=false retira un permiso que el rol da.
 */
public interface UsuarioPermisoDAO {
    /** Mapa permiso_codigo -> otorgar para el usuario. */
    Map<String, Boolean> listarPorUsuario(int usuarioId);
    /** Reemplaza todos los permisos granulares del usuario (borra e inserta). */
    void reemplazar(int usuarioId, Map<String, Boolean> permisos);
}

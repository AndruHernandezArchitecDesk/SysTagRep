package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Permiso;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface PermisoDAO {

    List<Permiso> listarTodos();

    Set<String> listarPorRol(int rolId);

    void actualizarPermisosDeRol(int rolId, List<String> codigos);

}

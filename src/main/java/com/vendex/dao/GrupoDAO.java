package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Grupo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface GrupoDAO {

    void guardar(Grupo g) throws SQLException;

    void actualizar(Grupo g);

    boolean existe(String nombre);

    void eliminar(int id);

    List<Grupo> listar();

}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Codigo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface CodigoDAO {

    void guardar(Codigo c) throws SQLException;

    boolean existe(String nombre);

    void actualizar(Codigo c);

    void eliminar(int id);

    List<Codigo> listar();

}

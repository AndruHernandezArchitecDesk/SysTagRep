package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Marca;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface MarcaDAO {

    void guardar(Marca m) throws SQLException;

    void actualizar(Marca m);

    boolean existe(String nombre);

    void eliminar(int id);

    List<Marca> listar();

}

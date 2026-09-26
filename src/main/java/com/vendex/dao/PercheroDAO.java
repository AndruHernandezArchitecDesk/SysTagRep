package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Perchero;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface PercheroDAO {

    void guardar(Perchero p);

    void actualizar(Perchero p);

    void eliminar(int id);

    void eliminarPorNombre(String nombrePerchero);

    List<Perchero> listar();

    List<String> listarNombres();

    List<Perchero> listarPorNombre(String nombrePerchero);

    Perchero obtenerPorId(int id);

}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.UbicacionPercha;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface UbicacionPerchaDAO {

    void guardar(UbicacionPercha u);

    void actualizar(UbicacionPercha u);

    void eliminar(int id);

    List<UbicacionPercha> listar();

}

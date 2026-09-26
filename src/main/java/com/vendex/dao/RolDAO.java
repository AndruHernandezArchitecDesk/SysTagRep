package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.Rol;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface RolDAO {

    List<Rol> listar();

    Rol obtenerPorId(int id);

    Rol obtenerPorNombre(String nombre);

    void actualizarLimiteDescuento(int rolId, java.math.BigDecimal limite);

}

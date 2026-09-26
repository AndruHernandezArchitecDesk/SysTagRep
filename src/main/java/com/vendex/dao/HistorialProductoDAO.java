package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.HistorialProducto;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface HistorialProductoDAO {

    void insertar(List<HistorialProducto> lista);

    void insertar(Connection con, List<HistorialProducto> lista) throws SQLException;

    List<HistorialProducto> listar();

    List<HistorialProducto> listarPorFecha(LocalDate fecha);

    boolean existeVentaPorInventarioIds(List<Integer> inventarioIds);

}

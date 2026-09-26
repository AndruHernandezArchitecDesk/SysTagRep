package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.UbicacionDetalle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface UbicacionDetalleDAO {

    void guardar(UbicacionDetalle u);

    void generarUbicaciones(int idPerchero, String prefijo, int cantidad);

    void ocupar(int idUbicacion, int idProducto, int cantidad);

    void liberar(int id);

    void liberarPorProducto(int idProducto);

    void eliminarLugar(int id);

    List<UbicacionDetalle> listarOcupados();

    void eliminarPorPerchero(int idPerchero);

    List<UbicacionDetalle> listarPorPerchero(int idPerchero);

    List<UbicacionDetalle> listarPorProducto(int idProducto);

    List<com.vendex.model.Inventario> listarProductosDisponibles();

}

package com.vendex.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface InventarioVehiculoDAO {

    void asociar(int inventarioId, int vehiculoId);

    void asociar(Connection con, int inventarioId, int vehiculoId) throws SQLException;

    void asociarMultiple(int inventarioId, List<Integer> vehiculoIds);

    void limpiarPorInventario(int inventarioId);

    void limpiarPorInventario(Connection con, int inventarioId) throws SQLException;

    List<Integer> listarVehiculoIdsPorInventario(int inventarioId);

    List<Integer> listarInventarioIdsPorVehiculo(int vehiculoId);

    List<com.vendex.model.Inventario> listarPorVehiculo(int vehiculoId, int sucursalId);

    List<com.vendex.model.Vehiculo> listarVehiculosPorInventario(int inventarioId);
}

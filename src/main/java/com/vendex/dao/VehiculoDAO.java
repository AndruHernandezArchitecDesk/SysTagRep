package com.vendex.dao;

import com.vendex.model.Vehiculo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface VehiculoDAO {

    List<Vehiculo> listar();

    List<String> listarMarcas();

    List<String> listarModelosPorMarca(String marca);

    List<Integer> listarAniosPorModelo(String marca, String modelo);

    Optional<Vehiculo> obtenerPorId(int id);

    Optional<Vehiculo> buscarPorVin(String vin17);

    int guardar(Vehiculo v);

    int guardar(Connection con, Vehiculo v) throws SQLException;

    void actualizar(Vehiculo v);

    void eliminar(int id);

    List<Vehiculo> buscar(String marca, String modelo, Integer anio);
}

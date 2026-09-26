package com.vendex.dao;

import com.vendex.model.Sucursal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SucursalDAO {

    List<Sucursal> listar();

    List<Sucursal> listarActivas();

    Optional<Sucursal> obtenerPorId(int id);

    Optional<Sucursal> obtenerPorCodigo(String codigo);

    Optional<Sucursal> obtenerCentral();

    int guardar(Sucursal s);

    void actualizar(Sucursal s);

    void desactivar(int id);

    // Overload transaccional
    int guardar(Connection con, Sucursal s) throws SQLException;

    void actualizar(Connection con, Sucursal s) throws SQLException;
}

package com.vendex.dao;

import com.vendex.model.PuntoEmision;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface PuntoEmisionDAO {
    List<PuntoEmision> listarPorSucursal(int sucursalId);
    Optional<PuntoEmision> obtenerPorId(int id);
    Optional<PuntoEmision> obtenerPorCodigoSucursal(int sucursalId, String codigo);
    int guardar(PuntoEmision p);
    void actualizar(PuntoEmision p);
    void desactivar(int id);
    int guardar(Connection con, PuntoEmision p) throws SQLException;
    void actualizar(Connection con, PuntoEmision p) throws SQLException;
}

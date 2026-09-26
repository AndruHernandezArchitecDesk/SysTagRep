package com.vendex.dao;

import com.vendex.model.TransferenciaInventario;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface TransferenciaInventarioDAO {

    int guardar(TransferenciaInventario t);

    int guardar(Connection con, TransferenciaInventario t) throws SQLException;

    List<TransferenciaInventario> listarPorInventario(int inventarioId);

    List<TransferenciaInventario> listarPorSucursal(int sucursalId, int limit);

    List<TransferenciaInventario> listarTodas(int limit);
}

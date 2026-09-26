package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CuentaPorPagar;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public interface CuentaPorPagarDAO {

    void insertar(CuentaPorPagar cpp);

    List<Object[]> listarCreditosActivos();

    void registrarAdelanto(int cppId, BigDecimal nuevoAdelanto);

    void marcarPagado(int cppId);

    void eliminarPorInventarios(List<Integer> inventarioIds);

    List<String[]> obtenerDetallesInventario(int inventarioId);

}

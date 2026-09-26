package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CuentaPorCobrar;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface CuentaPorCobrarDAO {

    void insertar(CuentaPorCobrar cpc);

    void insertar(Connection con, CuentaPorCobrar cpc) throws SQLException;

    List<Object[]> listarCreditosActivos();

    List<Object[]> listarPorCliente(int clienteId);

    void registrarAdelanto(int cpcId, BigDecimal nuevoAdelanto);

    void marcarPagado(int cpcId);

    List<String[]> obtenerDetallesVenta(int notaVentaId);

}

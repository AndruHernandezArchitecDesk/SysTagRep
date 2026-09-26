package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.FacturaRegistro;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface FacturaRegistroDAO {

    List<FacturaRegistro> obtenerNumFactura();

    int insertar(FacturaRegistro fr);

    int insertar(Connection con, FacturaRegistro fr) throws SQLException;

    void actualizarEstado(Connection con, String claveAcceso, String estado) throws SQLException;

    FacturaRegistro obtenerPorId(int id);

    FacturaRegistro obtenerPorClaveAcceso(String claveAcceso);

    void actualizarEstado(String claveAcceso, String estado);

    List<FacturaRegistro> listarPendientesSri();

    int contar(String filtro);

    List<FacturaRegistro> listarPaginado(int page, int pageSize, String filtro);

}

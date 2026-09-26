package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaDebitoRegistro;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.vendex.util.AppConstants;

public interface NotaDebitoRegistroDAO {

    int insertar(NotaDebitoRegistro nd);

    int insertar(Connection con, NotaDebitoRegistro nd) throws SQLException;

    void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException;

    NotaDebitoRegistro obtenerPorClave(String claveAcceso);

    NotaDebitoRegistro obtenerPorId(int id);

    List<NotaDebitoRegistro> listarPorFactura(int facturaRegistroId);

    BigDecimal sumarValorTotalPorFactura(int facturaRegistroId, String estadoFiltro);

    void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion);

    List<NotaDebitoRegistro> listarPendientesSri();

}

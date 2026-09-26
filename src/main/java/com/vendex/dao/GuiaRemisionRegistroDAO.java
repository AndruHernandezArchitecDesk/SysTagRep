package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.GuiaRemisionRegistro;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface GuiaRemisionRegistroDAO {

    int insertar(GuiaRemisionRegistro r);

    int insertar(Connection con, GuiaRemisionRegistro r) throws SQLException;

    GuiaRemisionRegistro obtenerPorClave(String clave);

    GuiaRemisionRegistro obtenerPorId(int id);

    List<GuiaRemisionRegistro> listarPendientesSri();

    void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion);

    void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException;

}

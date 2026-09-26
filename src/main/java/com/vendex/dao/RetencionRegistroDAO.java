package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.RetencionRegistro;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface RetencionRegistroDAO {

    int insertar(RetencionRegistro r);

    int insertar(Connection con, RetencionRegistro r) throws SQLException;

    RetencionRegistro obtenerPorClave(String clave);

    RetencionRegistro obtenerPorId(int id);

    List<RetencionRegistro> listarPendientesSri();

    void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion);

    void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException;

}

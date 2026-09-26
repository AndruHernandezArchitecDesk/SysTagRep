package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ComprobanteDAO {

    int obtenerSecuencial(String tipoComprobante);

    void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado);

    void insertar(String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado, String tipoComprobante);

    void insertar(Connection con, String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado, String tipoComprobante) throws SQLException;

    void insertar(Connection con, String claveAcceso, Integer idRelacionado, String numeroComprobante, String ambiente, String xmlGenerado) throws SQLException;

    void actualizarEstado(String claveAcceso, String estado, String mensaje, String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion);

    void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String xmlAutorizado, String numeroAutorizacion, String fechaAutorizacion) throws SQLException;

    void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion);

    void guardarEnvio(String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion, String tipoComprobante);

    void guardarEnvio(Connection con, String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion, String tipoComprobante) throws SQLException;

    void guardarEnvio(Connection con, String claveAcceso, String numeroComprobante, String ambiente, String xmlEnviado, String respuestaRecepcion, String respuestaAutorizacion, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException;

    int consultarSecuencial(String tipoComprobante);

}

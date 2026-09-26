package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaCreditoRegistro;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface NotaCreditoRegistroDAO {

    int insertar(NotaCreditoRegistro nc);

    int insertar(Connection con, NotaCreditoRegistro nc) throws SQLException;

    void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) throws SQLException;

    NotaCreditoRegistro obtenerPorClave(String claveAcceso);

    NotaCreditoRegistro obtenerPorId(int id);

    List<NotaCreditoRegistro> listarPorFactura(int facturaRegistroId);

    BigDecimal sumarValorModificacionPorFactura(int facturaRegistroId, String estadoFiltro);

    void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion);

    void actualizarXmlFirmado(String claveAcceso, String xmlFirmado);

    List<NotaCreditoRegistro> listarPendientesSri();

    List<NotaCreditoRegistro> listarPaginado(int page, int pageSize, String filtro);

    int contar(String filtro);

    List<NotaCreditoRegistro> listarTodas();

}

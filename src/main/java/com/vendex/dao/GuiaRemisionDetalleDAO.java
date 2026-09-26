package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.GuiaRemisionDetalle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface GuiaRemisionDetalleDAO {

    void insertarDetalles(int destinatarioId, List<GuiaRemisionDetalle> lista);

    void insertarDetalles(Connection con, int destinatarioId, List<GuiaRemisionDetalle> lista) throws SQLException;

    List<GuiaRemisionDetalle> listarPorDestinatarioId(int destinatarioId);

    List<GuiaRemisionDetalle> listarPorGuiaId(int guiaId);

}

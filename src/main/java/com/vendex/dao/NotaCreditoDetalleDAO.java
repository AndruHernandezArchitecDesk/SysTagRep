package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.NotaCreditoDetalle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface NotaCreditoDetalleDAO {

    void insertarDetalles(int notaCreditoId, List<NotaCreditoDetalle> detalles);

    void insertarDetalles(Connection con, int notaCreditoId, List<NotaCreditoDetalle> detalles) throws SQLException;

    List<NotaCreditoDetalle> listarPorNotaCreditoId(int notaCreditoId);

}

package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.FacturaDetalle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface FacturaDetalleDAO {

    void insertarDetalle(int facturaRegistroId, List<FacturaDetalle> detalles);

    void insertarDetalle(Connection con, int facturaRegistroId, List<FacturaDetalle> detalles) throws SQLException;

    List<FacturaDetalle> listarPorFacturaRegistroId(int facturaRegistroId);

    boolean existeVentaPorInventarioIds(List<Integer> inventarioIds);

}

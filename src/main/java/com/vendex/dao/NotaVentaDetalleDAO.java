package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.DetalleVenta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface NotaVentaDetalleDAO {

    void insertarDetalle(int notaVentaRegistroId, List<DetalleVenta> detalles);

}

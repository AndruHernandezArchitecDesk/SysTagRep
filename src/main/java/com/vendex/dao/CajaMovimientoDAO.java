package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CajaMovimiento;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface CajaMovimientoDAO {

    int insertar(CajaMovimiento m);

    List<CajaMovimiento> listarPorSesion(int sesionId);

    List<CajaMovimiento> listarPorFecha(java.time.LocalDate desde, java.time.LocalDate hasta);

    BigDecimal totalPorTipo(int sesionId, String tipo);

}

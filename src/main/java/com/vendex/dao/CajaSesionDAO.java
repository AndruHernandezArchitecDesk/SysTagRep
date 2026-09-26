package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.CajaSesion;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface CajaSesionDAO {

    int abrir(CajaSesion s);

    CajaSesion obtenerAbierta();

    CajaSesion obtenerPorId(int id);

    List<CajaSesion> listarPorFecha(java.time.LocalDate desde, java.time.LocalDate hasta);

    boolean cerrar(int id, BigDecimal montoFisico, BigDecimal diferencia, String observaciones);

    BigDecimal calcularTotalMovimientos(int sesionId);

}

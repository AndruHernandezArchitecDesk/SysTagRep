package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import com.vendex.model.DetalleVentaReporte;
import com.vendex.model.VentaResumen;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public interface VentaResumenDAO {

    List<VentaResumen> listar();

    List<VentaResumen> listarPorFecha(LocalDate fecha);

    List<VentaResumen> listarPorRango(LocalDate desde, LocalDate hasta);

    List<DetalleVentaReporte> listarDetalle(String tipo, int id);

    List<DetalleVentaReporte> listarDetallePorRango(LocalDate desde, LocalDate hasta);

}

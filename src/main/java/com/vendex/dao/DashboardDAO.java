package com.vendex.dao;

import com.vendex.config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface DashboardDAO {

    Map<String, Double> ventasPorDia(int ultimosDias);

    Map<String, Double> facturasPorDia(int ultimosDias);

    Map<String, Double> facturasNetasPorDia(int ultimosDias);

    Map<String, Integer> inventarioPorMarca();

    Map<String, Integer> inventarioPorGrupo();

    double ventasDelDia(LocalDate fecha);

    double ventasNetasDelDia(LocalDate fecha);

    Map<String, Double> notasCreditoPorDia(int ultimosDias);

    int notasCreditoEmitidasDelDia(LocalDate fecha);

    Map<String, Double> notasDebitoPorDia(int ultimosDias);

    int notasDebitoEmitidasDelDia(LocalDate fecha);

    int facturasEmitidasDelDia(LocalDate fecha);

    int productosVendidosDelDia(LocalDate fecha);

    int clientesAtendidosDelDia(LocalDate fecha);

    Map<String, Double> comprasPorDia(int ultimosDias);

}

package com.vendex.remote;

import com.vendex.dao.DashboardDAO;
import java.lang.Double;
import java.lang.Integer;
import java.time.LocalDate;
import java.util.Map;
import java.lang.String;

public class DashboardDAORest implements DashboardDAO {
    private final RestClient rest;
    public DashboardDAORest(RestClient rest) { this.rest = rest; }
    public Map<String, Double> ventasPorDia(int ultimosDias) { throw new UnsupportedOperationException("Dashboard.ventasPorDia"); }
    public Map<String, Double> facturasPorDia(int ultimosDias) { throw new UnsupportedOperationException("Dashboard.facturasPorDia"); }
    public Map<String, Double> facturasNetasPorDia(int ultimosDias) { throw new UnsupportedOperationException("Dashboard.facturasNetasPorDia"); }
    public Map<String, Integer> inventarioPorMarca() { throw new UnsupportedOperationException("Dashboard.inventarioPorMarca"); }
    public Map<String, Integer> inventarioPorGrupo() { throw new UnsupportedOperationException("Dashboard.inventarioPorGrupo"); }
    public double ventasDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.ventasDelDia"); }
    public double ventasNetasDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.ventasNetasDelDia"); }
    public Map<String, Double> notasCreditoPorDia(int ultimosDias) { throw new UnsupportedOperationException("Dashboard.notasCreditoPorDia"); }
    public int notasCreditoEmitidasDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.notasCreditoEmitidasDelDia"); }
    public Map<String, Double> notasDebitoPorDia(int ultimosDias) { throw new UnsupportedOperationException("Dashboard.notasDebitoPorDia"); }
    public int notasDebitoEmitidasDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.notasDebitoEmitidasDelDia"); }
    public int facturasEmitidasDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.facturasEmitidasDelDia"); }
    public int productosVendidosDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.productosVendidosDelDia"); }
    public int clientesAtendidosDelDia(LocalDate fecha) { throw new UnsupportedOperationException("Dashboard.clientesAtendidosDelDia"); }
    public Map<String, Double> comprasPorDia(int ultimosDias) { throw new UnsupportedOperationException("Dashboard.comprasPorDia"); }
}
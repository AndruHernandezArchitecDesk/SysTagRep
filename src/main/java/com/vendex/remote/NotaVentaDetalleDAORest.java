package com.vendex.remote;

import com.vendex.dao.NotaVentaDetalleDAO;
import java.util.List;

public class NotaVentaDetalleDAORest implements NotaVentaDetalleDAO {
    private final RestClient rest;
    public NotaVentaDetalleDAORest(RestClient rest) { this.rest = rest; }
    public void insertarDetalle(int notaVentaRegistroId, List detalles) { try { rest.post("/notas-venta-detalle/insertarDetalle", java.util.Map.of(), Void.class); } catch(Exception e) { throw new RuntimeException(e); } }
}
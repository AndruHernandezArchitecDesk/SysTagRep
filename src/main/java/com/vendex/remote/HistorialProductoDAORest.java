package com.vendex.remote;

import com.vendex.dao.HistorialProductoDAO;
import com.vendex.model.HistorialProducto;
import java.sql.Connection;
import java.util.List;
import java.time.LocalDate;

public class HistorialProductoDAORest extends RestDao<HistorialProducto> implements HistorialProductoDAO {
    public HistorialProductoDAORest(RestClient rest) { super(rest, "/historial-producto", HistorialProducto.class); }
    public void insertar(List lista) { throw new UnsupportedOperationException("HistorialProducto.insertar rest"); }
    public void insertar(Connection con, List lista) { throw new UnsupportedOperationException("HistorialProducto.insertar rest"); }
    public List<HistorialProducto> listar() { throw new UnsupportedOperationException("HistorialProducto.listar rest"); }
    public List<HistorialProducto> listarPorFecha(LocalDate fecha) { throw new UnsupportedOperationException("HistorialProducto.listarPorFecha rest"); }
    public boolean existeVentaPorInventarioIds(List inventarioIds) { throw new UnsupportedOperationException("HistorialProducto.existeVentaPorInventarioIds rest"); }
}
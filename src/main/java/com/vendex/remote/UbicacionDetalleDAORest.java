package com.vendex.remote;

import com.vendex.dao.UbicacionDetalleDAO;
import com.vendex.model.UbicacionDetalle;
import java.util.List;
import java.lang.String;

public class UbicacionDetalleDAORest extends RestDao<UbicacionDetalle> implements UbicacionDetalleDAO {
    public UbicacionDetalleDAORest(RestClient rest) { super(rest, "/ubicacion-detalle", UbicacionDetalle.class); }
    public void guardar(UbicacionDetalle u) { throw new UnsupportedOperationException("UbicacionDetalle.guardar rest"); }
    public void generarUbicaciones(int idPerchero, String prefijo, int cantidad) { throw new UnsupportedOperationException("UbicacionDetalle.generarUbicaciones rest"); }
    public void ocupar(int idUbicacion, int idProducto, int cantidad) { throw new UnsupportedOperationException("UbicacionDetalle.ocupar rest"); }
    public void liberar(int id) { throw new UnsupportedOperationException("UbicacionDetalle.liberar rest"); }
    public void liberarPorProducto(int idProducto) { throw new UnsupportedOperationException("UbicacionDetalle.liberarPorProducto rest"); }
    public void eliminarLugar(int id) { throw new UnsupportedOperationException("UbicacionDetalle.eliminarLugar rest"); }
    public List<UbicacionDetalle> listarOcupados() { throw new UnsupportedOperationException("UbicacionDetalle.listarOcupados rest"); }
    public void eliminarPorPerchero(int idPerchero) { throw new UnsupportedOperationException("UbicacionDetalle.eliminarPorPerchero rest"); }
    public List<UbicacionDetalle> listarPorPerchero(int idPerchero) { throw new UnsupportedOperationException("UbicacionDetalle.listarPorPerchero rest"); }
    public List<UbicacionDetalle> listarPorProducto(int idProducto) { throw new UnsupportedOperationException("UbicacionDetalle.listarPorProducto rest"); }
    public List<com.vendex.model.Inventario> listarProductosDisponibles() { throw new UnsupportedOperationException("UbicacionDetalle.listarProductosDisponibles rest"); }
}
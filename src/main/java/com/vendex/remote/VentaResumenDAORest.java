package com.vendex.remote;

import com.vendex.dao.VentaResumenDAO;
import com.vendex.model.DetalleVentaReporte;
import com.vendex.model.VentaResumen;
import java.util.List;
import java.time.LocalDate;
import java.lang.String;

public class VentaResumenDAORest extends RestDao<VentaResumen> implements VentaResumenDAO {
    public VentaResumenDAORest(RestClient rest) { super(rest, "/venta-resumen", VentaResumen.class); }
    public List<VentaResumen> listar() { throw new UnsupportedOperationException("VentaResumen.listar rest"); }
    public List<VentaResumen> listarPorFecha(LocalDate fecha) { throw new UnsupportedOperationException("VentaResumen.listarPorFecha rest"); }
    public List<VentaResumen> listarPorRango(LocalDate desde, LocalDate hasta) { throw new UnsupportedOperationException("VentaResumen.listarPorRango rest"); }
    public List<DetalleVentaReporte> listarDetalle(String tipo, int id) { throw new UnsupportedOperationException("VentaResumen.listarDetalle rest"); }
    public List<DetalleVentaReporte> listarDetallePorRango(LocalDate desde, LocalDate hasta) { throw new UnsupportedOperationException("VentaResumen.listarDetallePorRango rest"); }
}
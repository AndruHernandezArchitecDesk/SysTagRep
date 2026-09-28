package com.vendex.remote;

import com.vendex.dao.FacturaDetalleDAO;
import com.vendex.model.FacturaDetalle;
import java.sql.Connection;
import java.util.List;

public class FacturaDetalleDAORest extends RestDao<FacturaDetalle> implements FacturaDetalleDAO {
    public FacturaDetalleDAORest(RestClient rest) { super(rest, "/facturas-detalle", FacturaDetalle.class); }
    public void insertarDetalle(int facturaRegistroId, List detalles) { throw new UnsupportedOperationException("FacturaDetalle.insertarDetalle rest"); }
    public void insertarDetalle(Connection con, int facturaRegistroId, List detalles) { throw new UnsupportedOperationException("FacturaDetalle.insertarDetalle rest"); }
    public List<FacturaDetalle> listarPorFacturaRegistroId(int facturaRegistroId) { throw new UnsupportedOperationException("FacturaDetalle.listarPorFacturaRegistroId rest"); }
    public boolean existeVentaPorInventarioIds(List inventarioIds) { throw new UnsupportedOperationException("FacturaDetalle.existeVentaPorInventarioIds rest"); }
}
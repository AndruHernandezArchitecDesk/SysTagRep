package com.vendex.remote;

import com.vendex.dao.NotaCreditoDetalleDAO;
import com.vendex.model.NotaCreditoDetalle;
import java.sql.Connection;
import java.util.List;

public class NotaCreditoDetalleDAORest extends RestDao<NotaCreditoDetalle> implements NotaCreditoDetalleDAO {
    public NotaCreditoDetalleDAORest(RestClient rest) { super(rest, "/notas-credito-detalle", NotaCreditoDetalle.class); }
    public void insertarDetalles(int notaCreditoId, List detalles) { throw new UnsupportedOperationException("NotaCreditoDetalle.insertarDetalles rest"); }
    public void insertarDetalles(Connection con, int notaCreditoId, List detalles) { throw new UnsupportedOperationException("NotaCreditoDetalle.insertarDetalles rest"); }
    public List<NotaCreditoDetalle> listarPorNotaCreditoId(int notaCreditoId) { throw new UnsupportedOperationException("NotaCreditoDetalle.listarPorNotaCreditoId rest"); }
}
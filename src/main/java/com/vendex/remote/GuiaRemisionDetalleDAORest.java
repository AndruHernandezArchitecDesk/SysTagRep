package com.vendex.remote;

import com.vendex.dao.GuiaRemisionDetalleDAO;
import com.vendex.model.GuiaRemisionDetalle;
import java.sql.Connection;
import java.util.List;

public class GuiaRemisionDetalleDAORest extends RestDao<GuiaRemisionDetalle> implements GuiaRemisionDetalleDAO {
    public GuiaRemisionDetalleDAORest(RestClient rest) { super(rest, "/guia-remision-detalle", GuiaRemisionDetalle.class); }
    public void insertarDetalles(int destinatarioId, List lista) { throw new UnsupportedOperationException("GuiaRemisionDetalle.insertarDetalles rest"); }
    public void insertarDetalles(Connection con, int destinatarioId, List lista) { throw new UnsupportedOperationException("GuiaRemisionDetalle.insertarDetalles rest"); }
    public List<GuiaRemisionDetalle> listarPorDestinatarioId(int destinatarioId) { throw new UnsupportedOperationException("GuiaRemisionDetalle.listarPorDestinatarioId rest"); }
    public List<GuiaRemisionDetalle> listarPorGuiaId(int guiaId) { throw new UnsupportedOperationException("GuiaRemisionDetalle.listarPorGuiaId rest"); }
}
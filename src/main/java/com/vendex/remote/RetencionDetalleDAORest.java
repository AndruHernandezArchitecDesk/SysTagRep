package com.vendex.remote;

import com.vendex.dao.RetencionDetalleDAO;
import com.vendex.model.RetencionDetalle;
import java.sql.Connection;
import java.util.List;

public class RetencionDetalleDAORest extends RestDao<RetencionDetalle> implements RetencionDetalleDAO {
    public RetencionDetalleDAORest(RestClient rest) { super(rest, "/retencion-detalle", RetencionDetalle.class); }
    public void insertarDetalles(int docSustentoId, List lista) { throw new UnsupportedOperationException("RetencionDetalle.insertarDetalles rest"); }
    public void insertarDetalles(Connection con, int docSustentoId, List lista) { throw new UnsupportedOperationException("RetencionDetalle.insertarDetalles rest"); }
    public List<RetencionDetalle> listarPorDocumentoId(int docId) { throw new UnsupportedOperationException("RetencionDetalle.listarPorDocumentoId rest"); }
}
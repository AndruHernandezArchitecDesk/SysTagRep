package com.vendex.remote;

import com.vendex.dao.RetencionDocumentoSustentoDAO;
import com.vendex.model.RetencionDocumentoSustento;
import java.sql.Connection;
import java.util.List;

public class RetencionDocumentoSustentoDAORest extends RestDao<RetencionDocumentoSustento> implements RetencionDocumentoSustentoDAO {
    public RetencionDocumentoSustentoDAORest(RestClient rest) { super(rest, "/retencion-doc-sustento", RetencionDocumentoSustento.class); }
    public void insertarDocumentos(int retencionId, List lista) { throw new UnsupportedOperationException("RetencionDocumentoSustento.insertarDocumentos rest"); }
    public void insertarDocumentos(Connection con, int retencionId, List lista) { throw new UnsupportedOperationException("RetencionDocumentoSustento.insertarDocumentos rest"); }
    public List<RetencionDocumentoSustento> listarPorRetencionId(int retencionId) { throw new UnsupportedOperationException("RetencionDocumentoSustento.listarPorRetencionId rest"); }
}
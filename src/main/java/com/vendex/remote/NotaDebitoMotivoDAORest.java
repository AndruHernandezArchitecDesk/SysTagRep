package com.vendex.remote;

import com.vendex.dao.NotaDebitoMotivoDAO;
import com.vendex.model.NotaDebitoMotivo;
import java.sql.Connection;
import java.util.List;

public class NotaDebitoMotivoDAORest extends RestDao<NotaDebitoMotivo> implements NotaDebitoMotivoDAO {
    public NotaDebitoMotivoDAORest(RestClient rest) { super(rest, "/notas-debito-motivo", NotaDebitoMotivo.class); }
    public void insertarMotivos(int notaDebitoId, List motivos) { throw new UnsupportedOperationException("NotaDebitoMotivo.insertarMotivos rest"); }
    public void insertarMotivos(Connection con, int notaDebitoId, List motivos) { throw new UnsupportedOperationException("NotaDebitoMotivo.insertarMotivos rest"); }
    public List<NotaDebitoMotivo> listarPorNotaDebitoId(int notaDebitoId) { throw new UnsupportedOperationException("NotaDebitoMotivo.listarPorNotaDebitoId rest"); }
}
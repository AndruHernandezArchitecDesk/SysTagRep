package com.vendex.remote;

import com.vendex.dao.GuiaRemisionDestinatarioDAO;
import com.vendex.model.GuiaRemisionDestinatario;
import java.sql.Connection;
import java.util.List;

public class GuiaRemisionDestinatarioDAORest extends RestDao<GuiaRemisionDestinatario> implements GuiaRemisionDestinatarioDAO {
    public GuiaRemisionDestinatarioDAORest(RestClient rest) { super(rest, "/guia-remision-destinatario", GuiaRemisionDestinatario.class); }
    public void insertarDestinatarios(int guiaId, List lista) { throw new UnsupportedOperationException("GuiaRemisionDestinatario.insertarDestinatarios rest"); }
    public void insertarDestinatarios(Connection con, int guiaId, List lista) { throw new UnsupportedOperationException("GuiaRemisionDestinatario.insertarDestinatarios rest"); }
    public List<GuiaRemisionDestinatario> listarPorGuiaId(int guiaId) { throw new UnsupportedOperationException("GuiaRemisionDestinatario.listarPorGuiaId rest"); }
}
package com.vendex.remote;

import com.vendex.dao.TransferenciaInventarioDAO;
import com.vendex.model.TransferenciaInventario;
import java.sql.Connection;
import java.util.List;

public class TransferenciaInventarioDAORest extends RestDao<TransferenciaInventario> implements TransferenciaInventarioDAO {
    public TransferenciaInventarioDAORest(RestClient rest) { super(rest, "/transferencias", TransferenciaInventario.class); }
    public int guardar(TransferenciaInventario t) { throw new UnsupportedOperationException("TransferenciaInventario.guardar rest"); }
    public int guardar(Connection con, TransferenciaInventario t) { throw new UnsupportedOperationException("TransferenciaInventario.guardar rest"); }
    public List<TransferenciaInventario> listarPorInventario(int inventarioId) { throw new UnsupportedOperationException("TransferenciaInventario.listarPorInventario rest"); }
    public List<TransferenciaInventario> listarPorSucursal(int sucursalId, int limit) { throw new UnsupportedOperationException("TransferenciaInventario.listarPorSucursal rest"); }
    public List<TransferenciaInventario> listarTodas(int limit) { throw new UnsupportedOperationException("TransferenciaInventario.listarTodas rest"); }
}
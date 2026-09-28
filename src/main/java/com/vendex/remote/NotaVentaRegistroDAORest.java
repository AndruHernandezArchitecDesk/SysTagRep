package com.vendex.remote;

import com.vendex.dao.NotaVentaRegistroDAO;
import com.vendex.model.NotaVentaRegistro;
import java.util.List;

public class NotaVentaRegistroDAORest extends RestDao<NotaVentaRegistro> implements NotaVentaRegistroDAO {
    public NotaVentaRegistroDAORest(RestClient rest) { super(rest, "/notas-venta", NotaVentaRegistro.class); }
    public List<NotaVentaRegistro> obtenerNumNotaVenta() { throw new UnsupportedOperationException("NotaVentaRegistro.obtenerNumNotaVenta rest"); }
    public int insertar(NotaVentaRegistro nvr) { throw new UnsupportedOperationException("NotaVentaRegistro.insertar rest"); }
}
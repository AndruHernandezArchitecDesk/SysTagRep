package com.vendex.remote;

import com.vendex.dao.NotaDebitoRegistroDAO;
import com.vendex.model.NotaDebitoRegistro;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.lang.String;

public class NotaDebitoRegistroDAORest extends RestDao<NotaDebitoRegistro> implements NotaDebitoRegistroDAO {
    public NotaDebitoRegistroDAORest(RestClient rest) { super(rest, "/notas-debito", NotaDebitoRegistro.class); }
    public int insertar(NotaDebitoRegistro nd) { throw new UnsupportedOperationException("NotaDebitoRegistro.insertar rest"); }
    public int insertar(Connection con, NotaDebitoRegistro nd) { throw new UnsupportedOperationException("NotaDebitoRegistro.insertar rest"); }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("NotaDebitoRegistro.actualizarEstado rest"); }
    public NotaDebitoRegistro obtenerPorClave(String claveAcceso) { throw new UnsupportedOperationException("NotaDebitoRegistro.obtenerPorClave rest"); }
    public NotaDebitoRegistro obtenerPorId(int id) { throw new UnsupportedOperationException("NotaDebitoRegistro.obtenerPorId rest"); }
    public List<NotaDebitoRegistro> listarPorFactura(int facturaRegistroId) { throw new UnsupportedOperationException("NotaDebitoRegistro.listarPorFactura rest"); }
    public BigDecimal sumarValorTotalPorFactura(int facturaRegistroId, String estadoFiltro) { throw new UnsupportedOperationException("NotaDebitoRegistro.sumarValorTotalPorFactura rest"); }
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("NotaDebitoRegistro.actualizarEstado rest"); }
    public List<NotaDebitoRegistro> listarPendientesSri() { throw new UnsupportedOperationException("NotaDebitoRegistro.listarPendientesSri rest"); }
}
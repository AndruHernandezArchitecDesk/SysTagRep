package com.vendex.remote;

import com.vendex.dao.FacturaRegistroDAO;
import com.vendex.model.FacturaRegistro;
import java.sql.Connection;
import java.util.List;
import java.lang.String;

public class FacturaRegistroDAORest extends RestDao<FacturaRegistro> implements FacturaRegistroDAO {
    public FacturaRegistroDAORest(RestClient rest) { super(rest, "/facturas", FacturaRegistro.class); }
    public List<FacturaRegistro> obtenerNumFactura() { throw new UnsupportedOperationException("FacturaRegistro.obtenerNumFactura rest"); }
    public int insertar(FacturaRegistro fr) { throw new UnsupportedOperationException("FacturaRegistro.insertar rest"); }
    public int insertar(Connection con, FacturaRegistro fr) { throw new UnsupportedOperationException("FacturaRegistro.insertar rest"); }
    public void actualizarEstado(Connection con, String claveAcceso, String estado) { throw new UnsupportedOperationException("FacturaRegistro.actualizarEstado rest"); }
    public FacturaRegistro obtenerPorId(int id) { throw new UnsupportedOperationException("FacturaRegistro.obtenerPorId rest"); }
    public FacturaRegistro obtenerPorClaveAcceso(String claveAcceso) { throw new UnsupportedOperationException("FacturaRegistro.obtenerPorClaveAcceso rest"); }
    public void actualizarEstado(String claveAcceso, String estado) { throw new UnsupportedOperationException("FacturaRegistro.actualizarEstado rest"); }
    public List<FacturaRegistro> listarPendientesSri() { throw new UnsupportedOperationException("FacturaRegistro.listarPendientesSri rest"); }
    public int contar(String filtro) { throw new UnsupportedOperationException("FacturaRegistro.contar rest"); }
    public List<FacturaRegistro> listarPaginado(int page, int pageSize, String filtro) { throw new UnsupportedOperationException("FacturaRegistro.listarPaginado rest"); }
}
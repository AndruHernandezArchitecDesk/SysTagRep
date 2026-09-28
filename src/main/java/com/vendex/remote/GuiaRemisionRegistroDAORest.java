package com.vendex.remote;

import com.vendex.dao.GuiaRemisionRegistroDAO;
import com.vendex.model.GuiaRemisionRegistro;
import java.sql.Connection;
import java.util.List;
import java.lang.String;

public class GuiaRemisionRegistroDAORest extends RestDao<GuiaRemisionRegistro> implements GuiaRemisionRegistroDAO {
    public GuiaRemisionRegistroDAORest(RestClient rest) { super(rest, "/guia-remision", GuiaRemisionRegistro.class); }
    public int insertar(GuiaRemisionRegistro r) { throw new UnsupportedOperationException("GuiaRemisionRegistro.insertar rest"); }
    public int insertar(Connection con, GuiaRemisionRegistro r) { throw new UnsupportedOperationException("GuiaRemisionRegistro.insertar rest"); }
    public GuiaRemisionRegistro obtenerPorClave(String clave) { throw new UnsupportedOperationException("GuiaRemisionRegistro.obtenerPorClave rest"); }
    public GuiaRemisionRegistro obtenerPorId(int id) { throw new UnsupportedOperationException("GuiaRemisionRegistro.obtenerPorId rest"); }
    public List<GuiaRemisionRegistro> listarPendientesSri() { throw new UnsupportedOperationException("GuiaRemisionRegistro.listarPendientesSri rest"); }
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("GuiaRemisionRegistro.actualizarEstado rest"); }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("GuiaRemisionRegistro.actualizarEstado rest"); }
}
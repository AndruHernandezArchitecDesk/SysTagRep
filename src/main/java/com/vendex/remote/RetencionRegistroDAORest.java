package com.vendex.remote;

import com.vendex.dao.RetencionRegistroDAO;
import com.vendex.model.RetencionRegistro;
import java.sql.Connection;
import java.util.List;
import java.lang.String;

public class RetencionRegistroDAORest extends RestDao<RetencionRegistro> implements RetencionRegistroDAO {
    public RetencionRegistroDAORest(RestClient rest) { super(rest, "/retencion", RetencionRegistro.class); }
    public int insertar(RetencionRegistro r) { throw new UnsupportedOperationException("RetencionRegistro.insertar rest"); }
    public int insertar(Connection con, RetencionRegistro r) { throw new UnsupportedOperationException("RetencionRegistro.insertar rest"); }
    public RetencionRegistro obtenerPorClave(String clave) { throw new UnsupportedOperationException("RetencionRegistro.obtenerPorClave rest"); }
    public RetencionRegistro obtenerPorId(int id) { throw new UnsupportedOperationException("RetencionRegistro.obtenerPorId rest"); }
    public List<RetencionRegistro> listarPendientesSri() { throw new UnsupportedOperationException("RetencionRegistro.listarPendientesSri rest"); }
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("RetencionRegistro.actualizarEstado rest"); }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("RetencionRegistro.actualizarEstado rest"); }
}
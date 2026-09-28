package com.vendex.remote;

import com.vendex.dao.NotaCreditoRegistroDAO;
import com.vendex.model.NotaCreditoRegistro;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.lang.String;

public class NotaCreditoRegistroDAORest extends RestDao<NotaCreditoRegistro> implements NotaCreditoRegistroDAO {
    public NotaCreditoRegistroDAORest(RestClient rest) { super(rest, "/notas-credito", NotaCreditoRegistro.class); }
    public int insertar(NotaCreditoRegistro nc) { throw new UnsupportedOperationException("NotaCreditoRegistro.insertar rest"); }
    public int insertar(Connection con, NotaCreditoRegistro nc) { throw new UnsupportedOperationException("NotaCreditoRegistro.insertar rest"); }
    public void actualizarEstado(Connection con, String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("NotaCreditoRegistro.actualizarEstado rest"); }
    public NotaCreditoRegistro obtenerPorClave(String claveAcceso) { throw new UnsupportedOperationException("NotaCreditoRegistro.obtenerPorClave rest"); }
    public NotaCreditoRegistro obtenerPorId(int id) { throw new UnsupportedOperationException("NotaCreditoRegistro.obtenerPorId rest"); }
    public List<NotaCreditoRegistro> listarPorFactura(int facturaRegistroId) { throw new UnsupportedOperationException("NotaCreditoRegistro.listarPorFactura rest"); }
    public BigDecimal sumarValorModificacionPorFactura(int facturaRegistroId, String estadoFiltro) { throw new UnsupportedOperationException("NotaCreditoRegistro.sumarValorModificacionPorFactura rest"); }
    public void actualizarEstado(String claveAcceso, String estado, String mensaje, String numeroAutorizacion, String fechaAutorizacion) { throw new UnsupportedOperationException("NotaCreditoRegistro.actualizarEstado rest"); }
    public void actualizarXmlFirmado(String claveAcceso, String xmlFirmado) { throw new UnsupportedOperationException("NotaCreditoRegistro.actualizarXmlFirmado rest"); }
    public List<NotaCreditoRegistro> listarPendientesSri() { throw new UnsupportedOperationException("NotaCreditoRegistro.listarPendientesSri rest"); }
    public List<NotaCreditoRegistro> listarPaginado(int page, int pageSize, String filtro) { throw new UnsupportedOperationException("NotaCreditoRegistro.listarPaginado rest"); }
    public int contar(String filtro) { throw new UnsupportedOperationException("NotaCreditoRegistro.contar rest"); }
    public List<NotaCreditoRegistro> listarTodas() { throw new UnsupportedOperationException("NotaCreditoRegistro.listarTodas rest"); }
}
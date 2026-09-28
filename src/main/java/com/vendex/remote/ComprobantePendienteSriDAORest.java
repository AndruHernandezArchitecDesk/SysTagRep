package com.vendex.remote;

import com.vendex.dao.ComprobantePendienteSriDAO;
import com.vendex.model.ComprobantePendienteSri;
import java.util.List;
import java.time.LocalDateTime;
import java.lang.String;

public class ComprobantePendienteSriDAORest extends RestDao<ComprobantePendienteSri> implements ComprobantePendienteSriDAO {
    public ComprobantePendienteSriDAORest(RestClient rest) { super(rest, "/comprobantes-pendientes-sri", ComprobantePendienteSri.class); }
    public void encolar(String tipoComprobante, String claveAcceso, String numeroComprobante, String ambiente, String mensaje) { throw new UnsupportedOperationException("ComprobantePendienteSri.encolar rest"); }
    public void encolarFactura(String clave, String numero, String ambiente) { throw new UnsupportedOperationException("ComprobantePendienteSri.encolarFactura rest"); }
    public void encolar(String tipoComprobante, String claveAcceso, String numeroComprobante, String ambiente) { throw new UnsupportedOperationException("ComprobantePendienteSri.encolar rest"); }
    public List<ComprobantePendienteSri> listarParaReintentar(int limite) { throw new UnsupportedOperationException("ComprobantePendienteSri.listarParaReintentar rest"); }
    public List<ComprobantePendienteSri> listarTodos(int limite) { throw new UnsupportedOperationException("ComprobantePendienteSri.listarTodos rest"); }
    public int contarPendientes() { throw new UnsupportedOperationException("ComprobantePendienteSri.contarPendientes rest"); }
    public void marcarResultado(int id, String estado, String mensaje, LocalDateTime proximoIntento, int intentos) { throw new UnsupportedOperationException("ComprobantePendienteSri.marcarResultado rest"); }
    public void marcarReintento(int id, int intentos, LocalDateTime proximo, String mensaje) { throw new UnsupportedOperationException("ComprobantePendienteSri.marcarReintento rest"); }
    public void forzarReintentoAhora(String clave) { throw new UnsupportedOperationException("ComprobantePendienteSri.forzarReintentoAhora rest"); }
}
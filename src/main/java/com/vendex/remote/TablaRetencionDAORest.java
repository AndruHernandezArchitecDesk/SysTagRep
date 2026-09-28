package com.vendex.remote;

import com.vendex.dao.TablaRetencionDAO;
import com.vendex.model.TablaRetencion;
import java.util.List;
import java.lang.String;

public class TablaRetencionDAORest extends RestDao<TablaRetencion> implements TablaRetencionDAO {
    public TablaRetencionDAORest(RestClient rest) { super(rest, "/tabla-retencion", TablaRetencion.class); }
    public List<TablaRetencion> listarVigentes() { throw new UnsupportedOperationException("TablaRetencion.listarVigentes rest"); }
    public List<TablaRetencion> listarTodas() { throw new UnsupportedOperationException("TablaRetencion.listarTodas rest"); }
    public java.math.BigDecimal obtenerPorcentajeVigente(String codigoRetencion) { throw new UnsupportedOperationException("TablaRetencion.obtenerPorcentajeVigente rest"); }
}